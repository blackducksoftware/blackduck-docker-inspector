#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes sure the docker daemon is running, then invokes the hub-docker-inspector jar.
#
version="@VERSION@"
jarfile="hub-docker-inspector-${version}.jar"
encodingSetting="-Dfile.encoding=UTF-8"

function printUsage() {
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
}

# print an error message
err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
}

##################
# Start script
##################
if [ \( $# -lt 1 \) ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -h \) -o \( "$1" = --help \) ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -v \) -o \( "$1" = --version \) ]
then
	echo "$(basename $0) ${version}"
	exit 0
fi

options=( "$@" )
image="${options[${#options[@]}-1]}"
# remove surrounding quotes
image=${image//\"/}
unset "options[${#options[@]}-1]"

cd /opt/blackduck/hub-docker-inspector

# Reason for using exec: http://www.projectatomic.io/docs/docker-image-author-guidance/
if [[ "$image" == *.tar ]]
then
	exec java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarfile}" "--docker.tar=$image" ${options[*]}
else
	exec java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarfile}" "--docker.image=$image" ${options[*]}
fi
