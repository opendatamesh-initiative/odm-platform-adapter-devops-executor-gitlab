# ODM Platform DevOps Executor for GitLab

The GitLab executor is a utility-plane adapter of [Open Data Mesh Platform](https://github.com/opendatamesh-initiative) V2 that runs DevOps tasks as GitLab CI/CD pipelines. The [DevOps server](https://github.com/opendatamesh-initiative/odm-platform-pp-devops-server) calls it, in `full-control` mode, to start a pipeline, read its status and logs, and cancel it. The executor is stateless: every call carries the GitLab instance, the project, the ref and the token it needs, so it has no database and any number of replicas can run behind one address.

- **Source code:** [opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab)
- **Documentation:** [README](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md)
- **Issues:** [GitHub issues](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/issues)

## Tags

Each [GitHub release](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/releases) publishes one image tagged with its version, without the `v` prefix:

```bash
docker pull opendatamesh/odm-platform-adapter-devops-executor-gitlab:<version>
```

There is no `latest` tag: always pin a version.

## Requirements

| Dependency | Required | Notes |
| --- | --- | --- |
| GitLab | Yes | GitLab.com or a self-managed instance, version 18.1 or later (pipeline inputs on the Pipelines API). Tested with 19.4. |
| GitLab access token | Yes, on every call | Sent by the DevOps server in the `x-odm-token` header. A project, group or personal access token with the `api` scope, whose owner has at least the Developer role. Trigger tokens don't work. See [Token](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#token). |
| [DevOps server](https://hub.docker.com/r/opendatamesh/odm-platform-devops) | Yes | It calls the executor and sends it the token. See [DevOps configuration](#devops-configuration). |

No database is needed.

## Quick start

Starts the executor for GitLab.com:

```bash
docker run --name odm-executor-gitlab -p 9080:9080 \
  opendatamesh/odm-platform-adapter-devops-executor-gitlab:<version>
```

No variable is required: the token comes with each request, never from the container configuration. When it's up, the OpenAPI document is at `http://localhost:9080/q/openapi`.

For a self-managed GitLab, allow its base URL and, if it's the default instance, set it as the default:

```bash
docker run --name odm-executor-gitlab -p 9080:9080 \
  -e EXECUTOR_GITLAB_ALLOWED_BASE_URLS=https://gitlab.example.com,https://gitlab.com \
  -e EXECUTOR_GITLAB_DEFAULT_BASE_URL=https://gitlab.example.com \
  opendatamesh/odm-platform-adapter-devops-executor-gitlab:<version>
```

The executor sends a token only to an instance listed in `allowed-base-urls`; a request for any other instance is refused with **400**. The default base URL must be one of the allowed ones, or the executor doesn't start.

## DevOps configuration

Declare the executor on the DevOps server under `odm.utility-plane.executors`:

```yaml
odm:
  utility-plane:
    executors:
      polling-interval: 30s
      secrets-ttl: 4h   # longer than your longest pipeline: after the TTL the token is no longer sent
      services:
        gitlab:
          address: http://odm-executor-gitlab:9080
          execution-mode: full-control
```

The executor name (`gitlab` here) is chosen by the administrator and must match the name used in the descriptor tasks and in the executor's secret headers. See [Header lifecycle](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#header-lifecycle).

## Environment variables

### Image

The image runs a Quarkus native executable on Red Hat UBI 9 minimal, as user `1001`. There is no JVM, so `JAVA_OPTS` has no effect, and the image defines no variables of its own: configure it with the application properties below.

### Application

Every property can be set as an environment variable, using the Quarkus naming rules (for example, `executor.gitlab.default-base-url` becomes `EXECUTOR_GITLAB_DEFAULT_BASE_URL`). Lists are comma-separated, and an environment variable replaces the whole default list.

| Property | Description | Default |
| --- | --- | --- |
| `quarkus.http.port` | HTTP port. | `9080` |
| `executor.gitlab.allowed-base-urls` | GitLab instances a token may be sent to. At least one entry, each an absolute `http` or `https` URL. | `https://gitlab.com` |
| `executor.gitlab.default-base-url` | GitLab instance used when the request's `providerBaseUrl` is blank. Must be in `allowed-base-urls`. | `https://gitlab.com` |
| `executor.gitlab.http.connect-timeout` | Connect timeout for GitLab calls. | `5s` |
| `executor.gitlab.http.read-retries` | Retries for GitLab read calls. Pipeline creation is never retried. | `2` |
| `executor.gitlab.input-spec.cache-miss-retry-interval` | Wait between reads of the CI input spec while GitLab hasn't computed it yet. | `1s` |
| `executor.gitlab.input-spec.cache-miss-budget` | Total time spent waiting for the input spec. When it runs out, the start fails with **503** and no pipeline is created. | `20s` |
| `executor.gitlab.logs.max-job-log-size` | Maximum size kept from each job log (its tail). | `1M` |
| `executor.gitlab.logs.max-total-size` | Maximum size of a whole logs response. | `5M` |

The input-spec durations and the log sizes must be positive and `read-retries` must not be negative, or the executor doesn't start. Details: [Configuration](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#configuration).

## Ports and volumes

- **Port:** `9080` (HTTP), or the value of `quarkus.http.port`.
- **Volumes:** none. The executor keeps no state.

## Learn more

- [Endpoints and errors](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#endpoints)
- [Pipeline inputs](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#inputs)
- [Status mapping](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#status-mapping) and [Logs](https://github.com/opendatamesh-initiative/odm-platform-adapter-devops-executor-gitlab/blob/master/README.md#logs)
- [DevOps server](https://github.com/opendatamesh-initiative/odm-platform-pp-devops-server)
- [DevOps executor starter](https://hub.docker.com/r/opendatamesh/odm-platform-adapter-devops-executor-starter), a reference executor that simulates runs

Licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
