# Release Notes

## Version 8.1.3
### New features
* Added a Travis continuous integration deployment example.

## Version 8.1.2
### Changed features
* Updated Blackduck-docker-inspector.sh to download the Docker Inspector .jar file from the new Artifactory repository at sig-repo.synopsys.com.

## Version 8.1.1
### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when it was unable to read the image inspector container's log.

## Version 8.1.0
### Resolved issues
* Resolved an issue that could cause a No such file error on a file named classes.jsa when inspecting images containing Java.
### New features
* Added the property: output.containerfilesystem.excluded.paths.
* Added the command line --help=true switch for invoking help.
* Added the property output.include.squashedimage.

## Version 8.0.2
### Resolved issues
* Resolved an issue that could cause missing components on Fedora-based Docker images.

## Version 8.0.1
### Resolved issues
* Resolved an issue that could cause OpenSUSE components to be omitted from the Bill Of Materials. 
### New features
* Increased default image Inspector service timeout from two minutes to four minutes.

## Version 8.0.0
### New features
* Added the ability to collect only those components added to the image package manager database by your application layers, versus the platform layers on which your application is built.
* Added the ability to provide the full code location name.
### Removed features
* Docker exec mode (deprecated in Docker Inspector 7.0.0) is removed. Docker Inspector now supports HTTP client mode only.

## Version 7.3.3
### Resolved issues
* Resolved an issue that could prevent controlling the image inspector HTTP request service timeout through the service.timeout property.

## Version 7.3.2
### Resolved issues
* Resolved an issue that could cause RPM packages containing a value in the epoch field to be missing from the Black Duck Bill of Materials (BOM).

## Version 7.3.1
### Resolved issues
* Resolved an issue that may cause Hub Detect versions 5.2.0 and higher to fail with an error message of DOCKER extraction failed: null when invoking Docker Inspector on a non-Linux Docker image. 

## Version 7.3.0
### New features
* Added the property system.properties.path.

## Version 7.2.4
### Resolved issues
* Resolved an issue that may cause Docker Inspector to fail with an error message of Error inspecting image: Failed to parse docker configuration file (Unrecognized field "identitytoken").

## Version 7.2.3
### Changed features
* When constructing the container file system with the logging level set to DEBUG or TRACE : after applying each image layer, Docker Inspector now logs contents of the layer's metadata (json) file and the list of components.

## Version 7.2.2
### Resolved issues
* Resolved an issue that may cause Black Duck input/output data (BDIO) uploads to fail for openSUSE and Red Hat Docker images.

## Version 7.2.1
### Resolved issues
* Resolved an issue that may cause the Bill of Materials (BOM) creation to fail with an error message of Error in MAPPING_COMPONENTS displayed on the Black Duck Scans page for certain images.

## Version 7.2.0
### New features
* Added offline mode.
### Resolved issues
* Fixed an issue that may generate a warning message of Error creating hard link to be logged when inspecting certain images.
* Fixed an issue that may generate a warning message of Error removing whited-out file .../.wh..opq to be logged when inspecting images with opaque directory whiteout files.
* Reduced the disk space used in the working directory within the Inspector containers.

## Version 7.1.0
### Changed features
* Modified the format of the generated external identifiers to take advantage of the Black Duck KnowledgeBase preferred alias namespace feature.
* Modified the format of the generated external identifiers to include the epoch, when applicable, for RPM packages.

## Version 7.0.1
### Resolved issues
* When the logging level is set to DEBUG or higher, the contents of the image inspector service log are now included in the log output.
* The image inspector service is now started with the same logging level as Black Duck Docker Inspector.

## Version 7.0.0
### Changed features
* Hub Docker Inspector is now renamed to Black Duck Docker Inspector. The shell script, .jar filename, properties, and code blocks are updated accordingly.
* Black Duck Docker Inspector now runs in HTTP mode by default.
### Resolved issues
* HTTP client mode: Resolved an issue that prevents removal of the image inspector image upon completion.
* HTTP client mode: Resolved an issue with user-specified Black Duck project names and version names.
* HTTP client mode: Resolved an issue that caused the container filesystem to be provided even when it was not requested.

## Version 6.3.1
### Resolved issues
* Resolved an issue that could cause DEBUG-level warnings to be logged while inspecting images with file paths containing non-ASCII characters.
* Improved logging for when the image inspector service is started but never comes online.
* In http client mode > start service mode: if a health check fails, Docker Inspector now performs a <docker_logs>container operation to reveal the root problem.
* Orchestration platform properties are now included in the --help output.

## Version 6.3.0
### New features
* Added the --inspectorimagefamily command line argument, which prints the Inspector image family name.

## Version 6.2.0
### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when the image repository contained a : character and the image tag was not specified.
### New features
* Added the --pullairgapzip option.
* Improved multiple error messages.

## Version 6.1.0
### Resolved issues
* Added support for running on container application platforms such as Kubernetes and OpenShift.
* Renamed Rest Client Mode to HTTP Client Mode.
* Resolved an issue that prevented BDIO (Black Duck Input/Output data) from being uploaded to the Hub. This issue only affected HTTP client mode.
* Resolved an issue that caused Docker Inspector to fail when inspecting an image containing no package manager. This issue only affected HTTP client node.

## Version 6.0.4
### Resolved issues
* Resolved an issue wherein Hub Docker Inspector may fail if the target docker tarfile path contained spaces.

## Version 6.0.3
### Resolved issues
* Resolved an issue causing Hub Docker Inspector to produce an unnecessarily large container filesystem output file.

## Version 6.0.2
### Resolved issues
* Resolved an issue causing Hub Docker Inspector to fail when the image exists in the local cache but not the registry.

## Version 6.0.1
### Resolved issues
* Removed extraneous and possibly misleading log messages.
### New features
* Added the properties docker.image.repo and docker.image.tag to the usage message generated when using the command line argument --help.

## Version 6.0.0
### New features
* Added REST client mode.
### Changed features
* The available properties list included in the usage message, which displays when using the command line argument --help, is now sorted alphabetically.
* The format of the (optional) container filesystem output file name has changed.  The new container system file name is <image name>_<image tag>_ containerfilesystem.tar.gz or <image tarfilename>.tar.gz, depending on how the target image is specified.

## Version 5.1.0
### New features
* Added support for the JAVACMD environment variable.
### Resolved issues
* Improved performance and reliability.

## Version 5.0.0
### Removed features
* The dry.run property is removed. Instead of setting dry.run to true, you can now set upload.bdio to false.
* The output.include.dockertarfile property is removed. Instead, consider using the property output.include.containerfilesystem.
### Resolved issues
* Resolved an issue that could cause Hub Docker Inspector to fail when the local Docker registry contains unnamed images.

## Version 4.4.1
### Resolved issues
* Resolved an issue that could cause Hub Docker Inspector to fail to connect to Hub versions 4.5 or higher.

## Version 4.4.0
### New features
* Added the property cleanup.inspector.container.
* Added the ability to provide a Hub user access (API) token instead of username and password.
### Resolved issues
* Resolved an issue that caused Hub Docker Inspector to fail when the target image did not include a supported package manager database, such as an image based on the scratch image, with no Linux operating system installed. In this case, Hub Docker Inspector now succeeds, producing a Bill Of Materials (BDIO) file containing zero components.
* Resolved an issue that could cause hard link creation error messages to display in the log.
* Resolved an issue that could cause Hub Docker Inspector to fail to stop and also fail to remove the inspector Docker container.
* Resolved an issue that could cause Hub Docker Inspector to unnecessarily download its .jar file when the environment variable DOCKER_INSPECTOR_VERSION is set.
### Changed features
* The base name of the Inspector images and containers has changed from hub-docker-inspector to hub-imageinspector-ws.

## Version 4.3.2
### Resolved issues
* Resolved an issue that caused Hub Docker Inspector to remove existing files from the output directory.

## Version 4.3.1
### Resolved issues
* Resolved an issue that caused Hub Docker Inspector to fail when the target image repository URL included a port number.
* Resolved an issue that caused files to be left in the working directory even when cleanup.working.dir was set to true, which is the default value.
* Resolved an issue that caused the output Docker .tar filename to be shortened when the image repository name included a forward-slash character ('/').
* Resolved an issue wherein the log file may show a connection to the Hub when in fact there was no connection.

## Version 4.3.0
### New features
* Added the property upload.bdio. It replaces the now-deprecated property dry.run.
* Added the property cleanup.target.image.
* Added the property cleanup.inspector.image.

## Version 4.2.0
### New features
* Added the property inspector.repository.

## Version 4.1.0
### New features
* Directly invoking the .jar file now provides the ability to run multiple instances of Hub Docker Inspector on the same machine.
* Hub Docker Inspector stops and removes the Hub Docker Inspector container upon completion.

## Version 4.0.3
### Resolved issues
* Resolved an issue wherein after importing a certificate, Hub Docker Inspector may incorrectly display the error message Error inspecting image: Please import the certificate for <hub url> into your Java keystore.

## Version 4.0.2
### Resolved issues
* Added additional detail, including the list of supported properties, to the output of the  --help function.

## Version 4.0.1
### Resolved issues
* Resolved an issue which caused the --pulljar function to fail.

## Version 4.0.0
### New features
* All arguments, including the target Docker image or tarfile, are now passed as property values. The order is no longer important.
* All access to web servers (the Hub, Docker registry) is performed from your computer instead of a Docker container. This enables access to Docker images that exist only on your computer, and can help avoid certificate issues.  It also reduces challenges when accessing Docker registries.
* Added the property cleanup.working.dir.
### Removed features
* The property dev.mode is removed. Now, when property jar.path is set, the jar file is always copied into the Docker container.
* Hub Docker Inspector no longer prompts for the Hub password. If dry.run mode is disabled, which is the default, the Hub password is now required. 
* The no.prompt property is removed.
* Removed support for the old format of the --spring.config.location command line argument value.

## Version 3.2.1
### Resolved issues
* Resolved an issue with project names and versions that contain embedded spaces.

## Version 3.2.0
### New features
* Added support for a new format of the --spring.config.location command line argument value.

## Version 3.1.2
### Resolved issues
* Resolved an issue which caused Hub Docker Inspector to fail when a Hub username containing spaces was provided through the command line.

## Version 3.1.1
### Resolved issues
* Resolved an issue causing Hub Docker Inspector to fail when the .jar file path contained spaces.
* Resolved an issue causing Hub Docker Inspector to fail when the path of the current working directory contained spaces.
* Resolved an issue that could cause Hub Docker Inspector to continue executing after a failure.

## Version 3.1.0
### New features
* Hub Docker Inspector now performs the BDIO upload from your computer. Previously, this was done from a Docker container.  This eliminates the need to set the hub.always.trust.cert property to true when the Hub server certificate is installed on your computer.  This refers to your actual personal computer, and not to the local Hub server instance.
### Resolved issues
* Resolved an issue that could cause BDIO upload to fail when the BDIO filename is longer than 255 characters.
* Resolved an issue that could cause Hub Docker Inspector to incorrectly report the success/fail status.

## Version 3.0.0
### New features
* The first phase of processing, which is determining the operating system / package manager of the target image, now runs on your computer. Previously, it ran within a Docker container. The second phase of processing, determining the installed packages, still runs within a Docker container.
* When invoked on an image instead of a tar file, Hub Docker Inspector now uses the docker process (dockerd) running on your computer to pull the target image.
* The process within the Hub Docker Inspector container now runs as a non-root user.
### Removed features
* The output no longer includes a JSON file containing the dependency node tree.
* Properties, arguments, and environment variables used to configure Docker; for example, docker.registry, docker.registry.username, and others, are no longer required.  These are removed.
### Resolved issues
* The output.include.tarfile property name is changed to output.include.dockertarfile.
* The script hub-docker-inspector.sh returns 0 when it succeeds, and a non-zero value when it fails.

## Version 2.1.2
### Resolved issues
* Resolved an issue which caused Hub Docker Inspector to produce an incorrect JSON dependency node type.

## Version 2.1.1
### Resolved issues
* Resolved an issue which caused Hub Docker Inspector to produce an incorrect JSON dependency node name/version.

## Version 2.1.0
### New features
* Added the output.include.containerfilesystem property.

## Version 2.0.0
### Resolved issues
* When inspecting a tar file that contains multiple images/versions, Hub Docker Inspector now requires the docker.image.repo and docker.image.tag properties. These properties specify which image from the tar file are inspected.
* When not in dry run mode and no Hub password is provided, Hub Docker Inspector now prompts for it. This behavior is disabled using the no.prompt property.
* Changed property bdio.output.path to output.path.
* When the output.path property is set, the output now includes a JSON file containing the dependency node tree, in addition to the BDIO file.
* Added the output.include.tarfile property.
* Added the hub.codelocation.prefix property.
* Resolved an issue that may produce inaccurate results for packages maintained by multiple Linux projects.
* Improved the error message displayed when the user provides an incorrect Hub password.

## Version 1.2.1
### Resolved issues
* Resolved an issue which caused Hub Docker Inspector to fail when running on Windows.

## Version 1.2.0
### Resolved issues
* Added the runon property.
* Added support for the DOCKER_INSPECTOR_JAVA_OPTS environment variable.
### Resolved issues causing Hub Docker Inspector to fail when the current directory path or the given .tar file path contained spaces.

## Version 1.1.1
### Resolved issues
* Resolved an issue wherein output results may not be entirely consistent.

## Version 1.1.0
### Resolved issues
* Added dry.run and bdio.output.path properties.
* Removed install.dir and working.directory properties
* Added the ability to specify dockerd command line options.
### Known issues
* When you inspect the same image twice and specify different Hub project names or versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 1.0.2
### Resolved issues
* Added improvements to the way the Docker image files are extracted which reduces spurious error messages.
* Hub Docker Inspector now automatically upgrades the Hub Docker Inspector container when appropriate.
### Known issues
* When you inspect the same image twice and specify different Hub project names or versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 1.0.1
### Resolved issues
* Hub Docker Inspector now terminates earlier if Hub details are invalid.
* Added improvements to the way the Docker image files are extracted which reduces spurious error messages.
* Added support for Hub project names containing the "/" character.
### Known issues
* When you inspect the same image twice and specify different Hub project names or versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 1.0.0
### Resolved issues
* Additional security for the SCAN_CLI_OPTS environment variable.
### Known issues
* When you inspect the same image twice and specify different Hub project versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 0.1.4
### Resolved issues
* Resolved an issue that may produce inaccurate results caused by incorrectly ordered image layers.
* Added the ability to get an application.properties file template.
* Additional security for the BD_HUB_PASSWORD environment variable.
* DPKG systems: Now omits packages that are included in the dpkg list, but are not currently installed.
### Known issues
* The proxy password provided to Hub Docker Inspector is not secure in this version. If you communicate with the Hub server using a proxy, do not deploy in a production environment requiring password security.
* When you inspect the same image twice and specify different Hub project versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 0.1.3
### Resolved issues
* Resolved an issue which caused a 401 error when the password was specified both by the BD_HUB_PASSWORD environment variable and the --hub.password command line argument. In versions 0.1.3 and higher, if both are set, the command line argument overrides the environment variable.
### Known issues
* The passwords provided to Hub Docker Inspector are not encrypted in this version. Do not use in a production environment where the provided passwords must be kept secure.
* When you inspect the same image twice and specify different Hub project versions, the first project version's Bill of Materials (BOM) is cleared.

## Version 0.1.2
### Resolved issues
* Resolved an issue which caused a 403 error when uploading BDIO files to Hub 4.0.
### Known issues
* The passwords provided to Hub Docker Inspector are not encrypted in this version. Do not use in a production environment where the provided passwords must be kept secure.
* When you inspect an image after changing your Hub project version, the previous version's Bill of Materials is cleared.

## Version 0.1.1
### Resolved issues
* Resolved an issue which resulted in the following error while processing some images: Error extracting files from layer tar: groovy.lang.MissingPropertyException: No such property: getMessage for class: java.nio.file.FileAlreadyExistsException.
### Known issues
* The passwords provided to Hub Docker Inspector are not encrypted in this version. Do not use in a production environment where the provided passwords must be kept secure.
* When you inspect an image again after changing your Hub project version, the previous version's BOM is cleared.

## Version 0.1.0
### New features
* New --runon option allows you to instruct the utility to run directly on either CentOS or Alpine. If you are:
* Running the utility on a RedHat (RHEL) system
* Inspecting an image based on an operating system that uses either RPM or APK package manager
* Geting a no space left on device error
* Then use this option to instruct the utility to run on a platform that supports the target image package manager. For target images that use RPM, specify a value of centos. For images that use APK, specify a value of alpine. This option only works if the image is provided as a tarfile. For example:
./hub-docker-inspector --runon=centos myRedHatImage.tar
### Resolved issues
* Resolved an issue which failed to inspect older versions of CentOS with the error message: RpmExecutor : error: db5 error(30969) from dbenv>open: BDB0091 DB_VERSION_MISMATCH: Database environment version mismatch.
* Resolved an issue which in some cases produced file or link-related error messages when unpacking images. These generally do not interfere with the ability to produce the Hub project or Bill of Materials.
### Known issues
* The passwords provided to Hub Docker Inspector are not encrypted in this version. Do not use in a production environment where the provided passwords must be kept secure.
* When you inspect an image again after changing your Hub project version, the previous version's BOM is cleared.
* Cannot be run from a directory containing spaces in the name.

## Version 0.0.4
### Known issues
* The passwords provided to Hub Docker Inspector are not encrypted in this version. Do not use in a production environment where the provided passwords must be kept secure.
* When you inspect an image again after changing the Hub project version, the previous version's BOM is cleared.
* Cannot be run from a directory containing spaces in the name.
* Fails to inspect older versions of CentOS with error message: RpmExecutor : error: db5 error(30969) from dbenv>open: BDB0091 DB_VERSION_MISMATCH: Database environment version mismatch.
* Will in some cases produce file or link-related error messages unpacking images. These generally do not interfere with the ability to produce the Hub project or Bill of Materials.
