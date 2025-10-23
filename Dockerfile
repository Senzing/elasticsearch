ARG BASE_IMAGE=senzing/senzingapi-runtime:3.13.0@sha256:edca155d3601238fab622a7dd86471046832328d21f71f7bb2ae5463157f6e10
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
