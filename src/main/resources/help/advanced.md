## Advanced topics

### Isolation application components

If you are interested in components from the application layers of your image, but not interested in components
from the underlying platform layers, you can exclude components from platform layers from the results.

For example, if you build your application on ubuntu:latest (your Dockerfile starts
with FROM ubuntu:latest), you can exclude components from the ubuntu layer(s) so that
your Black Duck Bill Of Materials contains only components from your application layers.

First, find the layer ID of the platform's top layer. To do this:

1. Run the *docker inspect* command on the base image (in our example this is ubuntu:latest).
1. Find the last element in the RootFS.Layers array. This is the platform top layer ID. In the following example, this is 
sha256:b079b3fa8d1b4b30a71a6e81763ed3da1327abaf0680ed3ed9f00ad1d5de5e7c.

Set the value of the Docker Inspector property docker.platform.top.layer.id to the platform top layer ID.
For example:

./blackduck-docker-inspector.sh ... --docker.platform.top.layer.id=sha256:b079b3fa8d1b4b30a71a6e81763ed3da1327abaf0680ed3ed9f00ad1d5de5e7c

### Concurrent execution

You can inspect multiple images in parallel on the same computer when you directly invoke the .jar file. For example:

    # Get the latest blackduck-docker-inspector.sh
    curl -O  https://blackducksoftware.github.io/blackduck-docker-inspector/blackduck-docker-inspector.sh
    chmod +x ./blackduck-docker-inspector.sh
 
    # Determine the current Black Duck Docker Inspector version
    inspectorVersion=$(grep "^version=" blackduck-docker-inspector.sh|cut -d'"' -f2)
 
    # Download the latest Black Duck Docker Inspector .jar file to the current dir
    ./blackduck-docker-inspector.sh --pulljar
 
    # Execute multiple inspections in parallel
    java -jar ./blackduck-docker-inspector-${inspectorVersion}.jar --blackduck.url=<Black Duck url> --blackduck.username=<Black Duck username> --docker.image=alpine:3.5  &
    java -jar ./blackduck-docker-inspector-${inspectorVersion}.jar --blackduck.url=<Black Duck url> --blackduck.username=<Black Duck username> --docker.image=alpine:3.4  &
    java -jar ./blackduck-docker-inspector-${inspectorVersion}.jar --blackduck.url=<Black Duck url> --blackduck.username=<Black Duck username> --docker.image=alpine:3.3  &

### Alternative methods for setting property values

Docker Inspector gets its property values from
[Spring Boot's configuration mechanism](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).
Docker Inspector users can leverage Spring Boot capabilities beyond command line arguments
and environment variables (for example: hierarchy of property files, and placeholders)
to manage properties in more sophisticated ways.

### Passing passwords (etc.) in a more secure way

For greater security, sensitive property values such as passwords can be set via the environment variables
using one of the Spring Boot configuration mechanisms mentioned above.

For example, instead of passing --blackduck.password=mypassword on the command line, you can do the following:
  export BLACKDUCK_PASSWORD=mypassword
  ./blackduck-docker-inspector.sh --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername --docker.image=ubuntu:latest

Refer to [Spring Boot's configuration mechanism](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
for more information on using this capability.

### Air Gap mode

In Black Duck Docker Inspector versions 6.2.0 and higher, Black Duck provides an archive containing all files needed
to run Black Duck Docker Inspector without access to the internet. To download the Air Gap archive, run the command:

    ./blackduck-docker-inspector.sh --pullairgapzip
    
To create the Docker images from the Air Gap archive required by Black Duck Docker Inspector, run the commands:

    unzip blackduck-docker-inspector-<version>-air-gap.zip
    docker load -i blackduck-imageinspector-ws-alpine.tar
    docker load -i blackduck-imageinspector-ws-centos.tar
    docker load -i blackduck-imageinspector-ws-ubuntu.tar
    
To run in Air Gap mode, use the command:

    ./blackduck-docker-inspector.sh --upload.bdio=false --jar.path=./blackduck-docker-inspector<version>.jar --docker.tar=<tarfile>

### Configuring Docker Inspector for your Docker Engine and Registry

If you invoke Docker Inspector with an image reference (a repo:tag value vs. a .tar file),
it uses the docker-java library (https://github.com/docker-java/docker-java) to access the Docker registry to pull the image.

If “docker pull ” works from the command line, then docker inspector should also be able to pull that image,
because docker-java can be configured the same way as the docker command line utility.

But there are also other ways to configure docker-java. Details on how to configure docker-java
(and therefore Docker Inspector) for your Docker registry can be found at:
https://github.com/docker-java/docker-java#Configuration.

Docker Inspector does not override any of the configuration settings in the code,
so all of the other methods (properties, system properties, system environment) are available to you.

If you choose to use environment variables, and you are calling Docker Inspector from Detect,
you will need to prefix the environment variable names with "DETECT_DOCKER_PASSTHROUGH_" to
instruct detect to pass them on to Docker inspector. So in that scenario,
instead of "export SOMENAME=value", use "export DETECT_DOCKER_PASSTHROUGH_SOMENAME=value".

If you choose to use system properties (normally set using java -D),
and you are calling Docker Inspector from Detect, you will need to put the properties
in a file (e.g. mydockerproperties.properties) and use

    --detect.docker.passthrough.system.properties.path=mydockerproperties.properties

to point Docker Inspector to those property settings.

### Running Docker Inspector on Open Container Initiative (OCI) images

When given a docker image (--docker.image=repo:tag), Docker Inspector uses the
[docker-java library](https://github.com/docker-java/docker-java)
equivalent of [docker save](https://docs.docker.com/engine/reference/commandline/save/)
to save the image to a tar file. In this scenario, Docker Inspector
should be able to pull, save, and inspect any image that could be pulled using a "docker pull" command.
(Since Docker Inspector uses the docker-java library, the docker client executable does not actually need
to be installed on the machine).

When given a saved docker tarfile (--docker.tar=image.tar), Docker Inspector requires a
[Docker Image Specification v1.2.0](https://github.com/moby/moby/blob/master/image/spec/v1.2.md)
format file. To inspect [Open Container Initiative (OCI)](https://www.opencontainers.org/)
format image files, we recommend using [skopeo](https://github.com/containers/skopeo)
to convert them to Docker Image Specification v1.2.0 files. For example:

    skopeo copy oci:alpine-oci docker-archive:alpine-docker.tar
    
will convert an OCI image directory alpine-oci to a Docker Image Specification v1.2.0 format file
alpine-docker.tar that Docker Inspector can process when passed in with the
--docker.tar=alpine-docker.tar command line argument.
