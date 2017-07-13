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
}

function preProcessOptions() {
	for cmdlinearg in "$@"
	do
		if [[ $cmdlinearg == --runon=* ]]
		then
			runondistro=$(echo $cmdlinearg | cut -d '=' -f 2)
			echo "Will run on the ${runondistro} image"
			containername=hub-docker-inspector-${runondistro}
			imagename=hub-docker-inspector-${runondistro}
		fi
		if [[ $cmdlinearg == --spring.config.location=* ]]
		then
			propdir=$(echo $cmdlinearg | cut -d '=' -f 2)
			if [[ $propdir == */ ]]
			then
				propdir=$(echo $propdir | rev | cut -c 2- | rev)
			fi
		fi
		if [[ $cmdlinearg == --hub.password=* ]]
		then
			hub_password_set_on_cmd_line=true
		fi
	done
}

function checkForPassword() {
	if [ $hub_password_set_on_cmd_line = true -o -z "${BD_HUB_PASSWORD}" ]
	then
   	    echo Environment variable BD_HUB_PASSWORD is not set or is being overridden on the command line
	else
        echo BD_HUB_PASSWORD is set
	fi
}

function startContainer() {
	if [ $(docker ps |grep "${containername}\$" | wc -l) -gt 0 ]
	then
		echo ${containername} container is already running
	else
		echo ${containername} container is not running
		docker rm ${containername} 2> /dev/null
		echo "Pulling/running hub-docker-inspector Docker image"
		docker run --name ${containername} -it -d --privileged blackducksoftware/${imagename}:1.0.0 /bin/bash
	fi
}

function installPropertiesFile() {
	if [ -f ${propfile} ]
	then
		echo "Found ${propfile}"
		docker cp ${propfile} ${containername}:/opt/blackduck/hub-docker-inspector/config
	else
		echo "File ${propfile} not found."
		echo "Without this file, you will have to set all required properties via the command line."
		docker exec ${containername} rm -f /opt/blackduck/hub-docker-inspector/config/application.properties
	fi
}

##################
# Start script
##################

containername=hub-docker-inspector
imagename=hub-docker-inspector
propdir=.
hub_password_set_on_cmd_line=false

if [ $# -lt 1 ]
then
    printUsage
    exit -1
fi

if [ \( $1 = -v \) -o \( $1 = --version \) ]
then
	echo "$0 1.0.0"
	exit 0
fi

if [ \( $1 = -h \) -o \( $1 = --help \) ]
then
    printUsage
    exit 0
fi

if [ \( $1 = -p \) -o \( $1 = --get-properties \) ]
then
    startContainer
    echo "Copying application.properties template"
    docker cp hub-docker-inspector:/opt/blackduck/hub-docker-inspector/template/application.properties .
    exit 0
fi

preProcessOptions "$@"
propfile=${propdir}/application.properties
echo "Properties file: ${propfile}"

options=( "$@" )
image=${options[${#options[@]}-1]}
unset "options[${#options[@]}-1]"
checkForPassword
startContainer
installPropertiesFile

if [[ "$image" == *.tar ]]
then
	echo Inspecting image tar file: $image
	tarfilename=$(basename $image)
	docker cp $image ${containername}:/opt/blackduck/hub-docker-inspector/target/$tarfilename
	docker exec -e BD_HUB_PASSWORD -e SCAN_CLI_OPTS ${containername} /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} /opt/blackduck/hub-docker-inspector/target/$tarfilename
else
	echo Inspecting image: $image
	docker exec -e BD_HUB_PASSWORD -e SCAN_CLI_OPTS ${containername} /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} $image
fi

exit 0
