FROM nginx:stable

ARG CERTBOT_EMAIL
ARG DOMAIN_LIST

RUN apt-get update && apt-get -y install certbot cron wget bash python3-certbot-nginx

VOLUME /etc/letsencrypt

COPY entrypoint.sh /entrypoint.sh

CMD sh ./entrypoint.sh