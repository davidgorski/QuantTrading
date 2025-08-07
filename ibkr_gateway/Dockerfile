FROM debian:bookworm-slim

# Update and upgrade packages
RUN apt-get update && apt-get upgrade -y

# Install JDK and any needed utilities
RUN apt-get install -y openjdk-17-jre-headless unzip curl procps vim net-tools python3 python3-pip python3.11-venv

# We will put everything in the /app directory
WORKDIR /app

# Download and unzip client portal gateway
RUN mkdir gateway && \
    cd gateway && \
    curl -O https://download2.interactivebrokers.com/portal/clientportal.gw.zip && \
    unzip clientportal.gw.zip && \
    rm clientportal.gw.zip

# Copy our config so that the gateway will use it
COPY conf.yaml gateway/root/conf.yaml
COPY start.sh /app

ADD webapp webapp

# Expose the port so we can connect
EXPOSE 5055 5056

# Run the gateway
CMD sh ./start.sh