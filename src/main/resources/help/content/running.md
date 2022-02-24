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
It shares a directory with image inspector containers by mounting it as a volume.
You will need to configure your Docker settings to enable this file sharing.
The simplest way to do this is to add your home directory as a sharable directory
on the Docker settings Resources > FILE SHARING screen.

The shared directories are created under the value of property shared.dir.local.path,
so if you change the directory that property points to, be sure your Docker file sharing settings enable
sharing of that directory.

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

### Organizing components by layer

By default ${solution_name} will produce BDIO containing a component graph consisting only of components (linux packages).

Alternatively, you can direct ${solution_name} to organize components by image layer
by setting property `bdio.organize.components.by.layer=true`.
Run this way, ${solution_name} will produce BDIO containing image layers
at the top level of the graph, and components associated with each layer appearing as children of that layer.
This structure is visible in from the ${blackduck_product_name} project version Source display.

A side effect of this components-under-layers graph structure is
the categorization by ${blackduck_product_name} of all components as Transitive.

In the BDIO, layers are named `Layer{index}_{layer digest}`, where `{index}` is a two digit index starting at 00 to indicate layer ordering
within the image, and `{layer digest}` is the layer digest with ":" replaced with "_".
For example, the first layer of an image could be named:
`Layer00_sha256_1bcfbfaf95f95ea8a28711c83085dbbeceefa11576e1c889304aa5bacbaa6ac2`.

Because this feature produces BDIO in which the same component may appear at multiple points in the graph,
only ${blackduck_product_name} versions 2021.8.0 and newer have the ability to correctly display graphs organized by layer,
and only if *Admin > System Settings > Scan > Component Dependency Duplication Sensitivity* is set high enough to avoid removal of components
that appear multiple times in the graph (at minimum: 2).

When organizing components by layer, you must also choose whether or not to include removed (whited-out) components in the output.

#### Including removed components

If you include removed components, ${solution_name} will produce BDIO containing a graph with image layers
at the top level.  Under each image layer it will include all components present after the layer was
applied to the file system (including components added by lower layers that are still present). If a component is added by a lower layer
but later removed (whited-out) by a higher layer, it will not
appear under the layer that removed it or any higher layer (unless/until it is re-added by another layer).

The benefit of using this mode: for every component added by any layer, you can see where it was added, and where it was removed (if it was).

The downside of using this mode: components added by a lower layer but removed by a higher layer will appear in the BOM, even though they are not present in the final container filesystem.

To include removed components, set property `bdio.include.removed.components=true`.

#### Excluding removed components

If you exclude removed components (the default), ${solution_name} will behave as described in the section above
except that components not present in the final container filesystem will not appear on any layer in the BDIO graph.

The benefit of using this mode: components added by a lower layer but removed by a higher layer
(and therefore not present in the final container filesystem) will not appear in the BOM.

The downside of using this mode: if a component is added by a lower layer and removed by a higher layer, there is no evidence of that in the BDIO graph or the BOM.

To exclude removed components, set property `bdio.include.removed.components=false` (the default).
