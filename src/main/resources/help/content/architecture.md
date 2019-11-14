### Architecture overview

${solution_name} uses up to three container-based image inspector services, 
one for each of the supported Linux package manager database format).

The three image inspector services provide coverage of the three package manager database formats: dpkg, rpm, and apk.
By default, ${solution_name} submits its request to inspect the target image to the dpkg (Ubuntu) image inspector service. Any service 
redirects to the appropriate image inspector service if it cannot handle the request. For example,
if the target image is a Red Hat image, the Ubuntu inspector service, which cannot inspect a Red Hat image, 
redirects to the CentOS inspector
service, which can inspect a Red Hat image. If you know
that most of your images have either RPM or APK databases, you can improve performance by configuring
${solution_name} to send requests to the CentOS (RPM) or Alpine (APK) image inspector service using
the property *imageinspector.service.distro.default*.

In host mode (the default), ${solution_name} automatically uses the Docker engine to pull as
needed from Docker Hub
three images: ${image_repo_organization}/${inspector_image_name_base}-alpine, 
${image_repo_organization}/${inspector_image_name_base}-centos, and ${image_repo_organization}/${inspector_image_name_base}-ubuntu.
${solution_name} starts those services as needed,
and stops and removes the containers when ${solution_name} exits. It uses a shared volume to share files, such as the target Docker image,
between the ${solution_name} utility and the three service containers.

In container mode, start the container running ${solution_name} and the three image inspector container-based services such that
all four containers share a mounted volume and can communicate with each other using HTTP GET operations using base URLs that you provide.
For more information, refer to [Deployment](deployment.md).

### Execution phases

#### Host mode

In host mode, ${solution_name} performs the following steps on the host:

1. Pulls and saves the target image to a .tar file if you passed the image by *repo:tag*.
2. Checks to see if the default image inspector service is running. If not, it pulls the inspector image and
starts a container, mounting a shared volume.
3. Requests the Black Duck input/output (BDIO) file and container file system by sending an HTTP GET request to the image inspector service.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container has if you ran the target image. It does not run the target image.
2. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
3. Runs the image inspector's Linux package manager on the target image package manager database to get the list of
installed packages.
4. Produces and returns a BDIO (.jsonld) file consisting of a list of target image packages and, optionally, the container filesystem.

The following steps are performed back on the host when the request to the image inspector service returns:

1. Uploads the BDIO file to Black Duck.  Note that this can be disabled.
2. Copies the output files to the output directory.
3. Stops/removes the image inspector container.  Note that this can be disabled.

#### Container mode

In container mode, you start four containers in such a way that they share a mounted volume and can reach each other through HTTP GET operations using
base URLs that you provide:

* One container for ${solution_name}.
* One container for each of the three image inspector services: Alpine, CentOS, and Ubuntu.

In container mode, you must provide the image in a Docker-saved .tar file.

${solution_name}:

1. Requests the Black Duck input/output (BDIO) file and container file system using HTTP from the default image inspector service using a 
base URL that you have provided.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container has if you ran the target image. It does not run the target image.
1. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
1. Runs the image inspector's Linux package manager on the target image package manager database.
1. Produces and returns a BDIO (.jsonld) file consisting of a list of target image packages and, optionally, the container filesystem.

The following steps are performed back in the ${solution_name} container when the request to the image inspector service returns:

1. Uploads the BDIO file to Black Duck. Note that this can be disabled.
1. Copies the output files to the output directory.
