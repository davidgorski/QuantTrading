FROM debian:bookworm-slim

# Update and upgrade packages
RUN apt-get update && apt-get upgrade -y

# Install JDK and any needed utilities
RUN apt-get install -y python3 python3-pip python3.11-venv

# We will put everything in the /app directory
WORKDIR /app

COPY start.sh /app

ADD webapp webapp

# Run the gateway
CMD sh ./start.sh