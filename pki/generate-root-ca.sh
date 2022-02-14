#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
ROOT_KEYSTORE_FILE=my-root-ca.p12

# evaluate parameter
ROOT_PW_REGEX="^--password=[A-Za-z0-9]{1,25}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${ROOT_PW_REGEX} ]]
	then
		ROOT_PASSWORD=${ARG:11}
	fi
done

# generate key pair
keytool -J-Duser.language=en -genkeypair -alias root-ca -keyalg EC -groupname secp521r1 -sigalg SHA256withECDSA -dname "cn=cr-root, L=Rodgau, ST=Hessen, c=DE" \
-keypass ${ROOT_PASSWORD} -validity 1825 -storetype pkcs12 -keystore ${ROOT_KEYSTORE_FILE} -storepass ${ROOT_PASSWORD} -v \
-ext BasicConstraints=ca:true,PathLen:3

# export certificate
keytool -J-Duser.language=en -exportcert -rfc -alias root-ca -file root-ca.pem -keystore ${ROOT_KEYSTORE_FILE} -storepass ${ROOT_PASSWORD} -v
