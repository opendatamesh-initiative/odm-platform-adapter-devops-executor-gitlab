# GitLab executor

A stateless full-control DevOps executor for GitLab CI/CD. The DevOps server calls it to start a pipeline, read its status and logs, and cancel it. Each call runs against GitLab with the token and the pipeline location sent with that call. The service is a **Quarkus 3** application on port **9080** (`quarkus.http.port` in `application.yaml`).

## Requirements

- GitLab **19.4** tested; minimum **18.1** (pipeline inputs on the Pipelines API)
- Java 21
- Maven 3.9+ or the included `./mvnw` wrapper

## Run

Live reload:

```bash
./mvnw quarkus:dev
```

JVM package:

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Native executable (Mandrel/GraalVM locally, or container build via Docker):

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
```

Docker (native runtime image on port 9080; build the native binary first):

```bash
./mvnw package -DskipTests -Dnative -Dquarkus.native.container-build=true
docker build -t odm-platform-adapter-devops-executor-gitlab .
docker run --rm -p 9080:9080 odm-platform-adapter-devops-executor-gitlab
```

Tests:

```bash
./mvnw test
```

## Endpoints

All four calls are `POST` under `/api/v2/up/executor/tasks/`. Every body carries `executorParameters`.

| Method | Path | Request body | Response |
| --- | --- | --- | --- |
| `POST` | `/api/v2/up/executor/tasks/start` | `{ "executorParameters", "pipelineParameters" }` | `{ "providerRunId" }` (the GitLab pipeline id) |
| `POST` | `/api/v2/up/executor/tasks/status` | `{ "executorParameters", "providerRunId" }` | `{ "providerRunId", "status" }` |
| `POST` | `/api/v2/up/executor/tasks/logs` | `{ "executorParameters", "providerRunId" }` | `{ "content", "generatedAt" }` |
| `POST` | `/api/v2/up/executor/tasks/cancel` | `{ "executorParameters", "providerRunId" }` | empty **200** |

Errors:

| Status | When |
| --- | --- |
| **400** | Missing `executorParameters`, `dataProductRepo.externalIdentifier`, `providerRunId` or `x-odm-token`. The instance is not in `allowed-base-urls`. A required input has no value. GitLab rejected the request (for example a type, `options` or `regex` mismatch in an input). |
| **404** | The project or the pipeline is unknown to GitLab. |
| **409** | Cancel of a pipeline that has already ended (`success`, `failed`, `skipped` or `canceled`). |
| **502** | GitLab refused the token, or GitLab is unavailable. |
| **503** | The CI input spec is not computed yet (see [Inputs](#inputs)). No pipeline is created. |

### `executorParameters`

The executor reads these fields:

| Field | Use |
| --- | --- |
| `dataProductRepo.providerBaseUrl` | GitLab instance. A blank value falls back to `default-base-url`. Any other value must be listed in `allowed-base-urls`. |
| `dataProductRepo.externalIdentifier` | GitLab project (id or full path). Required. |
| `ref.name` | Branch or tag. GitLab resolves it. |

Other fields, such as `dataProductRepo.providerType`, `name` and `defaultBranch`, `repositoryKey`, `pipelineIdentifier` and `ref.type`, are ignored. The ref is resolved by GitLab, so the executor does not check whether it is a branch or a tag.

Example:

```json
{
  "dataProductRepo": {
    "providerBaseUrl": "https://gitlab.com",
    "externalIdentifier": "blindata/dumb-pipeline"
  },
  "ref": { "name": "main", "type": "BRANCH" }
}
```

## Token

- Header: `x-odm-token`. It is required on every call.
- Token type: a project, group or personal access token with the **`api`** scope. The owner needs at least the **Developer** role, because it runs pipelines. A trigger token cannot read status or logs, and it cannot cancel.
- The token is read from the header on each request, used for the GitLab calls of that request (as `PRIVATE-TOKEN`), and dropped. It is never stored, cached, logged, echoed in errors, or returned.

### Header lifecycle

The token passes through five hops before it reaches GitLab:

| Hop | Header as it travels | What transforms it |
| --- | --- | --- |
| 1. UI to Blindata agent | `x-odm-ag-<executorName>-executor-secret-token: <personal secret name>` | The UI sends the name of a personal secret, never its value. |
| 2. Blindata agent to DevOps | `x-odm-<executorName>-executor-secret-token: <token value>` | The agent looks up the personal secret by name and type, decrypts it, and replaces the `x-odm-ag-` header with the value. |
| 3. Inside DevOps | Cache entry `x-odm-token: <value>` for the executor and activity | DevOps keeps only the executor's secret headers, renames them to `x-odm-<secretType>`, and holds them in memory for `secrets-ttl`. Nothing is persisted. |
| 4. DevOps to this executor | `x-odm-token: <value>` | DevOps attaches the header to every start, status, logs and cancel call. Only the executor named in the task receives it. After the TTL, calls go out without it. |
| 5. This executor to GitLab | `PRIVATE-TOKEN: <value>` | The executor uses the token for this request only. |

The executor name (`gitlab` in the examples) is chosen by the administrator. It must be the same in the DevOps executor declaration, the agent's DevOps executor configuration, the descriptor tasks, and the header name.

## Inputs

The CI configuration declares its parameters with `spec:inputs`. The executor reads the declared inputs from GitLab for the ref before it creates the pipeline, and selects the pipeline parameters against them.

- A declared input whose name matches a pipeline parameter is sent with that value.
- A required input with no default and no parameter makes the start fail with **400**. The message names every missing input.
- A declared input with a default and no parameter is not sent, so GitLab applies the default.
- A pipeline parameter that matches no declared input is dropped. The executor logs its **name** only, never its value.
- A CI configuration with no `spec:inputs` receives no inputs.
- Typing is best effort, by declared type:

  | Declared type | Conversion |
  | --- | --- |
  | `string` | unchanged |
  | `number` | parsed as a number |
  | `boolean` | `true` or `false`, case-insensitive |
  | `array` | parsed as a JSON array |

  If parsing fails, the raw string is sent and GitLab's validation decides.

GitLab computes the input spec in the background and caches it. When the cache is cold, the executor retries the read every `cache-miss-retry-interval` until `cache-miss-budget` is used up. If the cache is still cold, the start fails with **503** and no pipeline is created.

## Status mapping

| GitLab pipeline status | Executor status |
| --- | --- |
| `created`, `waiting_for_resource`, `preparing`, `waiting_for_callback`, `pending`, `running`, `scheduled`, `canceling`, `manual` | `RUNNING` |
| `success` | `SUCCEEDED` |
| `failed`, `skipped` | `FAILED` |
| `canceled` (from DevOps, the GitLab UI, or GitLab auto-cancel) | `CANCELED` |
| any other value | `RUNNING` (the DevOps poll cap bounds the wait) |

A cancelled pipeline stays in GitLab's `canceling` state for up to about a minute, so a status read right after a cancel still returns `RUNNING` until GitLab reports `canceled`.

`manual` stays `RUNNING`. If nobody runs the manual job, the DevOps poll cap fails the task.

Cancel reads the pipeline first. A pipeline that has already ended returns **409** and GitLab is not called. A pipeline in `canceling` returns **200** and GitLab is not called again.

## Logs

- The log lists the pipeline's jobs, oldest first. Each job has a header with its name, stage and status, followed by its log.
- Each job log is cut to its tail, up to `max-job-log-size`. The total is capped by `max-total-size`. Truncation is marked in the text.
- ANSI colour codes and GitLab section markers are removed.
- A job with no log gets a note, not an error.
- Child and downstream pipelines are not followed.

## Configuration

Settings live under `executor.gitlab` in `src/main/resources/application.yaml`. Each value can be overridden with the usual Quarkus environment variables or system properties.

| Key | Default | Meaning |
| --- | --- | --- |
| `executor.gitlab.default-base-url` | `https://gitlab.com` | GitLab instance used when `providerBaseUrl` is blank. |
| `executor.gitlab.allowed-base-urls` | `https://gitlab.com` | List of GitLab instances a token may be sent to. Requests for any other instance are refused with 400. |
| `executor.gitlab.http.connect-timeout` | `5s` | Connect timeout for GitLab calls. |
| `executor.gitlab.http.read-retries` | `2` | Retries for GitLab read calls. Pipeline creation is never retried. |
| `executor.gitlab.input-spec.cache-miss-retry-interval` | `1s` | Wait between input spec reads while GitLab's cache is cold. |
| `executor.gitlab.input-spec.cache-miss-budget` | `20s` | Total time spent retrying a cold input spec cache. When it runs out, start fails with 503. |
| `executor.gitlab.logs.max-job-log-size` | `1M` | Maximum size kept from each job log (tail). |
| `executor.gitlab.logs.max-total-size` | `5M` | Maximum size of the whole logs response. |

The HTTP port is set by `quarkus.http.port` (default `9080`).

For a self-managed GitLab, add its base URL to `allowed-base-urls` and set `default-base-url` if it is the default instance.

## DevOps configuration

On the DevOps server (`odm-platform-pp-devops-server`):

```yaml
odm:
  utility-plane:
    executors:
      polling-interval: 30s       # pipelines take minutes; keeps GitLab load low
      secrets-ttl: 4h             # must exceed the longest pipeline: after the TTL, the token is no longer sent
      services:
        gitlab:
          address: http://odm-platform-adapter-devops-executor-gitlab:9080
          execution-mode: full-control
```

## Blindata agent configuration

Declare the executor `gitlab` in the agent's DevOps configuration with one secret of type `token` (`devops.executors.gitlab.secrets`). The UI then asks the user to pick a personal token secret for the executor, and the agent refuses an execute request that does not include it.

## Statelessness

The executor keeps no state. It has no database, no run store and no token cache. Each call carries everything GitLab needs: the instance, the project, the token and the pipeline id. Any number of replicas can run behind one address.
