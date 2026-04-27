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

# Nginx (server_name from Terraform; Certbot will add SSL server blocks when enabled).
cat > /etc/nginx/conf.d/nutriconsultas.conf <<'NUTRINX'
${nginx_config}
NUTRINX

# Drop stock default_server on _ to avoid duplicate listen + Certbot confusion.
rm -f /etc/nginx/conf.d/default.conf
nginx -t
systemctl enable --now nginx
systemctl reload nginx

# Let's Encrypt (HTTP-01): DNS for all names must point to this host before this runs.
if [ '${certbot_run_flag}' = '1' ] && [ -n '${certbot_d_flags}' ]; then
  dnf -y install epel-release || true
  if ! dnf -y install certbot python3-certbot-nginx; then
    echo "WARN: certbot install failed; install EPEL + certbot manually, then: certbot --nginx ..." >&2
  else
    CERTBOT_EMAIL="$(printf '%s' '${certbot_email_b64}' | base64 -d)"
    certbot_ok=0
    for attempt in 1 2 3 4 5 6; do
      if certbot --nginx --non-interactive --agree-tos --email "$$CERTBOT_EMAIL" ${certbot_d_flags} --redirect; then
        certbot_ok=1
        break
      fi
      echo "Certbot attempt $$attempt failed (DNS may not have propagated); retry in 90s..." >&2
      sleep 90
    done
    if [ "$$certbot_ok" = "1" ]; then
      systemctl enable --now certbot-renew.timer 2>/dev/null || systemctl enable --now certbot.timer 2>/dev/null || true
    else
      echo "WARN: Certbot did not obtain a certificate after retries. When DNS points here, run certbot manually via SSM." >&2
    fi
  fi
fi

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
