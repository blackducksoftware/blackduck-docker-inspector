#!/bin/bash -v

minikube start
eval $(minikube docker-env)

targetImageName=alpine
targetImageTag=latest
outputDir=/tmp/aaa
containerName=mytestcontainer

rm -rf "${outputDir}"
rm -f /tmp/savedimage.tar

#################################################################
# Pull/save image (wrapper)
#################################################################
docker pull "${targetImageName}:${targetImageTag}"
docker save -o /tmp/savedimage.tar "${targetImageName}:${targetImageTag}"

#################################################################
# Determine runOn image (jar on host)
#################################################################
./build/hub-docker-inspector.sh --jar.path=build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar \
	--determine.run.on.image.only=true \
	--cleanup.working.dir=false \
	--logging.level.com.blackducksoftware=INFO \
	--output.path="${outputDir}" \
	--output.include.dockertarfile=true \
	--docker.tar=/tmp/savedimage.tar

imageName=$(fgrep runOnImageName "${outputDir}/result.json" | cut -d'"' -f4)
imageTag=$(fgrep runOnImageTag "${outputDir}/result.json" | cut -d'"' -f4)
dockerTarfilename=$(fgrep dockerTarfilename "${outputDir}/result.json" | cut -d'"' -f4)
bdioFilename=$(fgrep bdioFilename "${outputDir}/result.json" | cut -d'"' -f4)

echo "Running on: ${imageName}:${imageTag}"

#################################################################
# Get runOn image / start and setup runOn container (wrapper)
#################################################################
kubectl run "${containerName}" --image="${imageName}:${imageTag}" --command -- tail -f /dev/null
echo "Pausing to give the pod time to start..."
sleep 10
podName=$(kubectl get pods | grep "${containerName}"  | tr -s " " | cut -d' ' -f1)
echo "podName: ${podName}"

podIsRunning=false
counter=0
while [[ $count -lt 10 ]]; do
	echo the counter is $counter
	kubectl get pods
	podStatus=$(kubectl get pods | grep "${containerName}"  | tr -s " " | cut -d' ' -f3)
	echo "podStatus: ${podStatus}"
	if [ "${podStatus}" == "Running" ]; then
		echo "The pod is ready"
		break
	else
		echo "The pod is NOT ready"
	fi
	echo "Pausing to give the pod time to start..."
	sleep 10
	count=$((count+1))
done

kubectl cp build/libs/hub-docker-inspector-5.0.0-SNAPSHOT.jar "${podName}:/opt/blackduck/hub-docker-inspector"
kubectl cp "${outputDir}/${dockerTarfilename}" "${podName}:/opt/blackduck/hub-docker-inspector/target"

#################################################################
# Inspect (jar in container)
#################################################################
kubectl exec -it "${podName}" -- java -Dfile.encoding=UTF-8 -jar /opt/blackduck/hub-docker-inspector/hub-docker-inspector-5.0.0-SNAPSHOT.jar --on.host=false "--docker.tar=/opt/blackduck/hub-docker-inspector/target/${dockerTarfilename}"

kubectl cp "${podName}:/opt/blackduck/hub-docker-inspector/output/${bdioFilename}" "${outputDir}"

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
