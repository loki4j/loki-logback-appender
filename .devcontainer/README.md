# Dev container

Everything below starts automatically with the container — no manual step.

| Service              | URL                                            | Notes                                      |
|----------------------|------------------------------------------------|--------------------------------------------|
| Docusaurus dev site  | http://localhost:3000/loki-logback-appender/   | live-reloads on edits under `docs/`        |
| Grafana              | http://localhost:8000                          | anonymous admin, Loki pre-provisioned      |
| Loki                 | http://localhost:3100                          | default push target for tests              |
| Loki (multi-tenant)  | http://localhost:3110                          | `-auth.enabled=true`, tenant `tenantX`     |

Loki and Grafana share the dev container's network namespace, so the `localhost:3100` / `localhost:3110` URLs hard-coded in the integration tests work unchanged.
