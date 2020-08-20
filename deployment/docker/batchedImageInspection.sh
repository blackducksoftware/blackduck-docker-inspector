#!/bin/bash

####################################################################
# This script demonstrates how you can use detect / docker inspector
# to inspect multiple docker images without starting /
# stopping the image inspector services for each one.
# This approach results in significantly faster throughput.
####################################################################

####################################################################
# ==> Adjust the value of localSharedDirPath
# This path cannot have any symbolic links in it
####################################################################
localSharedDirPath=/pathtoanemptydir
imageInspectorVersion=5.0.1
blackDuckUrl=https://yourblackduckserver.com
blackDuckUsername=yourblackduckusername
blackDuckPassword=yourblackduckpassword

####################################################################
# This script will start each imageinspector service (if
# it's not already running), and send 3 requests to inspect
# three images: alpine:latest, centos:latest, ubuntu:latest.
#
# This script will leave the imageinspector services running
# (and will re-use them on subsequent runs)
# For troubleshooting, you might need to do a "docker logs" on
# the imageinspector service container.
####################################################################

mkdir -p ${localSharedDirPath}/target

##############################
# Start the imageinspector services if not already running
##############################
alpineServiceIsUp=false

successMsgCount=$(curl http://localhost:9000/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The alpine service is up"
	alpineServiceIsUp=true
else
	# Start the image inspector service for alpine on port 9000
	docker run -d --user 1001 -p 9000:8081 --label "app=blackduck-imageinspector" --label="os=ALPINE" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-alpine blackducksoftware/blackduck-imageinspector-alpine:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=alpine --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

##############################
centosServiceIsUp=false

successMsgCount=$(curl http://localhost:9001/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The centos service is up"
	centosServiceIsUp=true
else
	# Start the image inspector service for centos on port 9001
	docker run -d --user 1001 -p 9001:8081 --label "app=blackduck-imageinspector" --label="os=UBUNTU" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-centos blackducksoftware/blackduck-imageinspector-centos:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=centos --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

##############################
ubuntuServiceIsUp=false

successMsgCount=$(curl http://localhost:9002/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The ubuntu service is up"
	ubuntuServiceIsUp=true
else
	# Start the image inspector service for ubuntu on port 9002
	docker run -d --user 1001 -p 9002:8081 --label "app=blackduck-imageinspector" --label="os=CENTOS" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-ubuntu blackducksoftware/blackduck-imageinspector-ubuntu:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=ubuntu --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi
#############################
# Wait until imageinspector services are ready
#############################
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
##############################

curl -O https://detect.synopsys.com/detect.sh
chmod +x detect.sh

# Call detect to call docker inspector
./detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN --detect.docker.image=alpine:latest --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=centos --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO 

./detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN --detect.docker.image=centos:latest --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=centos --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO 

./detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN --detect.docker.image=ubuntu:latest --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=centos --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.synopsys.integration=INFO 
