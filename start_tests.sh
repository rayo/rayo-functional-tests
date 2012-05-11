#!/bin/bash

##########################################
#
#  Setup
#
##########################################
CI_SERVER=ci.voxeolabs.net
TEST_DOMAIN=qa.voxeolabs.net
SECURITY_GROUP=tropo_server
INSTANCE_SIZE=m1.medium
CHEF_ENVIRONMENT=qa_ec2
SSH_USER=ec2-user
SSH_KEY_NAME=test_ssh_key
SSH_KEY_PATH=~/.ssh/test_ssh_key


#################################
#
# Test Specific values
#
#################################
HOSTNAME=rft
XMPP_SERVER_DOMAIN=xmpp.testing.voxeolabs.net #xmpp.${TEST_DOMAIN}
XMPP_SERVER_USERNAME=rayo
XMPP_SERVER_PASSWORD=p@ssword


NODE_HOSTNAME_EXTERNAL=${HOSTNAME}-node-ext.${TEST_DOMAIN}
NODE_HOSTNAME=${HOSTNAME}-node.${TEST_DOMAIN}
GATEWAY_HOSTNAME_EXTERNAL=${HOSTNAME}-gw-ext.${TEST_DOMAIN}
GATEWAY_HOSTNAME=${HOSTNAME}-gw.${TEST_DOMAIN}


echo "##### HOSTNAME => ${HOSTNAME}"
echo "##### ARTIFACT_URL => ${ARTIFACT_URL}"
echo "##### NODE_HOSTNAME_EXTERNAL => ${NODE_HOSTNAME_EXTERNAL}"
echo "##### NODE_HOSTNAME => ${NODE_HOSTNAME}"
echo "##### GATEWAY_HOSTNAME_EXTERNAL => ${GATEWAY_HOSTNAME_EXTERNAL}"
echo "##### GATEWAY_HOSTNAME => ${GATEWAY_HOSTNAME}"

rm -rf chef_deployment_logs
rm -rf remotelogs
mkdir -p chef_deployment_logs
mkdir -p remotelogs

##########################################
#
#  Make sure no instances are still running
#
##########################################

knife ec2 server delete `knife search node "name:${GATEWAY_HOSTNAME}" -Fj | grep instance_id | awk 'BEGIN{FS="\""} {print $4}'` -y || :
knife client delete ${GATEWAY_HOSTNAME} -y || :
knife node delete ${GATEWAY_HOSTNAME} -y || :

knife ec2 server delete `knife search node "name:${NODE_HOSTNAME}" -Fj | grep instance_id | awk 'BEGIN{FS="\""} {print $4}'` -y || :
knife client delete ${NODE_HOSTNAME} -y|| :
knife node delete ${NODE_HOSTNAME} -y|| :

##########################################
#
#  Deploy Instance
#
##########################################

knife ec2 server create \
--flavor ${INSTANCE_SIZE} \
--groups ${SECURITY_GROUP} \
--ssh-user ${SSH_USER} \
--environment ${CHEF_ENVIRONMENT} \
--identity-file ${SSH_KEY_PATH} \
--run-list 'role[base],role[rayo_gateway]' \
--node-name ${GATEWAY_HOSTNAME} \
--tags Name=${GATEWAY_HOSTNAME},Build=${BUILD_TAG},BuildId=${BUILD_NUMBER},Branch=${RAYO_GIT_BRANCH} \
--user-data ~/.chef/userdata/userdata.sh | tee chef_deployment_logs/rft-gateway_chef_deployment.$BUILD_NUMBER.log &

knife ec2 server create \
--flavor ${INSTANCE_SIZE} \
--groups ${SECURITY_GROUP} \
--ssh-user ${SSH_USER} \
--environment ${CHEF_ENVIRONMENT} \
--identity-file ${SSH_KEY_PATH} \
--run-list 'role[base],recipe[rayo_node]' \
--node-name ${NODE_HOSTNAME} \
--tags Name=${NODE_HOSTNAME},Build=${BUILD_TAG},BuildId=${BUILD_NUMBER},Branch=${RAYO_GIT_BRANCH} \
--user-data ~/.chef/userdata/userdata.sh | tee chef_deployment_logs/rft-node_rayo_chef_deployment.$BUILD_NUMBER.log &

wait

##########################################
#
#  Finish config
#
##########################################

# ssh -t -t jenkins@${CATCHER_HOSTNAME_EXTERNAL} <<-EOF
#   sudo /opt/voxeo/prism/bin/prism stop as
#   sudo /opt/voxeo/prism/bin/prism stop ms
#   sudo cp /opt/voxeo/prism/server/apps/tropo/WEB-INF/classes/tropo.xml /tmp/tropo.xml
#   sudo wget $ARTIFACT_URL -O /tmp/tropo.war
#   sudo rm -rf /opt/voxeo/prism/server/apps/tropo
#   sudo unzip -o /tmp/tropo.war -d /opt/voxeo/prism/server/apps/tropo
#   sudo mv /tmp/tropo.xml /opt/voxeo/prism/server/apps/tropo/WEB-INF/classes/tropo.xml
#   sudo sed -i -e"s/<category name=\"Rtp\">/<category name=\"Rtp\">\n\t<item name=\"CheckPacketSource\">0<\/item>/g" /opt/voxeo/prism/conf/config.xml # [ #1615349 - Remote join issues with media ]

#   if [ "$TEST_NAME_PREFIX" == "tiab" ]; then
#     sudo cp /opt/voxeo/prism/apps/rest/WEB-INF/tropo.properties /tmp/tropo.properties
#     sudo wget $REST_PREMISE_URL -O /tmp/rest.war
#     sudo rm -rf /opt/voxeo/prism/apps/rest
#     sudo unzip -o /tmp/rest.war -d /opt/voxeo/prism/apps/rest
#     sudo mv /tmp/tropo.properties /opt/voxeo/prism/apps/rest/WEB-INF/tropo.properties
#     sudo wget $TRANSFER_AGENT_URL -O /opt/transfer-agent.tar.gz
#     sudo tar zxvf /opt/transfer-agent.tar.gz
#     sudo chown voxeo.voxeo -R /opt/transfer-dns
#     sudo /etc/init.d/transfer-dns restart
#   else
#     sudo sed -i -e"s/CloudDnsAppMgr/MockAppMgr/" /opt/voxeo/prism/server/apps/tropo/WEB-INF/classes/tropo.xml
#   fi

#   sudo chown voxeo.voxeo -R /opt/voxeo/prism
#   sudo rm -f /opt/voxeo/prism/logs/sipmethod.log* /opt/voxeo/prism/logs/vcs/log*
#   sudo /opt/voxeo/prism/bin/prism start ms
#   sudo /opt/voxeo/prism/bin/prism start as
#   echo Ready
#   exit
# EOF

# ssh -t -t jenkins@${DRIVER_HOSTNAME_EXTERNAL} <<-EOF
#   sudo /opt/voxeo/prism/bin/prism stop as
#   sudo /opt/voxeo/prism/bin/prism stop ms
#   sudo wget http://ci-voxeolabs-net.s3.amazonaws.com/rayo/rayo-latest.war -O /tmp/rayo.war
#   sudo unzip -o /tmp/rayo.war -d /opt/voxeo/prism/apps/rayo
#   sudo sed -i -e"s/<xmpp:servdomain>localhost<\/xmpp:servdomain>/<xmpp:servdomain>localhost<\/xmpp:servdomain>\n\t<xmpp:servdomain>${DRIVER_HOSTNAME_EXTERNAL}<\/xmpp:servdomain>\n\t<xmpp:servdomain>${DRIVER_HOSTNAME}<\/xmpp:servdomain>/g" /opt/voxeo/prism/apps/rayo/WEB-INF/xmpp.xml
#   sudo bash -c 'echo ".*=usera@${DRIVER_HOSTNAME_EXTERNAL}" > /opt/voxeo/prism/apps/rayo/WEB-INF/classes/rayo-routing.properties'
#   sudo sed -i -e"s/<category name=\"Rtp\">/<category name=\"Rtp\">\n\t<item name=\"CheckPacketSource\">0<\/item>/g" /opt/voxeo/prism/conf/config.xml # [ #1615349 - Remote join issues with media ]
#   sudo chown voxeo.voxeo -R /opt/voxeo/prism
#   sudo rm -f /opt/voxeo/prism/logs/sipmethod.log* /opt/voxeo/prism/logs/vcs/log*
#   sudo /opt/voxeo/prism/bin/prism start ms
#   sudo /opt/voxeo/prism/bin/prism start as
#   echo Ready
#   exit
# EOF

##########################################
#
#  Run tests
#
##########################################

PARAMETERS="-Dxmpp.server=${XMPP_SERVER_DOMAIN}"
PARAMETERS="${PARAMETERS} -Drayo.server=${GATEWAY_HOSTNAME_EXTERNAL}"
PARAMETERS="${PARAMETERS} -Dxmpp.username=${XMPP_SERVER_USERNAME}"
PARAMETERS="${PARAMETERS} -Dxmpp.password=${XMPP_SERVER_PASSWORD}"
PARAMETERS="${PARAMETERS} -Dhudson.append.ext=true"
PARAMETERS="${PARAMETERS} -Dhudson.append.ext=true"
PARAMETERS="${PARAMETERS} -Dsip.dial.uri=sip:usera@${NODE_HOSTNAME_EXTERNAL}"

/data/.jenkins/tools/Maven_3.0.3/bin/mvn clean test install ${PARAMETERS}
