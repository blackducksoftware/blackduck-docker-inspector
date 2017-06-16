#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi blackducksoftware/hub-docker-inspector:0.0.4-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:0.0.4-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.0.4-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:0.0.4
docker rmi blackducksoftware/hub-docker-inspector-centos:0.0.4
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.0.4

docker images


