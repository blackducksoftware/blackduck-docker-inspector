#!/bin/bash

if [[ $# -ne 1 ]]; then
	echo "Usage: $0 <version>"
	exit -1
fi

echo "Moving the latest tag to version $1 images"
docker pull blackducksoftware/hub-docker-inspector-ubuntu:$1
docker pull blackducksoftware/hub-docker-inspector-centos:$1
docker pull blackducksoftware/hub-docker-inspector-alpine:$1

docker tag blackducksoftware/hub-docker-inspector-ubuntu:$1 blackducksoftware/hub-docker-inspector-ubuntu:latest
docker tag blackducksoftware/hub-docker-inspector-centos:$1 blackducksoftware/hub-docker-inspector-centos:latest
docker tag blackducksoftware/hub-docker-inspector-alpine:$1 blackducksoftware/hub-docker-inspector-alpine:latest

docker push blackducksoftware/hub-docker-inspector-ubuntu:latest
docker push blackducksoftware/hub-docker-inspector-centos:latest
docker push blackducksoftware/hub-docker-inspector-alpine:latest
