#!/bin/bash

####################################################################
# This script demonstrates how you can use detect / docker inspector
# running inside a docker container.
####################################################################

####################################################################
# ==> Adjust the value of localSharedDirPath
# This path cannot have any symbolic links in it
####################################################################
localSharedDirPath=~/blackduck/shared
imageInspectorVersion=4.4.0

####################################################################
# This script will start the imageinspector alpine service (if
# it's not already running).
#
# This script will leave the alpine imageinspector service running
# (and will re-use it on subsequent runs)
# For troubleshooting, you might need to do a "docker logs" on
# the imageinspector service container.
#
# To expand this to cover all linux distros, you would need two more "docker run" commands to start two more services (centos and ubuntu)
# on two other ports. All three "docker run" commands will have all 3 port numbers in them, so every service knows how to find
# every other service.
# The alpine service would then be able to redirect requests to those other services when necessary.
####################################################################

mkdir -p ${localSharedDirPath}/target

# Make sure the alpine service is running on host

alpineServiceIsUp=false

successMsgCount=$(curl http://localhost:9000/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The alpine image inspector service is up"
	alpineServiceIsUp=true
else
	# Start the image inspector service for alpine on port 9000
	docker run -d --user 1001 -p 9000:8081 --label "app=blackduck-imageinspector" --label="os=ALPINE" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-alpine blackducksoftware/blackduck-imageinspector-alpine:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=alpine --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

while [ "$alpineServiceIsUp" == "false" ]; do
	successMsgCount=$(curl http://localhost:9000/health | grep "\"status\":\"UP\"" | wc -l)
	if [ "$successMsgCount" -eq "1" ]; then
		echo "The alpine service is up"
		alpineServiceIsUp=true
		break
	fi
	echo "The alpine service is not up yet"
	sleep 15
done

# Make sure the centos service is running on host

centosServiceIsUp=false

successMsgCount=$(curl http://localhost:9001/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The centos image inspector service is up"
	centosServiceIsUp=true
else
	# Start the image inspector service for centos on port 9001
	docker run -d --user 1001 -p 9001:8081 --label "app=blackduck-imageinspector" --label="os=CENTOS" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-centos blackducksoftware/blackduck-imageinspector-centos:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=centos --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

while [ "$centosServiceIsUp" == "false" ]; do
	successMsgCount=$(curl http://localhost:9001/health | grep "\"status\":\"UP\"" | wc -l)
	if [ "$successMsgCount" -eq "1" ]; then
		echo "The centos service is up"
		centosServiceIsUp=true
		break
	fi
	echo "The centos service is not up yet"
	sleep 15
done

# Make sure the ubuntu service is running on host

ubuntuServiceIsUp=false

successMsgCount=$(curl http://localhost:9002/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The ubuntu image inspector service is up"
	ubuntuServiceIsUp=true
else
	# Start the image inspector service for ubuntu on port 9002
	docker run -d --user 1001 -p 9002:8081 --label "app=blackduck-imageinspector" --label="os=UBUNTU" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-ubuntu blackducksoftware/blackduck-imageinspector-ubuntu:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=ubuntu --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

while [ "$ubuntuServiceIsUp" == "false" ]; do
	successMsgCount=$(curl http://localhost:9002/health | grep "\"status\":\"UP\"" | wc -l)
	if [ "$successMsgCount" -eq "1" ]; then
		echo "The ubuntu service is up"
		ubuntuServiceIsUp=true
		break
	fi
	echo "The ubuntu service is not up yet"
	sleep 15
done

cd image
curl -O https://detect.synopsys.com/detect.sh
chmod +x detect.sh
docker build -t detect:1.0 .
cd ..
docker images|grep detect

# Start the detect container w/ the shared dir mounted
docker run --name detect -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared -it -d --network="host" detect:1.0 bash

# For testing: Save some docker images in the shared dir
# (Doing it from host since this detect container does not have docker)
docker save -o ${localSharedDirPath}/target/alpine.tar alpine:latest
chmod a+r ${localSharedDirPath}/target/alpine.tar
docker save -o ${localSharedDirPath}/target/centos.tar centos:latest
chmod a+r ${localSharedDirPath}/target/centos.tar
docker save -o ${localSharedDirPath}/target/ubuntu.tar ubuntu:latest
chmod a+r ${localSharedDirPath}/target/ubuntu.tar

echo "Next steps:"
echo "docker attach detect"
echo "cd /opt/blackduck/detect/"
echo "./detect.sh --blackduck.offline.mode=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=/opt/blackduck/blackduck-imageinspector/shared/target/alpine.tar --detect.docker.path.required=false --detect.docker.passthrough.imageinspector.service.url=http://localhost:9002 --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO --detect.docker.passthrough.shared.dir.path.local=/opt/blackduck/blackduck-imageinspector/shared/"
echo "./detect.sh --blackduck.offline.mode=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=/opt/blackduck/blackduck-imageinspector/shared/target/centos.tar --detect.docker.path.required=false --detect.docker.passthrough.imageinspector.service.url=http://localhost:9002 --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO --detect.docker.passthrough.shared.dir.path.local=/opt/blackduck/blackduck-imageinspector/shared/"
echo "./detect.sh --blackduck.offline.mode=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=/opt/blackduck/blackduck-imageinspector/shared/target/ubuntu.tar --detect.docker.path.required=false --detect.docker.passthrough.imageinspector.service.url=http://localhost:9002 --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO --detect.docker.passthrough.shared.dir.path.local=/opt/blackduck/blackduck-imageinspector/shared/"
