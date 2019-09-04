## Deploying Docker Inspector

### Deployment table of contents

* [Deployment overview](#deploymentoverview)
* [Using host mode on Linux machine (or Linux VM) with Docker](#deploymenthostmode)
* [Using container mode in a container orchestration platform such as Kubernetes, OpenShift, etc.](#deploymentcontainermode)
* [Deployment samples for commonly-used environments](#deploymentsamples)
* [Other deployment tips](#deploymentother)

<a name="deploymentoverview"></a>
### Deployment overview

Black Duck Docker Inspector can be run in either of the following modes:

1. Host mode, on a Linux machine (or Linux VM) with Docker.
2. Container mode, inside a container running on an orchestration platform such as Kubernetes, OpenShift, etc.

Each mode is discussed in detail below.

<a name="deploymenthostmode"></a>
### Using host mode on Linux machine (or Linux VM) with Docker

In this scenario, Docker Inspector is a command line utility that automatically pulls/runs and uses container-based services 
(and cleans them up when it's done). The Docker command, if installed on the machine, can be very useful for troubleshooting, but is not actually
required or used by Docker Inspector.

In this mode Docker Inspector does require access to a Docker Engine (very similar to the way the Docker client requires
access to a Docker Engine) so it can pull and run Docker images (it uses the https://github.com/docker-java/docker-java
library to perform Docker operations via the Docker Engine).

In this mode, Docker Inspector automatically pulls, runs, stops, and removes the container-based image inspector services
on which it depends. It accesses the services they provide via HTTP GET operations.

This is the default mode, and the simplest to use.

The documentation under Package Managers > Black Duck Docker Inspector at: https://synopsys.atlassian.net/wiki/spaces/INTDOCS
provides all the information that is normally required to run Docker Inspector in this mode.

<a name="deploymentcontainermode"></a>
### Using container mode in a container orchestration platform such as Kubernetes, OpenShift, etc.

In this scenario, Docker Inspector is a toolkit consisting of a command line utility (that you will run in one container), plus
three container-based services (which you must start). These four containers must:
(a) share a mounted volume (either persistent or temporary) that they will use to pass large files between containers, and
(b) be able to reach each other via HTTP GET operations using base URLs that you will provide.

#### Image Inspector Services

Docker Inspector consists of a command line utility (provided in a Java .jar, but sometimes invoked via a bash script)
and three image inspector services.

The required Docker operations (if any) are performed by the command line utility, while the image inspector services
perform the work of unpacking the target Docker image, extracting the Linux package manager database,
and running the Linux package manager against that database in order to extract installed packages
and translate them to components (actually externalIds) for Black Duck. If the image inspector service
finds in the target image a package manager database that is incompatible with its own package manager utility
(this happens when, for example, you run Docker Inspector on an Alpine image, but the request goes to the
Ubuntu image inspector service), the image inspector service will redirect the request to the appropriate
image inspector service. You can change the default image inspector service to reduce the likelihood
of redirects (resulting in shorter execution times). For example, if most of your target images are Alpine
you can set imageinspector.service.distro.default to alpine.

The image inspector service containers are downloaded from Docker Hub (blackducksoftware/blackduck-imageinspector-*).

<a name="deploymentsamples"></a>
### Deployment samples for commonly-used environments

Your deployment approach will be the same whether you are invoking Docker Inspector directly, or invoking it via Detect.
Most of the sample deployments use Detect simply because that is the most common use case.

Each sample deployment follows one of the two approaches described above, and are labelled accordingly below:
1. Utility (#1 above) (= Command Line Utility)
2. Toolkit (#2 above)

The challenges involved in deploying Docker Inspector using the 'toolkit' approach are:
1. Starting the four containers (one for Detect / Docker Inspector, plus three image inspector containers) such that they all share a common mounted volume
2. Ensuring that the containers can reach each other via HTTP GET operations using base URLs that your provide.

These deployment samples are intended to show how these challenges could be met. They are not intended to be used as-is in production.
You should understand the code before you use it. They do not represent the only way to deploy in each environment.

#### Kubernetes

Approach: Toolkit

Deployment notes: Each image inspector service runs in a separate pod.
The shared volume is a hostPath volume. The containers communicate via service URLs.

Download: curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/kubernetes/setup.txt

#### OpenShift

Approach: Toolkit

Deployment notes: All image inspector services run in a single pod. The shared volume is an emptyDir volume.
The containers communicate via localhost URLs.

Download: curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/openshift/setup.txt

#### Travis CI

Approach: Toolkit

Deployment notes: Uses the Travis CI docker service.
The containers communicate via localhost URLs.

Download: curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/travisci/travis.yml

#### GitLab CI

Approach: Toolkit

Deployment notes: Uses the GitLab CI shell executor.
The containers communicate via localhost URLs.

Download: curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/gitlabci/setup.sh

#### Circle CI

Approach: Utility

Deployment notes: 

Download: curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/circleci/config.yml

#### Docker (Detect running in container)

Approach: Toolkit

Deployment notes: The containers communicate via localhost URLs.

Download: https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/runDetectInContainer/setup.sh			

<a name="deploymentother"></a>
### Other deployment tips

#### Configuring Docker Inspector for your Docker Registry

If you invoke Docker Inspector with an image reference (vs. an image that has been saved to a .tar file),
it uses the docker-java library (https://github.com/docker-java/docker-java) to access the Docker registry
to pull the image. 

If “docker pull <targetimage>” works from the command line, then docker inspector should also be able
to pull that image, because docker-java can be configured the same way as the docker command line utility. 

But there are also other ways to configure docker-java. Details on how to configure docker-java
(and therefore Docker Inspector) for your Docker registry can
be found at: https://github.com/docker-java/docker-java#Configuration.

Docker Inspector does not override any of the configuration settings in the code,
so any of the other methods (properties, system properties, system environment) should work.

If you choose to use environment variables, and you are calling Docker Inspector from Detect,
you will need to prefix the environment variable names with "DETECT_DOCKER_PASSTHROUGH_" to
instruct detect to pass them on to Docker inspector.
So in that scenario, instead of "export SOMENAME=value", use "export DETECT_DOCKER_PASSTHROUGH_SOMENAME=value".

If you choose to use system properties (normally set using java -D),
and you are calling Docker Inspector from Detect, you will need to
put the properties in a file (e.g. mydockerproperties.properties) and use 
```
--detect.docker.passthrough.system.properties.path=mydockerproperties.properties
```
to point Docker Inspector to those property settings.


#### Running Detect on a project directory that exists within a Docker image

When you want to run Detect on a directory that exists within a docker image, you can use the following approach:
1. Run Detect on the image to generate the container filesystem for the image.
2. Run Detect on a directory within that container filesystem.

Detect performs these actions without running the image/container.

To see a simple example that illustrates this approach, use the following commands to download these sample files:
```
curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/runDetectInImageDir/runDetectInImageDir.sh
curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/runDetectInImageDir/Dockerfile
```

Please review the script before running it to make sure the side effects
(files and directories that it creates) are OK.
You'll need to make the script executable before you run it. 

#### Running the signature scanner on a specific directory within a Docker image

If you want to scan (with iScan) a specific directory within an image,
here at a very high level is how it could be done:
 
1. Run docker inspector on the target image to get the container file system.
You could also do this using detect using `--detect.docker.passthrough.*` properties.
Include the following Docker Inspector properties:
```
--upload.bdio=false                        # disable BDIO upload
--output.include.containerfilesystem=true  # tell DI to output the container file system
--output.path=<your output dir>            # tell DI where to put the output
```
2. Locate the container file system in the output dir (*.tar.gz) and untar it
3. cd into the directory (within the untar’d container file system) that you want to scan.
4. Invoke detect there.

#### Running Docker Inspector on an Open Container Initiative (OCI) image

When given a docker image (--docker.image=repo:tag), Docker Inspector uses
the [docker-java library](https://github.com/docker-java/docker-java)
equivalent of [docker save](https://docs.docker.com/engine/reference/commandline/save/)
to save the image to a tar file. In this scenario, Docker Inspector should be able to pull,
save, and inspect any image that could be pulled using a "docker pull" command.
(Since Docker Inspector uses the docker-java library, the docker client executable
does not actually need to be installed on the machine).

When given a saved docker tarfile (--docker.tar=image.tar), Docker Inspector
requires a [Docker Image Specification v1.2.0](https://github.com/moby/moby/blob/master/image/spec/v1.2.md)
format file. To inspect [Open Container Initiative (OCI)](https://www.opencontainers.org/)
format image files, we recommend using [skopeo](https://github.com/containers/skopeo)
to convert them to Docker Image Specification v1.2.0 files. For example:
```
skopeo copy oci:alpine-oci docker-archive:alpine-docker.tar
```
will convert an OCI image directory alpine-oci to a Docker Image Specification v1.2.0 format file alpine-docker.tar that Docker Inspector can process when passed in with the --docker.tar=alpine-docker.tar command line argument.

#### Inspecting multiple images more efficiently (using host mode)

By default, docker inspector will start, use, and then stop/remove either one or two containerized
image inspector services per run (per target image inspected). This may be appropriate when scanning
a single image, but when scanning many images, it is highly inefficient. 

The scanning of many images can be completed significantly faster by starting the image inspector services
once, and running multiple instances of docker inspector so that each one sends requests to the already-running
image inspector services.

The following script illustrates how this could be done in a Docker environment:
```
curl -O https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/batchedImageInspection.sh
```

To keep the example simple, this script only starts the alpine image inspector service.
In general, you will likely also need to start two more services: the ubuntu image inspector service
(for inspecting images built from dpkg-based linux distros), and the centos image inspector service
(for inspecting images built from rpm-based linux distros). It doesn't matter which service receives
the request; any service will redirect if necessary.