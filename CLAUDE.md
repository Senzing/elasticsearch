# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A small Java demo that exports every resolved entity from a Senzing v4 repository and bulk-indexes it into Elasticsearch, so entities can be searched in Kibana. It is a one-shot batch job, not a service: it runs, indexes everything, and exits. Elasticsearch and Senzing keep separate data stores, and nothing here keeps them in sync after the run.

## Build and run

The Maven project lives in `elasticsearch/`, not the repo root. Sources sit directly under `elasticsearch/src`, not `src/main/java`.

The Senzing v4 Java SDK (`com.senzing:sz-sdk`) is **not on Maven Central**. It ships with Senzing at `/opt/senzing/er/sdk/java/sz-sdk.jar` and must be installed into the local Maven repo first, at the version in the `sz-sdk.version` pom property:

```console
cd elasticsearch
mvn install:install-file -Dfile=/opt/senzing/er/sdk/java/sz-sdk.jar \
  -DgroupId=com.senzing -DartifactId=sz-sdk -Dversion=4.4.2 -Dpackaging=jar
mvn clean package          # produces target/g2elasticsearch-1.0.0-SNAPSHOT.jar (shaded, Main-Class = G2toElastic)
```

`sz-sdk` has `provided` scope and is not shaded in. The jar's manifest `Class-Path` points at `/opt/senzing/er/sdk/java/sz-sdk.jar`, so at runtime it uses the SDK that matches the installed native library. The SDK refuses to start if the jar version and native library version differ. To run outside Docker, put a matching `sz-sdk.jar` on `-cp`, point `-Djava.library.path` (or `LD_LIBRARY_PATH`/`DYLD_LIBRARY_PATH`) at Senzing's `lib` directory, and pass `--enable-native-access=ALL-UNNAMED` to silence the JNI warning on Java 24+.

The usual path is the Docker image. It is a multi-stage build: a `maven:*-eclipse-temurin-25` stage installs `sz-sdk.jar` (copied from the runtime image) and builds the jar, and the final stage is `senzing/senzingsdk-runtime` plus `openjdk-25-jre-headless`:

```console
docker build -t senzing/elasticsearch .
docker run --rm -e ELASTIC_HOSTNAME -e ELASTIC_PORT -e ELASTIC_INDEX_NAME \
  -e SENZING_ENGINE_CONFIGURATION_JSON --network=senzing-network senzing/elasticsearch
```

There are no tests and no lint config for the Java code. CI (`.github/workflows/`) only builds the Docker image, lints the workflows and spellchecks. The spellcheck dictionary is `.vscode/cspell.json`.

`elasticsearch/docker-compose.yaml` brings up a Postgres plus `senzing/init-database` on `senzing-network`. It does not start Elasticsearch or Kibana.

## Runtime configuration (environment variables)

- `SENZING_ENGINE_CONFIGURATION_JSON`: required. The program exits if it is unset. The v4 paths inside the runtime image are `CONFIGPATH=/etc/opt/senzing`, `RESOURCEPATH=/opt/senzing/er/resources` and `SUPPORTPATH=/opt/senzing/data`. `CONFIGPATH` must contain `cfgVariant.json`. When the repo uses SQLite, its `CONNECTION` must point to a path inside the container (the README mounts the DB at `/db`).
- `ELASTIC_HOSTNAME` (default `localhost`), `ELASTIC_PORT` (default `9200`), `ELASTIC_INDEX_NAME` (default `g2index`). The connection is plain `http://` with no authentication, so Elasticsearch must run with `xpack.security.enabled=false`.

## Architecture

All the logic is in `G2toElastic.main` (package `com.senzing.g2.elasticsearch`):

1. It builds an `SzCoreEnvironment` from the engine config and gets the `SzEngine`.
2. It calls `exportJsonEntityReport` with `SZ_ENTITY_INCLUDE_RECORD_JSON_DATA` plus `SZ_EXPORT_INCLUDE_ALL_ENTITIES`, loops on `fetchNext` until it returns `null`, then calls `closeExportReport`.
3. It wraps each entity line in `G2EntityData`, which reduces the entity to a minimal document: a `JSON_DATA` array (each record's original JSON) and a `RECORDS` array of `{DATA_SOURCE, RECORD_ID}`. Other entity data (features, relationships) is deliberately dropped so it doesn't show up in search results.
4. `JsonStringifier` converts every scalar in that document to a string before indexing. This avoids Elasticsearch dynamic-mapping type conflicts between records.
5. A `BulkIngester` (25 operations per batch, 250 ms flush interval) sends the documents to the index. A `BulkListener` counts per-document failures, and the program exits 1 if any document fails. Documents get auto-generated ES IDs, not the entity ID. Rerunning the job therefore creates duplicates unless the index is deleted first.

`JsonFieldValueFinder`, `G2RecordInfo.getElasticSearchRecordIdentifier` and `G2EntityData.getElasticSearchEntityIdentifier` are helpers that the current flow doesn't use.

Documents are parsed and built with `jakarta.json` (the Parsson implementation comes in through `elasticsearch-java`). The Elasticsearch client serializes with its own Jackson mapper. It logs through SLF4J, which `slf4j-nop` silences.

## Gotchas

- The Java release (25) is set by `maven.compiler.release` in `pom.xml`. Keep it in line with the JRE installed in the Dockerfile.
- `sz-sdk.version` is only the coordinate the build compiles against. The Dockerfile installs whatever `sz-sdk.jar` the base image ships under that version (read back with `mvn help:evaluate`), and at runtime the image's own jar is used through `Class-Path`. So a base-image bump builds and runs without touching the property. Keep it current anyway so local builds compile against the same API.
- The Dockerfile hard-codes the jar name `g2elasticsearch-1.0.0-SNAPSHOT.jar` in three places. Update them if you change the artifact version.
- Every PR that changes the `Dockerfile` must also bump `ENV REFRESHED_AT` in it, because CI (`docker-verify-refreshed-at-updated.yaml`) enforces this.
- Dependabot updates the dependency versions in `pom.xml` regularly.
- `/senzing-code-review` (`.claude/commands/`) runs Senzing's standard PR review prompt.
