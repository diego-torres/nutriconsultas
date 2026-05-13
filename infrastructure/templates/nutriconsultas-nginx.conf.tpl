# Injected by Terraform templatefile — use $$nginx_var so Terraform emits $nginx_var for nginx directives.
server {
  listen 80 default_server;
  listen [::]:80 default_server;
  server_name ${nginx_server_names};
  client_max_body_size 50M;
  location / {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $$http_host;
    proxy_set_header X-Real-IP $$remote_addr;
    proxy_set_header X-Forwarded-For $$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $$scheme;
  }
}
