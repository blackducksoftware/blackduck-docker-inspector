# Architecture

Docker Inspector uses up to three container-based image inspector services (one for each of the supported Linux package manager database formats).

In host mode (the default), Docker Inspector automatically uses the Docker engine to pull (from Docker Hub) blackducksoftware/blackduck-imageinspector-alpine, 
blackducksoftware/blackduck-imageinspector-centos, and blackducksoftware/blackduck-imageinspector-ubuntu), starts those services as needed,
and stops and removes the containers when Docker Inspector exits. It uses a shared volume to share files, such as the target Docker image,
between the Docker Inspector utility and the three service containers.

In container mode, you will start the container running Docker Inspector and the three image inspector container-based services such that
all four containers share a mounted volume and can communicate with each other via HTTP GET operations using base URLs that you will provide. For information on how to do this, refer to the *deployment* help topic.

## Execution phases

### Host mode

In host mode, Docker Inspector performs the following steps on the host:

1. Pulls and saves the target image to a .tar file (if you passed they image by repo:tag).
1. Checks to see if the default image inspector service is running. If not, it pulls the image and starts a container, mounting a shared volume.
1. Requests the Black Duck input/output (BDIO) file and container file system using HTTP from the image inspector service.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container would have if you ran the target image. (It does not run the target image because it does not trust it.)
1. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
1. Runs the image inspector's Linux package manager on the target image package manager database.
1. Produces and returns a BDIO (.jsonld) file consisting of a list of target image packages and optionally, the container filesystem.

The following steps are performed back on the host when the request to the image inspector service returns:

1. Uploads the BDIO file to Black Duck (this can be disabled).
1. Copies the output files to the output directory.
1. Stops/removes the image inspector container (this can be disabled).

### Container mode

In container mode, you start four containers in such a way that they share a mounted volume and can reach each other via HTTP GET operations using
base URLs that you provide:

* One container for Docker Inspector
* One container for each of the three image inspector services (alpine, centos, and ubuntu)

In container mode, you must provide the image in a docker saved .tar file.

Docker Inspector:

1. Requests the Black Duck input/output (BDIO) file and container file system using HTTP from the default image inspector service using a 
base URL that you have provided.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container would have if you ran the target image. (It does not run the target image because it does not trust it.)
1. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
1. Runs the image inspector's Linux package manager on the target image package manager database.
1. Produces and returns a BDIO (.jsonld) file consisting of a list of target image packages and optionally, the container filesystem.

The following steps are performed back in the Docker Inspector container when the request to the image inspector service returns:

1. Uploads the BDIO file to Black Duck (this can be disabled).
1. Copies the output files to the output directory.