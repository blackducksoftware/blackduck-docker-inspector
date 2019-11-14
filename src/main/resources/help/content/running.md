### Running the latest version

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

### Running a specific version

By default, ${script_name} runs the latest version of
${solution_name} by downloading, if necessary, and running the latest ${solution_name} .jar.
To run a specific version of ${solution_name}:

    export DOCKER_INSPECTOR_VERSION={version}
    ./${script_name} {${solution_name} arguments}

For example:

    export DOCKER_INSPECTOR_VERSION=8.1.0
    ./${script_name} --upload.bdio=false --docker.image=ubuntu:latest

### Running the .jar file

The advantage of running ${project_name}.jar is that it ensures that you always run the latest
version of ${solution_name}. However, sometimes it is better to run the .jar directly.

You can download any version of the ${solution_name} .jar from ${binary_repo_url_base}/webapp/#/artifacts/browse/tree/General/bds-integrations-release/com/synopsys/integration/${project_name}.

Use this Java command to run it:

    java -jar ${project_name}-{version}.jar {${solution_name} arguments}

### Inspecting an image by image repo:tag

To run ${solution_name} on a Docker image from your local cache or a registry:

    ./${script_name} --docker.image={repo}:{tag}

If you omit the :{tag}, it defaults to :latest.

### Inspecting an image saved to a .tar file

To run ${solution_name} on a Docker image .tar file:

    docker save -o {name}.tar {repo}:{tag}
    ./${script_name} --docker.tar={name}.tar
    
If your tar file contains multiple images, ${solution_name} can only inspect one of them.
You can specify which image you want to inspect using *--docker.image.repo* and *--docker.image.tag*. For example, to select *ubuntu:latest*
from a .tar file that contains *ubuntu:latest* and other images:

    ./${script_name} --docker.tar=multipleimages.tar --docker.image.repo=ubuntu --docker.image.tag=latest
