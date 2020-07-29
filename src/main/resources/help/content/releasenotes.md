#### Version 9.1.0
##### New features
* Added support for running on Windows 10 Enterprise Edition by executing the ${solution_name} .jar file directly.
* Added property use.platform.default.docker.host (default to true).

#### Changed feature
* Changed default working directory from /tmp to $HOME/blackduck/docker-inspector

#### Version 9.0.2
##### Resolved issues
* Resolved an issue that could cause ${solution_name} to fail when using existing image inspector services when given a target docker .tar file that resided outside the directory shared with the image inspector container(s).

#### Version 9.0.1
##### Resolved issues
* Resolved an issue that could cause files to be omitted from the squashed image produced by ${solution_name}. The problem occurred on images that declared a directory opaque and added files to that directory within the same layer that declared it opaque.

#### Version 9.0.0
##### Changed feature
* The internal format of the Black Duck Input Output (BDIO) file that is produced is now compatible with ${detect_product_name} version 6.3 and later.

#### Version 8.3.1
##### Resolved issues
* Fixed an issue that prevented the *linux.distro* property from working correctly.

#### Version 8.3.0
##### New features
* Docker Inspector now writes a summary of results to the file results.json, located in the output directory.

#### Version 8.2.3
##### Resolved issues
* Eliminated the Spring banner from the log to facilitate piping help output through a Markdown formatter.

#### Version 8.2.2
##### Resolved issues
* Increased the default value of *service.timeout* from four minutes to ten minutes.
* The time Docker Inspector waits for an image inspector service to come online is now controlled using the *service.timeout* property.

#### Version 8.2.1
##### Resolved issues
* Resolved an issue that caused *blackduck-docker-inspector.sh* to display the help overview even when a different help topic is requested.
* Resolved an issue that caused Docker Inspector to return the full container filesystem even when only application components are requested (*docker.platform.top.layer.id* is specified).

#### Version 8.2.0
##### New features
* Added support for Java 11.
* Added the ability to generate help by topic (--help {topic}).
* Added the ability to generate help in HTML.
* Added the ability to write help output to a given file.

#### Version 8.1.6
##### Changed feature
* Adjusted logging to ensure that sensitive information does not display in a debug log.

#### Version 8.1.5
##### Resolved issues
* Resolved an issue that could cause Docker Inspector to incorrectly identify the package manager of the target image.

#### Version 8.1.4
##### New features
* Added a GitLab continuous integration deployment example.

#### Version 8.1.3
##### New features
* Added a Travis continuous integration deployment example.

#### Version 8.1.2
##### Changed features
* Updated *blackduck-docker-inspector.sh* to download the Docker Inspector .jar file from the new Artifactory repository at sig-repo.synopsys.com.

#### Version 8.1.1
##### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when it was unable to read the image inspector container log.

#### Version 8.1.0
##### Resolved issues
* Resolved an issue that could cause a *No such file* error on files named *classes.jsa* when inspecting images containing Java.
##### New features
* Added the property *output.containerfilesystem.excluded.paths*.
* Added the command line switch *--help=true* for invoking help.
* Added the property *output.include.squashedimage*.

#### Version 8.0.2
##### Resolved issues
* Resolved an issue that could cause missing components on Fedora-based Docker images.

#### Version 8.0.1
##### Resolved issues
* Resolved an issue that could cause OpenSUSE components to be omitted from the Bill Of Materials. 
##### New features
* Increased default image Inspector service timeout from two minutes to four minutes.

#### Version 8.0.0
##### New features
* Added the ability to collect only those components added to the image package manager database by your application layers, versus the platform layers on which your application is built.
* Added the ability to provide the full code location name.
##### Removed features
* Docker exec mode (deprecated in Docker Inspector 7.0.0) is removed. Docker Inspector now supports HTTP client mode only.

#### Version 7.3.3
##### Resolved issues
* Resolved an issue that could prevent controlling the image inspector HTTP request service timeout through the property *service.timeout*.

#### Version 7.3.2
##### Resolved issues
* Resolved an issue that could cause RPM packages containing a value in the epoch field to be missing from the Black Duck Bill of Materials (BOM).

#### Version 7.3.1
##### Resolved issues
* Resolved an issue that could cause Hub Detect versions 5.2.0 and higher to fail with an error message of *DOCKER extraction failed: null* when invoking Docker Inspector on a non-Linux Docker image. 

#### Version 7.3.0
##### New features
* Added the property *system.properties.path*.

#### Version 7.2.4
##### Resolved issues
* Resolved an issue that could cause Docker Inspector to fail with an error message of *Error inspecting image: Failed to parse docker configuration file (Unrecognized field "identitytoken")*.

#### Version 7.2.3
##### Changed features
* When constructing the container file system with the logging level set to DEBUG or TRACE: after applying each image layer, Docker Inspector now logs contents of the layer's metadata (json) file and the list of components.

#### Version 7.2.2
##### Resolved issues
* Resolved an issue that could cause Black Duck input/output data (BDIO) uploads to fail for openSUSE and Red Hat Docker images.

#### Version 7.2.1
##### Resolved issues
* Resolved an issue that could cause the Black Duck BOM creation to fail with an error message of *Error in MAPPING_COMPONENTS* displayed on the Black Duck Scans page for certain images.

#### Version 7.2.0
##### New features
* Added offline mode.
##### Resolved issues
* Resolved an issue that could generate a warning message of *Error creating hard link* to be logged when inspecting certain images.
* Resolved an issue that could generate a warning message of *Error removing whited-out file .../.wh..opq* to be logged when inspecting images with opaque directory whiteout files.
* Reduced the disk space used in the working directory within the Inspector containers.

#### Version 7.1.0
##### Changed features
* Modified the format of the generated external identifiers to take advantage of the Black Duck KnowledgeBase preferred alias namespace feature.
* Modified the format of the generated external identifiers to include the epoch, when applicable, for RPM packages.

#### Version 7.0.1
##### Resolved issues
* When the logging level is set to DEBUG or higher, the contents of the image inspector service log are now included in the log output.
* The image inspector service is now started with the same logging level as Black Duck Docker Inspector.

#### Version 7.0.0
##### Changed features
* Hub Docker Inspector is now renamed to Black Duck Docker Inspector. The shell script, .jar filename, properties, and code blocks are updated accordingly.
* Black Duck Docker Inspector now runs in HTTP mode by default.
##### Resolved issues
* HTTP client mode: Resolved an issue that prevents removal of the image inspector image upon completion.
* HTTP client mode: Resolved an issue with user-specified Black Duck project names and version names.
* HTTP client mode: Resolved an issue that caused the container filesystem to be provided even when it was not requested.

#### Version 6.3.1
##### Resolved issues
* Resolved an issue that could cause DEBUG-level warnings to be logged while inspecting images with file paths containing non-ASCII characters.
* Improved logging for when the image inspector service is started but never comes online.
* In http client mode > start service mode: if a health check fails, Docker Inspector now performs a *docker logs* operation on the container to reveal the root problem.
* Orchestration platform properties are now included in the *--help* output.

#### Version 6.3.0
##### New features
* Added the *--inspectorimagefamily* command line argument, which prints the Inspector image family name.

#### Version 6.2.0
##### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when the image repository contained a : character and the image tag was not specified.
##### New features
* Added the *--pullairgapzip* option.
* Improved error messages.

#### Version 6.1.0
##### Resolved issues
* Added support for running on container application platforms such as Kubernetes and OpenShift.
* Renamed Rest Client Mode to HTTP Client Mode.
* Resolved an issue that prevented BDIO (Black Duck Input/Output data) from being uploaded to the Hub. This issue only impacted HTTP client mode.
* Resolved an issue that caused Docker Inspector to fail when inspecting an image containing no package manager. This issue only impacted HTTP client node.

#### Version 6.0.4
##### Resolved issues
* Resolved an issue wherein Hub Docker Inspector may fail if the target Docker tarfile path contained spaces.

#### Version 6.0.3
##### Resolved issues
* Resolved an issue causing Hub Docker Inspector to produce an unnecessarily large container filesystem output file.

#### Version 6.0.2
##### Resolved issues
* Resolved an issue causing Hub Docker Inspector to fail when the image exists in the local cache but not the registry.

#### Version 6.0.1
##### Resolved issues
* Removed extraneous and possibly misleading log messages.
##### New features
* Added the properties *docker.image.repo* and *docker.image.tag* to the usage message generated when using the command line argument *--help*.

#### Version 6.0.0
##### New features
* Added REST client mode.
##### Changed features
* The available properties list included in the usage message, which displays when using the command line argument *--help*, is now sorted alphabetically.
* The format of the (optional) container filesystem output file name has changed.  The new container system file name is *{image name}_{image tag}_ containerfilesystem.tar.gz* or *{image tarfilename}.tar.gz*, depending on how the target image is specified.