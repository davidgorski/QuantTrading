FROM python:3.10-slim

# Update and upgrade packages
RUN apt-get update && apt-get upgrade -y

# Install JDK and any needed utilities
RUN apt-get install -y python3 python3-pip python3.11-venv

# We will put everything in the /app directory
WORKDIR /webapp

COPY app.py /webapp

# Run the gateway
CMD ["gunicorn", "--bind", "0.0.0.0:5056", "app:app"]