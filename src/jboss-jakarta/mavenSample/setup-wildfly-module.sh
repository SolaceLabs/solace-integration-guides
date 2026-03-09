#!/bin/bash 
set -e

echo "=== Setting up WildFly container ==="

# Remove existing container if present
echo "Removing existing WildFly container..."
docker rm -f wildfly 2>/dev/null || true

# Start WildFly
echo "Creating WildFly container..."
docker create -p 18080:8080 -p 19990:9990 --name=wildfly --network solace-net quay.io/wildfly/wildfly

# Deploy Solace JCA Resource Adapter
echo "Deploying Solace JCA Resource Adapter..."
#docker exec wildfly /bin/bash -c "mkdir -p /opt/jboss/wildfly/modules/com/solacesystems/ra/main"
UNZIP_DIR=`mktemp -d`
mkdir -p ${UNZIP_DIR}/com/solacesystems/ra/main/
cp ../sol-jms-jakarta-ra-0.0.0-SNAPSHOT.rar ${UNZIP_DIR}/com/solacesystems/ra/main/
pushd ${UNZIP_DIR}
pushd com/solacesystems/ra/main/
unzip sol-jms-jakarta-ra-0.0.0-SNAPSHOT.rar
popd
chmod -R a+r ./
docker cp ./ wildfly:/opt/jboss/wildfly/modules/
popd
docker cp module.xml  wildfly:/opt/jboss/wildfly/modules/com/solacesystems/ra/main/
docker cp jts_module.xml  wildfly:/opt/jboss/wildfly/modules/system/layers/base/org/jboss/jts/main/module.xml
docker cp standalone.xml wildfly:/opt/jboss/wildfly/standalone/configuration/

# Deploy the EJB application
echo "Deploying EJB application..."
docker cp ear/target/EJBSample-ear-1.0.0-SNAPSHOT.ear wildfly:/opt/jboss/wildfly/standalone/deployments/

echo "Starting WildFly container..."
docker start wildfly

