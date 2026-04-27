#!/bin/bash
# Amazon Linux 2023 — install PostgreSQL 15 and a database (runs once on first boot).
set -euxo pipefail
exec > >(tee /var/log/postgresql-user-data.log) 2>&1

# Skip full `dnf -y update` to keep first boot within a few minutes; apply patches via SSM later.
# contrib supplies uuid-ossp (used below); install even if server was already present.
dnf -y install postgresql15 postgresql15-server postgresql15-contrib

# AL2023 postgresql15-server: PGDATA is /var/lib/pgsql/data and unit is postgresql.service
# (not /usr/pgsql-15/... or postgresql-15, which are RHEL / Software Collections layouts).
PG_DATA="/var/lib/pgsql/data"
if [ ! -f "$${PG_DATA}/PG_VERSION" ]; then
  if [ -x /usr/bin/postgresql-setup ]; then
    /usr/bin/postgresql-setup --initdb
  elif [ -x /usr/pgsql-15/bin/postgresql-15-setup ]; then
    /usr/pgsql-15/bin/postgresql-15-setup initdb
  elif [ -x /usr/pgsql-15/bin/postgresql-setup ]; then
    /usr/pgsql-15/bin/postgresql-setup --initdb
  else
    echo "Cannot find postgresql-setup (expected /usr/bin/postgresql-setup on AL2023)" >&2
    exit 1
  fi
fi

systemctl enable --now postgresql
sleep 3

# listen on all addresses; 5432 is not exposed to the public internet, only the app’s SG
sed -i "s/^#*listen_addresses *=.*/listen_addresses = '*'/g" $${PG_DATA}/postgresql.conf

# VPC-only clients (CIDR of the default or chosen VPC)
echo "host all all ${vpc_cidr} scram-sha-256" >> $${PG_DATA}/pg_hba.conf
systemctl reload postgresql

sudo -u postgres psql -c "SELECT 1" postgres

set +e
sudo -u postgres psql -c "CREATE USER ${db_user} WITH ENCRYPTED PASSWORD '${db_password}';" 2>/dev/null
set -e

set +e
sudo -u postgres psql -c "CREATE DATABASE ${db_name} OWNER ${db_user};" 2>/dev/null
set -e
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE ${db_name} TO ${db_user};" 2>/dev/null || true

# Spring Boot: grant usage on public schema
sudo -u postgres psql -c "GRANT USAGE, CREATE ON SCHEMA public TO ${db_user};" -d "${db_name}" 2>/dev/null || true
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${db_user};" -d "${db_name}" 2>/dev/null || true
sudo -u postgres psql -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${db_user};" -d "${db_name}" 2>/dev/null || true

set +e
sudo -u postgres psql -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" -d "${db_name}" 2>/dev/null
set -e

systemctl restart postgresql

# Admin access: SSM only. Security group has no 22; stop the SSH daemon on the host.
systemctl stop sshd 2>/dev/null || true
systemctl mask --now sshd 2>/dev/null || true

echo "PostgreSQL bootstrap done."
