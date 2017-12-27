#!/bin/bash -v

targetImageName=alpine
targetImageTag=latest


targetImageDir=/tmp
targetImageTarfile=savedimage.tar
outputDir=/tmp/hub-docker-inspector-output
inspectOnContainerName=hub-docker-inspector-inspect
peekOnContainerName=hub-docker-inspector-peek
peekOnImageName=blackducksoftware/hub-docker-inspector-alpine
peekOnImageTag=4.1.0

minikube start
eval $(minikube docker-env)

rm -rf "${outputDir}"
mkdir "${outputDir}"
rm -f "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Pull/save target image (wrapper)
#################################################################
docker pull "${targetImageName}:${targetImageTag}"
docker save -o "${targetImageDir}/${targetImageTarfile}" "${targetImageName}:${targetImageTag}"
chmod a+r "${targetImageDir}/${targetImageTarfile}"

#################################################################
# Get peekOn image / start and setup peekOn container (wrapper)
#################################################################
kubectl run "${peekOnContainerName}" --image="${peekOnImageName}:${peekOnImageTag}" --command -- tail -f /dev/null
echo "Pausing to give the peekOn pod time to start..."
sleep 10
peekPodName=$(kubectl get pods | grep "${peekOnContainerName}"  | tr -s " " | cut -d' ' -f1)
echo "peekPodName: ${peekPodName}"

podIsRunning=false
counter=0
while [[ $counter -lt 30 ]]; do
	echo the counter is $counter
	kubectl get pods
	peekOnPodStatus=$(kubectl get pods | grep "${peekOnContainerName}"  | tr -s " " | cut -d' ' -f3)
	echo "peekOnPodStatus: ${peekOnPodStatus}"
	if [ "${peekOnPodStatus}" == "Running" ]; then
		echo "The peekOn pod is ready"
		break
	else
		echo "The peekOn pod is NOT ready"
	fi
	echo "Pausing to give the peekOn pod time to start..."
	sleep 10
	count=$((count+1))
done
if [ "${peekOnPodStatus}" != "Running" ]; then
	echo "peekOn pod never started!"
	exit -1
fi
echo "peekOnPod ${peekPodName}, is running"

kubectl cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${peekPodName}:/opt/blackduck/hub-docker-inspector"
kubectl cp "${targetImageDir}/${targetImageTarfile}" "${peekPodName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Determine inspectOn image (jar on peekOnContainer)
#################################################################
kubectl exec -it "${peekPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	--determine.run.on.image.only=true \
	--logging.level.com.blackducksoftware=INFO \
	--docker.tar="/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

kubectl cp "${peekPodName}:/opt/blackduck/hub-docker-inspector/output/result.json" "${outputDir}"

ls "${outputDir}"

inspectOnImageName=$(fgrep inspectOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
inspectOnImageTag=$(fgrep inspectOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Running on: ${inspectOnImageName}:${inspectOnImageTag}"

#################################################################
# Get inspectOn image / start and setup inspectOn container (wrapper)
#################################################################
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
kubectl exec -it "${inspectPodName}" -- \
	java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--on.host=false \
	"--docker.tar=/opt/blackduck/hub-docker-inspector/target/${targetImageTarfile}"

kubectl cp "${inspectPodName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

ls "${outputDir}"

#################################################################
# Upload BDIO (jar on host)
#################################################################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--upload.bdio.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--output.path="${outputDir}" \
	--hub.url=https://int-hub02.dc1.lan \
	--hub.username=sysadmin \
	--hub.password=blackduck


#################################################################
# Clean up minikube VM (wrapper)
#################################################################
minikube stop
minikube delete
