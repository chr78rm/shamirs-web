#!/bin/bash

set -o errexit # terminate on error
ARGS=$* # all parameter

# evaluate parameter
ALIAS_REGEX="^--alias=[a-z0-9-]{1,25}$"
ENDUSER_KEYSTORE_REGEX="^--keystore=[A-Za-z]+[A-Za-z0-9-]{1,50}$"
CERTIFICATE_VALIDITY_REGEX="^--cert-validity=[0-9]{2,4}$"
COMMON_NAME_REGEX="^--common-name=[A-Za-z0-9-]{1,50}$"
IM_PW_REGEX="^--password=[A-Za-z0-9]{1,25}$"

# defaults
COMMON_NAME_ARG=localhost

for ARG in ${ARGS} 
do
	if [[ ${ARG} =~ ${ALIAS_REGEX} ]]
	then
		ALIAS_ARG=${ARG:8}
	fi
	if [[ ${ARG} =~ ${ENDUSER_KEYSTORE_REGEX} ]]
	then
		KEYSTORE_ARG=${ARG:11}
	fi
	if [[ ${ARG} =~ ${CERTIFICATE_VALIDITY_REGEX} ]]
	then
		CERTIFICATION_VALIDITY_ARG=${ARG:16}
	fi
	if [[ ${ARG} =~ ${COMMON_NAME_REGEX} ]]
	then
		COMMON_NAME_ARG=${ARG:14}
	fi
	if [[ ${ARG} =~ ${IM_PW_REGEX} ]]
	then
		IM_PASSWORD=${ARG:11}
	fi
done
echo alias=${ALIAS_ARG}
echo keystore=${KEYSTORE_ARG}
echo certification-validity=${CERTIFICATION_VALIDITY_ARG}
echo common-name=${COMMON_NAME_ARG}

# constants
IM_KEYSTORE_FILE=my-intermediate-ca.p12
ENDUSER_KEYSTORE_FILE=${KEYSTORE_ARG}.p12
ENDUSER_PASSWORD=changeit
ENDUSER_TRUSTSTORE_FILE=${KEYSTORE_ARG}-trust.p12
DNAME="CN=${COMMON_NAME_ARG}, L=Rodgau, ST=Hessen, C=DE"

# generate key pair
keytool -J-Duser.language=en -genkeypair -alias ${ALIAS_ARG} -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "${DNAME}" \
-keypass ${ENDUSER_PASSWORD} -validity 1825 -storetype pkcs12 -keystore ${ENDUSER_KEYSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v

# request pkcs10 certification
keytool -J-Duser.language=en -certreq -alias ${ALIAS_ARG} -sigalg SHA256withRSA -file certreq.p10 -keypass ${ENDUSER_PASSWORD} -keystore ${ENDUSER_KEYSTORE_FILE} \
-storepass ${ENDUSER_PASSWORD} -storetype pkcs12

# process pkcs10 certification request for enduser id
keytool -J-Duser.language=en -gencert -rfc -infile certreq.p10 -outfile ${KEYSTORE_ARG}.pem -alias intermediate-ca -sigalg SHA256withRSA -validity ${CERTIFICATION_VALIDITY_ARG} -startdate +0d \
-keypass ${IM_PASSWORD} -storepass ${IM_PASSWORD} -storetype pkcs12 -keystore ${IM_KEYSTORE_FILE}

# import root certificate
keytool -J-Duser.language=en -importcert -noprompt -alias root-ca -file root-ca.pem -keystore ${ENDUSER_KEYSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v

# import intermediate certificate
keytool -J-Duser.language=en -importcert -noprompt -alias intermediate-ca -file intermediate-ca.pem -keystore ${ENDUSER_KEYSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v

# import the certificate reply for enduser-id
keytool -J-Duser.language=en -importcert -keystore ${ENDUSER_KEYSTORE_FILE} -alias ${ALIAS_ARG} -file ${KEYSTORE_ARG}.pem -keypass ${ENDUSER_PASSWORD} -storepass ${ENDUSER_PASSWORD} -v

# list entries
keytool -J-Duser.language=en -list -keystore ${ENDUSER_KEYSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v

# make truststore with certificates
keytool -J-Duser.language=en -importcert -noprompt -alias root-ca -file root-ca.pem -keystore ${ENDUSER_TRUSTSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v
keytool -J-Duser.language=en -importcert -alias intermediate-ca -file intermediate-ca.pem -keystore ${ENDUSER_TRUSTSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v
keytool -J-Duser.language=en -importcert -alias ${ALIAS_ARG} -file ${KEYSTORE_ARG}.pem -keystore ${ENDUSER_TRUSTSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v

# list entries
keytool -J-Duser.language=en -list -keystore ${ENDUSER_TRUSTSTORE_FILE} -storepass ${ENDUSER_PASSWORD} -v
