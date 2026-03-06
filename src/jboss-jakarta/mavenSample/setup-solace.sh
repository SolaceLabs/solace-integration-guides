#!/bin/bash
set -e

echo "=== Setting up Solace PubSub+ Broker container ==="

# Remove existing container if present
echo "Removing existing Solace container..."
docker rm -f solace 2>/dev/null || true

# Start Solace broker
echo "Starting Solace broker container..."
docker run -d \
  -p 8080:8080 \
  -p 55555:55555 \
  -p 9001:9001 \
  --shm-size=1g \
  --env username_admin_globalaccesslevel=admin \
  --env username_admin_password=admin \
  --name=solace \
  --network solace-net \
  solace/solace-pubsub-standard

# Wait for broker to start
echo "Waiting for Solace broker to start (~60 seconds)..."
sleep 60

# Configure the broker
echo "Configuring message VPN, queues, JNDI, and REST service..."
docker cp solace-broker-config.cli solace:/usr/sw/jail/cliscripts/
docker exec solace /usr/sw/loads/currentload/bin/cli -A -s solace-broker-config.cli

