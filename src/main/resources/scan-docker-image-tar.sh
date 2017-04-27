#!/bin/bash
#
# This script (copied to the Docker container hub-docker will run in)
# makes it easier to invoke hub-docker (especially from outside the container).
#
#
if [ $# -lt 1 ]
  then
    echo "Usage: $0 <path to Docker image tar file> [options]"
    echo "options:"
    echo "  --linux.distro=YourSpecificDistro"
    exit -1
fi

imageFile=$1
shift

cd /opt/blackduck/hub-docker
cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker/working --docker.tar=$imageFile $*"
echo "executing: $cmd"
$cmd
