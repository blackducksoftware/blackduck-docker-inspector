#!/bin/bash

targetImageName=alpine
targetImageTag=latest


targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
identifyOnContainerName=hub-docker-inspector-identify
inspectOnContainerName=hub-docker-inspector-inspect
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
echo "Pausing to give the identifyOn pod time to start..."
sleep 10
identifyPodName=$(kubectl get pods | grep "${identifyOnContainerName}"  | tr -s " " | cut -d' ' -f1)
echo "identifyPodName: ${identifyPodName}"

podIsRunning=false
counter=0
while [[ $counter -lt 30 ]]; do
	echo the counter is $counter
	kubectl get pods
	identifyOnPodStatus=$(kubectl get pods | grep "${identifyOnContainerName}"  | tr -s " " | cut -d' ' -f3)
	echo "identifyOnPodStatus: ${identifyOnPodStatus}"
	if [ "${identifyOnPodStatus}" == "Running" ]; then
		echo "The identifyOn pod is ready"
		break
	else
		echo "The identifyOn pod is NOT ready"
	fi
	echo "Pausing to give the identifyOn pod time to start..."
	sleep 10
	count=$((count+1))
done
if [ "${identifyOnPodStatus}" != "Running" ]; then
	echo "identifyOn pod never started!"
	exit -1
fi
echo "identifyOnPod ${identifyPodName}, is running"

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

echo kubectl cp "${identifyPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
kubectl cp "${identifyPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"

ls "${outputDir}"

inspectOnImageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Docker image selected for target image inspection: ${inspectOnImageName}:${inspectOnImageTag}"

#################################################################
# Get inspectOn image / start and setup inspectOn container (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Starting container for target image inspection"
echo "--------------------------------------------------------------"
kubectl run "${inspectOnContainerName}" --image="${inspectOnImageName}:${inspectOnImageTag}" --command -- tail -f /dev/null
echo "Pausing to give the inspectOn pod time to start..."
sleep 10
inspectPodName=$(kubectl get pods | grep "${inspectOnContainerName}"  | tr -s " " | cut -d' ' -f1)
echo "inspectPodName: ${inspectPodName}"

podIsRunning=false
counter=0
while [[ $counter -lt 10 ]]; do
	echo the counter is $counter
	kubectl get pods
	inspectOnPodStatus=$(kubectl get pods | grep "${inspectOnContainerName}"  | tr -s " " | cut -d' ' -f3)
	echo "inspectOnPodStatus: ${inspectOnPodStatus}"
	if [ "${inspectOnPodStatus}" == "Running" ]; then
		echo "The inspectOn pod is ready"
		break
	else
		echo "The inspectOn pod is NOT ready"
	fi
	echo "Pausing to give the inspectOn pod time to start..."
	sleep 10
	counter=$((counter+1))
done
if [ "${inspectOnPodStatus}" != "Running" ]; then
	echo "inspectOn pod never started!"
	exit -1
fi
echo "inspectOnPod ${inspectPodName}, is running"

kubectl cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${inspectPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp "${targetImageDir}/${targetImageTarfile}" "${inspectPodName}:/opt/blackduck/hub-docker-inspector/target"

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
	
echo kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilePath="${outputDir}/${bdioFilename}"
echo kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"
kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"
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
echo "Pausing to give the uploadOn pod time to start..."
sleep 10
uploadPodName=$(kubectl get pods | grep "${uploadOnContainerName}"  | tr -s " " | cut -d' ' -f1)
echo "uploadPodName: ${uploadPodName}"

podIsRunning=false
counter=0
while [[ $counter -lt 10 ]]; do
	echo the counter is $counter
	kubectl get pods
	uploadOnPodStatus=$(kubectl get pods | grep "${uploadOnContainerName}"  | tr -s " " | cut -d' ' -f3)
	echo "uploadOnPodStatus: ${uploadOnPodStatus}"
	if [ "${uploadOnPodStatus}" == "Running" ]; then
		echo "The uploadOn pod is ready"
		break
	else
		echo "The uploadOn pod is NOT ready"
	fi
	echo "Pausing to give the uploadOn pod time to start..."
	sleep 10
	counter=$((counter+1))
done
if [ "${uploadOnPodStatus}" != "Running" ]; then
	echo "uploadOn pod never started!"
	exit -1
fi
echo "uploadOnPod ${uploadPodName}, is running"
kubectl cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${uploadPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp "${bdioFilePath}" "${uploadPodName}:/opt/blackduck/hub-docker-inspector/output"
kubectl exec -it "${uploadPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=false \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=true \
	--bdio.path="/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" \
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
