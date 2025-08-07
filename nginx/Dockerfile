FROM nginx:stable

ARG CERTBOT_EMAIL
ARG DOMAIN_LIST

VOLUME /etc/letsencrypt

COPY entrypoint.sh /entrypoint.sh

CMD sh ./entrypoint.sh