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
localSharedDirPath=/blackduck/shared
imageInspectorVersion=4.3.3
blackDuckUrl=https://blackduck.yourdomain.com
blackDuckUsername=yourblackduckusername
blackDuckPassword=yourblackduckpassword
projectName=TestProject

####################################################################
# This script will start the imageinspector alpine service (if
# it's not already running), and send it 3 requests to inspect
# alpine:latest
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

alpineServiceIsUp=false

successMsgCount=$(curl http://localhost:9000/health | grep "\"status\":\"UP\"" | wc -l)
if [ "$successMsgCount" -eq "1" ]; then
	echo "The service is up"
	alpineServiceIsUp=true
else
	# Start the image inspector service for alpine on port 9000
	docker run -d --user 1001 -p 9000:8081 --label "app=blackduck-imageinspector" --label="os=ALPINE" -v ${localSharedDirPath}:/opt/blackduck/blackduck-imageinspector/shared --name blackduck-imageinspector-alpine blackducksoftware/blackduck-imageinspector-alpine:${imageInspectorVersion} java -jar /opt/blackduck/blackduck-imageinspector/blackduck-imageinspector.jar --server.port=8081 --current.linux.distro=alpine --inspector.url.alpine=http://localhost:9000 --inspector.url.centos=http://localhost:9001 --inspector.url.ubuntu=http://localhost:9002
fi

while [ "$alpineServiceIsUp" == "false" ]; do
	successMsgCount=$(curl http://localhost:9000/health | grep "\"status\":\"UP\"" | wc -l)
	if [ "$successMsgCount" -eq "1" ]; then
		echo "The service is up"
		alpineServiceIsUp=true
		break
	fi
	echo "The service is not up yet"
	sleep 15
done

docker save -o ${localSharedDirPath}/target/alpine1.tar alpine:latest
cp ${localSharedDirPath}/target/alpine1.tar ${localSharedDirPath}/target/alpine2.tar
cp ${localSharedDirPath}/target/alpine1.tar ${localSharedDirPath}/target/alpine3.tar
chmod a+r ${localSharedDirPath}/target/alpine1.tar
chmod a+r ${localSharedDirPath}/target/alpine2.tar
chmod a+r ${localSharedDirPath}/target/alpine3.tar

curl -O https://blackducksoftware.github.io/hub-detect/hub-detect.sh
chmod +x hub-detect.sh

# Call detect to call docker inspector
./hub-detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=${localSharedDirPath}/target/alpine1.tar --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=alpine --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.blackducksoftware.integration=DEBUG --detect.project.name=${projectName} --detect.project.version.name=alpine1 --detect.project.codelocation.prefix=1

./hub-detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=${localSharedDirPath}/target/alpine2.tar --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=alpine --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.blackducksoftware.integration=DEBUG --detect.project.name=${projectName} --detect.project.version.name=alpine2 --detect.project.codelocation.prefix=2

./hub-detect.sh --blackduck.url=${blackDuckUrl} --blackduck.username=${blackDuckUsername} --blackduck.password=${blackDuckPassword} --blackduck.trust.cert=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS --detect.docker.tar=${localSharedDirPath}/target/alpine3.tar --detect.docker.passthrough.imageinspector.service.url=http://localhost:9000 --imageinspector.service.distro.default=alpine --detect.docker.passthrough.shared.dir.path.local=${localSharedDirPath} --detect.docker.passthrough.shared.dir.path.imageinspector=/opt/blackduck/blackduck-imageinspector/shared --detect.docker.passthrough.imageinspector.service.start=false --logging.level.com.blackducksoftware.integration=DEBUG --detect.project.name=${projectName} --detect.project.version.name=alpine3 --detect.project.codelocation.prefix=3
