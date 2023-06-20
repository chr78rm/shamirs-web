#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
SCRIPT_DIR=$(dirname $(realpath $0))

# defaults
export JDK_NAME=openjdk-20.0.1
export SHA256_CHECKSUM=4248a3af4602dbe2aefdb7010bc9086bf34a4155888e837649c90ff6d8e8cef9
export DOWNLOAD_LINK=https://download.java.net/java/GA/jdk20.0.1/b4887098932d415489976708ad6d1a4b/9/GPL/openjdk-20.0.1_linux-x64_bin.tar.gz
UBUNTU_VERSION=ubuntu-22.04
export MY_UID=1000
export MY_GID=1000

# build image
cd ${SCRIPT_DIR}
docker build --tag=my-openjdk:${JDK_NAME}-${UBUNTU_VERSION} --tag=my-openjdk:latest --build-arg JDK_NAME --build-arg SHA256_CHECKSUM --build-arg DOWNLOAD_LINK \
	 --build-arg MY_UID --build-arg MY_GID --file=Dockerfile-openjdk-ubuntu .
