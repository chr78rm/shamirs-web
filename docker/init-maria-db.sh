#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
PROJECT_DIR=projects/shamirs-web
DATA_DIR=data/mariadb/1

# evaluate parameter
MARIADB_ROOT_PW_REGEX="^--mariadb_root_pw=[A-Za-z0-9\(\)]{1,25}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${MARIADB_ROOT_PW_REGEX} ]]
	then
		ROOT_PW=${ARG:18}
	fi
done
echo ROOT_PW=$ROOT_PW
echo PROJECT_DIR=$HOME/$PROJECT_DIR
echo DATA_DIR=$HOME/$DATA_DIR

# check preconditions
if [ x$ROOT_PW = "x" ]
then
	echo MariaDBs root password is missing. Aborting ...
	exit
fi

docker run --publish 127.0.0.1:3306:3306 --name docker-mariadb --env MARIADB_ROOT_PASSWORD="$ROOT_PW" --detach --rm --mount type=bind,src=$HOME/$DATA_DIR,dst=/var/lib/mysql \
--health-cmd='mysqladmin --password="$MARIADB_ROOT_PASSWORD" ping' --health-interval=5s --health-retries=6 mariadb:10.6.3-focal

while [ $(docker inspect --format={{.State.Health.Status}} docker-mariadb) != "healthy" ]
do
	echo Health check status = $(docker inspect --format={{.State.Health.Status}} docker-mariadb)
	sleep 1; 
done

docker exec --interactive docker-mariadb bash -c 'exec mysql --user=root --password="$MARIADB_ROOT_PASSWORD" --verbose' < $HOME/$PROJECT_DIR/sql/mariadb/create-database.sql
docker exec --interactive docker-mariadb bash -c 'exec mysql --user=shamir --password=Msiw47Ut129 --database=shamirs_db --verbose' < $HOME/$PROJECT_DIR/sql/mariadb/create-schema.sql

