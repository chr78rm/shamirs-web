#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
DATA_DIR=data/mariadb/1

# evaluate parameter
DATA_DIR_REGEX="^--data-dir=[A-Za-z0-9/]{1,50}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${DATA_DIR_REGEX} ]]
	then
		DATA_DIR=${ARG:11}
	fi
done
echo DATA_DIR=$HOME/$DATA_DIR

docker run --publish 127.0.0.1:3306:3306 --name docker-mariadb --detach --rm --mount type=bind,src=$HOME/$DATA_DIR,dst=/var/lib/mysql \
--health-cmd='mysqladmin --password="$MARIADB_ROOT_PASSWORD" ping' --health-interval=5s --health-retries=6 mariadb:10.6.3-focal

while [ $(docker inspect --format={{.State.Health.Status}} docker-mariadb) != "healthy" ]
do
	echo Health check status = $(docker inspect --format={{.State.Health.Status}} docker-mariadb)
	sleep 1; 
done
