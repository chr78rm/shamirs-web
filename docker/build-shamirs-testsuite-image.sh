#!/bin/bash

# terminate on error
set -o errexit

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# build image
cd ${PROJECT_DIR}/shamirs-test-suite
mvn clean verify -P docker

# switch back
cd ${CURRENT_DIR}
