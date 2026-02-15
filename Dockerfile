ARG BASE_IMAGE=senzing/senzingapi-runtime:3.13.0@sha256:c9c3502b35fbcc30d3cdbe3597392f964c7a15db52736dac938d28916d121f70
FROM ${BASE_IMAGE}

ENV REFRESHED_AT=2025-10-22

LABEL Name="senzing/elasticsearch" \
      Maintainer="support@senzing.com" \
      Version="1.1.0"

# Run as "root" for system installation.

USER root

COPY elasticsearch /build
WORKDIR /build

RUN apt-get update \
  && apt-get -y install --no-install-recommends \
      postgresql-client \
      openjdk-21-jre-headless \
      maven \
  && apt-get -y clean \
  && mvn clean install \
  && mkdir /app \
  && cp target/g2elasticsearch-1.0.0-SNAPSHOT.jar /app/ \
  && rm -rf /build \
  && apt-get -y remove maven \
  && apt-get -y autoremove \
  && apt-get -y clean

HEALTHCHECK CMD test -f /app/g2elasticsearch-1.0.0-SNAPSHOT.jar

USER 1001

WORKDIR /app
CMD ["java", "-jar", "g2elasticsearch-1.0.0-SNAPSHOT.jar"]
