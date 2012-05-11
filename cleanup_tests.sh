#!/bin/bash

echo -e "##########################################\n#\n# Cleanup\n#\n##########################################"

cd "$WORKSPACE"
rm -rf remotelogs

mkdir -p remotelogs/gw/vcs
mkdir -p remotelogs/node/vcs

ssh -t -t jenkins@${HOSTNAME}-gw-ext.qa.voxeolabs.net <<-EOF
  sudo /opt/voxeo/prism/bin/prism stop as
  sudo /opt/voxeo/prism/bin/prism stop ms
  echo Stopped
  exit
EOF

ssh  jenkins@${HOSTNAME}-node-ext.qa.voxeolabs.net <<-EOF
  sudo /opt/voxeo/prism/bin/prism stop as
  sudo /opt/voxeo/prism/bin/prism stop ms
  echo Stopped
  exit
EOF

scp jenkins@${HOSTNAME}-gw-ext.qa.voxeolabs.net:/opt/voxeo/prism/logs/sipmethod.log* remotelogs/gw
scp jenkins@${HOSTNAME}-gw-ext.qa.voxeolabs.net:/opt/voxeo/prism/logs/vcs/log* remotelogs/gw/vcs
scp jenkins@${HOSTNAME}-node-ext.qa.voxeolabs.net:/opt/voxeo/prism/logs/sipmethod.log* remotelogs/node
scp jenkins@${HOSTNAME}-node-ext.qa.voxeolabs.net:/opt/voxeo/prism/logs/vcs/log* remotelogs/node/vcs

echo -e "##########################################\n#\n# Make sure no instances are still running\n#\n##########################################"

knife ec2 server delete `knife search node "name:${HOSTNAME}-gw.qa.voxeolabs.net" -Fj | grep instance_id | awk 'BEGIN{FS="\""} {print $4}'` -y
knife client delete ${HOSTNAME}-gw.qa.voxeolabs.net -y
knife node delete ${HOSTNAME}-gw.qa.voxeolabs.net -y

knife ec2 server delete `knife search node "name:${HOSTNAME}-node.qa.voxeolabs.net" -Fj | grep instance_id | awk 'BEGIN{FS="\""} {print $4}'` -y
knife client delete ${HOSTNAME}-node.qa.voxeolabs.net -y
knife node delete ${HOSTNAME}-node.qa.voxeolabs.net -y
