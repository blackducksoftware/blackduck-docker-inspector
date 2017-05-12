#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes sure the docker daemon is running, then invokes the hub-docker-inspector jar.
#
if [ \( $# -lt 1 \) -o \( $1 = -h \) -o \( $1 = --help \) ]
then
    echo ""
    echo "Usage: $0 [options] <image>"
    echo "<image> can be in either of two forms:"
    echo "	<docker image name>[:<docker image version>]"
    echo "	<saved image tarfile; must have .tar extension>"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    echo ""
    echo "Run this command from the directory that contains the application.properties,"
    echo "configured with your Hub connection details (hub.url, hub.username, and hub.password),"
	echo "and Docker Hub connection details (docker.registry.username and docker.registry.password)."
	echo ""
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

if [[ "$image" == *.tar ]]
then
	cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.tar=$image ${options[*]}"
else
	cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.image=$image ${options[*]}"
fi

echo "executing: $cmd"
$cmd
