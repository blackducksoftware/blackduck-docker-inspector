#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#

if [ $# -lt 1 ]
then
    echo "Usage: $0 [options] <image>"
    echo "<image> can be of either of two forms:"
    echo "	<docker image name>[:<docker image version>]"
    echo "	<saved image tarfile; must have .tar extension>"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    exit -1
fi

options=( "$@" )
image=${options[${#options[@]}-1]}
unset "options[${#options[@]}-1]"

if [ $(docker ps |grep "hub-docker-inspector" | wc -l) -gt 0 ]
then
	echo hub-docker-inspector container is already running
else
	echo hub-docker-inspector container is not running
	docker rm hub-docker-inspector 2> /dev/null
	docker run --name hub-docker-inspector -it -d --privileged blackducksoftware/hub-docker-inspector:0.0.1 /bin/bash 2> /dev/null
fi

docker cp application.properties hub-docker-inspector:/opt/blackduck/hub-docker-inspector/config

if [[ "$image" == *.tar ]]
then
	echo Inspecting image tar file: $image
	tarfilename=$(basename $image)
	docker cp $image hub-docker-inspector:/opt/blackduck/hub-docker-inspector/target/$tarfilename
	docker exec hub-docker-inspector /opt/blackduck/hub-docker-inspector/inspect-docker-image-tar.sh ${options[*]} /opt/blackduck/hub-docker-inspector/target/$tarfilename
else
	echo Inspecting image: $image
	docker exec hub-docker-inspector /opt/blackduck/hub-docker-inspector/inspect-docker-image-from-repo.sh ${options[*]} $image
fi

