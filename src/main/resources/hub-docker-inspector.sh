#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with your Hub connection details (hub.url, hub.username, and hub.password),
# and Docker Hub connection details (docker.registry.username and docker.registry.password).

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

err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
}

# Look through args for ones this script needs to act on
function preProcessOptions() {
	for cmdlinearg in "$@"
	do
#		if [[ "$cmdlinearg" == --runon=* ]]
#		then
#			runondistro=$(echo "$cmdlinearg" | cut -d '=' -f 2)
#		fi
		if [[ "$cmdlinearg" == --spring.config.location=* ]]
		then
			propdir=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			if [[ "$propdir" == */ ]]
			then
				propdir=$(echo "$propdir" | rev | cut -c 2- | rev)
			fi
		fi
		if [[ "$cmdlinearg" == --hub.password=* ]]
		then
			hub_password_set_on_cmd_line=true
		fi
		if [[ "$cmdlinearg" == --output.path=* ]]
		then
			outputPath=$(echo "$cmdlinearg" | cut -d '=' -f 2)
		fi
		if [[ "$cmdlinearg" == --working.dir.path=* ]]
		then
			workingDir=$(echo "$cmdlinearg" | cut -d '=' -f 2)
		fi
		if [[ "$cmdlinearg" == --no.prompt=true ]]
		then
			noPromptMode=true
			echo "Running in \"no prompt\" mode"
		fi
		if [[ "$cmdlinearg" == --dry.run=true ]]
		then
			dryRunMode=true
			echo "Running in \"dry run\" mode"
		fi
	done
}

# Prompt user for Hub Password
function promptForHubPassword() {
	read -s -p "Hub Password has not been set. Please enter Hub password: " hubPassword
	echo ""
	export BD_HUB_PASSWORD="${hubPassword}"
}

# Inform user on whether password is set via env var
function checkForPassword() {
	if [ $hub_password_set_on_cmd_line == true -o -z "${BD_HUB_PASSWORD}" ]
	then
   	    echo Environment variable BD_HUB_PASSWORD is not set or is being overridden on the command line
	else
        echo BD_HUB_PASSWORD is set
	fi
	if [ $hub_password_set_on_cmd_line == false -a -z "${BD_HUB_PASSWORD}" -a $dryRunMode == false ]
	then
   	    
		if [ $noPromptMode == false ]
		then
			promptForHubPassword
		else
			err "The Hub password has not been provided, and \"no prompt\" mode is enabled"
			exit -1
		fi
	fi
}

# Start the hub-docker-inspector container
function startContainer() {
	docker rm "${containername}" 2> /dev/null
	echo "Pulling/running hub-docker-inspector Docker image"
	docker run --name "${containername}" -it -d --privileged "blackducksoftware/${imagename}:${version}" bash
}

# Check to be sure the hub-docker-container is running
function ensureContainerRunning() {
	if [ $(docker ps |grep "${containername}\$" | wc -l) -gt 0 ]
	then
		echo "The ${containername} container is already running"
		containerVersion=$(docker exec hub-docker-inspector ls /opt/blackduck/hub-docker-inspector | grep \.jar | sed s/hub-docker-inspector-// | sed 's/\.jar//')
		echo "The ${containername} container is running version ${containerVersion}"
		if [ "${version}" != "${containerVersion}" ]
		then
			echo "Stopping old container"
			docker stop hub-docker-inspector
			startContainer
		fi
		
	else
		echo "${containername} container is not running"
		startContainer
	fi
}

# Copy application.properties into the container
function installPropertiesFile() {
	if [ -f "${propfile}" ]
	then
		echo "Found ${propfile}"
		docker cp "${propfile}" "${containername}:/opt/blackduck/hub-docker-inspector/config"
	else
		echo "File ${propfile} not found."
		echo "Without this file, you will have to set all required properties via the command line."
		docker exec "${containername}" rm -f /opt/blackduck/hub-docker-inspector/config/application.properties
	fi
}

# Get a property value from the given properties file
# Usage: get_property FILE KEY
function get_property {
	grep "^$2=" "$1" 2> /dev/null | cut -d'=' -f2
}

# determine which image/container to run on/in
function determineRunOnImage {
	if [ -z "${runondistro}" ]
	then
		echo "Looking in ${propfile} for runon"
		runondistro=$(get_property "${propfile}" "runon")
	fi
	if [ -z "${runondistro}" ]
	then
		echo "Will run on the default (ubuntu) image"
		containername="hub-docker-inspector"
		imagename="hub-docker-inspector"
	else
		echo "Will run on the ${runondistro} image"
		containername="hub-docker-inspector-${runondistro}"
		imagename="hub-docker-inspector-${runondistro}"
	fi
}

##################
# Start script
##################
version="@VERSION@"
outputPath=""
containername=hub-docker-inspector
imagename=hub-docker-inspector
propdir=.
hub_password_set_on_cmd_line=false
noPromptMode=false
dryRunMode=false
createdWorkingDir=false

if [ $# -lt 1 ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -v \) -o \( "$1" = --version \) ]
then
	echo "$(basename $0) ${version}"
	exit 0
fi

if [ \( "$1" = -h \) -o \( "$1" = --help \) ]
then
    printUsage
    exit 0
fi

if [ \( "$1" = -p \) -o \( "$1" = --get-properties \) ]
then
	echo "************ NOT IMPLEMENTED"
	exit -1
    #ensureContainerRunning
    #echo "Copying application.properties template"
    #docker cp hub-docker-inspector:/opt/blackduck/hub-docker-inspector/template/application.properties .
    #exit 0
fi

preProcessOptions "$@"
propfile="${propdir}/application.properties"
echo "Properties file: ${propfile}"

if [ -z "${outputPath}" ]
then
	echo "Looking in ${propfile} for output.path"
	outputPath=$(get_property "${propfile}" "output.path")
	echo "output path: ${outputPath}"
fi

if [ -z "${workingDir}" ]
then
	echo "Looking in ${propfile} for output.path"
	workingDir=$(get_property "${propfile}" "working.dir.path")
	echo "output path: ${workingDir}"
fi
if [ -z "${workingDir}" ]
then
	workingDir="$(mktemp -d)"
	createdWorkingDir=true
	echo "Created working directory: ${workingDir}"
fi

######determineRunOnImage
options=( "$@" )
image="${options[${#options[@]}-1]}"
unset "options[${#options[@]}-1]"
checkForPassword
#ensureContainerRunning
#installPropertiesFile

# TODO TEMP
echo "******* TEMP: getting hub-docker-inspector-3.0.0-SNAPSHOT.jar from latest build"
cp ~/Documents/git/hub-docker-inspector/build/distributions/*.zip ~/Documents/Files/hub-inspector/test
pushd ~/Documents/Files/hub-inspector/test
unzip -o hub-docker-inspector-3.0.0-SNAPSHOT.zip
popd
cp ~/Documents/Files/hub-inspector/test/hub-docker-inspector-3.0.0-SNAPSHOT.jar .
# END TEMP

if [[ "$image" == *.tar ]]
then
	echo "Inspecting image tar file: $image"
	if [ ! -r "${image}" ]
	then
		err "ERROR: Tar file ${image} does not exist"
		exit -1
	fi
	tarfilename=$(basename "$image")
	
	# TODO TEMP
	echo "******* invoking launcher script"
	chmod +x ~/Documents/git/hub-docker-inspector/build/hub-docker-inspector-launcher.sh
	~/Documents/git/hub-docker-inspector/build/hub-docker-inspector-launcher.sh ${options[*]} "--host.working.dir.path=${workingDir}" "\"$image\""
	echo "******* DONE invoking launcher script"

else
	echo Inspecting image: $image
	# TODO TEMP
	echo "******* invoking launcher script"
	chmod +x ~/Documents/git/hub-docker-inspector/build/hub-docker-inspector-launcher.sh
	~/Documents/git/hub-docker-inspector/build/hub-docker-inspector-launcher.sh ${options[*]} "--host.working.dir.path=${workingDir}" "\"$image\""
	echo "******* DONE invoking launcher script"
fi

if [ ! -z "${outputPath}" ]
then
	if [ -f "${outputPath}" ]
	then
		err "ERROR: Unable to copy BDIO output file to ${outputPath} because it is an existing file"
		exit -2
	fi
	if [ ! -e "${outputPath}" ]
	then
		mkdir -p "${outputPath}"
	fi
	echo "----- hostname: $(hostname)"
	echo "----- ls -l ${workingDir}/output"
	ls -l "${workingDir}/output"
	echo "----- ls -ld ${outputPath}"
	ls -ld "${outputPath}"
	echo "Copying output to ${outputPath}"
	cp "${workingDir}"/output/* "${outputPath}"
fi

if [ $createdWorkingDir == true ]
then
	echo "DISABLED: Removing ${workingDir}"
	###rm -rf ${workingDir}
fi

exit 0
