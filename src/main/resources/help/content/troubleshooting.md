## Troubleshooting

To troubleshoot issues with Docker Inspector, run with DEBUG logging:

    --logging.level.com.synopsys=DEBUG
    
Here are suggestions related to specific problems:

### Problem: When directly invoking the .jar file, an error message displays which reads "Malformed input or input contains unmappable characters."

Possible cause: Your local character encoding does not match the target container file system character encoding.

Solution/workaround: Set the character encoding to UTF-8 when invoking java:
                     
    java -Dfile.encoding=UTF-8 ...
    
### Problem: You must run Black Duck Docker Inspector on a non-Linux computer.

Solution/workaround: You may be able to run Black Duck Docker Inspector within a Linux Docker container running on
your non-Linux computer using the following process.

Warning: This method involves running a privileged container
which will not be acceptable in some environments.

On your non-Linux computer, run:

    docker run -it -d --name inspectorhost --privileged ${image_repo_organization}/blackduck-imageinspector-ubuntu:3.0.0
    docker attach inspectorhost

Then, in the inspectorhost container, run:

    mkdir -p /opt/blackduck/dockerinspector
    cd /opt/blackduck/dockerinspector
    apt-get update
    apt-get install -y apt-transport-https ca-certificates curl software-properties-common default-jre vim
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    apt-get install -y docker-ce
    dockerd --storage-driver=vfs  &
    curl -O https://${source_repo_organization}.github.io/${project_name}/${script_name}; chmod +x ${script_name}; chmod +x ${script_name}
    dockerd --storage-driver=vfs 2> dockerd_stderr.log > dockerd_stdout.log &
    ./${script_name}  ...

It's possible that additional steps such as configuration of dockerd, logging into the Docker registry,
and others, are required to give Docker running inside the inspectorhost container access to the Docker
images that Black Duck Docker Inspector must pull. One way to reduce extra steps is to save the target
Docker image as a .tar file on your computer,
use docker cp to copy it into the container, and run Black Duck Docker Inspector on that .tar file.

### Problem: Property values being set in unexpected ways.

Possible cause: Black Duck Docker Inspector is built using the Spring Boot application framework.
Spring Boot provides a variety of ways to set property values. This can produce unexpected results if,
for example, you have an environment variable whose name maps to a Black Duck Docker Inspector property name.
Refer to the
[Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
for more details.

### Problem: The image inspector service cannot write to the mounted volume; SELinux is enabled

When this happens, the following error may appear in the container log: 

    Exception thrown while getting image packages: Error inspecting image: /opt/blackduck/blackduck-imageinspector/shared/run_.../output/..._containerfilesystem.tar.gz (Permission denied)
    ...
    Caused by: java.io.FileNotFoundException: /opt/blackduck/hub-imageinspector-ws/shared/run_.../output/..._containerfilesystem.tar.gz (Permission denied)

Possible cause: SELinux policy configuration.

Solution/workaround: Add the svirt_sandbox_file_t label to Docker Inspector's shared directory.
This enables the Docker Inspector services running in Docker containers to write to it:
                     
    sudo chcon -Rt svirt_sandbox_file_t /tmp/${project_name}-files/shared/

### Problem: The image inspector service cannot read from the mounted volume

When this happens, the following error may appear in the container log: 

    Error inspecting image: /opt/blackduck/blackduck-imageinspector/shared/run_.../<image>.tar (Permission denied)
    
Possible cause: The Linux umask value on the machine running Docker Inspector is too restrictive.

Solution/workaround: Set umask to 022 when running Docker Inspector. The cause could be a umask value
that prevents read access to the file, or read or execute access to the directory.
Docker Inspector requires an umask value that does not remove read permissions from files,
and does not remove read or execute permissions from directories. For example, a umask of 022 works.
