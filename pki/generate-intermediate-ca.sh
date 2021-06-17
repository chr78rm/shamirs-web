#!/bin/bash

IM_KEYSTORE_FILE=my-intermediate-ca.p12
IM_PASSWORD=Rqu4u0rmgbp0fsO1LHSAoV2Be
ROOT_KEYSTORE_FILE=my-root-ca.p12
ROOT_PASSWORD=QgpzVx8eZ33iDGIwLi48dE4vT

# generate key pair
keytool -genkeypair -alias intermediate-ca -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -dname "cn=cr-intermediate, L=Rodgau, ST=Hessen, c=DE" \
-keypass ${IM_PASSWORD} -validity 1825 -storetype pkcs12 -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v 

# request pkcs10 certification
keytool -certreq -alias intermediate-ca -sigalg SHA256withRSA -file certreq.p10 -keypass ${IM_PASSWORD} -keystore ${IM_KEYSTORE_FILE} \
-storepass ${IM_PASSWORD} -storetype pkcs12

# process pkcs10 certification request for intermediate-ca
keytool -gencert -rfc -infile certreq.p10 -outfile intermediate-ca.pem -alias root-ca -sigalg SHA256withECDSA -validity 365 \
-keypass ${ROOT_PASSWORD} -storepass ${ROOT_PASSWORD} -storetype pkcs12 -keystore ${ROOT_KEYSTORE_FILE} -ext BasicConstraints=ca:true,PathLen:2

# import root certificate
keytool -importcert -noprompt -alias root-ca -file root-ca.pem -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v

# import the certificate reply for intermediate-ca
keytool -importcert -keystore ${IM_KEYSTORE_FILE} -alias intermediate-ca -file intermediate-ca.pem -keypass ${IM_PASSWORD} -storepass ${IM_PASSWORD} -v

# list entries
keytool -list -keystore ${IM_KEYSTORE_FILE} -storepass ${IM_PASSWORD} -v
