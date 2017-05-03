#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi hub-inspector:0.0.1
docker rmi hub-inspector-centos:0.0.1
docker rmi hub-inspector-alpine:0.0.1

docker images


