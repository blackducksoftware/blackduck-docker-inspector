# Architecture

Docker Inspector uses up to three container-based image inspector services (one for each of the supported Linux package manager database formats).
In host mode (the default), Docker Inspector automatically uses the Docker engine to pull (from Docker Hub) blackducksoftware/blackduck-imageinspector-alpine, 
blackducksoftware/blackduck-imageinspector-centos, and blackducksoftware/blackduck-imageinspector-ubuntu), starts those services as needed,
and stops and removes the containers when Docker Inspector exits. It uses a shared volume to share files, such as the target Docker image,
between the Docker Inspector utility and the three service containers.

For information on running Docker Inspector inside a container (container mode), refer to the deployment help topic.

# Execution phases

TBD