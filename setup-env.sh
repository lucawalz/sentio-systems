#!/bin/bash

# Sentio Systems - Environment Setup Script
# This script helps set up your local development environment

set -e  # Exit on error

echo "Sentio Systems - Environment Setup"
echo "======================================"
echo ""

# Check if .env already exists
if [ -f ".env" ]; then
    echo "WARNING: .env file already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Setup cancelled."
        exit 1
    fi
fi

# Copy the example file
echo "Copying .env.example to .env..."
cp .env.example .env

echo ""
echo "Generating secure passwords..."
echo ""

# Generate passwords
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '=')
KC_ADMIN_PASSWORD=$(openssl rand -base64 32 | tr -d '=')
KC_CLIENT_SECRET=$(openssl rand -hex 32)

# Update .env file with generated passwords (macOS compatible)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s|POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=${DB_PASSWORD}|" .env
    sed -i '' "s|KEYCLOAK_ADMIN_PASSWORD=.*|KEYCLOAK_ADMIN_PASSWORD=${KC_ADMIN_PASSWORD}|" .env
    sed -i '' "s|KEYCLOAK_ADMIN_CLIENT_SECRET=.*|KEYCLOAK_ADMIN_CLIENT_SECRET=${KC_CLIENT_SECRET}|" .env
else
    # Linux
    sed -i "s|POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=${DB_PASSWORD}|" .env
    sed -i "s|KEYCLOAK_ADMIN_PASSWORD=.*|KEYCLOAK_ADMIN_PASSWORD=${KC_ADMIN_PASSWORD}|" .env
    sed -i "s|KEYCLOAK_ADMIN_CLIENT_SECRET=.*|KEYCLOAK_ADMIN_CLIENT_SECRET=${KC_CLIENT_SECRET}|" .env
fi

echo "SUCCESS: Environment file created!"
echo ""
echo "Generated credentials:"
echo "========================="
echo "Database Password:        ${DB_PASSWORD}"
echo "Keycloak Admin Password:  ${KC_ADMIN_PASSWORD}"
echo "Keycloak Client Secret:   ${KC_CLIENT_SECRET}"
echo ""
echo "IMPORTANT: Save these credentials securely!"
echo "The .env file has been created with these values."
echo ""
echo "Generating Keycloak realm configuration..."
# Generate the realm JSON from template with the actual client secret
# Use | as delimiter instead of / to handle base64 characters (which include /)
sed "s|KEYCLOAK_CLIENT_SECRET_PLACEHOLDER|${KC_CLIENT_SECRET}|g" \
    init-scripts/sentio-realm.json.template > init-scripts/sentio-realm.json
echo "Keycloak realm configuration generated."
echo ""
echo "Next steps:"
echo "1. Review the .env file and adjust any ports if needed"
echo "2. Run 'docker-compose up -d' to start the services"
echo "3. Access Keycloak at http://localhost:8080 (admin/[password])"
echo ""
echo "Setup complete!"
