#!/bin/bash

set -euo pipefail

echo "Generating RSA key pair for JWT signing..."

# Create keys directory if it doesn't exist
mkdir -p keys

# Generate private key (PKCS#8 format)
openssl genrsa -out keys/private_key_temp.pem 2048

# Convert private key to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
    -in keys/private_key_temp.pem \
    -out keys/private_key.pem

# Generate public key from private key
openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem

# Clean up temporary file
rm keys/private_key_temp.pem

# Set appropriate permissions
chmod 600 keys/private_key.pem
chmod 644 keys/public_key.pem

echo "✅ RSA keys generated successfully:"
echo "   - keys/private_key.pem (PKCS#8 format)"
echo "   - keys/public_key.pem"
