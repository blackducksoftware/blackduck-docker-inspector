#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes it easier to invoke hub-docker-inspector (especially from outside the container).
#
#
if [ $# -lt 1 ]
then
    echo "Usage: $0 [options] <docker image name>[:<docker image version>]"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    exit -1
fi

if [ $(docker info 2>&1 |grep "Server Version"|wc -l) -gt 0 ]
then
	echo dockerd is already running
else
	echo starting dockerd...
	dockerd --storage-driver=vfs 2> /dev/null > /dev/null &
	sleep 3
fi
docker info 2>&1 | grep "Server Version"

options=( "$@" )
image=${options[${#options[@]}-1]}
unset "options[${#options[@]}-1]"

cd /opt/blackduck/hub-docker-inspector
cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.image=$image ${options[*]}"

echo "executing: $cmd"
$cmd
