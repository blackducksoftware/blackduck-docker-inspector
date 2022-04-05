#!/bin/bash
#
# This script runs on the host machine, and uses blackduck-imageinspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with any properties you're not setting via the command line.

# To override the dir to which blackduck-docker-inspector.sh will download files,
# set DOCKER_INSPECTOR_JAR_DIR
DOCKER_INSPECTOR_JAR_DIR=${DOCKER_INSPECTOR_JAR_DIR:-/tmp/blackduck-docker-inspector}

# If you want to pass any additional options to
# curl, specify DOCKER_INSPECTOR_CURL_OPTS in your environment.
# For example, to specify a proxy, you would set
# DOCKER_INSPECTOR_CURL_OPTS=--proxy http://myproxy:3128
DOCKER_INSPECTOR_CURL_OPTS=${DOCKER_INSPECTOR_CURL_OPTS:-}

# DOCKER_INSPECTOR_VERSION should be set in your
# environment if you wish to use a version different
# from LATEST.
jarVersion=${DOCKER_INSPECTOR_VERSION}

JAVACMD=${JAVACMD:-java}

# To use an existing blackduck-docker-inspector jar instead of downloading one,
# set DOCKER_INSPECTOR_JAR_PATH
DOCKER_INSPECTOR_JAR_PATH=${DOCKER_INSPECTOR_JAR_PATH:-}

#Getting the proxy settings from the environment
PROXY_HOST=${BLACKDUCK_PROXY_HOST}
PROXY_PORT=${BLACKDUCK_PROXY_PORT}
PROXY_USERNAME=${BLACKDUCK_PROXY_USERNAME}
PROXY_PASSWORD=${BLACKDUCK_PROXY_PASSWORD}

ARTIFACTORY_BASE_URL=https://sig-repo.synopsys.com

#Getting the proxy settings from the command line switches
for i in "$@"
do
case $i in
    --blackduck.proxy.host=*)
    PROXY_HOST="${i#*=}"
    shift # past argument=value
    ;;
    --blackduck.proxy.port=*)
    PROXY_PORT="${i#*=}"
    shift # past argument=value
    ;;
    --blackduck.proxy.username=*)
    PROXY_USERNAME="${i#*=}"
    shift # past argument=value
    ;;
     --blackduck.proxy.password=*)
    PROXY_PASSWORD="${i#*=}"
    shift # past argument=value
    ;;
    *)
          # ignored option
    ;;
esac
done

#Putting together the Curl proxy options
CURL_PROXY_OPTIONS=""
if [ ! -z "${PROXY_HOST}" ]; then
	CURL_PROXY_OPTIONS="--proxy ${PROXY_HOST}:${PROXY_PORT}"

	if [ ! -z "${PROXY_USERNAME}" ]; then
		CURL_PROXY_OPTIONS="${CURL_PROXY_OPTIONS} --proxy-anyauth --proxy-user ${PROXY_USERNAME}:${PROXY_PASSWORD}"
	fi
fi

##################
# Initialize
##################
latestCommitIdFileUrl="https://blackducksoftware.github.io/blackduck-docker-inspector/latest-commit-id.txt"
localCommitIdFile="${DOCKER_INSPECTOR_JAR_DIR}/blackduck-docker-inspector-latest-commit-id.txt"
currentVersionCommitId=""
version="9.4.3"
inspectorImageFamily="blackduck-imageinspector"
encodingSetting="-Dfile.encoding=UTF-8"
userSpecifiedJarPath=""
jarPathSpecified=false
latestReleaseVersion=
blackduckUsernameArgument=""
blackduckProjectNameArgument=""
blackduckProjectVersionArgument=""
dockerTarArgument=""

function printUsage() {
	echo ""
    echo "Usage: $0 [options]"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    echo ""
    echo "Run this command from the directory that contains the application.properties,"
    echo "configured with any properties not set via the command line."
	echo ""
	echo "For greater security, the Black Duck password can be set via the environment variable BD_PASSWORD"
	echo ""
	echo "For example:"
	echo "  export BD_PASSWORD=mypassword"
	echo "  $0 --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername ubuntu"
	echo ""
	echo "Documentation is under Package Managers > Black Duck Docker Inspector at: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS"
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

function deriveCurrentVersionCommitId() {
	log "Checking ${localCommitIdFile} for commit ID"
	if [ -f "${localCommitIdFile}" ]; then
		log "Existing version commit ID file: ${localCommitIdFile}"
		currentVersionCommitId=$( <"${localCommitIdFile}" )
		log "Current version commit ID: ${currentVersionCommitId}"
	fi
	log "The currently-installed version of the blackduck-docker-inspector jar file: ${currentVersionCommitId}"
}

function deriveLatestVersionCommitId() {
	log "Downloading ${latestCommitIdFileUrl} to ${localCommitIdFile}"
	curl ${DOCKER_INSPECTOR_CURL_OPTS} ${CURL_PROXY_OPTIONS} -o "${localCommitIdFile}" "${latestCommitIdFileUrl}"
	latestVersionCommitId=$( <"${localCommitIdFile}" )
	log "The latest version of the blackduck-docker-inspector jar file: ${latestVersionCommitId}"
}

function deriveJarDetails() {
	# If the user specified a version: get that
	if [ -z "${jarVersion}" ]; then
	  deriveLatestReleaseVersion
	  latestReleasedJarUrl="${ARTIFACTORY_BASE_URL}/bds-integrations-release/com/synopsys/integration/blackduck-docker-inspector/${latestReleaseVersion}/blackduck-docker-inspector-${latestReleaseVersion}.jar"
	  latestReleasedAirGapZipUrl="${ARTIFACTORY_BASE_URL}/bds-integrations-release/com/synopsys/integration/blackduck-docker-inspector/${latestReleaseVersion}/blackduck-docker-inspector-${latestReleaseVersion}-air-gap.zip"
      selectedJarUrl="${latestReleasedJarUrl}"
      selectedAirGapUrl="${latestReleasedAirGapZipUrl}"
      deriveLatestReleasedFilename
      deriveLatestReleasedAirGapZipFilename
      selectedJarFilename="${latestReleasedFilename}"
      selectedAirGapZipFilename="${latestReleasedAirGapZipFilename}"
      downloadedJarPath="${DOCKER_INSPECTOR_JAR_DIR}/${selectedJarFilename}"
      downloadedAirGapZipPath="${DOCKER_INSPECTOR_JAR_DIR}/${selectedAirGapZipFilename}"
    else
      log "Will download blackduck-docker-inspector-${jarVersion}.jar"
      rm -f "${localCommitIdFile}" # Local commit ID won't apply to this jar
      if [[ $jarVersion == *"SNAPSHOT"* ]]; then
      	selectedRepoKey="bds-integrations-snapshot"
      else
      	selectedRepoKey="bds-integrations-release"
      fi
      selectedJarUrl="${ARTIFACTORY_BASE_URL}/${selectedRepoKey}/com/synopsys/integration/blackduck-docker-inspector/${jarVersion}/blackduck-docker-inspector-${jarVersion}.jar"
      selectedAirGapZipUrl="${ARTIFACTORY_BASE_URL}/${selectedRepoKey}/com/synopsys/integration/blackduck-docker-inspector/${jarVersion}/blackduck-docker-inspector-${jarVersion}-air-gap.zip"
      downloadedJarPath="${DOCKER_INSPECTOR_JAR_DIR}/blackduck-docker-inspector-${jarVersion}.jar"
      downloadedAirGapZipPath="${DOCKER_INSPECTOR_JAR_DIR}/blackduck-docker-inspector-${jarVersion}-air-gap.zip"
    fi
    log "Selected jar: ${selectedJarUrl}"
    log "  local path: ${downloadedJarPath}"
    log "Selected Air Gap Zip: ${selectedAirGapZipUrl}"
    log "  local path: ${downloadedAirGapZipPath}"
}

function determineIsJarDownloadRequired() {
	jarDownloadRequired=true
	if [ ! -f "${downloadedJarPath}" ]; then
		log "You don't have a blackduck-docker-inspector jar file at ${downloadedJarPath}, so it will be downloaded."
	elif [[ ! -z ${jarVersion} ]] && [[ ${jarVersion} != *"SNAPSHOT"* ]]; then
	    log "You specified jar version ${jarVersion}, it's not a snapshot, and it exists at ${downloadedJarPath}; no need to download it."
		jarDownloadRequired=false
	elif [ "${currentVersionCommitId}" != "${latestVersionCommitId}" ] ; then
		log "${downloadedJarPath} needs to be downloaded. (Snapshot versions are downloaded every time; Released versions are not.)"
	else
		log "${downloadedJarPath} is up-to-date."
		jarDownloadRequired=false
	fi
}

function downloadJarIfRequired() {
	if [ ${jarDownloadRequired} == true ]; then
		curl ${DOCKER_INSPECTOR_CURL_OPTS} ${CURL_PROXY_OPTIONS} --fail -L -o "${downloadedJarPath}" "${selectedJarUrl}"
		if [[ $? -ne 0 ]]
		then
			err "Download of ${selectedJarUrl} failed."
			exit -1
		fi
		log "Saved ${selectedJarUrl} to ${downloadedJarPath}"
	fi
}

function prepareLatestJar() {
	deriveCurrentVersionCommitId
	deriveLatestVersionCommitId
	deriveJarDetails
	determineIsJarDownloadRequired
	downloadJarIfRequired
}

#
function deriveLatestReleaseVersion() {
	if [[ -z "${latestReleaseVersion}" ]]; then
		latestReleaseVersion=$(curl ${DOCKER_INSPECTOR_CURL_OPTS} ${CURL_PROXY_OPTIONS} ${ARTIFACTORY_BASE_URL}/bds-integrations-release/com/synopsys/integration/blackduck-docker-inspector/maven-metadata.xml | grep latest | sed -e 's@<latest>@@' -e 's@</latest>@@' -e 's/^[ \t]*//')
	fi
	echo "Latest release version: ${latestReleaseVersion}"
}

#
function deriveLatestReleasedFilename() {
	log "Deriving name of latest released jar file"
	deriveLatestReleaseVersion
    latestReleasedFilename=blackduck-docker-inspector-${latestReleaseVersion}.jar
    log "Latest released jar filename: ${latestReleasedFilename}"
}

#
function deriveLatestReleasedAirGapZipFilename() {
	log "Deriving name of latest released AirGap Zip file"
	deriveLatestReleaseVersion
    latestReleasedAirGapZipFilename=blackduck-docker-inspector-${latestReleaseVersion}-air-gap.zip
    log "Latest released AirGap Zip filename: ${latestReleasedAirGapZipFilename}"
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
		if [[ "${cmdlinearg}" == --jar.path=* ]]
		then
			userSpecifiedJarPath=$(echo "${cmdlinearg}" | cut -d '=' -f 2)
			userSpecifiedJarPath=$(expandPath "${userSpecifiedJarPath}")
			jarPathSpecified=true
		elif [[ "$cmdlinearg" == --blackduck.username=* ]]
                then
                        blackduckUsername=$(echo "$cmdlinearg" | cut -d '=' -f 2)
                        blackduckUsernameEscaped=$(escapeSpaces "${blackduckUsername}")
                        blackduckUsernameArgument="--blackduck.username=${blackduckUsernameEscaped}"
                elif [[ "$cmdlinearg" == --blackduck.project.name=* ]]
                then
                        blackduckProjectName=$(echo "$cmdlinearg" | cut -d '=' -f 2)
                        blackduckProjectNameEscaped=$(escapeSpaces "${blackduckProjectName}")
                        blackduckProjectNameArgument="--blackduck.project.name=${blackduckProjectNameEscaped}"
                elif [[ "$cmdlinearg" == --blackduck.project.version=* ]]
                then
                        blackduckProjectVersion=$(echo "$cmdlinearg" | cut -d '=' -f 2)
                        blackduckProjectVersionEscaped=$(escapeSpaces "${blackduckProjectVersion}")
                        blackduckProjectVersionArgument="--blackduck.project.version=${blackduckProjectVersionEscaped}"
                elif [[ "$cmdlinearg" == --docker.tar=* ]]
                then
                        dockerTar=$(echo "$cmdlinearg" | cut -d '=' -f 2)
                        dockerTarEscaped=$(escapeSpaces "${dockerTar}")
                        dockerTarArgument="--docker.tar=${dockerTarEscaped}"
		elif [[ "${cmdlinearg}" == --spring.config.location=* ]]
		then
			springConfigLocation=$(echo "${cmdlinearg}" | cut -d '=' -f 2)
			if ! [[ "${springConfigLocation}" == */application.properties ]]
			then
				if [[ "${springConfigLocation}" == */ ]]
				then
					springConfigLocation="${springConfigLocation}application.properties"
				else
					springConfigLocation="${springConfigLocation}/application.properties"
				fi
			fi
			options[${cmdlineargindex}]="--spring.config.location=${springConfigLocation}"
		else
			options[${cmdlineargindex}]="${cmdlinearg}"
		fi
		(( cmdlineargindex += 1 ))
	done
}

##################
# Start script
##################

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

if [ "$1" = --inspectorimagefamily ]
then
	echo ${inspectorImageFamily}
	exit 0
fi

if [ \( "$1" = -j \) -o \( "$1" = --pulljar \) ]
then
	deriveLatestReleasedFilename
    deriveJarDetails
	curl ${DOCKER_INSPECTOR_CURL_OPTS} ${CURL_PROXY_OPTIONS} --fail -L -o "${latestReleasedFilename}" "${latestReleasedJarUrl}"
	if [[ $? -ne 0 ]]
	then
		err "Download of ${latestReleasedJarUrl} failed."
		exit -1
	fi
	log "Saved ${latestReleasedJarUrl} to $(pwd)"
	exit 0
fi

if [ \( "$1" = -a \) -o \( "$1" = --pullairgapzip \) ]
then
	deriveLatestReleasedAirGapZipFilename
    deriveJarDetails
	curl ${DOCKER_INSPECTOR_CURL_OPTS} ${CURL_PROXY_OPTIONS} --fail -L -o "${latestReleasedAirGapZipFilename}" "${latestReleasedAirGapZipUrl}"
	if [[ $? -ne 0 ]]
	then
		err "Download of ${latestReleasedAirGapZipUrl} failed."
		exit -1
	fi
	log "Saved ${latestReleasedAirGapZipUrl} to $(pwd)"
	exit 0
fi

if [ ! -z "${DOCKER_INSPECTOR_JAR_PATH}" ]; then
	userSpecifiedJarPath="${DOCKER_INSPECTOR_JAR_PATH}"
	userSpecifiedJarPath=$(expandPath "${userSpecifiedJarPath}")
	log "DOCKER_INSPECTOR_JAR_PATH env var resolved to ${userSpecifiedJarPath}"
	jarPathSpecified=true
fi

preProcessOptions "$@"

log "Jar dir: ${DOCKER_INSPECTOR_JAR_DIR}"
mkdir -p "${DOCKER_INSPECTOR_JAR_DIR}"

if [[ ${jarPathSpecified} == true ]]
then
	jarPath="${userSpecifiedJarPath}"
else
	prepareLatestJar
	jarPath="${downloadedJarPath}"
fi

log "jarPath: ${jarPath}"
log "Options: ${options[*]}"
log "Jar dir: ${DOCKER_INSPECTOR_JAR_DIR}"
${JAVACMD} "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarPath}" ${options[*]} ${blackduckUsernameArgument} ${blackduckProjectNameArgument} ${blackduckProjectVersionArgument} ${dockerTarArgument}
status=$?
log "Return code: ${status}"
exit ${status}
