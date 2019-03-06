FROM ubuntu:latest
RUN apt-get update -y && \
                apt install -y openjdk-8-jdk vim curl dnsutils && \
		apt-get install net-tools && \
                apt-get -y clean
RUN mkdir -p /opt/blackduck/detect && \
	mkdir -p /opt/blackduck/blackduck-imageinspector/shared
COPY detect.sh /opt/blackduck/detect/
