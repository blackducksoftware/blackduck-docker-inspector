#!/bin/bash
#
# This script (copied to the Docker container hub-inspector will run in)
# makes it easier to invoke hub-inspector (especially from outside the container).
#
#
if [ $# -lt 1 ]
then
    echo "Usage: $0 <docker image name>[:<docker image version>] [options]"
    echo "options:"
    echo "  --linux.distro=YourSpecificDistro"
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

image=$1
shift

cd /opt/blackduck/hub-inspector
cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-inspector/working --docker.image=$image $*"


echo "executing: $cmd"
$cmd
