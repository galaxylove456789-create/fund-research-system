# FundResearch KingbaseES Docker

This directory wraps the official KingbaseES Docker image used by the project.

The wrapper does three things:

1. Starts the official KingbaseES entrypoint.
2. Creates the `fund_research` database if it does not exist.
3. Runs `*.sql` files placed in `docker/kingbase/initdb/` once per Docker volume.

To re-run init SQL from scratch:

```powershell
docker compose down
docker volume rm FundResearch_kingbase-data
docker compose up -d --build
```

If the volume already exists, new SQL files are not automatically re-run. Import them manually or remove the volume.
