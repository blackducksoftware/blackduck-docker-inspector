#!/bin/bash
#
# This script (copied to the Docker container hub-docker will run in)
# makes it easier to invoke hub-docker (especially from outside the container).
#
if [ $# -ne 1 ] && [ $# -ne 2 ]; then
    echo "Usage: $0 <docker image name> [<docker image version>]"
    exit -1
fi

if [ $(service docker status|grep "Docker is running"|wc -l) -gt 0 ]
then
	echo dockerd is already running
else
	service docker start
	sleep 1
fi
service docker status

cd /opt/blackduck/hub-docker
if [ $# -eq 1 ]; then
	java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker/working --docker.image.name=$1
else
	java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker/working --docker.image.name=$1 --docker.tag.name=$2
fi
