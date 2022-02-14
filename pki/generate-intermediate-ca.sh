#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# defaults
ROOT_KEYSTORE_FILE=my-root-ca.p12
IM_KEYSTORE_FILE=my-intermediate-ca.p12


# evaluate parameter
ROOT_PW_REGEX="^--root-password=[A-Za-z0-9]{1,25}$"
IM_PW_REGEX="^--password=[A-Za-z0-9]{1,25}$"
for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${ROOT_PW_REGEX} ]]
	then
		ROOT_PASSWORD=${ARG:16}
	fi
	if [[ ${ARG} =~ ${IM_PW_REGEX} ]]
	then
		IM_PASSWORD=${ARG:11}
	fi
done

# generate key pair
keytool -J-Duser.language=en -genkeypair -alias intermediate-ca -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "cn=cr-intermediate, L=Rodgau, ST=Hessen, c=DE" \
-keypass ${IM_PASSWORD} -validity 1825 -storetype pkcs12 -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v 

# request pkcs10 certification
keytool -J-Duser.language=en -certreq -alias intermediate-ca -sigalg SHA256withRSA -file certreq.p10 -keypass ${IM_PASSWORD} -keystore ${IM_KEYSTORE_FILE} \
-storepass ${IM_PASSWORD} -storetype pkcs12

# process pkcs10 certification request for intermediate-ca
keytool -J-Duser.language=en -gencert -rfc -infile certreq.p10 -outfile intermediate-ca.pem -alias root-ca -sigalg SHA256withECDSA -validity 365 \
-keypass ${ROOT_PASSWORD} -storepass ${ROOT_PASSWORD} -storetype pkcs12 -keystore ${ROOT_KEYSTORE_FILE} -ext BasicConstraints=ca:true,PathLen:2

# import root certificate
keytool -J-Duser.language=en -importcert -noprompt -alias root-ca -file root-ca.pem -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v

# import the certificate reply for intermediate-ca
keytool -J-Duser.language=en -importcert -keystore ${IM_KEYSTORE_FILE} -alias intermediate-ca -file intermediate-ca.pem -keypass ${IM_PASSWORD} -storepass ${IM_PASSWORD} -v

# list entries
keytool -J-Duser.language=en -list -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v
