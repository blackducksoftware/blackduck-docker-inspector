
_Help version: ${program_version}_

${solution_name} inspects Docker images to discover packages (components).
It utilizes the appropriate Linux package manager to provide a list of
the packages installed by the package manager, and creates a Black Duck 
project with a Bill of Materials (BOM) consisting of those packages as components.
Because it relies on the Linux package manager as its source,
the discovered packages are limited to those installed and managed using the Linux package manager.

${solution_name} can discover package manager-installed components in
Linux Docker images that use the DPKG, RPM, or APK package manager database formats.

${solution_name} can inspect non-Linux images (for example, Windows images,
and images that contain no operating system), but 
will discover zero components. This can be useful if the target image
container file system that ${solution_name} can produce as output is needed
for signature scanning.

After running the ${solution_name} on an image, navigate to Black Duck to view the BOM created by 
${solution_name}.

### Modes of operation

${solution_name} has two modes:

* Host mode, for running on a server or virtual machine (VM) where ${solution_name} can perform Docker operations using a Docker Engine.
* Container mode, for running in a container started by Docker, Kubernetes, OpenShift, and others.

#### Host mode

Host mode (default) is for servers/VMs where ${solution_name} can perform Docker operations using a Docker Engine.

In host mode, ${solution_name} is a utility that automates the process of using Black Duck to discover security, license, and operational risks
associated with Linux-based Docker images. It discovers components using the target Docker image's package manager; therefore, the results
are limited to those components (packages) of which the package manager is aware. ${solution_name} does this without running
the image, so it is safe to run on untrusted images.

${solution_name} can pull the target image; in other words, the Docker image you want to inspect, from a Docker registry such
as Docker Hub. Alternatively, you can save an image to a .tar file by using the *docker save* command. Then, run ${solution_name}
on the .tar file. ${solution_name} supports Docker Image Specification v1.2.0 format .tar files.

#### Container mode

Container mode is for container orchestration environments; for example, Kubernetes, OpenShift, and others, where ${solution_name} runs
inside a container where it cannot perform Docker operations. For information on running ${solution_name} in container mode,
refer to [Deploying](deployment.md).

### Requirements

Requirements for ${solution_name} are:

* The current version of Black Duck. Visit [this page](${blackduck_release_page}) to determine the current version. 
* Linux, MacOS, or Windows 10 Enterprise.
    - On Windows, ${solution_name} must be executed from ${detect_product_name} version 6.6.0 or later, or by executing the ${solution_name} .jar directly. There is no equivalent to ${script_name} for Windows.
* Access to the internet. For information on running without access to the internet, refer to [Air Gap mode](advanced.md#air-gap-mode).
* Java (JRE) versions 8, 11, or 15.
* Three available ports for the image inspector services. By default, these ports are 9000, 9001, and 9002.
* The environment must be set up such that files created by ${solution_name} are readable by all. On Linux, this means an appropriate umask value (for example, 002 or 022 would work). On Windows, this means the working directory must be readable by all.
* Files passed to ${solution_name} via the *docker.tar* property must be readable by all.
* Images passed to ${solution_name} via the *docker.tar* property must conform to [Docker Image Specification v1.2.0](https://github.com/moby/moby/blob/master/image/spec/v1.2.md) (the format produced by the "docker save" command).
* When invoking ${solution_name} using ${script_name}:
    - curl
    - bash
* In host mode: access to a Docker Engine versions 17.09 or higher.
* In container mode: you must start the ${solution_name} container that meets the preceding requirements, and three container-based
"image inspector" services. All four of these containers must share a mounted volume and be able to reach each other through HTTP GET operations using base URLs
that you provide. For more information, refer to [Deploying](deployment.md).
    
### Getting started

#### Invoking from ${detect_product_name}

For many users, invoking ${solution_name} from ${detect_product_name} will be the best option.
${detect_product_name} provides the following benefits:

1. It automatically downloads (if necessary) the latest version of ${solution_name}.
This is the only way to get this capability on Windows.
2. It discovers components that ${solution_name} is unable to discover by also invoking
the ${blackduck_product_name} Signature Scanner on the target image container file system.

Refer to the ${detect_product_name} documentation for more information.

#### Invoking ${solution_name} directly

The following command format always fetches and runs the latest version of ${solution_name}:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) {${solution_name} arguments}

For example:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --help
    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --upload.bdio=false --docker.image=ubuntu

An alternative is to download and run the latest ${solution_name} script:

    curl -O  ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}
    chmod +x ${script_name}
    ./${script_name} {${solution_name} arguments}

The advantage of using the ${solution_name} script is that it ensures you always run the latest version of the ${solution_name} .jar.

Another alternative is to download the ${solution_name} .jar (using the script) and run the .jar directly:

    bash <(curl -s ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}) --pulljar
    java -jar ${project_name}-{version}.jar {${solution_name} arguments}

### Passing arguments to ${solution_name}

Running ${solution_name} typically involves invoking the script or .jar with some command line arguments:

    ${script_name} {${solution_name} arguments}
    
${solution_name} command line arguments consist of property assignments. Any supported property can be set by adding to the command line
a property assignment of the form:

	--{property name}={value}

Alternatively, any supported property can be set by adding to a text file named
*application.properties* in the current directory a line of the form:

    {property name}={value}

An *application.properties* file can contain multiple property assignments.

There are other alternative methods for setting properties. For more information, refer to [Running](running.md).

### Help

Available help topics:

* overview (this page)
* architecture
* running
* properties
* advanced
* deployment
* troubleshooting
* releasenotes
* all

To display a help topic, run ${solution_name} with either *-h* or *--help* followed by a topic. For example:

    -h properties
    
To display multiple help topics, use a comma-separated list of help topics. For example:

    -h overview,properties,running

To display all help topics, use topic *all*:

    -h all

To write help to a file, add *--help.output.path={directory or file path}*:

    -h all --help.output.format=html --help.output.path=.

Help content is output in Markdown format, which can be formatted ("pretty printed")
using tools like mdless (https://brettterpstra.com/2015/08/21/mdless-better-markdown-in-terminal/).
