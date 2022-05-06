#!/bin/bash

# terminate on error
set -o errexit

# all parameter
ARGS=$*

# evaluate parameter
ALIAS_REGEX="^--alias=[a-z0-9-]{1,25}$"
KEYSTORE_REGEX="^--keystore=[A-Za-z]+[A-Za-z0-9-]{1,50}.p12$"

# defaults
ALIAS=docker-shamirs-service-id
KEYSTORE=docker-service-id.p12

# evaluate args
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${ALIAS_REGEX} ]]
	then
		ALIAS=${ARG:8}
	fi
	if [[ ${ARG} =~ ${KEYSTORE_REGEX} ]]
	then
		KEYSTORE=${ARG:11}
	fi
done
echo KEYSTORE=${KEYSTORE}
echo ALIAS=${ALIAS}

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# start container
docker run --interactive --tty --rm --network=shamirs-network --hostname=shamirs-service --name=shamirs-service --publish 8443:8443 --env TZ="Europe/Berlin" \
--mount type=bind,src=${PROJECT_DIR}/data/log,dst=/home/vodalus/shamirs-service/log --detach shamirs-service:latest --keystore=${KEYSTORE} --alias=${ALIAS}
