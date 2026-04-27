#!/bin/bash
# App: Java 21, nginx (port 80 -> 3000), env file, systemd. Copy the Spring Boot JAR to start.
set -euxo pipefail
exec > >(tee /var/log/app-user-data.log) 2>&1

dnf -y install java-21-amazon-corretto-headless nginx awscli

if ! getent group nutri >/dev/null; then
  groupadd -r nutri
fi
if ! getent passwd nutri >/dev/null; then
  useradd -r -g nutri -d /opt/nutriconsultas -s /sbin/nologin nutri
fi

install -d -o root -g nutri -m 750 /opt/nutriconsultas

# JDBC env: base64 in Terraform; decode with printf|base64 — no $() (avoids $$ + $( escaping bugs in user_data).
{
  echo "JDBC_DATABASE_URL=jdbc:postgresql://${db_private_ip}:5432/${db_name}"
  echo "JDBC_DATABASE_USERNAME=${db_user}"
  echo -n "JDBC_DATABASE_PASSWORD="
  printf '%s' '${db_password_b64}' | base64 -d
  echo
} > /opt/nutriconsultas/app.env
chown root:nutri /opt/nutriconsultas/app.env
chmod 640 /opt/nutriconsultas/app.env

# In the next heredoc, 'NGX' stops bash from expanding. Terraform runs first: $$ in this
# .tpl file becomes a single $ in the generated script, which nginx will read as a variable.
cat > /etc/nginx/conf.d/nutriconsultas.conf <<'NGX'
server {
  listen 80 default_server;
  listen [::]:80 default_server;
  server_name _;
  client_max_body_size 50M;
  location / {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $$http_host;
    proxy_set_header X-Real-IP $$remote_addr;
    proxy_set_header X-Forwarded-For $$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $$scheme;
  }
}
NGX

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
