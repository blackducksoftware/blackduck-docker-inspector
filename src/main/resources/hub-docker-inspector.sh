#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with your Hub connection details (hub.url, hub.username, and hub.password),
# and Docker Hub connection details (docker.registry.username and docker.registry.password).

# To override the default location of /tmp/hub-docker-inspector, specify
# your own DOCKER_INSPECTOR_JAR_DIR in your environment and
# *that* location will be used.
DOCKER_INSPECTOR_JAR_DIR=${DOCKER_INSPECTOR_JAR_DIR:-/tmp/hub-docker-inspector}

# If you want to pass any additional options to
# curl, specify DOCKER_INSPECTOR_CURL_OPTS in your environment.
# For example, to specify a proxy, you would set
# DOCKER_INSPECTOR_CURL_OPTS=--proxy http://myproxy:3128
DOCKER_INSPECTOR_CURL_OPTS=${DOCKER_INSPECTOR_CURL_OPTS:-}

# DOCKER_INSPECTOR_LATEST_RELEASE_VERSION should be set in your
# environment if you wish to use a version different
# from LATEST.
DOCKER_INSPECTOR_RELEASE_VERSION=${DOCKER_INSPECTOR_LATEST_RELEASE_VERSION}

latestReleasedJarUrl='http://prd-eng-repo01.dc2.lan:8181/artifactory/bds-integrations/com/blackducksoftware/integration/hub-docker-inspector/\[RELEASE\]/hub-docker-inspector-\[RELEASE\].jar'

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

# Write warning to stdout
warn() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: WARNING: $@"
}

# Write error message to stderr
err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: ERROR: $@" >&2
}

function getLatestJar() {
	log "Jar dir: ${DOCKER_INSPECTOR_JAR_DIR}"
	versionFileDestinationFile="${DOCKER_INSPECTOR_JAR_DIR}/hub-docker-inspector-latest-commit-id.txt"
	currentVersionCommitId=""
	#### TODO maybe get this working later... For now: never assume jar is up to date
#	if [ -f $versionFileDestinationFile ]; then
#		log "Existing version commit ID file: ${versionFileDestinationFile}"
#		currentVersionCommitId=$( <$versionFileDestinationFile )
#		log "Current version commit ID: ${currentVersionCommitId}"
#	fi

	mkdir -p "${DOCKER_INSPECTOR_JAR_DIR}"
	log "executing: curl $DOCKER_INSPECTOR_CURL_OPTS -o $versionFileDestinationFile https://blackducksoftware.github.io/hub-docker-inspector/latest-commit-id.txt"
	curl $DOCKER_INSPECTOR_CURL_OPTS -o $versionFileDestinationFile https://blackducksoftware.github.io/hub-docker-inspector/latest-commit-id.txt
	latestVersionCommitId=$( <$versionFileDestinationFile )
	echo "The latest version of the hub-docker-inspector jar file: ${latestVersionCommitId}"
	echo "The currently-installed version of the hub-docker-inspector jar file: ${currentVersionCommitId}"
	
	# If the user specified a version: get that
	if [ -z "${DOCKER_INSPECTOR_RELEASE_VERSION}" ]; then
      selectedJarUrl="${latestReleasedJarUrl}"
      latestReleasedVersion=$(curl 'http://prd-eng-repo01.dc2.lan:8181/artifactory/api/search/latestVersion?g=com.blackducksoftware.integration&a=hub-docker-inspector')
      selectedJarFilename=hub-docker-inspector-${latestReleasedVersion}.jar
      downloadedJarPath="${DOCKER_INSPECTOR_JAR_DIR}/${selectedJarFilename}"
    else
      selectedJarUrl="http://prd-eng-repo01.dc2.lan:8181/artifactory/bds-integrations/com/blackducksoftware/integration/hub-docker-inspector/${DOCKER_INSPECTOR_RELEASE_VERSION}/hub-docker-inspector-${DOCKER_INSPECTOR_RELEASE_VERSION}.jar"
      downloadedJarPath="${DOCKER_INSPECTOR_JAR_DIR}/hub-docker-inspector-${DOCKER_INSPECTOR_RELEASE_VERSION}.jar"
    fi
    echo "will look for : ${selectedJarUrl}"
    echo "*** downloadedJarPath: ${downloadedJarPath}"
    
	mustDownloadJar=1
	if [ ! -f "${downloadedJarPath}" ]; then
		echo "You don't have a hub-docker-inspector jar file at ${downloadedJarPath}, so it will be downloaded."
	elif [ "$currentVersionCommitId" != "$latestVersionCommitId" ] ; then
		echo "${downloadedJarPath} is no longer the latest version; the newer version will be downloaded."
	else
		echo "${downloadedJarPath} is up-to-date."
		mustDownloadJar=0
	fi
	
	if [ $mustDownloadJar -eq 1 ]; then
		curl ${DOCKER_INSPECTOR_CURL_OPTS} --fail -L -o "${downloadedJarPath}" "${selectedJarUrl}"
		if [[ $? -ne 0 ]]
		then
			err "Download of ${selectedJarUrl} failed."
			exit -1
		fi
		log "Saved ${selectedJarUrl} to ${downloadedJarPath}"
	fi
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
		if [[ "$cmdlinearg" == --jar.path=* ]]
		then
			userSpecifiedJarPath=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			userSpecifiedJarPath=$(expandPath "${userSpecifiedJarPath}")
			userSpecifiedJarPathEscaped=$(escapeSpaces "${userSpecifiedJarPath}")
			options[${cmdlineargindex}]="--jar.path=${userSpecifiedJarPathEscaped}"
			jarPathAlreadySpecifiedOnCmdLine=true
		elif [[ "$cmdlinearg" == --spring.config.location=* ]]
		then
			# Once IDETECT-339 is done/released, this clause can go away
			springConfigLocation=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			if ! [[ "$springConfigLocation" == file:* ]]
			then
				springConfigLocation="file:${springConfigLocation}"
			fi
			if ! [[ "$springConfigLocation" == */application.properties ]]
			then
				if [[ "$springConfigLocation" == */ ]]
				then
					springConfigLocation="${springConfigLocation}application.properties"
				else
					springConfigLocation="${springConfigLocation}/application.properties"
				fi
			fi
			options[${cmdlineargindex}]="--spring.config.location=${springConfigLocation}"
		else
			if [[ "${cmdlineargindex}" -eq $(( $# - 1)) ]]
			then
				if [[ "${cmdlinearg}" =~ ^--.*=.* ]]
				then
					options[${cmdlineargindex}]="${cmdlinearg}"
				else
					image="${cmdlinearg}"
					if [[ "${image}" == *.tar ]]
					then
						warn "This command line format is deprecated. Please replace the final argument ${image} with --docker.tar=${image}"
						options[${cmdlineargindex}]="--docker.tar=${cmdlinearg}"
					else
						warn "This command line format is deprecated. Please replace the final argument ${image} with --docker.image=${image}"
						options[${cmdlineargindex}]="--docker.image=${cmdlinearg}"
					fi
				fi
			else
				options[${cmdlineargindex}]="${cmdlinearg}"
			fi
		fi
		(( cmdlineargindex += 1 ))
	done
}

##################
# Start script
##################
version="@VERSION@"
encodingSetting="-Dfile.encoding=UTF-8"
userSpecifiedJarPath=""
jarPathAlreadySpecifiedOnCmdLine=false

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
	curl ${DOCKER_INSPECTOR_CURL_OPTS} --fail -L -O "${latestReleasedJarUrl}"
	if [[ $? -ne 0 ]]
	then
		err "Download of ${latestReleasedJarUrl} failed."
		exit -1
	fi
	log "Saved ${latestReleasedJarUrl} to $(pwd)"
fi

preProcessOptions "$@"

newJarPathAssignment=""
if [[ $jarPathAlreadySpecifiedOnCmdLine == false ]]
then
	getLatestJar
	jarPath="${downloadedJarPath}"
	newJarPathAssignment="--jar.path=${jarPath}"
else
	jarPath="${userSpecifiedJarPath}"
fi

log "jarPath: ${jarPath}"
log "newJarPathAssignment: ${newJarPathAssignment}"
log "Options: ${options[*]}"
log "Jar dir: ${DOCKER_INSPECTOR_JAR_DIR}"
java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarPath}" "${newJarPathAssignment}" ${options[*]}
status=$?
log "Return code: ${status}"
exit ${status}
