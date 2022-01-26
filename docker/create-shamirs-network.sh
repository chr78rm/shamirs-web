#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

NETWORK_ID=$(docker network ls --quiet --filter name=shamirs-network)
if [ "x$NETWORK_ID" = "x" ] 
then
    docker network create shamirs-network
fi
