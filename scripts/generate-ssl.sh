#!/bin/bash
# MediChain SSL Certificate Generator
# Generates a 2048-bit RSA self-signed certificate for nginx
# Usage: ./scripts/generate-ssl.sh

set -euo pipefail

SSL_DIR="$(dirname "$0")/../nginx/ssl"

# Create the ssl directory if it doesn't exist
mkdir -p "$SSL_DIR"

# Generate a 2048-bit RSA private key and self-signed certificate
# CN=medichain.local — valid for 365 days
openssl req -x509 \
  -nodes \
  -days 365 \
  -newkey rsa:2048 \
  -keyout "$SSL_DIR/medichain.key" \
  -out "$SSL_DIR/medichain.crt" \
  -subj "/CN=medichain.local" \
  -addext "subjectAltName=DNS:medichain.local,DNS:localhost,IP:127.0.0.1"

# Make files readable by all (nginx runs as non-root)
chmod 644 "$SSL_DIR/medichain.crt"
chmod 600 "$SSL_DIR/medichain.key"

echo "✅ Self-signed SSL certificate generated successfully:"
echo "   Certificate: $SSL_DIR/medichain.crt"
echo "   Key:         $SSL_DIR/medichain.key"
echo "   CN:          medichain.local"
echo "   Expires:     365 days from now"
