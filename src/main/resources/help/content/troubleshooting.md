### Troubleshooting overview

To troubleshoot issues with ${solution_name}, run with DEBUG logging:

    --logging.level.com.synopsys=DEBUG
    
The following suggestions are related to specific problems.

### Problem: When directly invoking the .jar file, an error message displays "Malformed input or input contains unmappable characters."

Possible cause: Your local character encoding does not match the target container file system character encoding.

Solution/workaround: Set the character encoding to UTF-8 when invoking Java:
                     
    java -Dfile.encoding=UTF-8 ...
    
### Problem: You must run ${solution_name} on a non-Linux computer.

Solution/workaround: You may be able to run ${solution_name} within a Linux Docker container running on
your non-Linux computer using the following process.

Warning: This method involves running a privileged container
which will not be acceptable in some environments.

On your non-Linux computer, run:

    docker run -it -d --name inspectorhost --privileged ${image_repo_organization}/${inspector_image_name_base}-ubuntu:3.0.0
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
    curl -O ${script_hosting_scheme}://${source_repo_organization}.${script_hosting_domain}/${project_name}/${script_name}; chmod +x ${script_name}
    dockerd --storage-driver=vfs 2> dockerd_stderr.log > dockerd_stdout.log &
    ./${script_name}  ...

It's possible that additional steps such as configuration of dockerd, logging into the Docker registry,
and others, are required to give Docker running inside the inspectorhost container access to the Docker
images that ${solution_name} must pull. One way to reduce extra steps is to save the target
Docker image as a .tar file on your computer,
use *docker cp* to copy it into the container, and run ${solution_name} on that .tar file.

### Problem: Property values are set in unexpected ways.

Possible cause: ${solution_name} is built using the Spring Boot application framework.
Spring Boot provides a variety of ways to set property values. This can produce unexpected results if,
for example, you have an environment variable whose name maps to a ${solution_name} property name.
Refer to the
[Spring Boot documentation](${spring_boot_config_doc_url})
for more details.

### Problem: The image inspector service cannot write to the mounted volume; SELinux is enabled.

When this happens, the following error may appear in the container log: 

    Exception thrown while getting image packages: Error inspecting image: ${container_image_inspector_dir_path}/shared/run_.../output/..._containerfilesystem.tar.gz (Permission denied)
    ...
    Caused by: java.io.FileNotFoundException: /opt/blackduck/hub-imageinspector-ws/shared/run_.../output/..._containerfilesystem.tar.gz (Permission denied)

Possible cause: SELinux policy configuration.

Solution/workaround: Add the *svirt_sandbox_file_t* label to ${solution_name}'s shared directory.
This enables the ${solution_name} services running in Docker containers to write to it:
                     
    sudo chcon -Rt svirt_sandbox_file_t /tmp/${project_name}-files/shared/

### Problem: The image inspector service cannot read from the mounted volume.

When this happens, the following error may appear in the container log: 

    Error inspecting image: ${container_image_inspector_dir_path}/shared/run_.../{image}.tar (Permission denied)
    
Possible cause: The Linux umask value on the machine running ${solution_name} is too restrictive.

Solution/workaround: Set the umask value to 022 when running ${solution_name}. The cause could be a umask value
that prevents read access to the file, or read or execute access to the directory.
${solution_name} requires a umask value that does not remove read permissions from files 
and does not remove read or execute permissions from directories. For example, a umask value of 022 works.

### Problem: ${solution_name} cannot perform Docker operations because the remote access port is not enabled on the Docker engine.

When this happens, the following error may display in the log:

    Error inspecting image: java.io.IOException: Couldn't load native library
    Stack trace: javax.ws.rs.ProcessingException: java.io.IOException: Couldn't load native library

In versions of ${solution_name} prior to 8.2.0, the logged error was:

    Error inspecting image: Could not initialize class org.newsclub.net.unix.NativeUnixSocket
    Stack trace: java.lang.NoClassDefFoundError: Could not initialize class org.newsclub.net.unix.NativeUnixSocket

Possible cause: The TCP socket for remote access is not enabled on the Docker engine. For more information, refer to [Docker documentation](https://docs.docker.com/engine/reference/commandline/dockerd/#daemon-socket-option).

Solution/workaround: Follow the instructions in the [Docker documentation](https://docs.docker.com/engine/reference/commandline/dockerd/#daemon-socket-option) to
open the TCP port on the Docker engine.

