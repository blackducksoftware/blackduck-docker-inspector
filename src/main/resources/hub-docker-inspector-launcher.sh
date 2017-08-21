#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes sure the docker daemon is running, then invokes the hub-docker-inspector jar.
#
version="@VERSION@"
jarfile="hub-docker-inspector-${version}.jar"
encodingSetting="-Dfile.encoding=UTF-8"

function printUsage() {
	echo ""
    echo "Usage: $0 [options] <image>"
    echo "<image> can be in either of two forms:"
    echo "	<docker image name>[:<docker image version>]"
    echo "	<saved image tarfile; must have .tar extension>"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    echo ""
    echo "Run this command from the directory that contains the application.properties,"
    echo "configured with your Hub connection details (hub.url, hub.username, and hub.password),"
	echo "and Docker Hub connection details (docker.registry.username and docker.registry.password)."
	echo ""
}

# print an error message
err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
}

# Start dockerd
function startDocker() {
	echo starting dockerd...
	cd /opt/blackduck/hub-docker-inspector
	rm -f dockerd_stdout.log
	rm -f dockerd_stderr.log
	dockerd --storage-driver=vfs ${DOCKERD_OPTS} 2> dockerd_stderr.log > dockerd_stdout.log &
	
	for i in 1 .. 5
	do
		echo "Pausing to give dockerd a chance to start"
		sleep 3
		if [ $(docker info 2>&1 |grep "Server Version"|wc -l) -gt 0 ]
		then
			echo dockerd started
			dockerRunning=true
			break
		fi
	done
}

# Stop/remove any old hub-docker-inspector-* containers
# since, via the API, env vars can only be passed 
# when starting a container
function initContainers() {
	echo "Stopping old containers, if they are running"
	docker stop hub-docker-inspector-alpine 2> container_stderr.log > container_stdout.log
	docker stop hub-docker-inspector-centos 2>> container_stderr.log >> container_stdout.log
	echo "Removing old containers, if they are running"
	docker rm hub-docker-inspector-alpine 2>> container_stderr.log >> container_stdout.log
	docker rm hub-docker-inspector-centos 2>> container_stderr.log >> container_stdout.log
	echo "Done removing old containers"
}

# Start dockerd if its not already running
function initDocker() {
	dockerRunning=false
	if [ $(docker info 2>&1 |grep "Server Version"|wc -l) -gt 0 ]
	then
		echo dockerd is already running
		dockerRunning=true
	else		
		startDocker
	fi

	if [ "$dockerRunning" == false ]
	then
		err Unable to start dockerd
		exit -1
	fi

	docker info 2>&1 | grep "Server Version"
	
	initContainers
}

##################
# Start script
##################
if [ \( $# -lt 1 \) ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -h \) -o \( "$1" = --help \) ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -v \) -o \( "$1" = --version \) ]
then
	echo "$(basename $0) ${version}"
	exit 0
fi

# If docker is installed (master container): start docker
if [ $(ls -l /usr/bin/docker 2> /dev/null |wc -l) -gt 0 ]
then
	echo "Running on primary container"
	initDocker
fi

options=( "$@" )
image="${options[${#options[@]}-1]}"
# remove surrounding quotes
image=${image//\"/}
unset "options[${#options[@]}-1]"

cd /opt/blackduck/hub-docker-inspector
rm -rf output/*

if [[ "$image" == *.tar ]]
then
	java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarfile}" "--docker.tar=$image" ${options[*]}
else
	java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarfile}" "--docker.image=$image" ${options[*]}
fi
