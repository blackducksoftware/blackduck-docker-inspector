#
# GitLab CI Example
#

# Start gitlab
docker pull store/gitlab/gitlab-ce:11.10.4-ce.0
docker run --detach --hostname gitlab.example.com --publish 443:443 --publish 80:80 --publish 2222:22 --name gitlab --restart always --volume /Users/billings/gitlab/config:/etc/gitlab --volume ${HOME}/gitlab/logs:/var/log/gitlab --volume ${HOME}/gitlab/data:/var/opt/gitlab gitlab/gitlab-ce:latest

echo -n "Press Return/Enter when gitlab is fully up"
read discardedinput

gitlabip=$(docker exec gitlab hostname -I)

# Register runner
docker run --rm -t -i -v ${HOME}/java8home/jre1.8.0_221/:/javahome -v ${HOME}/gitlab-runner/config:/etc/gitlab-runner gitlab/gitlab-runner register --non-interactive --executor shell --url http://${gitlabip} --registration-token "iJ4xjx_UwR-iP_CcMHJR" --description "java runner" --run-untagged=true

sleep 10

# Start runner
docker run -d --name gitlab-runner --restart always -v ${HOME}/java8home/jre1.8.0_221/:/javahome -v ${HOME}/gitlab-runner/config:/etc/gitlab-runner gitlab/gitlab-runner --debug run


