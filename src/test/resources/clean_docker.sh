#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi blackducksoftware/centos_minus_vim_plus_bacula:1.0

docker rmi blackducksoftware/hub-docker-inspector:1.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.0.1
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.1

docker rmi blackducksoftware/hub-docker-inspector:1.0.0
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.0

docker rmi blackducksoftware/hub-docker-inspector:1.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:0.1.5
docker rmi blackducksoftware/hub-docker-inspector-centos:0.1.5
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.1.5

docker rmi blackducksoftware/hub-docker-inspector:0.1.5-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:0.1.5-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.1.5-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:0.1.4
docker rmi blackducksoftware/hub-docker-inspector-centos:0.1.4
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.1.4

docker rmi blackducksoftware/hub-docker-inspector:0.1.4-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:0.1.4-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:0.1.4-SNAPSHOT

docker images


