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
	echo "To get an application.properties template file:"
	echo "  $0 --get-properties"
	echo ""
	echo "For greater security, the Hub password can be set via the environment variable BD_HUB_PASSWORD"
	echo ""
	echo "For example:"
	echo "  export BD_HUB_PASSWORD=mypassword"
	echo "  $0 --hub.url=http://hub.mydomain.com:8080/ --hub.username=myusername ubuntu"
	echo ""
	echo "Documentation: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/48435867/Hub+Docker+Inspector"
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
		if [[ $cmdlinearg == --bdio.output.path=* ]]
		then
			bdioOutputPath=$(echo $cmdlinearg | cut -d '=' -f 2)
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
	docker rm ${containername} 2> /dev/null
	echo "Pulling/running hub-docker-inspector Docker image"
	docker run --name ${containername} -it -d --privileged blackducksoftware/${imagename}:${version} /bin/bash
}

function ensureContainerRunning() {
	if [ $(docker ps |grep "${containername}\$" | wc -l) -gt 0 ]
	then
		echo "The ${containername} container is already running"
		containerVersion=$(docker exec hub-docker-inspector ls /opt/blackduck/hub-docker-inspector | grep \.jar | sed s/hub-docker-inspector-// | sed 's/\.jar//')
		echo "The ${containername} container is already running, and running version ${containerVersion}"
		if [ ${version} != ${containerVersion} ]
		then
			echo "Stopping old container"
			docker stop hub-docker-inspector
			startContainer
		fi
		
	else
		echo ${containername} container is not running
		startContainer
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
version=@VERSION@
bdioOutputPath=/tmp/myoutput/output_bdio.json
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
	echo "$(basename $0) ${version}"
	exit 0
fi

if [ \( $1 = -h \) -o \( $1 = --help \) ]
then
    printUsage
    exit 0
fi

if [ \( $1 = -p \) -o \( $1 = --get-properties \) ]
then
    ensureContainerRunning
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
ensureContainerRunning
installPropertiesFile

if [[ "$image" == *.tar ]]
then
	echo Inspecting image tar file: $image
	if [ ! -r ${image} ]
	then
		echo "ERROR: Tar file ${image} does not exist"
		exit -1
	fi
	tarfilename=$(basename $image)
	docker exec ${containername} rm -f /opt/blackduck/hub-docker-inspector/target/$tarfilename
	docker cp $image ${containername}:/opt/blackduck/hub-docker-inspector/target/$tarfilename
	docker exec -e BD_HUB_PASSWORD -e SCAN_CLI_OPTS -e http_proxy -e https_proxy -e HTTP_PROXY -e HTTPS_PROXY -e DOCKERD_OPTS ${containername} /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} /opt/blackduck/hub-docker-inspector/target/$tarfilename
else
	echo Inspecting image: $image
	docker exec -e BD_HUB_PASSWORD -e SCAN_CLI_OPTS -e http_proxy -e https_proxy -e HTTP_PROXY -e HTTPS_PROXY -e DOCKERD_OPTS ${containername} /opt/blackduck/hub-docker-inspector/hub-docker-inspector-launcher.sh ${options[*]} $image
fi

if [ ! -z ${bdioOutputPath} ]
then
	######## TODO verify it's an existing dir ###########
	docker cp ${containername}:/opt/blackduck/hub-docker-inspector/output/. ${bdioOutputPath}
fi

exit 0
