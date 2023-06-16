# Senzing with ElasticSearch

## Synopsis

This code project demonstrates how the Senzing engine may be used with an ElasticSearch indexing engine.

## Overview

ElasticSearch provides enhanced searching capabilities on entity data.

The Senzing data repository contains data records and observations about known entities.
It determines which records match/merge to become single resolved entities.
These resolved entities can be indexed through the ElasticSearch engine, to provide more searchable data entities.

ElasticSearch stores its indexed entity data in a separate data repository than the Senzing engine does.
Thus, ElasticSearch and Senzing must both be managed in order to keep them in sync.

### Expectations

- **Space:** This repository and demonstration require 4 GB free disk space.
- **Time:** Budget 30 minutes to get the demonstration up-and-running, depending on CPU and network speeds.
- **Background knowledge:** This repository assumes a working knowledge of:
  - [docker](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/docker.md)
  - [ElasticSearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html)
  - [git](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/git.md)
  - [Kibana](https://www.elastic.co/guide/en/kibana/current/install.html)

## Prerequisites

1. [docker](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/docker.md) -
   Minimum version: [20.10.16](https://docs.docker.com/engine/release-notes/#201016)
1. [docker-compose](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/docker-compose.md) -
   Minimum version: [1.29.0](https://docs.docker.com/compose/release-notes/#1290)
1. [mvn]()

## Demonstration

### ElasticSearch stack

The following instructions will bring up a docker-compose stack that contains a
PostgreSql database,

1. :pencil2: Specify a new directory to hold demonstration artifacts on the local host.
   Example:

    ```console
    export SENZING_DEMO_DIR=~/my-senzing

    ```

    1. :warning:
       **macOS** - [File sharing](https://github.com/Senzing/knowledge-base/blob/main/HOWTO/share-directories-with-docker.md#macos)
       must be enabled for `SENZING_DEMO_DIR`.
    1. :warning:
       **Windows** - [File sharing](https://github.com/Senzing/knowledge-base/blob/main/HOWTO/share-directories-with-docker.md#windows)
       must be enabled for `SENZING_DEMO_DIR`.

1. Set environment variables.
   Example:

    ```console
    export PGADMIN_DIR=${SENZING_DEMO_DIR}/pgadmin
    export POSTGRES_DIR=${SENZING_DEMO_DIR}/postgres
    export RABBITMQ_DIR=${SENZING_DEMO_DIR}/rabbitmq
    export SENZING_GID=$(id -g)
    export SENZING_UID=$(id -u)
    export SENZING_VAR_DIR=${SENZING_DEMO_DIR}/var

    ```

1. Create directories.
   Example:

    ```console
    mkdir -p ${PGADMIN_DIR} ${POSTGRES_DIR} ${RABBITMQ_DIR} ${SENZING_VAR_DIR}
    chmod -R 777 ${SENZING_DEMO_DIR}

    ```

1. Get versions of Docker images.
   Example:

    ```console
    curl -X GET \
        --output ${SENZING_DEMO_DIR}/docker-versions-stable.sh \
        https://raw.githubusercontent.com/Senzing/knowledge-base/main/lists/docker-versions-stable.sh
    source ${SENZING_DEMO_DIR}/docker-versions-stable.sh

    ```

1. Download `docker-compose.yaml` and Docker images.
   Example:

    ```console
    curl -X GET \
        --output ${SENZING_DEMO_DIR}/docker-compose.yaml \
        "https://raw.githubusercontent.com/Senzing/docker-compose-demo/main/resources/postgresql/docker-compose-rabbitmq-postgresql-with-ELK.yaml"
    cd ${SENZING_DEMO_DIR}
    sudo --preserve-env docker-compose pull

    ```

1. Bring up Senzing docker-compose stack.
   Example:

    ```console
    cd ${SENZING_DEMO_DIR}
    sudo --preserve-env docker-compose up

    ```

1. Allow time for the components to be downloaded, start, and initialize.
    1. There will be errors in some Docker logs as they wait for dependent services to become available.
       `docker-compose` isn't the best at orchestrating Docker container dependencies.

### Startup ElasticSearch

Start an instance of ElasticSearch and your favorite elastic search user interface.
Kibana is a recommended Elastic Search user interface and will be assumed for the remainder of this demonstration.
For guidance on how to get an instance of ES and Kibana running vist our doc on
[How to Bring Up an ELK Stack](https://github.com/Senzing/knowledge-base/blob/main/HOWTO/bring-up-ELK-stack.md).

1. :pencil2: Set local environment variables.
   These variables may be modified, but do not need to be modified.
   The variables are used throughout the installation procedure.

    ```console
    export GIT_ACCOUNT=senzing
    export GIT_REPOSITORY=elasticsearch
    export GIT_ACCOUNT_DIR=~/${GIT_ACCOUNT}.git
    export GIT_REPOSITORY_DIR="${GIT_ACCOUNT_DIR}/${GIT_REPOSITORY}"
    ```

1. Clone the repository

    ```console
    cd {GIT_ACCOUNT_DIR}
    git clone https://github.com/Senzing/elasticsearch.git
    cd {GIT_REPOSITORY_DIR}
    ```

1. :thinking: Set the
    [SENZING_ENGINE_CONFIGURATION_JSON](https://github.com/Senzing/knowledge-base/blob/b9588bcc22e92993fbd5415172c2abd8d0402356/lists/environment-variables.md#senzing_engine_configuration_json)
    environment variable.
    Example:

    ```console
    export SENZING_ENGINE_CONFIGURATION_JSON='
    {
        "PIPELINE": {
            "CONFIGPATH": "/etc/opt/senzing",
            "RESOURCEPATH": "/opt/senzing/g2/resources",
            "SUPPORTPATH": "/opt/senzing/data"
        },
        "SQL": {
            "CONNECTION": "sqlite3://na:na@/var/opt/senzing/sqlite/G2C.db"
        }
    }
    ```

1. :thinking: Set ElasticSearch local environment variables. The hostname and port must point towards the exposed port that your ElasticSearch instance has. The index name can be whatever you want; conforming to ElasticSearch's index syntax.

    ```console
    export ELASTIC_HOSTNAME=localhost
    export ELASTIC_PORT=9200
    export ELASTIC_INDEX_NAME=g2index
    ```

1. Build the interface for ElasticSearch.

    ```console
    cd ${GIT_REPOSITORY_DIR}/elasticsearch

    mvn \
      -Dmaven.repo.local=${GIT_REPOSITORY_DIR}/elasticsearch/maven_resources \
      install
    ````

1. ✏️ Copy the library into a working directory

    ```console
    sudo mkdir /opt/senzing/g2/elasticsearch
    cd /opt/senzing/g2/elasticsearch

    sudo cp \
      ${GIT_REPOSITORY_DIR}/elasticsearch/target/g2elasticsearch-1.0.0-SNAPSHOT.jar \
      /opt/senzing/g2/elasticsearch/g2elasticsearch.jar
    ```

1. 🤔 make sure to source the senzing environment with `setupEnv` in the **same console window** that will be running the created `jar`. Instructions on how to do so are in the quickstart below

1. 🤔 Run the indexer, **make sure that you already have some data loaded into Senzing**, if you don't have your own data to load and/or are experimenting, you can use our truthset with instructions from the [quickstart](https://senzing.zendesk.com/hc/en-us/articles/115002408867-Quickstart-Guide-)

```console
java -classpath g2elasticsearch.jar com.senzing.g2.elasticsearch.G2toElastic
```

### Search your data

1. Open up Kibana in a web browser, default: [localhost://5601](localhost:5601)

1. Navigate to the discover tab

<img width="200" alt="image" src="https://github.com/Senzing/elasticsearch/assets/49598357/b7663a5b-b940-4ca6-b3b6-dc0250a5f3ba">

1. If all was done correctly, you should now see a new screen with a button to "Create data view". Click this and type in the name of the index that was created, this was the `ELASTIC_INDEX_NAME` variable set early, and should also appear on the right side of the popup

1. Press "Create data view" at the bottom of the screen, now you can view your created index and do searches. If you want to do fuzzy searches click on "Saved Query" and switch the language to lucene. [Here](https://www.elastic.co/guide/en/elasticsearch/reference/8.8/query-dsl-query-string-query.html#query-string-fuzziness) you can view the lucene syntax and how to do fuzzy searches
<img width="246" alt="image" src="https://github.com/SamMacy/elasticsearch/assets/49598357/c77b8f8b-6877-4701-9677-511e5aafb81f">
