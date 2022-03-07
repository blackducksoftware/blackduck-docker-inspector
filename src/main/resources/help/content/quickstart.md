To help you get started using ${solution_name}, here's a simple example.

## Step 1: Determine the repo:tag of the Docker image you want to inspect

To run ${solution_name} on a Docker image from [Docker Hub](https://hub.docker.com), use Docker Hub to identify the repo:tag of the image you want to inspect.
For example: *ubuntu:latest*.

To run ${solution_name} on an image that is already on your machine (perhaps because you built it), use the REPOSITORY and TAG columns
from the output of the *docker images* command
to identify the repo:tag of the image you want to inspect.

On the ${solution_name} command line, pass your repo:tag using this argument:

    --docker.image={repo}:{tag}

When you specify the target image using the *docker.image* property, ${solution_name} will perform the equivalent of a `docker pull` on the image before inspecting it, in an attempt to ensure that it is inspecting the latest image. To avoid the `docker pull`, specify the image using the *docker.image.id* property instead.

## Step 2 (optional): Determine your ${blackduck_product_name} URL and credentials

To enable ${solution_name} to upload results to ${blackduck_product_name}, you will need a ${blackduck_product_name} URL, username, and password. (For
information on using an API token to authenticate with ${blackduck_product_name}, refer to [Properties](properties.md)).

On the ${solution_name} command line, pass your ${blackduck_product_name} connection details using the following arguments:

    --blackduck.url={your ${blackduck_product_name} URL}
    --blackduck.username={your ${blackduck_product_name} username}
    --blackduck.password={your ${blackduck_product_name} password}

As an alternative, to run ${solution_name} without connecting to ${blackduck_product_name}, pass this argument on the command line:

    --upload.bdio=false

## Step 3: Run ${solution_name}

To inspect an image and upload the results to ${blackduck_product_name}:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --blackduck.url={your ${blackduck_product_name} URL} --blackduck.username={your ${blackduck_product_name} username} --blackduck.password={your ${blackduck_product_name} password} --docker.image={repo}:{tag}

## Step 4: Review your results in ${blackduck_product_name}

To see your results, log into ${blackduck_product_name}, and look for a project with a name matching the docker image *repo*. The version name will
match the docker image *tag*.
