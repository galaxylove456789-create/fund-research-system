#!/bin/bash
set -e

DB_NAME="${FUND_DB_NAME:-fund_research}"
DB_USER="${DB_USER:-system}"
INIT_DIR="/docker-entrypoint-initdb.d"
INIT_MARKER="/home/kingbase/userdata/.fundresearch-init-done"

/home/kingbase/docker-entrypoint.sh &
entrypoint_pid="$!"

echo "[FundResearch] Waiting for KingbaseES to accept local connections..."
for i in $(seq 1 90); do
  if ksql -U "${DB_USER}" -d kingbase -c "select 1" >/dev/null 2>&1; then
    break
  fi
  sleep 2
  if [ "$i" -eq 90 ]; then
    echo "[FundResearch] KingbaseES did not become ready in time."
    exit 1
  fi
done

if ! ksql -U "${DB_USER}" -d kingbase -tAc "select 1 from pg_database where datname='${DB_NAME}'" | grep -q 1; then
  echo "[FundResearch] Creating database ${DB_NAME}..."
  ksql -U "${DB_USER}" -d kingbase -c "CREATE DATABASE ${DB_NAME};"
else
  echo "[FundResearch] Database ${DB_NAME} already exists."
fi

if [ ! -f "${INIT_MARKER}" ]; then
  if [ -d "${INIT_DIR}" ]; then
    shopt -s nullglob
    for sql_file in "${INIT_DIR}"/*.sql; do
      echo "[FundResearch] Running init SQL: ${sql_file}"
      ksql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" -f "${sql_file}"
    done
    shopt -u nullglob
  fi
  touch "${INIT_MARKER}"
  echo "[FundResearch] Initialization marker written: ${INIT_MARKER}"
else
  echo "[FundResearch] Init SQL already processed. Remove volume kingbase-data to re-run initialization."
fi

wait "${entrypoint_pid}"
