## Overview

Black Duck Docker Inspector inspects Docker images to discover packages (components).
It utilizes the appropriate Linux package manager to provide a list of
the installed (by the package manager) packages, and creates a Black Duck project with a Bill of Materials (BOM) consisting of those packages as components.
Because it relies on the Linux package manager as its source, the discovered packages are limited to those installed and managed using the Linux package manager.

Black Duck Docker Inspector can inspect Linux Docker images that support dpkg, rpm, or apk package manager formats.

After running the Black Duck Docker Inspector on an image, navigate to Black Duck to view the Bill of Materials (BOM) created by 
Black Duck Docker Inspector.

### Modes of operation

Docker Inspector has two modes:

* Host mode, for running on a Linux machine (or VM) where Docker Inspector can perform Docker operations via a Docker engine
* Container mode, for running in a container (started by Docker, Kubernetes, OpenShift, etc.)

#### Host mode

Host mode (the default mode) is for Linux machines (and VMs) where Docker Inspector can perform Docker operations via a Docker engine.

In host mode, Black Duck Docker Inspector is a utility that automates the process of using Black Duck to discover security, license, and operational risks
associated with Linux-based Docker images. It discovers components using the target Docker image's package manager; therefore, the results
are limited to those components (packages) of which the package manager is aware. Black Duck Docker Inspector does this without running
the image, so it is safe to run on untrusted images.

Black Duck Docker Inspector can pull the target image; in other words, the Docker image you want to inspect, from a Docker registry such
as Docker Hub. Alternatively, you can save an image to a .tar file by using the Docker Save command. Then, run Black Duck Docker Inspector
on the .tar file. Docker Inspector supports Docker Image Specification v1.2.0 format .tar files.

#### Container mode

Container mode is for container orchestration environments (Kubernetes, OpenShift, etc.) where Docker Inspector will run
inside a container, where it cannot perform Docker operations. For information on running Docker Inspector in container mode,
refer to the *deployment* help topic.

### Requirements

Requirements for Black Duck Docker Inspector are:

* The current version of Black Duck. Visit [this page](${blackduck_release_page}) to determine the current version. 
* Linux.
* Access to the internet. For information on running without access to the internet, refer to Air Gap mode..
* curl.
* bash.
* Java (JRE) version 8 or 11.
* Three available ports for the image inspector services.  By default, these ports are 9000, 9001, and 9002.
* In host mode: Access to a Docker Engine (version 17.09 or higher).
* In container mode: You must start the Docker Inspector container that meets the requirements above, and three container-based
"image inspector" services. 
All four of these containers must share a mounted volume and be able to reach each other via HTTP GET operations using base URLs
that you provide.
    
### Getting started

The following command format will always fetch and run the latest version of Docker Inspector:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) {Docker Inspector arguments}

For example:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --help
    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --upload.bdio=false --docker.image=ubuntu

An alternative is to download and run the latest Docker Inspector script:

    curl -O  ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}
    chmod +x ${script_name}
    ./${script_name} {Docker Inspector arguments}

The advantage of using the Docker Inspector script is that it will ensure you always run the latest version of the Docker Inspector .jar.

Another alternative is to download the Docker Inspector .jar (using the script) and run the .jar directly:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --pulljar
    java -jar ${project_name}-{version}.jar {Docker Inspector arguments}

### Passing arguments to Docker Inspector

Usage: ${script_name} {Docker Inspector arguments}
Docker Inspector arguments consist of property assignments. 
Any supported property can be set by adding to the command line
a property assignment of the form:
	--{property name}={value}

Alternatively, any supported property can be set by adding to a text file named
application.properties (in the current directory) a line of the form:
{property name}={value}

There are other alternative methods for setting properties. For more information, refer to the *running* help topic.

### Help

Available help topics:
* overview (this page)
* architecture
* running
* properties
* advanced
* deployment
* troubleshooting
* releasenotes
* all

To display a help topic, run Docker Inspector with either -h or --help followed by a topic. For example:
    -h properties
    
To display multiple help topics, use a comma-separated list of help topics. For example:
    -h overview,properties,running

To display all help topics, use topic "all":
    -h all

To change the format of the help output to HTML, add --help.output.format=html:
    -h all --help.output.format=html

To write help to a file, add --help.output.path={directory or file path}:
    -h all --help.output.format=html --help.output.path=.
