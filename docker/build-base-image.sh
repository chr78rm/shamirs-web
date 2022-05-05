#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# defaults
JDK_NAME=openjdk-18.0.1.1
SHA256_CHECKSUM=4f81af7203fa4c8a12c9c53c94304aab69ea1551bc6119189c9883f4266a2b24
DOWNLOAD_LINK=https://download.java.net/java/GA/jdk18.0.1.1/65ae32619e2f40f3a9af3af1851d6e19/2/GPL/openjdk-18.0.1.1_linux-x64_bin.tar.gz
UBUNTU_VERSION=ubuntu-20.04
MY_UID=1000
MY_GID=1000

# build image
cd ${PROJECT_DIR}/docker
docker build --tag=my-openjdk:${JDK_NAME}-${UBUNTU_VERSION} --tag=my-openjdk:latest --build-arg JDK_NAME --build-arg SHA256_CHECKSUM --build-arg DOWNLOAD_LINK \
	 --build-arg MY_UID --build-arg MY_GID --file=Dockerfile-openjdk-ubuntu .
