FROM my-openjdk:latest

RUN mkdir shamirs-service shamirs-service/target shamirs-service/log
COPY target/shamirs-service.jar shamirs-service/target
COPY shamirs-docker-application.properties shamirs-service/application.properties
WORKDIR shamirs-service

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/urandom", "-jar", "target/shamirs-service.jar"]
