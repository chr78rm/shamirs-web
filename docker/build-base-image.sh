#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
SCRIPT_DIR=$(dirname $(realpath $0))

# defaults
export JDK_NAME=openjdk-20
export SHA256_CHECKSUM=bb863b2d542976d1ae4b7b81af3e78b1e4247a64644350b552d298d8dc5980dc
export DOWNLOAD_LINK=https://download.java.net/java/GA/jdk20/bdc68b4b9cbc4ebcb30745c85038d91d/36/GPL/openjdk-20_linux-x64_bin.tar.gz
UBUNTU_VERSION=ubuntu-22.04
export MY_UID=1000
export MY_GID=1000

# build image
cd ${SCRIPT_DIR}/docker
docker build --tag=my-openjdk:${JDK_NAME}-${UBUNTU_VERSION} --tag=my-openjdk:latest --build-arg JDK_NAME --build-arg SHA256_CHECKSUM --build-arg DOWNLOAD_LINK \
	 --build-arg MY_UID --build-arg MY_GID --file=Dockerfile-openjdk-ubuntu .
