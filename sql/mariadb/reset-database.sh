#!/bin/bash

mysql --defaults-extra-file=shamir-db.user.ini < create-schema.sql
mysql --defaults-extra-file=shamir-db.user.ini < setup-scenario.sql
