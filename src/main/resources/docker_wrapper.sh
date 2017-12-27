#!/bin/bash -v

targetImageName=alpine
targetImageTag=latest

targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
peekOnContainerName=hub-docker-inspector-peek
inspectOnContainerName=hub-docker-inspector-inspect
peekOnImageName=blackducksoftware/hub-docker-inspector-alpine
peekOnImageTag=4.1.0

rm -rf "${outputDir}"
mkdir "${outputDir}"
rm -f "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Pull/save image (wrapper)
#################################################################
docker pull "${targetImageName}:${targetImageTag}"
docker save -o "${targetImageDir}/${targetImageTarfile}" "${targetImageName}:${targetImageTag}"
chmod a+r "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Determine inspectOn image (jar on host)
#################################################################
#./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
#	--determine.run.on.image.only=true \
#	--cleanup.working.dir=false \
#	--logging.level.com.blackducksoftware=INFO \
#	--output.path="${outputDir}" \
#	--output.include.dockertarfile=true \
#	--docker.tar=/tmp/savedimage.tar
#
#imageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
#imageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
#dockerTarfilename=$(fgrep dockerTarfilename "${outputDir}/result.json" | cut -d'"' -f4)
#bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)
#
#echo "Running on: ${imageName}:${imageTag}"

#################################################################
# Get peekOn image / start and setup peekOn container (wrapper)
#################################################################
docker run -it -d --name "${peekOnContainerName}" "${peekOnImageName}:${peekOnImageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${peekOnContainerName}:/opt/blackduck/hub-docker-inspector"
docker cp "${targetImageDir}/${targetImageTarfile}" "${peekOnContainerName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Determine inspectOn image (jar on peekOnContainer)
#################################################################
docker exec "${peekOnContainerName}" \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--determine.run.on.image.only=true \
	--logging.level.com.blackducksoftware=INFO \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

docker cp "${peekOnContainerName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"

ls "${outputDir}"

inspectOnImageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Running on: ${inspectOnImageName}:${inspectOnImageTag}"

# ====== end of new stuff






#################################################################
# Get inspectOn image / start and setup inspectOn container (wrapper)
#################################################################
docker run -it -d --name "${inspectOnContainerName}" "${inspectOnImageName}:${inspectOnImageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector"
docker cp "${targetImageDir}/${targetImageTarfile}" "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector/target"
ls "${outputDir}"

#################################################################
# Inspect (jar in container)
#################################################################
echo docker exec "${inspectOnContainerName}" java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar --on.host=false "--docker.tar=/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"
docker exec "${inspectOnContainerName}" java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar --on.host=false "--docker.tar=/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

docker cp "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

#################################################################
# Upload BDIO (jar on host)
#################################################################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--upload.bdio.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--output.path="${outputDir}" \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


#################################################################
# Clean up container (wrapper)
#################################################################
docker stop "${peekOnContainerName}"
docker rm "${peekOnContainerName}"
docker stop "${inspectOnContainerName}"
docker rm "${inspectOnContainerName}"
