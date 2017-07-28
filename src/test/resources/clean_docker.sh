#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi blackducksoftware/centos_minus_vim_plus_bacula:1.0

docker rmi blackducksoftware/hub-docker-inspector:1.1.1
docker rmi blackducksoftware/hub-docker-inspector-centos:1.1.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.1.1

docker rmi blackducksoftware/hub-docker-inspector:1.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.1.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.1.0
docker rmi blackducksoftware/hub-docker-inspector-centos:1.1.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.1.0

docker rmi blackducksoftware/hub-docker-inspector:1.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.1.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.0.3
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.3
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.3

docker rmi blackducksoftware/hub-docker-inspector:1.0.3-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.3-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.3-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.0.2
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.2
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.2

docker rmi blackducksoftware/hub-docker-inspector:1.0.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.2-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.0.1
docker rmi blackducksoftware/hub-docker-inspector-centos:1.0.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.0.1

docker images


