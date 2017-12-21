#!/bin/bash -v

rm -rf /tmp/aaa
#rm -rf /tmp/aaa/*.jsonld

##############################
# Get runOn image
##############################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--determine.run.on.image.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--output.path=/tmp/aaa \
	--output.include.dockertarfile=true \
	alpine:latest

imageName=$(fgrep runOnImageName /tmp/aaa/result.json | cut -d'"' -f4)
imageTag=$(fgrep runOnImageTag /tmp/aaa/result.json | cut -d'"' -f4)

echo "Running on: ${imageName}:${imageTag}"

##############################
# Inspect
##############################
#docker run -it -d --name mytestcontainer "${imageName}:${imageTag}" /bin/bash
docker cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar mytestcontainer:/opt/blackduck/hub-docker-inspector
docker cp /tmp/aaa/alpine_latest.tar mytestcontainer:/opt/blackduck/hub-docker-inspector/target
ls /tmp/aaa
docker exec mytestcontainer java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar --on.host=false --docker.tar=/opt/blackduck/hub-docker-inspector/target/alpine_latest.tar

docker cp mytestcontainer:/opt/blackduck/hub-docker-inspector/output/alpine_lib_apk_alpine_latest_bdio.jsonld /tmp/aaa

##############################
# Upload BDIO
##############################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--upload.bdio.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=TRACE \
	--output.path=/tmp/aaa \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck
