#!/bin/bash

targetImageName=alpine
targetImageTag=latest


targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
identifyOnContainerName=hub-docker-inspector-identify
uploadOnContainerName=hub-docker-inspector-upload

identifyOnImageName=blackducksoftware/hub-docker-inspector-alpine
identifyOnImageTag=4.1.0


function ensureKubeRunning() {
	kubeRunning=$(minikube status | grep "minikube: Running" | wc -l)
	if [[ ${kubeRunning} -eq 0 ]]; then
		echo "Starting minikube"
		minikube start
	else
		echo "minikube is already running"
	fi
	eval $(minikube docker-env)
}

function waitForPodToStart() {
	newContainerName=$1
	newPodName=""
	
	echo "Pausing to give the new pod for container ${newContainerName} time to start..."
	sleep 15
	newPodName=$(kubectl get pods | grep "${newContainerName}"  | tr -s " " | cut -d' ' -f1)
	echo "newPodName: ${newPodName}"

	podIsRunning=false
	counter=0
	while [[ $counter -lt 10 ]]; do
		echo the counter is $counter
		kubectl get pods
		newPodStatus=$(kubectl get pods | grep "${newContainerName}"  | tr -s " " | cut -d' ' -f3)
		echo "newPodStatus: ${newPodStatus}"
		if [ "${newPodStatus}" == "Running" ]; then
			echo "The new pod running container ${newContainerName} is ready"
			break
		else
			echo "The new pod is NOT ready"
		fi
		echo "Pausing to give the new pod time to start..."
		sleep 10
		counter=$((counter+1))
	done
	if [ "${newPodStatus}" != "Running" ]; then
		echo "The new pod for container ${newContainerName} never started!"
		exit -1
	fi
	echo "New Pod ${newPodName}, is running container ${newContainerName}"
}

ensureKubeRunning
rm -rf "${outputDir}"
mkdir "${outputDir}"
rm -f "${targetImageDir}/${targetImageTarfile}"

kubectl get pods

#################################################################
# Pull/save target image (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Pulling/saving the target image"
echo "--------------------------------------------------------------"
docker pull "${targetImageName}:${targetImageTag}"
docker save -o "${targetImageDir}/${targetImageTarfile}" "${targetImageName}:${targetImageTag}"
chmod a+r "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Get "target image pkg mgr identification" image / start and setup "target image pkg mgr identification" container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Starting container for target image package manager identification"
echo "--------------------------------------------------------------"
kubectl run "${identifyOnContainerName}" --image="${identifyOnImageName}:${identifyOnImageTag}" --command -- tail -f /dev/null
waitForPodToStart "${identifyOnContainerName}"
identifyPodName="${newPodName}"

kubectl cp --container="${identifyOnContainerName}" build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${identifyPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp --container="${identifyOnContainerName}" "${targetImageDir}/${targetImageTarfile}" "${identifyPodName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Determine inspectOn image (jar on identifyOnContainer)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Identifying target image package manager"
echo "--------------------------------------------------------------"
rm -rf "${outputDir}"
mkdir "${outputDir}"
kubectl exec -it "${identifyPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=true \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=false \
	--logging.level.com.blackducksoftware=INFO \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

echo kubectl cp --container="${identifyOnContainerName}" "${identifyPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
kubectl cp --container="${identifyOnContainerName}" "${identifyPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"

ls "${outputDir}"

inspectOnOsName=$(fgrep inspectOnOsName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Docker image selected for target image inspection: ${inspectOnImageName}:${inspectOnImageTag} (OS name: ${inspectOnOsName})"

inspectOnContainerName="hub-docker-inspector-${inspectOnOsName}"

#################################################################
# Get inspectOn image / start and setup inspectOn container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Starting container for target image inspection"
echo "--------------------------------------------------------------"
kubectl run "${inspectOnContainerName}" --image="${inspectOnImageName}:${inspectOnImageTag}" --command -- tail -f /dev/null
waitForPodToStart "${inspectOnContainerName}"
inspectPodName="${newPodName}"

kubectl cp --container="${inspectOnContainerName}" build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${inspectPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp --container="${inspectOnContainerName}" "${targetImageDir}/${targetImageTarfile}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Inspect (jar in container)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Target image inspection"
echo "--------------------------------------------------------------"
rm -rf "${outputDir}"
mkdir "${outputDir}"
kubectl exec -it "${inspectPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=false \
	--inspect=true \
	--inspect.in.container=false \
	--upload.bdio=false \
	"--docker.tar=/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

kubectl exec -it "${inspectPodName}" -- \
	ls -lrt /opt/blackduck/hub-docker-inspector/output
	
echo kubectl cp --container="${inspectOnContainerName}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
kubectl cp --container="${inspectOnContainerName}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilePath="${outputDir}/${bdioFilename}"
echo kubectl cp --container="${inspectOnContainerName}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"
kubectl cp --container="${inspectOnContainerName}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"
echo "BDIO file: ${bdioFilePath}"
ls -l "${bdioFilePath}"

#################################################################
# Upload BDIO (jar on host)
# Or, simply set --upload.bdio=true in the inspect phase.
# Or, run the .jar directly on this machine, similar to below
# but with --on.host=true and --bdio.path pointing to local dir.
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Uploading BDIO file (BOM) to Hub"
echo "--------------------------------------------------------------"
kubectl run "${uploadOnContainerName}" --image="${inspectOnImageName}:${inspectOnImageTag}" --command -- tail -f /dev/null
waitForPodToStart "${uploadOnContainerName}"
uploadPodName="${newPodName}"

kubectl cp --container="${uploadOnContainerName}" build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${uploadPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp --container="${uploadOnContainerName}" "${bdioFilePath}" "${uploadPodName}:/opt/blackduck/hub-docker-inspector/output"
kubectl exec -it "${uploadPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=false \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=true \
	--bdio.path="/opt/blackduck/hub-docker-inspector/output" \
	--logging.level.com.blackducksoftware=INFO \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


#################################################################
# Clean up minikube VM (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Deleting deployments"
echo "--------------------------------------------------------------"
kubectl get pods
kubectl delete deployment "${identifyOnContainerName}"
kubectl delete deployment "${inspectOnContainerName}"
kubectl delete deployment "${uploadOnContainerName}"
kubectl get pods
