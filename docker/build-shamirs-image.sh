#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# build image
cd ${PROJECT_DIR}/shamirs-service
export BUILD_TIME=$(date +%Y%m%d-%H%M%S)
docker build --tag=shamirs-service:${BUILD_TIME} --tag=shamirs-service:latest --file=Dockerfile .

# switch back
cd ${CURRENT_DIR}
