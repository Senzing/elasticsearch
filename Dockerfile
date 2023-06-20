ARG BASE_IMAGE=debian:11.7-slim@sha256:924df86f8aad741a0134b2de7d8e70c5c6863f839caadef62609c1be1340daf5
FROM ${BASE_IMAGE}

ENV REFRESHED_AT=2023-06-19

LABEL Name="senzing/elasticsearch" \
      Maintainer="support@senzing.com" \
      Version="1.1.0	"
      
HEALTHCHECK CMD ["/app/healthcheck.sh"]

# Run as "root" for system installation.

USER root

ARG MAVEN_VERSION=3.8.6

RUN apt-get update \
 && apt-get -y install \
      python3 \
      python3-pip \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*
 
 RUN apt update \
 && apt -y install \
      build-essential \
      curl \
      gdb \
      jq \
      libbz2-dev \
      libffi-dev \
      libgdbm-dev \
      libncursesw5-dev \
      libreadline-dev \
      libsqlite3-dev \
      libssl-dev \
      libssl1.1 \
      lsb-release \
      odbc-postgresql \
      odbcinst \
      postgresql-client \
      python3-dev \
      python3-pip \
      sqlite3 \
      tk-dev \
      unixodbc \
      vim \
      wget \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Install packages via PIP.

COPY requirements.txt ./
RUN pip3 install --upgrade pip \
 && pip3 install -r requirements.txt \
 && rm requirements.txt

# Install OpenJDK-11
RUN apt-get update  \
 && apt-get install -y openjdk-11-jre-headless  \
 && apt-get clean

RUN curl -O -k https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
 && tar xzvf apache-maven-${MAVEN_VERSION}-bin.tar.gz
 
ENV PATH = ${PATH}:/apache-maven-${MAVEN_VERSION}/bin


# Install packages via apt.

# Copy files from repository.

COPY ./rootfs /

