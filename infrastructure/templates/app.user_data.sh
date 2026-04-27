#!/bin/bash
# App: Java 21, nginx (port 80 -> 3000), env file, systemd. Copy the Spring Boot JAR to start.
# No bash -x: user_data contains secret material; avoid writing it to cloud-init logs.
set -euo pipefail
exec > >(tee /var/log/app-user-data.log) 2>&1

dnf -y install java-21-amazon-corretto-headless nginx awscli

if ! getent group nutri >/dev/null; then
  groupadd -r nutri
fi
if ! getent passwd nutri >/dev/null; then
  useradd -r -g nutri -d /opt/nutriconsultas -s /sbin/nologin nutri
fi

install -d -o root -g nutri -m 750 /opt/nutriconsultas

# app.env: JDBC + OAuth + AWS + optional keys. Secrets arrive base64-encoded from Terraform.
{
  echo "JDBC_DATABASE_URL=jdbc:postgresql://${db_private_ip}:5432/${db_name}"
  echo "JDBC_DATABASE_USERNAME=${db_user}"
  echo -n "JDBC_DATABASE_PASSWORD="
  printf '%s' '${db_password_b64}' | base64 -d
  echo
  echo -n "AUTH_CLIENT="
  printf '%s' '${auth_client_b64}' | base64 -d
  echo
  echo -n "AUTH_SECRET="
  printf '%s' '${auth_secret_b64}' | base64 -d
  echo
  echo -n "AUTH_ISSUER="
  printf '%s' '${auth_issuer_b64}' | base64 -d
  echo
  echo -n "AWS_BUCKET="
  printf '%s' '${aws_bucket_b64}' | base64 -d
  echo
  echo -n "AWS_KEY="
  printf '%s' '${aws_key_b64}' | base64 -d
  echo
  echo -n "AWS_SECRET="
  printf '%s' '${aws_secret_b64}' | base64 -d
  echo
  echo -n "MAPS_KEY="
  printf '%s' '${maps_key_b64}' | base64 -d
  echo
  echo -n "RECAPTCHA_SECRET_KEY="
  printf '%s' '${recaptcha_secret_key_b64}' | base64 -d
  echo
} > /opt/nutriconsultas/app.env
chown root:nutri /opt/nutriconsultas/app.env
chmod 640 /opt/nutriconsultas/app.env

# Nginx: content from templates/nutriconsultas-nginx.conf (literal $var — not $$), injected by Terraform.
cat > /etc/nginx/conf.d/nutriconsultas.conf <<'NUTRINX'
${nginx_config}
NUTRINX

systemctl enable --now nginx

cat > /etc/systemd/system/nutriconsultas.service <<'UNIT'
[Unit]
Description=Nutriconsultas (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=nutri
Group=nutri
WorkingDirectory=/opt/nutriconsultas
ConditionPathExists=/opt/nutriconsultas/app.jar
EnvironmentFile=-/opt/nutriconsultas/app.env
ExecStart=/usr/bin/java -Dserver.port=3000 -Djava.net.preferIPv4Stack=true -Xms256m -Xmx512m -XX:+UseG1GC -jar /opt/nutriconsultas/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

chown -R root:nutri /opt/nutriconsultas
systemctl daemon-reload

# Admin access: SSM only. Security group has no 22; do not run sshd on the host.
systemctl stop sshd 2>/dev/null || true
systemctl mask --now sshd 2>/dev/null || true

echo "After the first JAR (CI or S3+SSM), use: systemctl enable --now nutriconsultas. Shell: SSM Session Manager only (no port 22)."
