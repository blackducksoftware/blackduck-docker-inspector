# Overview

## Host mode

Host mode (the default mode) is designed for Linux machines (and VMs) where it can perform Docker operations via a Docker engine.

In host mode, Black Duck Docker Inspector is a utility that automates the process of using Black Duck to discover security, license, and operational risks
associated with Linux-based Docker images. It discovers components using the target Docker image's package manager; therefore, the results
are limited to those components (packages) of which the package manager is aware. Black Duck Docker Inspector does this without running
the image, so it is safe to run on untrusted images.

Black Duck Docker Inspector can pull the target image; in other words, the Docker image you want to inspect, from a Docker registry such
as Docker Hub. Alternatively, you can save an image to a .tar file by using the Docker Save command. Then, run Black Duck Docker Inspector
on the .tar file. Docker Inspector supports Docker Image Specification v1.2.0 format .tar files.

In either scenario, Black Duck Docker Inspector utilizes the appropriate standard Linux package manager (dpkg, rpm, and apk) to discover
the installed open source packages, and create a Black Duck project with a Bill of Materials (BOM) consisting of those packages as components.
Therefore the discovered packages are limited to those installed and managed using the Linux package manager.

Black Duck Docker Inspector can inspect Docker images that support dpkg, rpm, or apk package manager formats.

After running the Black Duck Docker Inspector, navigate to Black Duck to view the Bill of Materials (BOM) created by 
Black Duck Docker Inspector.

Black Duck Docker Inspector can:

* Inspect any Linux Docker image using its package manager (dpkg, rpm, or apk).
* Produce a Black Duck Bill of Materials based on the output without running the image.

## Container mode

Container mode is designed for container orchestration environments (Kubernetes, OpenShift, etc.) where Docker Inspector will run
inside a container, where it cannot perform Docker operations. For information on container mode, refer to the deployment help topic.

# Requirements

Docker Inspector can run in a variety of container environments such as Docker, Kubernetes, and OpenShift, but requires different
setup and configuration for different environments. By default, Docker Inspector expects that it is running on a Linux server or
virtual machine (not a container), that is configured for and has access to a Docker engine that it uses to pull images, start
containers, and other activities. For information on running Docker Inspector in other environments; for example, Kubernetes,
OpenShift, within a container, and others, refer to the deployment help topic.

Requirements for Black Duck Docker Inspector are:

* The current version of Black Duck. Visit [this page](https://github.com/blackducksoftware/hub/releases) to determine the current version. 
* Linux.
* Access to the internet. For information on running without access to the internet, refer to Air Gap mode..
* Either one of the following:
    * Host mode: Access to a Docker Engine (version 17.09 or higher).
    * Container mode: A container application environment as described in the GitHub wiki.
* The curl utility.
* Bash.
* Java version 8.  Other versions of Java are not currently supported.
* Three available ports for the image inspector services.  By default, these ports are 9000, 9001, and 9002.
* The blackduck-docker-inspector.sh script, downloadable at https://github.com/blackducksoftware/blackduck-docker-inspector/tree/gh-pages You can download the latest version of blackduck-docker-inspector.sh using the following commands:

        curl -O  https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh
        chmod +x blackduck-docker-inspector.sh

# Getting started

The following command format will always run the latest version of Docker Inspector:

    bash <(curl -s https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh) <Docker Inspector arguments>

For example:

    bash <(curl -s https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh) --upload.bdio=false --docker.image=ubuntu

An alternative is to download and run the latest Docker Inspector script:

    curl -O  https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh
    chmod +x blackduck-docker-inspector.sh
    ./blackduck-docker-inspector.sh <Docker Inspector arguments>

The advantage of using the Docker Inspector script is that it will ensure you always run the latest version of the Docker Inspector .jar.

Another alternative is to download the Docker Inspector .jar (using the script) and run the .jar directly:

    curl -O  https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh
    chmod +x blackduck-docker-inspector.sh
    ./blackduck-docker-inspector.sh --pulljar
    java -jar blackduck-docker-inspector-8.1.3.jar <Docker Inspector arguments>

# Usage

Usage: blackduck-docker-inspector.sh <Docker Inspector arguments>
Docker Inspector arguments: any supported property can be set by adding to the command line
a property assignment of the form:
	--<property name>=<value>

Alternatively, any supported property can be set by adding to a text file named
application.properties (in the current directory) a line of the form:
<property name>=<value>

For greater security, the Black Duck password can be set via the environment variable BD_PASSWORD.
For example:
  export BD_PASSWORD=mypassword
  ./blackduck-docker-inspector.sh --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername --docker.image=ubuntu:latest

Other help topics available:
* running
* properties
* architecture
* advanced
* deployment

To display a help topic, run Docker Inspector with:
    -h <help topic>