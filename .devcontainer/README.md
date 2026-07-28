# Dev container

Everything below starts automatically with the container — no manual step.

| Service              | URL                     | Notes                                     |
|----------------------|-------------------------|-------------------------------------------|
| Docusaurus dev site  | http://localhost:3000   | live-reloads on edits under `docs/`       |
| Grafana              | http://localhost:8000   | anonymous admin, Loki pre-provisioned     |
| Loki                 | http://localhost:3100   | default push target for tests             |
| Loki (multi-tenant)  | http://localhost:3110   | `-auth.enabled=true`, tenant `tenantX`    |

Loki and Grafana share the dev container's network namespace, so the
`localhost:3100` / `localhost:3110` URLs hard-coded in the integration tests
work unchanged.

## Docs

The dev server runs as its own compose service (`docs`), so Docker supervises
it exactly like Loki and Grafana. It runs `npm install && npm start` on start,
which means http://localhost:3000 comes up on its own. Edits to
`docs/docus/docs/*.md` and to the site files under `docs/docus/website` are
picked up and the browser reloads itself over port 35729.

```bash
docker logs -f <project>_docs_1        # dev server output
docker restart <project>_docs_1        # restart after changing siteConfig.js
cd docs/docus/website && npm run build # static build, from the dev container
```

`node_modules` lives in the workspace and is shared between the `docs` service
and the dev container's own Node, so an `npm install` from either is visible to
the other. That is also why both are pinned to Node 16 — mismatched versions
would fight over native modules.
