#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
DATA_DIR=data/mariadb/1

# evaluate parameter
MARIADB_ROOT_PW_REGEX="^--mariadb_root_pw=[A-Za-z0-9\(\)]{1,25}$"
DATA_DIR_REGEX="^--data-dir=[A-Za-z0-9/]{1,50}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${MARIADB_ROOT_PW_REGEX} ]]
	then
		ROOT_PW=${ARG:18}
	fi
	if [[ ${ARG} =~ ${DATA_DIR_REGEX} ]]
	then
		DATA_DIR=${ARG:11}
	fi
done

# print parameter
echo ROOT_PW=$ROOT_PW
echo DATA_DIR=$HOME/$DATA_DIR

# check preconditions
if [ x$ROOT_PW = "x" ]
then
	echo MariaDBs root password is missing. Aborting ...
	exit
fi

# start container with health check
HEALTH_CMD="mysqladmin --user=root --password=$ROOT_PW --silent ping"
docker run --publish 127.0.0.1:3306:3306 --name docker-mariadb --detach --rm --env TZ=Europe/Berlin --mount type=bind,src=$HOME/$DATA_DIR,dst=/var/lib/mysql \
--health-cmd='$HEALTH_CMD' --health-interval=5s --health-retries=6 mariadb:10.6.3-focal

# wait until container is healthy
while [ $(docker inspect --format={{.State.Health.Status}} docker-mariadb) != "healthy" ]
do
	echo Health check status = $(docker inspect --format={{.State.Health.Status}} docker-mariadb)
	sleep 1; 
done