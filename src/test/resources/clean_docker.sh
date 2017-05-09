#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi hub-docker-inspector:0.0.1
docker rmi hub-docker-inspector-centos:0.0.1
docker rmi hub-docker-inspector-alpine:0.0.1

docker rmi blackducksoftware/hub-docker-inspector:0.0.1
docker rmi blackducksoftware/hub-docker-inspector-centos:0.0.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.0.1

docker images


