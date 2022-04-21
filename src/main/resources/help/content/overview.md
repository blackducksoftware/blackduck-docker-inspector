
_Help version: ${program_version}_

${solution_name} is invoked by ${detect_product_name} on a Docker image to:

1. Discover packages (components) installed in a Linux image by the Linuix package manager.
2. Provide to ${detect_product_name}, for any image, potentially useful targets for  signature and binary scanning.

${solution_name} does not run the target image, so it is safe to run it on untrusted images.

While earlier versions of ${solution_name} could be run standalone,
the only way to use ${solution_name} now and in the future is
to ${detect_product_name} on a Docker image. When you run ${detect_product_name}
on a Docker image, it will automatically run 
${solution_name}. See the ${detect_product_name} documentation for more information on
running ${detect_product_name}.

## Package (component) discovery

For package discovery on a Linux image, ${solution_name} extracts the Linux package manager
database from the image, and utilizes the appropriate Linux package manager to provide a list of
the installed packages, which
it returns to ${detect_product_name} in BDIO (Black Duck Input Output) format.
Because it relies on the Linux package manager as its source of this data,
the discovered packages are limited to those installed and managed using the Linux package manager.

${solution_name} can discover package manager-installed components in
Linux Docker images that use the DPKG, RPM, or APK package manager database formats.

## Signature and binary scan targets

Signature and binary scan targets contain the container file system.
The container file system
is the file system that a container created from the target image would start with. The
container file system is (by default) returned to ${detect_product_name} in two forms:
as an archive file that contains the container file system (the preferred format for binary
scanning), and as a saved squashed (single layer) image
that contains the container file system (the preferred format for signature scanning).

## Non-linux images

When run on a non-Linux image (for example, a Windows image,
or an image that contain no operating system), ${solution_name}
will return to ${detect_product_name} a BDIO file with zero components
along with the signature and binary scan targets.
Components may be discovered for these images
during the signature and/or binary scanning perfomed by
${detect_product_name}.

## Modes of operation

${solution_name} has two modes:

* Host mode, for running on a server or virtual machine (VM) where ${solution_name} can perform Docker operations using a Docker Engine.
* Container mode, for running inside a container started by Docker, Kubernetes, OpenShift, and others.

In either mode, ${solution_name} runs as a ${detect_product_name} inspector to extend the capaibilities of ${detect_product_name}.
${solution_name} is more complex than most ${detect_product_name} inspectors because it relies on container-based services
(the image inspector services)
to perform its job. When running on a host machine that has access to a Docker Engine ("host mode"),
${solution_name} can start and manage the image inspector services (containers) automatically.
When ${detect_product_name} and ${solution_name} are running within a Docker container
("container mode"), the image inspector services must be started and managed by the user or
the container orchestration system.

### Host mode

Host mode (the default) is for servers/VMs where ${solution_name} can perform Docker operations (such as pulling an image)
using a Docker Engine.

Host mode requires that ${solution_name} can access a Docker Engine. https://github.com/docker-java/docker-java utilizes the
[docker-java library](https://github.com/docker-java/docker-java) to act as a client of that Docker Engine.
This enables ${solution_name} to pull the target image from a Docker registry such
as Docker Hub. Alternatively, you can save an image to a .tar file by using the *docker save* command. Then, run ${solution_name}
on the .tar file. ${solution_name} supports Docker Image Specification v1.2.0 format .tar files.

In Host mode, ${solution_name} can also pull, run, stop, and remove the image inspector service images as needed,
greatly simplifying usage, and greatly increasing run time.

### Container mode

Container mode is for container orchestration environments (Kubernetes, OpenShift, etc.)
where ${detect_product_name} and ${solution_name} run
inside a container where ${solution_name} cannot perform Docker operations.
For information on running ${solution_name} in container mode,
refer to [Deploying](deployment.md).

It possible to utilize container mode when running ${detect_product_name} and ${solution_name} on a host
that supports host mode. Container mode is more difficult to manage than host mode,
but you might choose container mode in order to increase throughput (to scan more images per hour).
Most of the time spent by ${solution_name} running in host mode is spent starting and stopping the image inspector services.
When these services are already running (in the usual sense of the word "service")
as they do in container mode,
${solution_name} much more quickly than it would in host mode.

## Requirements

Requirements for including ${solution_name} in a ${detect_product_name} run
include of all of ${detect_product_name}'s requirements plus:

* Three available ports for the image inspector services. By default, these ports are 9000, 9001, and 9002.
* The environment must be configured so that files created by ${solution_name} are readable by all. On Linux, this means an appropriate umask value (for example, 002 or 022 would work). On Windows, this means the
Detect "output" directory (controlled by the ${detect_product_name} property *detect.output.path*)
must be readable by all.
* In host mode: access to a Docker Engine versions 17.09 or higher.
* In container mode: you must start the ${solution_name} container that meets the preceding requirements, and three container-based
"image inspector" services. All four of these containers must share a mounted volume and be able to reach each other through HTTP GET operations using base URLs
that you provide. For more information, refer to [Deploying](deployment.md).
    
## Running ${solution_name}

To invoke ${solution_name}, pass a docker image to 
${detect_product_name} via one of the following properties:

* detect.docker.image
* detect.docker.image.id
* detect.docker.image
* detect.docker.tar

See the ${detect_product_name} documentation for details.

## Advanced usage

The most common cases of ${solution_name} can be configured using 
${detect_product_name} properties. However, there are scenarios (including container mode)
that require access to ${solution_name} properties for which there is no corresponding
${detect_product_name} property. To set these 
${solution_name} properties, use the
${detect_product_name} detect.docker.passthrough property
(see the ${detect_product_name} documentation for details on how to use detect.docker.passthrough).
