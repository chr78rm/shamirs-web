#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# start container
docker run --interactive --tty --rm --network=shamirs-network --hostname=shamirs-service --name=shamirs-service --publish 127.0.0.1:8443:8443 \
--mount type=bind,src=${PROJECT_DIR}/data/log,dst=/home/vodalus/shamirs-service/log --detach shamirs-service:latest
