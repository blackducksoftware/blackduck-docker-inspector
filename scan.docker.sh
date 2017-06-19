#!/usr/bin/env bash
#
# Execute Docker Scanning, both CLI and Inspector
#
# use coding style from: https://google.github.io/styleguide/shell.xml

set -fb

readonly THISDIR=$(cd "$(dirname "$0")" ; pwd)
readonly BASEDIR=$(cd "$(dirname "$0")/.." ; pwd)
readonly MY_NAME=$(basename "$0")
readonly INSPECTOR_SHELL_SCRIPT="${THISDIR}/hub-docker-inspector.sh"
readonly DOCKER_SCAN_FETCH_URL="https://blackducksoftware.github.io/hub-docker-inspector/scan.docker.sh"
readonly DOCKER_SCAN_SHELL_SCRIPT="${THISDIR}/scan.docker.sh"
readonly EXECUTABLE_SHELL_SCRIPT="${THISDIR}/.scan.docker.sh"
readonly IMAGE_TARFILE="${THISDIR}/toBeScanned.tar"

function usage() {
   >&2 echo "Error: Missing or invalid arguments."
   echo ""
   echo "Example usage: ${0} --image jetty:9.3.11-jre8-alpine --username sysadmin --host my_hub_host.domain.com --port 8443 --scheme https"
   echo "   port defaults to 443, and scheme defaults to https"
   echo ""
   echo "Usage: $0 --image IMAGENAME[:TAG]  [--no-scan] [--no-inspect] [--inspector-version VERSION] scan.cli.sh arguments from 'Options' listed below:"
   "${THISDIR}"/scan.cli.sh --help
   echo ""
   exit 1
}

function using_proxy_instructions() {
   echo "-----------------------------------------------------------------------------"
   echo "You may need set the default proxies for Wget to use for http and https."
   echo "To do this, create a ~/.wgetrc file."
   echo "This will override the values in the environment."
   echo "Add these settings (using your proxy's URL) to the .wgetrc file:"
   echo ""
   echo "https_proxy=http://proxy.yourdomain.com:18023/"
   echo "http_proxy=http://proxy.yourdomain.com:18023/"
   echo ""
   echo "If your proxy requires authentication, add these additional two lines:"
   echo "proxy_user=username"
   echo "proxy_password=password"
   echo "----------------------------------------------------------------------------"
   echo ""
}

function get_remote_file() {
  readonly REQUEST_URL=$1
  readonly OUTPUT_FILENAME=$2
  readonly TEMP_FILE="${THISDIR}/tmp.file"
  if [ -n "$(which wget)" ]; then
    wget_output=$(wget -O "${TEMP_FILE}"  "$REQUEST_URL" 2>&1)
    local wget_success=$?
    if [[ $wget_success -eq 0 ]]; then
      mv "${TEMP_FILE}" "${OUTPUT_FILENAME}"
      chmod 755 "${OUTPUT_FILENAME}"
    else
      if [[  "${wget_output}" == "${wget_output#*Resolving blackducksoftware.github.io}" ]]; then
        using_proxy_instructions
      fi
      return 1
    fi
  else
    return 2
  fi
}

function clean_up() {
   rm -f "${IMAGE_TARFILE}"
}
function self_clean_up() {
   rm -f "${EXECUTABLE_SHELL_SCRIPT}"
}

function update_self_and_invoke() {
  get_remote_file "${DOCKER_SCAN_FETCH_URL}" "${EXECUTABLE_SHELL_SCRIPT}"
  if [ $? -ne 0 ]; then
    echo "Using installed scan.docker.sh"
    cp "${DOCKER_SCAN_SHELL_SCRIPT}" "${EXECUTABLE_SHELL_SCRIPT}"
  fi
  exec "${EXECUTABLE_SHELL_SCRIPT}" "$@"
}

function pull_image_if_necessary() {
  local image=$1
  local existing_image=$(docker images -q "$image")
  if [ -z "$existing_image" ]; then
    echo "Existing image not present, pulling from docker repository"
    docker pull "$image"
    existing_image=$(docker images -q "$image")
    if [ -z "${existing_image}" ]; then
      echo "Unable to pull image: $image, exiting"
      exit 5
    fi
  fi
}

function perform_validations() {
  if [ ! -w "${THISDIR}" ]; then
    echo "Directory ${THISDIR} is not writable!"
    echo "That directory must be writable by this user in order to execute this script"
    echo "Please fix the directory permissions and try again"
    exit 10
  fi
  local opt_count=$1
  if [ $opt_count -lt 3 ]; then
    "All required options not present!"
    usage
  fi
  if [ -z "$(which docker)" ]; then
    echo "Docker is required to be in your PATH and it cannot be found."
    exit 2
  fi
  # there IS a docker, now validate that this user can execute it
  valid_docker=$(docker info 2>&1)
  valid_docker=${valid_docker%%*permission denied*}
  if [ -z "${valid_docker}" ]; then
    echo "You do not have permission to run Docker. "
    echo "You may need to be a user in the docker group, a root user, or have sudo access."
    exit 3
  fi
}

function main() {
  cp "${EXECUTABLE_SHELL_SCRIPT}" "${DOCKER_SCAN_SHELL_SCRIPT}"
  local hub_port="443"
  local hub_scheme="https"
  local hub_project=""
  local hub_version=""
  local name_arg=""
  local do_scan=1
  local do_inspect=1
  local option_count=0
  local inspector_version="0.0.4"
  while [[ $# -ge 1 ]]; do
    opt="$1"
    case $opt in
      --image)
        local docker_image="$2"
        shift
        ((option_count++))
        ;;
      --host)
        local hub_host="$2"
        shift
        ((option_count++))
        ;;
      --username)
        local hub_user="$2"
        shift
        ((option_count++))
	;;
      --port)
        hub_port="$2"
        shift
        ;;
      --scheme)
        hub_scheme="$2"
        shift
        ;;
      --inspector-version)
        inspector_version="$2"
        shift
        ;;
      --name)
        name_arg="$2"
        shift
        ;;
      --project)
        hub_project="$2"
        shift
        ;;
      --release)
        hub_version="$2"
        shift
        ;;
      --no-scan)
        do_scan=0
        ;;
      --no-inspect)
        do_inspect=0
        ;;
      *)
	optional_args="${optional_args} $1"
        ;;
    esac
    shift
  done

  perform_validations $option_count

  # if the --name scan.cli option was not provided
  if [ -z "${name_arg}" ]; then
    name_arg="--name ${docker_image}"
  else
    name_arg="--name ${name_arg}"
  fi

  readonly HUB_URL="${hub_scheme}://${hub_host}:${hub_port}/"
  if [ -z "$hub_project" ]; then
    hub_project="${docker_image%:*}"
  fi
  if [ -z "$hub_version" ]; then
    hub_version="${docker_image#*:}"
  fi

  # update_docker_inspector
  inspector_script_name="hub-docker-inspector"
  if [ -n "$inspector_version" ]; then
    inspector_script_name="hub-docker-inspector-${inspector_version}"
  fi
  readonly INSPECTOR_FETCH_URL="https://blackducksoftware.github.io/hub-docker-inspector/${inspector_script_name}.sh"
  get_remote_file "${INSPECTOR_FETCH_URL}" "${INSPECTOR_SHELL_SCRIPT}"
  if [ $? -ne 0 ]; then
    echo "Using default hub-docker-inspector.sh"
  fi

  pull_image_if_necessary $docker_image

  docker save "$docker_image" -o "${IMAGE_TARFILE}" 2>/dev/null
  if [ $? -eq 0 ]; then
    if [ $do_scan -eq 1 ]; then
      echo "Perform scan:"
      "${THISDIR}"/scan.cli.sh --host $hub_host --port $hub_port --scheme $hub_scheme --username $hub_user --project "$hub_project" --release "$hub_version" $name_arg $optional_args "${IMAGE_TARFILE}"
    fi
    if [ $do_inspect -eq 1 ]; then
      echo "Conduct  Inspection:"
      "${INSPECTOR_SHELL_SCRIPT}" --hub.username=$hub_user --hub.url=$HUB_URL --hub.project.name="$hub_project" --hub.project.version="$hub_version" "$INSPECTOR_OPTS" "${IMAGE_TARFILE}"
    fi
  else
    echo "Unable to save: ${docker_image} to a tar file."
    echo "Please verify the image name:tag and try again."
  fi
}

# the 'real' version will be executed as .scan.docker.sh
if [[ $MY_NAME = \.* ]]; then
   # invoke real main program
   trap "clean_up; self_clean_up" EXIT
   main "$@"
else
   # update myself and invoke updated version
   trap clean_up EXIT
   update_self_and_invoke "$@"
fi

