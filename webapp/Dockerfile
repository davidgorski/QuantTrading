FROM python:3.10-slim

# We will put everything in the /app directory
WORKDIR /webapp

COPY app.py /webapp
COPY requirements.txt /webapp

# Update and upgrade packages
RUN apt-get update && apt-get upgrade -y

# Install JDK and any needed utilities
RUN pip install --no-cache-dir -r requirements.txt

# Run the gateway
CMD ["gunicorn", "--bind", "0.0.0.0:5056", "app:app"]