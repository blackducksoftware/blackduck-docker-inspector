#!/bin/bash
#
# This script (copied to the Docker container hub-inspector will run in)
# makes it easier to invoke hub-inspector (especially from outside the container).
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

cd /opt/blackduck/hub-inspector
cmd="java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-inspector/working --docker.tar=$imageFile $*"
echo "executing: $cmd"
$cmd
