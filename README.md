# Starter executor

Reference implementation of the DevOps executor v2 API. It is a **Quarkus 3** service that simulates pipeline runs in memory for end-to-end tests and demos. Runs are dropped when the process stops. There is no database: `quarkus.datasource.active` and `quarkus.hibernate-orm.active` are `false`.

The HTTP port is **9080** (`quarkus.http.port` in `application.yaml`).

## Requirements

- Java 21
- Maven 3.9+ (or the included `./mvnw` wrapper)
- Quarkus 3.40.1, from `blindata-backend-service-template-quarkus` 1.0.1 (`template.yaml`)

## Run

Live reload:

```bash
./mvnw quarkus:dev
```

Packaged runner (`target/quarkus-app/`):

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Native executable (Mandrel/GraalVM locally, or container build via Docker):

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
./target/odm-platform-adapter-devops-executor-starter-*-runner
```

Docker (native runtime image on port 9080; build the native binary first):

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
docker build -t odm-platform-adapter-devops-executor-starter .
docker run --rm -p 9080:9080 odm-platform-adapter-devops-executor-starter
```

Tests (`@QuarkusTest`; `%test` sets `executor.simulation.run-duration` to `200ms`):

```bash
./mvnw test
```

## Endpoints

| Method | Path | Request | Response |
| --- | --- | --- | --- |
| `POST` | `/api/v2/up/executor/tasks/start` | JSON body with `executorParameters` and `pipelineParameters` | `{ "providerRunId": "starter-<uuid>" }` |
| `GET` | `/api/v2/up/executor/tasks/status?providerRunId=` | Query parameter `providerRunId` | `{ "providerRunId", "status" }` where `status` is `RUNNING`, `SUCCEEDED`, `FAILED`, or `CANCELED` |
| `GET` | `/api/v2/up/executor/tasks/logs?providerRunId=` | Query parameter `providerRunId` | `{ "content", "generatedAt" }` |
| `POST` | `/api/v2/up/executor/tasks/cancel` | `{ "providerRunId" }` | empty **200** |

A start request without `executorParameters` returns **400**, and the body names `executorParameters`. An unknown `providerRunId` returns **404**. Cancel of a run that has already ended returns **409**.

`executorParameters` carries `repositoryKey`, `dataProductRepo` (`providerType`, `providerBaseUrl`, `externalIdentifier`, `name`, `ownerId`, `ownerType`, `remoteUrlHttp`, `defaultBranch`), `ref` (`name`, `type`), and `pipelineIdentifier`. Unknown JSON properties are ignored. No activity or task identifiers are required.

## Simulation

Defaults (`executor.simulation` in `application.yaml`):

- `run-duration`: `5s`
- `outcome`: `SUCCEEDED`

A run stays `RUNNING` until `startedAt` plus the planned duration, then reports the planned outcome.

Two reserved pipeline parameters override those defaults for that run. The starter reads them the way a pipeline reads its own inputs.

- `starter.outcome`: `SUCCEEDED` or `FAILED`, compared case-insensitively. Any other value keeps the configured default.
- `starter.durationSeconds`: a non-negative integer number of seconds. Any other value keeps the configured default.

The logs endpoint returns one block. It lists the run id, repository name, `repositoryKey`, ref name and type, pipeline identifier, pipeline parameters as received, the names of request headers whose names start with `x-odm-` (never the header values), the start and end times, and the outcome.

Each call writes one info log line that contains only the provider run id. Header values, secret values, and parameter values are never written to the process log.

## DevOps configuration

Declare the starter on the DevOps server (`application-dev.yml`, and the same shape in `application-test.yml`):

```yaml
odm:
  utility-plane:
    executor-services:
      starter:
        address: http://localhost:9080
        execution-mode: full-control
```
