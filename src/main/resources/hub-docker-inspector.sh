#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with your Hub connection details (hub.url, hub.username, and hub.password),
# and Docker Hub connection details (docker.registry.username and docker.registry.password).

# To override the default location of /tmp, specify
# your own DOCKER_INSPECTOR_TEMP_DIR in your environment and
# *that* location will be used.
DOCKER_INSPECTOR_TEMP_DIR=${DOCKER_INSPECTOR_TEMP_DIR:-/tmp/hub-docker-inspector}

function printUsage() {
	echo ""
    echo "Usage: $0 [options]"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    echo ""
    echo "Run this command from the directory that contains the application.properties,"
    echo "configured with your Hub connection details (hub.url, hub.username, and hub.password),"
	echo "and Docker Hub connection details (docker.registry.username and docker.registry.password)."
	echo ""
	echo "For greater security, the Hub password can be set via the environment variable BD_HUB_PASSWORD"
	echo ""
	echo "For example:"
	echo "  export BD_HUB_PASSWORD=mypassword"
	echo "  $0 --hub.url=http://hub.mydomain.com:8080/ --hub.username=myusername ubuntu"
	echo ""
	echo "Documentation: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/48435867/Hub+Docker+Inspector"
}

# Write message to stdout
log() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@"
}

# Write message to stderr
err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
}

# Expand tilde
function expandPath() {
	echo "${@/#~/$HOME}"
}

# escape spaces
function escapeSpaces() {
	echo "${@// /%20}"
}

# Look through args for ones this script needs to act on
function preProcessOptions() {
	cmdlineargindex=0
	for cmdlinearg in "$@"
	do
		if [[ "$cmdlinearg" == --spring.config.location=* ]]
		then
			propdir=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			if [[ "${propdir}" == file:* ]]
			then
				propdir="${propdir:5}"
			fi
			propdir=$(expandPath "${propdir}")
			if [[ "$propdir" == */ ]]
			then
				propdir=$(echo "$propdir" | rev | cut -c 2- | rev)
			fi
		elif [[ "$cmdlinearg" == --hub.password=* ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			hub_password_set_on_cmd_line=true
		elif [[ "$cmdlinearg" == --jar.path=* ]]
		then
			jarPath=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			jarPath=$(expandPath "${jarPath}")
			jarPathEscaped=$(escapeSpaces "${jarPath}")
			options[${cmdlineargindex}]="--jar.path=${jarPathEscaped}"
			jarPathAlreadySet=true
		elif [[ "$cmdlinearg" == --no.prompt=true ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			noPromptMode=true
		elif [[ "$cmdlinearg" == --dry.run=true ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			dryRunMode=true
		else
			options[${cmdlineargindex}]="${cmdlinearg}"
		fi
		(( cmdlineargindex += 1 ))
	done
}

# Get a property value from the given properties file
# Usage: getProperty FILE KEY
function getProperty {
	grep "^$2=" "$1" 2> /dev/null | cut -d'=' -f2
}

# Pull the latest jar down to the current working directory
function pullJar {
	log "Getting hub-docker-inspector.jar from github"
	jarUrl="https://blackducksoftware.github.io/hub-docker-inspector/hub-docker-inspector-${version}.jar"
	curl --fail -O  ${jarUrl}
	curlStatus=$?
	if [[ "${curlStatus}" != "0" ]]
	then
		err "ERROR ${curlStatus} fetching ${jarUrl}"
		err "If you have the hub-docker-inspector .jar file, you can set the jar.path property to the path to the .jar file"
		exit ${curlStatus}
	fi
}

##################
# Start script
##################
version="@VERSION@"
encodingSetting="-Dfile.encoding=UTF-8"
propdir=.
hub_password_set_on_cmd_line=false
noPromptMode=false
dryRunMode=false
createdWorkingDir=false
jarPath=""
jarPathAlreadySet=false
workingDir=$(expandPath "${DOCKER_INSPECTOR_TEMP_DIR}")

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

if [ \( "$1" = -j \) -o \( "$1" = --pulljar \) ]
then
    pullJar
    exit 0
fi

preProcessOptions "$@"
propfile="${propdir}/application.properties"
log "Properties file: ${propfile}"

if [ -z "${jarPath}" ]
then
	jarPath=$(getProperty "${propfile}" "jar.path")
	jarPath=$(expandPath "${jarPath}")
fi
if [ -z "${jarPath}" ]
then
	pushd "${DOCKER_INSPECTOR_TEMP_DIR}" > /dev/null
	pullJar
	popd > /dev/null
	jarPath="${workingDir}/hub-docker-inspector-${version}.jar"
fi
log "Jar path: ${jarPath}"

checkForPassword
newJarPathAssignment=""
if [[ $jarPathAlreadySet == false ]]
then
	newJarPathAssignment="--jar.path=${jarPath}"
fi

log "jarPath: ${jarPath}"
log "Options: ${options[*]}"
java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarPath}" "${newJarPathAssignment}" "--host.working.dir.path=${workingDir}" ${options[*]}
status=$?

if [[ "${status}" -ne "0" ]]
then
	exit "${status}"
fi

if [ $createdWorkingDir == true ]
then
	log "Removing ${workingDir}"
	rm -rf ${workingDir}
fi

exit ${status}
