#!/bin/bash

# =============================================================================
# RSA Key Generation Script for Propertize Platform
# =============================================================================
# This script generates RSA key pairs for JWT signing and verification.
#
# Usage:
#   ./generate-rsa-keys.sh [output_directory]
#
# Default output: ./config/keys/
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default output directory
OUTPUT_DIR="${1:-config/keys}"

# Key file names
PRIVATE_KEY="private_key.pem"
PUBLIC_KEY="public_key.pem"

echo -e "${YELLOW}=== RSA Key Generation for Propertize Platform ===${NC}"
echo ""

# Create output directory if it doesn't exist
if [ ! -d "$OUTPUT_DIR" ]; then
    echo -e "Creating output directory: $OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"
fi

# Check if keys already exist
if [ -f "$OUTPUT_DIR/$PRIVATE_KEY" ] || [ -f "$OUTPUT_DIR/$PUBLIC_KEY" ]; then
    echo -e "${YELLOW}Warning: Key files already exist in $OUTPUT_DIR${NC}"
    read -p "Do you want to overwrite them? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Aborted. Existing keys not modified.${NC}"
        exit 1
    fi
fi

# Generate private key
echo -e "Generating 2048-bit RSA private key..."
openssl genrsa -out "$OUTPUT_DIR/$PRIVATE_KEY" 2048 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to generate private key${NC}"
    exit 1
fi

# Extract public key
echo -e "Extracting public key..."
openssl rsa -in "$OUTPUT_DIR/$PRIVATE_KEY" -pubout -out "$OUTPUT_DIR/$PUBLIC_KEY" 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to extract public key${NC}"
    exit 1
fi

# Set secure permissions
chmod 600 "$OUTPUT_DIR/$PRIVATE_KEY"
chmod 644 "$OUTPUT_DIR/$PUBLIC_KEY"

echo ""
echo -e "${GREEN}✅ RSA keys generated successfully!${NC}"
echo ""
echo "Files created:"
echo "  - Private key: $OUTPUT_DIR/$PRIVATE_KEY (600 permissions)"
echo "  - Public key:  $OUTPUT_DIR/$PUBLIC_KEY (644 permissions)"
echo ""
echo "Configuration:"
echo "  1. Copy keys to each service's config/keys/ directory"
echo "  2. Or set environment variables:"
echo "     export JWT_RSA_PUBLIC_KEY_PATH=$OUTPUT_DIR/$PUBLIC_KEY"
echo "     export JWT_RSA_PRIVATE_KEY_PATH=$OUTPUT_DIR/$PRIVATE_KEY"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT: Keep the private key secure and never commit it to version control!${NC}"
echo ""

# Verify keys
echo "Verifying generated keys..."
openssl rsa -in "$OUTPUT_DIR/$PRIVATE_KEY" -check -noout 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Private key is valid${NC}"
else
    echo -e "${RED}❌ Private key verification failed${NC}"
    exit 1
fi

# Show key info
echo ""
echo "Key Information:"
openssl rsa -in "$OUTPUT_DIR/$PRIVATE_KEY" -text -noout 2>/dev/null | head -2
