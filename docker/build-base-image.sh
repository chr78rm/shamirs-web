#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# defaults
JDK_VERSION=17.0.2

# build image
cd ${PROJECT_DIR}/docker
docker build --tag=my-openjdk:${JDK_VERSION}-ubuntu-focal --file=Dockerfile-openjdk-ubuntu .

# switch back
cd ${CURRENT_DIR}
