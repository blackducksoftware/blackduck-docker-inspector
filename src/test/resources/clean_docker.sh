#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi blackducksoftware/centos_minus_vim_plus_bacula:1.0
docker rmi blackducksoftware/whiteouttest:1.0

docker rmi blackducksoftware/hub-docker-inspector:3.0.0
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0

docker rmi blackducksoftware/hub-docker-inspector:3.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:2.1.2
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.2
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.2

docker rmi blackducksoftware/hub-docker-inspector:2.1.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.2-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:2.1.1
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.1

docker rmi blackducksoftware/hub-docker-inspector:2.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:2.1.0
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.0

docker rmi blackducksoftware/hub-docker-inspector:2.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:2.0.0
docker rmi blackducksoftware/hub-docker-inspector-centos:2.0.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.0.0

docker rmi blackducksoftware/hub-docker-inspector:2.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:2.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.0.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.3.0
docker rmi blackducksoftware/hub-docker-inspector-centos:1.3.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.3.0

docker rmi blackducksoftware/hub-docker-inspector:1.3.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.3.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.3.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.2.2
docker rmi blackducksoftware/hub-docker-inspector-centos:1.2.2
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.2.2

docker rmi blackducksoftware/hub-docker-inspector:1.2.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.2.2-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.2.2-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:1.2.1
docker rmi blackducksoftware/hub-docker-inspector-centos:1.2.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.2.1

docker rmi blackducksoftware/hub-docker-inspector:1.2.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:1.2.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:1.2.1-SNAPSHOT

docker images


