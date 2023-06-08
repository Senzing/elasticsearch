# Senzing with ElasticSearch

## Overview

This code project demonstrates how the G2 engine may be used with an ElasticSearch indexing engine.  ElasticSearch provides enhanced searching capabilities on entity data.  This project is currently in the prototype stage and the APIs and functionality are expected to change significantly.

The G2 data repository contains data records and observations about known entities.  It determines which records match/merge to become single resolved entities.  These resolved entities can be indexed through the ElasticSearch engine, to provide more searchable data entities.

ElasticSearch stores its indexed entity data in a separate data repository than the G2 engine does.  Thus, ElasticSearch and G2 must both be managed in order to keep them in sync.

This Java project shows how these two tools can be combined in common code.

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
  - [elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html)
  - [kibana](https://www.elastic.co/guide/en/kibana/current/install.html)


## Demonstration

### Startup elasticsearch

Start an instance of elasticsearch and your favorite elastic search UI, kibana is recommended and will be assumed for the remainer of this demonstration. 
For guidence on how to get an instance of ES and kibana running vist [How to Bring Up an ELK Stack](https://senzing.atlassian.net/wiki/spaces/~5b326ce59049987392f679a4/pages/2656927745/How+to+bring+up+an+ELK+stack)

1. :pencil2: Set GitHub repository environment variables.  These variables may be modified, but do not need to be modified.  The variables are used throughout the installation procedure.

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
    
1. :thinking: Make sure the [SENZING_ENGINE_CONFIGURATION_JSON](https://github.com/Senzing/knowledge-base/blob/b9588bcc22e92993fbd5415172c2abd8d0402356/lists/environment-variables.md#senzing_engine_configuration_json) is set to the senzing project that has or will have the data loaded to.

3. :thinking: Set elasticsearch local environment variables. The hostname and port must point towards the exposed port that your elasticsearch instance has. The index name can be whatever you want; conforming to elasticsearch's index syntax.

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

1. ✏️ Copy the interface library into a working directory

    ```console
    sudo mkdir ${SENZING_DIR}/g2/elasticsearch
    cd ${SENZING_DIR}/g2/elasticsearch

    sudo cp \
      ${GIT_REPOSITORY_DIR}/elasticsearch/target/g2elasticsearch-1.0.0-SNAPSHOT.jar \
      ${SENZING_DIR}/g2/elasticsearch/g2elasticsearch.jar
    ```

1. 🤔 Run the indexer, **make sure that you already have some data loaded into G2**, if you don't you can use our truthset with instructions from the [quickstart](https://senzing.zendesk.com/hc/en-us/articles/115002408867-Quickstart-Guide-)

```console
java -classpath g2elasticsearch.jar com.senzing.g2.elasticsearch.G2toElastic
```

### View Data

1. Open up kibana in a web browser, default: [localhost://5601](localhost:5601)

1. Navigate to the discover tab, here you can view your created index and do searches. If you want to do fuzzy searches click on "Saved Query" and switch the language to lucene. [Here](https://www.elastic.co/guide/en/elasticsearch/reference/8.8/query-dsl-query-string-query.html#query-string-fuzziness) you can view the lucene syntax and how to do fuzzy searches
<img width="246" alt="image" src="https://github.com/SamMacy/elasticsearch/assets/49598357/c77b8f8b-6877-4701-9677-511e5aafb81f">
