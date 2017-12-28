#!/bin/bash

targetImageName=alpine
targetImageTag=latest

targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
identifyOnContainerName=hub-docker-inspector-identify
inspectOnContainerName=hub-docker-inspector-inspect
identifyOnImageName=blackducksoftware/hub-docker-inspector-alpine
identifyOnImageTag=4.1.0

rm -rf "${outputDir}"
mkdir "${outputDir}"
rm -f "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Pull/save image (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Pulling/saving the target image"
echo "--------------------------------------------------------------"
docker pull "${targetImageName}:${targetImageTag}"
docker save -o "${targetImageDir}/${targetImageTarfile}" "${targetImageName}:${targetImageTag}"
chmod a+r "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Get "target image pkg mgr identification" image / start and setup "target image pkg mgr identification" container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Starting container for target image package manager identification"
echo "--------------------------------------------------------------"
docker run -it -d --name "${identifyOnContainerName}" "${identifyOnImageName}:${identifyOnImageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${identifyOnContainerName}:/opt/blackduck/hub-docker-inspector"
docker cp "${targetImageDir}/${targetImageTarfile}" "${identifyOnContainerName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Determine inspectOn image (jar on identifyOnContainer)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Identifying target image package manager"
echo "--------------------------------------------------------------"
docker exec "${identifyOnContainerName}" \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=true \
	--inspect=false \
	--inspect.in.container=false \
	--logging.level.com.blackducksoftware=INFO \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

docker cp "${identifyOnContainerName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"

ls "${outputDir}"

inspectOnImageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Docker image selected for target image inspection: ${inspectOnImageName}:${inspectOnImageTag}"
rm -rf "${outputDir}"
mkdir "${outputDir}"

#################################################################
# Get inspectOn image / start and setup inspectOn container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Starting container for target image inspection"
echo "--------------------------------------------------------------"
docker run -it -d --name "${inspectOnContainerName}" "${inspectOnImageName}:${inspectOnImageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector"
docker cp "${targetImageDir}/${targetImageTarfile}" "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Inspect (jar in container)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Target image inspection"
echo "--------------------------------------------------------------"
docker exec "${inspectOnContainerName}" java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--inspect=true \
	--upload.bdio=false \
	--inspect.in.container=false \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

docker cp "${inspectOnContainerName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

#################################################################
# Upload BDIO (jar on host)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Uploading BDIO file (BOM) to Hub"
echo "--------------------------------------------------------------"
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--identify.pkg.mgr=false \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--bdio.path="${outputDir}" \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


#################################################################
# Clean up container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "docker_wrapper.sh: Stopping/removing containers"
echo "--------------------------------------------------------------"
docker stop "${identifyOnContainerName}"
docker rm "${identifyOnContainerName}"
docker stop "${inspectOnContainerName}"
docker rm "${inspectOnContainerName}"
