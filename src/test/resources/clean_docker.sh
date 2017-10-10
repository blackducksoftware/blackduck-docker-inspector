#!/bin/bash

docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

docker rmi blackducksoftware/centos_minus_vim_plus_bacula:1.0
docker rmi blackducksoftware/whiteouttest:1.0

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.1.1
docker rmi blackducksoftware/hub-docker-inspector-centos:3.1.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.1.1

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:3.1.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.1.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.1.0
docker rmi blackducksoftware/hub-docker-inspector-centos:3.1.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.1.0

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:3.1.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.1.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.1
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.1
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.1

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.1-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.1-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.0
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.0-RC3
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0-RC3
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0-RC3

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.0-RC2
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0-RC2
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0-RC2

docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.0-RC1
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0-RC1
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0-RC1

docker rmi blackducksoftware/hub-docker-inspector:3.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-ubuntu:3.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-centos:3.0.0-SNAPSHOT
docker rmi blackducksoftware/hub-docker-inspector-alpine:3.0.0-SNAPSHOT

docker rmi blackducksoftware/hub-docker-inspector:2.1.2
docker rmi blackducksoftware/hub-docker-inspector-centos:2.1.2
docker rmi blackducksoftware/hub-docker-inspector-alpine:2.1.2

docker images
