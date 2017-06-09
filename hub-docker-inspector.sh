#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with your Hub connection details (hub.url, hub.username, and hub.password),
# and Docker Hub connection details (docker.registry.username and docker.registry.password).
#
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
    exit -1
}

if [ $# -lt 1 ]
then
    printUsage
fi

if [ \( $1 = -v \) -o \( $1 = --version \) ]
then
	echo "$0 0.0.4-SNAPSHOT"
	exit -1
fi

if [ \( $1 = -h \) -o \( $1 = --help \) ]
then
    printUsage
fi

options=( "$@" )
image=${options[${#options[@]}-1]}
unset "options[${#options[@]}-1]"

# Collect proxy env vars
if [ -z ${SCAN_CLI_OPTS+x} ]
then
	echo SCAN_CLI_OPTS is not set
else
	echo SCAN_CLI_OPTS is set
	for cli_opt in $SCAN_CLI_OPTS 
	do
		if [[ $cli_opt == -Dhttp.proxyHost=* ]]
		then
			options=( ${options[*]} "--hub.proxy.host=$(echo $cli_opt | cut -d '=' -f 2)" )
		fi
		if [[ $cli_opt == -Dhttp.proxyPort=* ]]
		then
			options=( ${options[*]} "--hub.proxy.port=$(echo $cli_opt | cut -d '=' -f 2)" )
		fi
		if [[ $cli_opt == -Dhttp.proxyUser=* ]]
		then
			options=( ${options[*]} "--hub.proxy.username=$(echo $cli_opt | cut -d '=' -f 2)" )
		fi
		if [[ $cli_opt == -Dhttp.proxyPassword=* ]]
		then
			options=( ${options[*]} "--hub.proxy.password=$(echo $cli_opt | cut -d '=' -f 2)" )
		fi
	done
fi

if [ -z ${BD_HUB_PASSWORD+x} ]
then
        echo Environment variable BD_HUB_PASSWORD is not set
else
        echo BD_HUB_PASSWORD is set
        options=( ${options[*]} --hub.password=$BD_HUB_PASSWORD )
fi

if [ $(docker ps |grep "hub-docker-inspector" | wc -l) -gt 0 ]
then
	echo hub-docker-inspector container is already running
else
	echo hub-docker-inspector container is not running
	docker rm hub-docker-inspector 2> /dev/null
	echo "Pulling/running hub-docker-inspector Docker image"
	docker run --name hub-docker-inspector -it -d --privileged blackducksoftware/hub-docker-inspector:0.0.4-SNAPSHOT /bin/bash
fi

if [ -f application.properties ]
then
	echo "Found application.properties"
	docker cp application.properties hub-docker-inspector:/opt/blackduck/hub-docker-inspector/config
else
	echo "application.properties file not found in current directory."
	echo "Without this file, you will have to set all required properties via the command line."
	docker exec hub-docker-inspector rm -f /opt/blackduck/hub-docker-inspector/config/application.properties
fi


if [[ "$image" == *.tar ]]
then
	echo Inspecting image tar file: $image
	tarfilename=$(basename $image)
	docker cp $image hub-docker-inspector:/opt/blackduck/hub-docker-inspector/target/$tarfilename
	docker exec hub-docker-inspector /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} /opt/blackduck/hub-docker-inspector/target/$tarfilename
else
	echo Inspecting image: $image
	docker exec hub-docker-inspector /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} $image
fi

exit 0
