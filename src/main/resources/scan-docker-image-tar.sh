#!/bin/bash
#
# This script (copied to the Docker container hub-docker will run in)
# makes it easier to invoke hub-docker (especially from outside the container).
#
if [ $# -eq 0 ]
  then
    echo "Usage: $0 <path to Docker image tar file>"
    exit -1
fi
cd /opt/blackduck/hub-docker
java -jar hub-docker-*.jar --working.directory=/opt/blackduck/hub-docker/working --docker.tar=$1