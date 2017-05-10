#!/bin/bash

if [ $# -lt 1 ]
then
    echo "Usage: $0 [options] <docker image name>[:<docker image version>]"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    exit -1
fi

if [ $(docker ps |grep "hub-docker-inspector" | wc -l) -gt 0 ]
then
	echo hub-docker-inspector container is already running
else
	echo hub-docker-inspector container is not running
	docker rm hub-docker-inspector 2> /dev/null
	docker run --name hub-docker-inspector -it -d --privileged blackducksoftware/hub-docker-inspector:0.0.1 /bin/bash 2> /dev/null
fi

docker cp application.properties hub-docker-inspector:/opt/blackduck/hub-docker-inspector/config
docker exec hub-docker-inspector /opt/blackduck/hub-docker-inspector/inspect-docker-image-from-repo.sh $*
