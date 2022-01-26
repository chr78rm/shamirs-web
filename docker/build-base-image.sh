#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
JDK_VERSION=17.0.2

docker build --tag=my-openjdk:${JDK_VERSION}-ubuntu-focal --file=Dockerfile-openjdk-ubuntu .
