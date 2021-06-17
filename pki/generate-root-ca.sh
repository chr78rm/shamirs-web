#!/bin/bash

ROOT_KEYSTORE_FILE=my-root-ca.p12
ROOT_PASSWORD=QgpzVx8eZ33iDGIwLi48dE4vT

# generate key pair
keytool -genkeypair -alias root-ca -keyalg EC -groupname secp521r1 -sigalg SHA256withECDSA -dname "cn=cr-root, L=Rodgau, ST=Hessen, c=DE" \
-keypass ${ROOT_PASSWORD} -validity 1825 -storetype pkcs12 -keystore ${ROOT_KEYSTORE_FILE} -storepass ${ROOT_PASSWORD} -v \
-ext BasicConstraints=ca:true,PathLen:3

# export certificate
keytool -exportcert -rfc -alias root-ca -file root-ca.pem -keystore ${ROOT_KEYSTORE_FILE} -storepass ${ROOT_PASSWORD} -v
