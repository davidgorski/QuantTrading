FROM nginx:stable

ARG CERTBOT_EMAIL
ARG DOMAIN_LIST

RUN apt-get update && apt-get -y install certbot cron wget bash python3-certbot-nginx
RUN certbot certonly --standalone --agree-tos -m "${CERTBOT_EMAIL}" -n -d ${DOMAIN_LIST}
RUN rm -rf /var/lib/apt/lists/*
RUN echo "PATH=$PATH" > /etc/cron.d/certbot-renew
RUN echo "@daily certbot renew --nginx >> /var/log/cron.log 2>&1" >>/etc/cron.d/certbot-renew
RUN crontab /etc/cron.d/certbot-renew

VOLUME /etc/letsencrypt

CMD [ "sh", "-c", "cron && nginx -g 'daemon off;'" ]