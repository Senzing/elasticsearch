# Senzing with ElasticSearch

## Overview

This code project demonstrates how the G2 engine may be used with an ElasticSearch indexing engine.  ElasticSearch provides enhanced searching capabilities on entity data.

The G2 data repository contains data records and observations about known entities.  It determines which records match/merge to become single resolved entities.  These resolved entities can be indexed through the ElasticSearch engine, to provide more searchable data entities.

ElasticSearch stores its indexed entity data in a separate data repository than the G2 engine does.  Thus, ElasticSearch and G2 must both be managed in order to keep them in sync.

### Preamble

At [Senzing](http://senzing.com),
we strive to create GitHub documentation in a
"[don't make me think](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/dont-make-me-think.md)" style.
For the most part, instructions are copy and paste.
Whenever thinking is needed, it's marked with a "thinking" icon :thinking:.
Whenever customization is needed, it's marked with a "pencil" icon :pencil2:.
If the instructions are not clear, please let us know by opening a new
[Documentation issue](https://github.com/Senzing/template-python/issues/new?template=documentation_request.md)
describing where we can improve.   Now on with the show...

### Legend

1. :thinking: - A "thinker" icon means that a little extra thinking may be required.
   Perhaps there are some choices to be made.
   Perhaps it's an optional step.
1. :pencil2: - A "pencil" icon means that the instructions may need modification before performing.
1. :warning: - A "warning" icon means that something tricky is happening, so pay attention.

### Expectations

- **Space:** This repository and demonstration require X GB free disk space.
- **Time:** Budget 30 minutes to get the demonstration up-and-running, depending on CPU and network speeds.
- **Background knowledge:** This repository assumes a working knowledge of:

  - [Docker](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/docker.md)
  - [Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html)
  - [git](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/git.md)
  - [kibana](https://www.elastic.co/guide/en/kibana/current/install.html)
## Prerequisites

1. [Docker](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/docker.md)
1. [git](https://github.com/Senzing/knowledge-base/blob/main/WHATIS/git.md).

## Demonstration

### Load Data
- 🤔 Data needs to be loaded into a Senzing project to post to elasticsearch, if you don't have any data to load, or don't know how, visit our [quickstart](https://senzing.zendesk.com/hc/en-us/articles/115002408867-Quickstart-Guide-).

### Startup elasticsearch

- Start an instance of elasticsearch and your favorite elastic search UI, kibana is recommended and will be assumed for the remainder of this demonstration. 
For guidance on how to get an instance of ES and kibana running vist our doc on [How to Bring Up an ELK Stack](https://github.com/Senzing/knowledge-base/blob/main/HOWTO/bring-up-ELK-stack.md).

### Build project

1. :pencil2: Set local environment variables.  These variables may be modified, but do not need to be modified.  The variables are used throughout the installation procedure.

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
    
1. :thinking: Make sure the [SENZING_ENGINE_CONFIGURATION_JSON](https://github.com/Senzing/knowledge-base/blob/main/lists/environment-variables.md#senzing_engine_configuration_json) environment variable is set to the Senzing installation that the data was loaded into earlier

3. :thinking: Set elasticsearch local environment variables. The hostname and port must point towards the exposed port that the elasticsearch instance has. The index name can be anything; conforming to elasticsearch's index syntax.

    ```console
    export ELASTIC_HOSTNAME=localhost
    export ELASTIC_PORT=9200
    export ELASTIC_INDEX_NAME=g2index
    ```

1. Build the docker container.

   ```console
   cd {GIT_REPOSITORY_DIR}
   sudo docker build -t senzing/elasticsearch .
   ```

### Run the indexer

#### Using a local sql project

1. We will mount the sqlite database; make sure the `CONNECTION` string in our config json points to where it is mounted. In this example the `CONNECTION` will need to be set to `/db`

    ```console
    docker run \
      --interactive \
      --rm \
      --tty \
      -e ${ELASTIC_HOSTNAME} \
      -e ${ELASTIC_PORT} \
      -e ${ELASTIC_INDEX_NAME} \
      -e ${SENZING_ENGINE_CONFIGURATION_JSON} \
      --volume ~/senzing/var/sqlite:/db \
      senzing/elasticsearch
    ```
### Search data

1. Open up kibana in a web browser, default: [localhost:5601](http://localhost:5601)

2. Navigate to the discover tab 

<img width="200" alt="image" src="https://github.com/Senzing/elasticsearch/assets/49598357/b7663a5b-b940-4ca6-b3b6-dc0250a5f3ba">

3. Create Index.
   * If all was done correctly, a new screen with a button to "Create data view" should appear.
   * Click this and in the `index pattern` box type the name of the index that was created, this was the `ELASTIC_INDEX_NAME` variable set early, and should also appear on the right side of the popup.
   * The `Name` field can be set but is not required.

5. Press "Save data view to Kibana" at the bottom of the screen, now can view the created index and do searches. If fuzzy searches are needed click on "Saved Query" and switch the language to lucene. [Here](https://www.elastic.co/guide/en/elasticsearch/reference/8.8/query-dsl-query-string-query.html#query-string-fuzziness) you can view the lucene syntax and how to do fuzzy searches
<img width="246" alt="image" src="https://github.com/SamMacy/elasticsearch/assets/49598357/c77b8f8b-6877-4701-9677-511e5aafb81f">
