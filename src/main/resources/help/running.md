# Running Docker Inspector

## Running a specific version

By default, blackduck-docker-inspector.sh runs the latest version of
Docker Inspector (by downloading, if necessary, and running the latest Docker Inspector .jar).
To run a specific version of Docker Inspector instead:

    export DOCKER_INSPECTOR_VERSION=<version>
    ./blackduck-docker-inspector.sh <Docker Inspector arguments>

For example:

    export DOCKER_INSPECTOR_VERSION=8.1.0
    ./blackduck-docker-inspector.sh --upload.bdio=false --docker.image=ubuntu:latest

## Running the .jar file

The advantage of running blackduck-docker-inspector.jar is that it ensures you alway run the latest
version of Docker Inspector. However, sometimes it is better to run the .jar directly.

You can download any version of the Docker Inspector .jar from https://sig-repo.synopsys.com/webapp/#/artifacts/browse/tree/General/bds-integrations-release/com/synopsys/integration/blackduck-docker-inspector.

Use the java command to run it:

    java -jar blackduck-docker-inspector-<version>.jar <Docker Inspector arguments>

## Inspecting an image by image repo:tag

To run Docker Inspector on Docker image from your local cache or a registry:

    ./blackduck-docker-inspector.sh --docker.image=<repo>:<tag>

If you omit the :<tag>, it will default to :latest.

## Inspecting an image saved to a .tar file

To run Docker Inspector on a Docker image .tar file:

    docker save -o <name>.tar <repo>:<tag>
    ./blackduck-docker-inspector.sh --docker.tar=<name>.tar
    
If your tar file contains multiple images, Docker Inspector can only inspect one of them.
You can specify which image you want to inspect using --docker.image.repo and --docker.image.tag. For example, to select ubuntu:latest
from a .tar file that contains ubuntu:latest and other images:

    ./blackduck-docker-inspector.sh --docker.tar=multipleimages.tar --docker.image.repo=ubuntu --docker.image.tag=latest