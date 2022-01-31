#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# directories
CURRENT_DIR=$(pwd)
PROJECT_DIR=$(dirname $(dirname $(realpath $0)))

# defaults
# PROJECT_DIR=projects/shamirs-web
DATA_DIR=${PROJECT_DIR}/data/mariadb/1
MARIADB_TAG=10.6.5-focal
USER_PW=Msiw47Ut129
DOCKER_BRIDGE_GATEWAY=172.17.0.1
DOCKER_SHAMIRS_GATEWAY=172.19.0.1
DOCKER_SHAMIRS_IP=172.19.0.3

# evaluate parameter
MARIADB_ROOT_PW_REGEX="^--mariadb_root_pw=[A-Za-z0-9\(\)]{1,25}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${MARIADB_ROOT_PW_REGEX} ]]
	then
		ROOT_PW=${ARG:18}
	fi
done

# print parameter
echo ROOT_PW=$ROOT_PW
echo PROJECT_DIR=$PROJECT_DIR
echo DATA_DIR=$DATA_DIR

# check preconditions
if [ x$ROOT_PW = "x" ]
then
	echo MariaDBs root password is missing. Aborting ...
	exit
fi

HEALTH_CMD="mysqladmin --user=root --password=$ROOT_PW --silent ping"
docker run --publish 127.0.0.1:3306:3306 --name docker-mariadb --env MARIADB_ROOT_PASSWORD="$ROOT_PW" --env TZ=Europe/Berlin --detach --rm --mount type=bind,src=$DATA_DIR,dst=/var/lib/mysql \
--health-cmd='$HEALTH_CMD' --health-interval=5s --health-retries=6 --network=shamirs-network --hostname=shamirs-db mariadb:$MARIADB_TAG

while [ $(docker inspect --format={{.State.Health.Status}} docker-mariadb) != "healthy" ]
do
	echo Health check status = $(docker inspect --format={{.State.Health.Status}} docker-mariadb)
	sleep 1; 
done
sleep 1

docker exec --interactive docker-mariadb bash -c 'exec mysql --user=root --password="$MARIADB_ROOT_PASSWORD" --verbose' <<-EOF
CREATE OR REPLACE USER 'shamir'@'localhost' IDENTIFIED BY '${USER_PW}';
CREATE OR REPLACE USER 'shamir'@'${DOCKER_BRIDGE_GATEWAY}' IDENTIFIED BY '${USER_PW}';
CREATE OR REPLACE USER 'shamir'@'${DOCKER_SHAMIRS_GATEWAY}' IDENTIFIED BY '${USER_PW}';
CREATE OR REPLACE USER 'shamir'@'${DOCKER_SHAMIRS_IP}' IDENTIFIED BY '${USER_PW}';
CREATE OR REPLACE DATABASE shamirs_db;
GRANT ALL ON shamirs_db.* TO 'shamir'@'localhost';
GRANT ALL ON shamirs_db.* TO 'shamir'@'${DOCKER_BRIDGE_GATEWAY}';
GRANT ALL ON shamirs_db.* TO 'shamir'@'${DOCKER_SHAMIRS_GATEWAY}';
GRANT ALL ON shamirs_db.* TO 'shamir'@'${DOCKER_SHAMIRS_IP}';
GRANT FILE ON *.* TO 'shamir'@'localhost';
EOF

docker exec --interactive docker-mariadb bash -c "exec mysql --user=shamir --password=$USER_PW --database=shamirs_db --verbose" < $PROJECT_DIR/sql/mariadb/create-schema.sql

