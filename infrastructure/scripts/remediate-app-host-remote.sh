#!/usr/bin/env bash
# Executed ON the EC2 instance by ssm-remediate-app-host.sh (AWS-RunShellScript).
set -euxo pipefail

REGION="us-east-1"

BUCKET="$(aws ssm get-parameter --name /nutriconsultas/deploy/s3_bucket --query Parameter.Value --output text --region "$REGION")"
KEY="$(aws ssm get-parameter --name /nutriconsultas/deploy/s3_key --query Parameter.Value --output text --region "$REGION")"

aws s3 cp "s3://${BUCKET}/${KEY}" /tmp/nutriconsultas-web.jar --region "$REGION" --no-progress

sudo install -o nutri -g nutri -m 640 /tmp/nutriconsultas-web.jar /opt/nutriconsultas/app.jar
rm -f /tmp/nutriconsultas-web.jar

sudo systemctl daemon-reload
sudo systemctl enable nutriconsultas
sudo systemctl restart nutriconsultas

for _ in $(seq 1 45); do
	if curl -sf -o /dev/null --max-time 3 http://127.0.0.1:3000/; then
		echo "app listening on 3000"
		break
	fi
	sleep 2
done

sudo tee /etc/nginx/conf.d/nutriconsultas.conf >/dev/null <<'NGX'
server {
	listen 80 default_server;
	listen [::]:80 default_server;
	server_name minutriporcion.com www.minutriporcion.com;
	client_max_body_size 50M;
	location / {
		proxy_pass http://127.0.0.1:3000;
		proxy_set_header Host $http_host;
		proxy_set_header X-Real-IP $remote_addr;
		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
		proxy_set_header X-Forwarded-Proto $scheme;
	}
}
NGX

sudo nginx -t
sudo systemctl reload nginx

if ! command -v certbot >/dev/null 2>&1; then
	sudo dnf -y install certbot python3-certbot-nginx
fi
sudo certbot --nginx --non-interactive --agree-tos --register-unsafely-without-email \
	-d minutriporcion.com -d www.minutriporcion.com --redirect

systemctl is-active nutriconsultas
ss -tlnp | grep -E ':443|:80|:3000' || true
