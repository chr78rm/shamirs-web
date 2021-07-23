#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
PROJECT_DIR=projects/shamirs-web

# print parameter
echo PROJECT_DIR=$HOME/$PROJECT_DIR

# check preconditions
if [ $(docker container ls | grep docker-mariadb | wc --lines) = 0 ]
then
	echo MariaDB is not running. Aborting ...
	exit
fi
if [ ! $(docker inspect --format={{.State.Health.Status}} docker-mariadb) = "healthy" ]
then
	echo MariaDB is not healthy. Aborting ...
	exit
fi

docker exec --interactive docker-mariadb bash -c 'exec mysql --user=shamir --password=Msiw47Ut129 --database=shamirs_db --verbose' < $HOME/$PROJECT_DIR/sql/mariadb/create-schema.sql
docker exec --interactive docker-mariadb bash -c 'exec mysql --user=shamir --password=Msiw47Ut129 --database=shamirs_db --verbose' < $HOME/$PROJECT_DIR/sql/mariadb/setup-scenario.sql
