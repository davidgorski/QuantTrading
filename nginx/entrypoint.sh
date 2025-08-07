certbot certonly --standalone --agree-tos -m "${CERTBOT_EMAIL}" -n -d ${DOMAIN_LIST}
rm -rf /var/lib/apt/lists/*
echo "PATH=$PATH" > /etc/cron.d/certbot-renew
echo "@daily certbot renew --nginx >> /var/log/cron.log 2>&1" >>/etc/cron.d/certbot-renew
crontab /etc/cron.d/certbot-renew
cron && nginx -g 'daemon off;'