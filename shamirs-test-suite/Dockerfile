FROM my-openjdk:17.0.2-ubuntu-focal
USER root
RUN apt-get update
RUN apt-get update && apt-get upgrade --assume-yes && apt-get --assume-yes install mariadb-client
USER vodalus
RUN mkdir shamirs-test-suite shamirs-test-suite/target shamirs-test-suite/log shamirs-test-suite/sql shamirs-test-suite/sql/mariadb
COPY target/shamirs-test-suite.jar shamirs-test-suite/target
COPY shamirs-app.properties shamirs-test-suite/app.properties
COPY test-config.xml shamirs-test-suite
COPY sql/mariadb/setup-scenario.sql sql/mariadb/shamir-db.user.ini shamirs-test-suite/sql/mariadb
WORKDIR shamirs-test-suite
