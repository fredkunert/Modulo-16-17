#!/bin/sh
# Executado só na primeira inicialização do volume. Cria o usuário dono do schema "produtos":
# a aplicação (Flyway) cria schema, tabelas e views com ele, sem precisar de superusuário.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v app_user="$APP_DB_USER" -v app_pass="$APP_DB_PASSWORD" -v db="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_pass';
GRANT CONNECT, CREATE ON DATABASE :"db" TO :"app_user";
SQL
