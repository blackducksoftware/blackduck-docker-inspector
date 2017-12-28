#!/bin/bash

targetImageName=alpine
targetImageTag=latest


targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
inspectOnContainerName=hub-docker-inspector-inspect
identifyOnContainerName=hub-docker-inspector-identify
identifyOnImageName=blackducksoftware/hub-docker-inspector-alpine
identifyOnImageTag=4.1.0

minikube start
eval $(minikube docker-env)

rm -rf "${outputDir}"
mkdir "${outputDir}"
rm -f "${targetImageDir}/${targetImageTarfile}"

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

kubectl cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${identifyPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp "${targetImageDir}/${targetImageTarfile}" "${identifyPodName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Determine inspectOn image (jar on identifyOnContainer)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Identifying target image package manager"
echo "--------------------------------------------------------------"
kubectl exec -it "${identifyPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=true \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=false \
	--logging.level.com.blackducksoftware=INFO \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

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
kubectl exec -it "${inspectPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=false \
	--inspect=true \
	--inspect.in.container=false \
	--upload.bdio=false \
	"--docker.tar=/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

ls "${outputDir}"

#################################################################
# Upload BDIO (jar on host)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Uploading BDIO file (BOM) to Hub"
echo "--------------------------------------------------------------"
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--identify.pkg.mgr=false \
	--inspect=false \
	--inspect.in.container=false \
	--upload.bdio=true \
	--bdio.path="${outputDir}" \
	--logging.level.com.blackducksoftware=INFO \
	--output.path="${outputDir}" \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


#################################################################
# Clean up minikube VM (wrapper)
#################################################################
echo "--------------------------------------------------------------"
echo "kube_wrapper.sh: Stopping/removing minikube"
echo "--------------------------------------------------------------"
minikube stop
minikube delete
