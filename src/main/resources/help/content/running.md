### Considerations when running on Windows

#### Run from ${detect_product_name} version 6.6.0 or later, or run the ${solution_name} .jar file directly

The following is only a consideration if you are not running ${solution_name}
from ${detect_product_name}.

When executing ${solution_name} directly on Linx or Mac,
you can use the ${solution_name} bash script ${script_name} that:

1. Downloads (if necessary) the latest ${solution_name} .jar file, and
2. Executes it with the arguments you have provided.

There is no equivalent script for Windows, so on Windows you must download
the ${solution_name} .jar and execute it directly.

Download the ${solution_name} .jar file from the
[Synopsys artifactory server](${binary_repo_url_base}/webapp/#/artifacts/browse/tree/General/bds-integrations-snapshot/com/synopsys/integration/blackduck-docker-inspector).

To execute the ${solution_name} .jar:

````
java -jar ${project_name}-{version}.jar {${solution_name} arguments}
````

#### Docker file sharing settings

${solution_name} requires the ability to share directories with the image inspector containers.
You will need to configure your Docker settings to enable this file sharing.
The simplest way to do this is to add your home directory on the Docker settings

It shared a directory with image inspector containers by mounting it as a volume.solution_name.
The shared directories are created under the value of property shared.dir.local.path.



#### Docker restrictions

Docker on Windows has restrictions that impact ${solution_name}:

1. Docker can be configured to pull either Linix images, or Windows images.
You can see how your Docker installation is configured by looking
at the *OSType* value in the output of the *docker info* command.
If Docker is configured for Linix images, it cannot pull Windows images,
and vice versa. The command to change Docker's *OSType* value appears
in the Docker Desktop menu. Refer to Docker documentation for more information.
2. When pulling Windows images, Docker requires (a) that the architecture of the
pulled image matches the architecture of your machine, and (b) that the Windows version
of the pulled image is a close match to the Windows version of your machine.

It is a good practice to make sure you can pull the target
image using the *docker pull* command from the command line. If Docker
cannot pull the image, then ${solution_name} won't be able to either.
If you cannot pull the image from the command line, consider
both of the potential issues mentioned above.`

### Running the latest version (Linux/Mac only)

The following command format always fetches and runs the latest version of ${solution_name}:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) {${solution_name} arguments}

For example:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --help
    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --upload.bdio=false --docker.image=ubuntu

An alternative is to download and run the latest ${solution_name} script:

    curl -O  ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}
    chmod +x ${script_name}
    ./${script_name} {${solution_name} arguments}

The advantage of using the ${solution_name} script is that it ensures that you are always running the latest version of the ${solution_name} .jar.

Another alternative is to download the ${solution_name} .jar (using the script) and run the .jar directly:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --pulljar
    java -jar ${project_name}-{version}.jar {${solution_name} arguments}

### Running a specific version (Linux/Mac only)

By default, ${script_name} runs the latest version of
${solution_name} by downloading, if necessary, and running the latest ${solution_name} .jar.
To run a specific version of ${solution_name}:

    export DOCKER_INSPECTOR_VERSION={version}
    ./${script_name} {${solution_name} arguments}

For example:

    export DOCKER_INSPECTOR_VERSION=8.1.0
    ./${script_name} --upload.bdio=false --docker.image=ubuntu:latest

### Running the .jar file

The advantage of running ${script_name} is that it ensures that you always run the latest
version of ${solution_name}. However, sometimes it is better to run the .jar directly.

You can download any version of the ${solution_name} .jar from ${binary_repo_url_base}/webapp/#/artifacts/browse/tree/General/bds-integrations-release/com/synopsys/integration/${project_name}.

Use the following Java command to run it:

````
java -jar ${project_name}-{version}.jar {${solution_name} arguments}
````

### Inspecting an image by image repo:tag

To run ${solution_name} on a Docker image from your local cache or a registry:

````
./${script_name} --docker.image={repo}:{tag}
````

Or:

````
java -jar ${project_name}-{version}.jar --docker.image={repo}:{tag}
````
    
If you omit the :{tag}, it defaults to :latest.

### Inspecting an image saved to a .tar file

To run ${solution_name} on a Docker image .tar file:

    docker save -o {name}.tar {repo}:{tag}
    ./${script_name} --docker.tar={name}.tar
    
If your tar file contains multiple images, ${solution_name} can only inspect one of them.
You can specify the image to inspect using *--docker.image.repo* and *--docker.image.tag*. For example, to select *ubuntu:latest*
from a .tar file that contains *ubuntu:latest* and other images:

    ./${script_name} --docker.tar=multipleimages.tar --docker.image.repo=ubuntu --docker.image.tag=latest

### Inspecting a local image by image ID

When inspecting a local image, you have the option of specifying the image by its ID. First,
get the image ID from the *IMAGE ID* column of the output of the *docker images* command.
Then use the *docker.image.id* property to pass the image ID to ${solution_name}:

    ./${script_name} --docker.image.id={image ID}
    
The method only works for local images.