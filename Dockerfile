ARG BASE_IMAGE=senzing/senzingapi-runtime:latest
FROM ${BASE_IMAGE}

ENV REFRESHED_AT=2023-06-19

LABEL Name="senzing/elasticsearch" \
  Maintainer="support@senzing.com" \
  Version="1.1.0"

# Run as "root" for system installation.

USER root

COPY elasticsearch /build
WORKDIR /build

RUN apt-get update \
  && apt-get -y install postgresql-client \
  && apt-get -y install openjdk-11-jre-headless maven \
  && apt-get -y clean \
  && ls \
  && mvn clean install \
  && mkdir /app \
  && cp target/g2elasticsearch-1.0.0-SNAPSHOT.jar /app/ \
  && cd / \
  && rm -rf /build \
  && apt-get -y remove maven \
  && apt-get -y autoremove \
  && apt-get -y clean

mvn clean install
HEALTHCHECK CMD test -f /app/g2elasticsearch-1.0.0-SNAPSHOT.jar

USER 1001

WORKDIR /app
CMD ["java", "-jar", "g2elasticsearch-1.0.0-SNAPSHOT.jar"]
