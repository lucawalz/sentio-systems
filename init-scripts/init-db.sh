#!/bin/bash
set -e

# This init script runs when the PostgreSQL container first starts
# The sentio user is created by POSTGRES_USER environment variable
# The sentio database is auto-created by PostgreSQL with the same name as POSTGRES_USER
# We only need to create the Keycloak database

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create the Keycloak database
    CREATE DATABASE keycloak;
EOSQL

echo "Database initialization complete: keycloak database created"
