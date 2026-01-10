#!/bin/bash

# Configuration
KEYCLOAK_URL="http://localhost:8080"
REALM="sentio"
CLIENT_ID="sentio-backend"
# Sourcing secret from .env would be better, but for this helper script we'll use the one we know or grep it
# We will try to grep it from .env if it exists, otherwise use the default/known one
ENV_FILE=".env"
CLIENT_SECRET=""

if [ -f "$ENV_FILE" ]; then
  CLIENT_SECRET=$(grep KEYCLOAK_ADMIN_CLIENT_SECRET "$ENV_FILE" | cut -d '=' -f2)
fi

# Fallback or override if empty
if [ -z "$CLIENT_SECRET" ]; then
  echo "Error: Could not find KEYCLOAK_ADMIN_CLIENT_SECRET in .env"
  exit 1
fi

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <username> <password>"
  exit 1
fi

USERNAME=$1
PASSWORD=$2

echo "Getting token for user: $USERNAME..."

# Use -v to show details if it fails, and store output
RESPONSE=$(curl -v -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" 2>&1)

# Extract access_token
ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')

if [ -n "$ACCESS_TOKEN" ]; then
  echo ""
  echo "SUCCESS! Here is your token:"
  echo ""
  echo "$ACCESS_TOKEN"
  echo ""
  echo "Copy the token above and paste it into Swagger Authorize box."
else
  echo "Failed to get token."
  echo "Response: $RESPONSE"
fi
