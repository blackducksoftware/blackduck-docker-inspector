## Deploying Docker Inspector

### Deployment overview

Black Duck Docker Inspector can be run in either of the following modes:

1. Host mode, on a Linux machine (or Linux VM) with Docker.
2. Container mode, inside a container running on an orchestration platform such as Kubernetes, OpenShift, etc.

Each mode is discussed in detail below.

### Important notes regarding deployment sample code

The deployment samples provided are intended to possible approaches to the challenges
involved in deploying Docker Inspector. They are not intended to be used as-is in production.
You should understand the code before you use it. They do not represent the only way to deploy in each environment.

Your deployment approach will be the same whether you are invoking Docker Inspector directly, or invoking it via Detect.
Most of the sample deployments use Detect simply because that is the most common use case.

### Using host mode on Linux machine (or Linux VM) with Docker

In this scenario, Docker Inspector is a command line utility that automatically pulls/runs and uses container-based services 
(and cleans them up when it's done). The Docker command, if installed on the machine, can be very useful for troubleshooting, but is not actually
required or used by Docker Inspector.

In this mode Docker Inspector does require access to a Docker Engine (very similar to the way the Docker client requires
access to a Docker Engine) so it can pull and run Docker images (it uses the ${docker_java_project_url}
library to perform Docker operations via the Docker Engine).

In this mode, Docker Inspector automatically pulls, runs, stops, and removes the container-based image inspector services
on which it depends. It accesses the services they provide via HTTP GET operations.

This is the default mode, and the simplest to use.

### Using container mode in a container orchestration platform such as Kubernetes, OpenShift, etc.

In this scenario, Docker Inspector is a toolkit consisting of a command line utility (that you will run in one container), plus
three container-based services (which you must start). These four containers must:
(a) share a mounted volume (either persistent or temporary) that they will use to pass large files between containers, and
(b) be able to reach each other via HTTP GET operations using base URLs that you will provide.

### Image Inspector Services

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

The image inspector service containers are downloaded from Docker Hub (${image_repo_organization}/${inspector_image_name_base}-*).

### Deployment sample for Kubernetes

Approach: Toolkit

Deployment notes: Each image inspector service runs in a separate pod.
The shared volume is a hostPath volume. The containers communicate via service URLs.

Download: curl -O ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/kubernetes/setup.txt

### Deployment sample for OpenShift

Approach: Toolkit

Deployment notes: All image inspector services run in a single pod. The shared volume is an emptyDir volume.
The containers communicate via localhost URLs.

Download: curl -O ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/openshift/setup.txt

### Deployment sample for Travis CI

Approach: Toolkit

Deployment notes: Uses the Travis CI docker service.
The containers communicate via localhost URLs.

Download: curl -O ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/travisci/travis.yml

### Deployment sample for GitLab CI

Approach: Toolkit

Deployment notes: Uses the GitLab CI shell executor.
The containers communicate via localhost URLs.

Download: curl -O ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/gitlabci/setup.sh

### Deployment sample for Circle CI

Approach: Utility

Deployment notes: 

Download: curl -O ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/circleci/config.yml

### Deployment sample for Docker, with Detect running in container

Approach: Toolkit

Deployment notes: The containers communicate via localhost URLs.

Download: ${source_raw_content_url_base}/${source_repo_organization}/${project_name}/master/deployment/docker/runDetectInContainer/setup.sh			

### Configuring Docker Inspector for your Docker Registry

If you invoke Docker Inspector with an image reference (vs. an image that has been saved to a .tar file),
it uses the docker-java library (${docker_java_project_url}) to access the Docker registry
to pull the image. 

If “docker pull <targetimage>” works from the command line, then docker inspector should also be able
to pull that image, because docker-java can be configured the same way as the docker command line utility. 

But there are also other ways to configure docker-java. Details on how to configure docker-java
(and therefore Docker Inspector) for your Docker registry can
be found at: ${docker_java_project_url}#Configuration.

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
