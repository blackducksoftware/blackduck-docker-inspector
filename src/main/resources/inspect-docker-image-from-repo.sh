#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes it easier to invoke hub-docker-inspector (especially from outside the container).
#
#
if [ $# -lt 1 ]
then
    echo "Usage: $0 <docker image name>[:<docker image version>] [options]"
    echo "options:"
    echo "  --linux.distro=YourSpecificDistro"
    exit -1
fi

if [ $(docker info 2>&1 |grep "Server Version"|wc -l) -gt 0 ]
then
	echo dockerd is already running
else
	dockerd --storage-driver=vfs 2>&1 > /dev/null &
	sleep 3
fi
docker info 2>&1 | grep "Server Version"

image=$1
shift

cd /opt/blackduck/hub-docker-inspector
cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.image=$image $*"


echo "executing: $cmd"
$cmd
