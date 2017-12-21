#!/bin/bash -v

targetImageName=alpine
targetImageTag=latest
outputDir=/tmp/aaa
containerName=mytestcontainer

rm -rf "${outputDir}"
#rm -rf "${outputDir}/*.jsonld"

##############################
# Get runOn image
##############################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--determine.run.on.image.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--output.path="${outputDir}" \
	--output.include.dockertarfile=true \
	--docker.image="${targetImageName}:${targetImageTag}"

imageName=$(fgrep runOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
imageTag=$(fgrep runOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
dockerTarfilename=$(fgrep dockerTarfilename "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Running on: ${imageName}:${imageTag}"

##############################
# Inspect
##############################
docker run -it -d --name "${containerName}" "${imageName}:${imageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${containerName}:/opt/blackduck/hub-docker-inspector"
docker cp "${outputDir}/${dockerTarfilename}" "${containerName}:/opt/blackduck/hub-docker-inspector/target"
ls "${outputDir}"
docker exec "${containerName}" java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar --on.host=false "--docker.tar=/opt/blackduck/hub-docker-inspector/target/${dockerTarfilename}"

docker cp "${containerName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

##############################
# Upload BDIO
##############################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--upload.bdio.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=TRACE \
	--output.path="${outputDir}" \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


##############################
# Clean up container
##############################
docker stop "${containerName}"
docker rm "${containerName}"
