#!/bin/bash
#
# This script provides a simple illustration of how you might run detect on a specific
# directory (say, a maven project) within an image, without running that image as
# a container.
#
# This script performs the following steps:
#
# 1. Uses the accompanying Dockerfile to create an image called mavenproject:1.
#    This represents an image that you want to inspect. Inside this image is a maven project: /home/my-app
# 2. Downloads detect
# 3. Runs detect (+ docker inspector) to generate the container filesystem .tar.gz file.
# 4. Untars the container filesystem
# 5. cd’s into the maven project /home/my-app
# 6. Runs detect there; the “BDIO Generated:” log messages shows where the results are written
# The generated .jsonld file can be uploaded to Black Duck. As an alternative, you could
# provide your Black Duck server details to detect, and detect will upload it for you.
#
mkdir image
cp Dockerfile image
cd image
docker build -t mavenproject:1 .
cd ..

curl -O https://blackducksoftware.github.io/hub-detect/hub-detect.sh
chmod +x hub-detect.sh
./hub-detect.sh --blackduck.offline.mode=true --detect.docker.image=mavenproject:1 --detect.docker.passthrough.output.path=/tmp/output --detect.tools.excluded=SIGNATURE_SCAN,POLARIS
rm -rf containerfilesystem
mkdir -p containerfilesystem
cd containerfilesystem
tar xvf /tmp/output/mavenproject_1_containerfilesystem.tar.gz

cd image_mavenproject_v_1/home/my-app
../../../../hub-detect.sh --blackduck.offline.mode=true --detect.tools.excluded=SIGNATURE_SCAN,POLARIS
