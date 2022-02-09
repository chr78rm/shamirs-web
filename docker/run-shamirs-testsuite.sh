#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# start container
docker run --interactive --tty --rm --network=shamirs-network --hostname=shamirs-test-suite --name=shamirs-test-suite \
--mount type=bind,src=${PROJECT_DIR}/data/log,dst=/home/vodalus/shamirs-test-suite/log shamirs-test-suite:latest
