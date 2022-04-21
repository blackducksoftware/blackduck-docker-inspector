### Considerations when running on Windows

#### Docker file sharing settings

${solution_name} requires the ability to share directories with the image inspector containers.
It shares a directory with image inspector containers by mounting it as a volume.
You will need to configure your Docker settings to enable this file sharing.
The simplest way to do this is to add your home directory as a sharable directory
on the Docker settings Resources > FILE SHARING screen.

The shared directories are created under the ${detect_product_name} output directory
(controlled by ${detect_product_name} *detect.output.path*).
If you change the location of the ${detect_product_name} output directory, be sure your Docker file sharing settings enable
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

