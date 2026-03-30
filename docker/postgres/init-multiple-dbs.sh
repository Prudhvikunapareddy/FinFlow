#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE DATABASE auth_db;
  CREATE DATABASE application_db;
  CREATE DATABASE document_db;
  CREATE DATABASE admin_db;
EOSQL
