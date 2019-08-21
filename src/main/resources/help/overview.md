Usage: blackduck-docker-inspector.sh <options>
options: any supported property can be set by adding to the command line
an option of the form:
	--<property name>=<value>

Alternatively, any supported property can be set by adding to a text file named
application.properties (in the current directory) a line of the form:
<property name>=<value>

For greater security, the Black Duck password can be set via the environment variable BD_PASSWORD.
For example:
  export BD_PASSWORD=mypassword
  ./blackduck-docker-inspector.sh --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername --docker.image=ubuntu:latest

For the list of properties, use:
    -h properties
    
For more detailed help on deploying Docker Inspector, use:
    -h deployment

Documentation is under Package Managers > Black Duck Docker Inspector at: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS