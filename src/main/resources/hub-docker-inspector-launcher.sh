#!/bin/bash
#
# This script (copied to the Docker container hub-docker-inspector will run in)
# makes sure the docker daemon is running, then invokes the hub-docker-inspector jar.
#
if [ \( $# -lt 1 \) -o \( $1 = -h \) -o \( $1 = --help \) ]
then
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
    exit -1
fi

# If docker is installed (master container): start docker
if [ $(ls -l /usr/bin/docker 2> /dev/null |wc -l) -gt 0 ]
then
	dockerRunning=false
	if [ $(docker info 2>&1 |grep "Server Version"|wc -l) -gt 0 ]
	then
		echo dockerd is already running
		dockerRunning=true
	else
		echo starting dockerd...
		cd /opt/blackduck/hub-docker-inspector
		rm -f dockerd_stdout.log
		rm -f dockerd_stderr.log
		dockerd --storage-driver=vfs 2> dockerd_stderr.log > dockerd_stdout.log &
	
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
	fi

	if [ $dockerRunning == false ]
	then
		echo Unable to start dockerd
		exit -1
	fi

	docker info 2>&1 | grep "Server Version"
fi

options=( "$@" )
image=${options[${#options[@]}-1]}
unset "options[${#options[@]}-1]"

cd /opt/blackduck/hub-docker-inspector

if [[ "$image" == *.tar ]]
then
	cmd="java -Dfile.encoding=UTF-8 -jar hub-docker-@VERSION@.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.tar=$image ${options[*]}"
else
	cmd="java -Dfile.encoding=UTF-8 -jar hub-docker-@VERSION@.jar --working.directory=/opt/blackduck/hub-docker-inspector/working --docker.image=$image ${options[*]}"
fi

echo "executing: $cmd"
$cmd
