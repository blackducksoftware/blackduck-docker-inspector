## Deploying Docker Inspector

Black Duck Docker Inspector can be run in either of the following modes:

1. Host mode, on a Linux machine (or Linux VM) with Docker.
2. Container mode, inside a container running on an orchestration platform such as Kubernetes, OpenShift, etc.

Each mode is discussed in detail below.

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

### Using container mode in a container orchestration platform such as Kubernetes, OpenShift, etc.

In this scenario, Docker Inspector is a toolkit consisting of a command line utility (that you will run in one container), plus
three container-based services (which you must start). These four containers must:
(a) share a mounted volume (either persistent or temporary) that they will use to pass large files between containers, and
(b) be able to reach each other via HTTP GET operations using base URLs that you will provide.

Image Inspector Services

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

Deployment samples for commonly-used environments:

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


Environment			Approach	Deployment Notes				Sample Deployment (curl this URL)

Kubernetes			Toolkit		Separate pods; hostPath volume; service URLs	https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/kubernetes/setup.txt
OpenShift			Toolkit		Single pod; emptyDir volume; localhost URLs	https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/openshift/setup.txt
Travis CI			Toolkit		docker service; localhost URLs			https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/travisci/travis.yml
GitLab CI			Toolkit		shell executoer; localhost URLs			https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/gitlabci/setup.sh
Circle CI			Utility								https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/circleci/config.yml
Docker (Detect in container)	Toolkit		localhost URLs					https://raw.githubusercontent.com/blackducksoftware/blackduck-docker-inspector/master/deployment/docker/runDetectInContainer/setup.sh

### Running Detect on a project directory that exists within a Docker image

### Running the signature scanner on a specific directory within a Docker image

### Inspecting multiple images more efficiently (using host mode)
