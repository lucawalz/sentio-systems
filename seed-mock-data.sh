#!/bin/bash
# Unified Mock Data Seeding Script for Sentio Backend
# Usage: ./seed-mock-data.sh <username> <password> <device1_id> <device1_code> <device2_id> <device2_code>
# Example: ./seed-mock-data.sh test 'Test123!!' bcba08a1-8ea9-41aa-a9d1-51ecc85ac387 WN6X-QG2D f95e3948-1835-494c-9684-3dc56cde83fc CQRH-8PKY

set -e

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8083}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="sentio"
CLIENT_ID="sentio-backend"

# Load client secret from .env
ENV_FILE=".env"
CLIENT_SECRET=""
if [ -f "$ENV_FILE" ]; then
    CLIENT_SECRET=$(grep KEYCLOAK_ADMIN_CLIENT_SECRET "$ENV_FILE" | cut -d '=' -f2)
fi

if [ -z "$CLIENT_SECRET" ]; then
    echo "❌ Error: Could not find KEYCLOAK_ADMIN_CLIENT_SECRET in .env"
    exit 1
fi

# Check arguments
if [ "$#" -ne 6 ]; then
    echo "Usage: $0 <username> <password> <device1_id> <device1_code> <device2_id> <device2_code>"
    echo "Example: $0 test 'Test123!!' bcba08a1-8ea9-41aa-a9d1-51ecc85ac387 WN6X-QG2D f95e3948-1835-494c-9684-3dc56cde83fc CQRH-8PKY"
    echo ""
    echo "Device IDs and pairing codes are shown when you register devices in the frontend."
    exit 1
fi

USERNAME=$1
PASSWORD=$2
DEVICE1_ID=$3
DEVICE1_CODE=$4
DEVICE2_ID=$5
DEVICE2_CODE=$6

echo "🌱 Sentio Mock Data Seeder"
echo "========================="
echo "Backend: $BACKEND_URL"
echo "User: $USERNAME"
echo ""

# Wait for backend to be ready
echo "⏳ Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -s "$BACKEND_URL/actuator/health" > /dev/null 2>&1; then
        echo "✅ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Backend not responding after 30 attempts."
        exit 1
    fi
    echo "   Attempt $i/30..."
    sleep 2
done

# Get auth token using the same method as get_token.sh
echo ""
echo "🔐 Getting authentication token for user: $USERNAME..."

RESPONSE=$(curl -s -X POST \
    "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET" \
    -d "username=$USERNAME" \
    -d "password=$PASSWORD" \
    -d "grant_type=password" 2>&1)

ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Failed to get token!"
    echo "Response: $RESPONSE"
    exit 1
fi

echo "✅ Token acquired"

# Function to make authenticated API call
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    curl -s -X "$method" "$BACKEND_URL$endpoint" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "$data" 2>/dev/null
}

echo ""
echo "📱 Pairing Devices..."
echo "---------------------"

# Pair device 1: Weather Station
echo "   Pairing device 1: $DEVICE1_ID..."
PAIR_RESPONSE1=$(curl -s -X POST "$BACKEND_URL/api/devices/pair" \
    -H "Content-Type: application/json" \
    -d "{\"deviceId\": \"$DEVICE1_ID\", \"pairingCode\": \"$DEVICE1_CODE\"}" 2>&1)

DEVICE1_TOKEN=$(echo "$PAIR_RESPONSE1" | grep -o '"deviceToken":"[^"]*' | grep -o '[^"]*$')
if [ -z "$DEVICE1_TOKEN" ]; then
    echo "   ❌ Failed to pair device 1!"
    echo "   Response: $PAIR_RESPONSE1"
    echo "   Make sure the device is registered and the pairing code hasn't expired."
    exit 1
fi
echo "   ✓ Device 1 paired: $DEVICE1_ID (Weather Station)"

# Pair device 2: Garden Camera
echo "   Pairing device 2: $DEVICE2_ID..."
PAIR_RESPONSE2=$(curl -s -X POST "$BACKEND_URL/api/devices/pair" \
    -H "Content-Type: application/json" \
    -d "{\"deviceId\": \"$DEVICE2_ID\", \"pairingCode\": \"$DEVICE2_CODE\"}" 2>&1)

DEVICE2_TOKEN=$(echo "$PAIR_RESPONSE2" | grep -o '"deviceToken":"[^"]*' | grep -o '[^"]*$')
if [ -z "$DEVICE2_TOKEN" ]; then
    echo "   ❌ Failed to pair device 2!"
    echo "   Response: $PAIR_RESPONSE2"
    echo "   Make sure the device is registered and the pairing code hasn't expired."
    exit 1
fi
echo "   ✓ Device 2 paired: $DEVICE2_ID (Garden Camera)"

echo "✅ Devices paired successfully"

echo ""
echo "📍 Sending GPS via MQTT (simulates real device behavior)..."
echo "------------------------------------------------------------"

# Read MQTT config from .env or use defaults
MQTT_HOST="localhost"
MQTT_PORT="1883"
if [ -f "$ENV_FILE" ]; then
    # Read values, but keep defaults if not found
    _HOST=$(grep "^MQTT_HOST=" "$ENV_FILE" | cut -d '=' -f2)
    _PORT=$(grep "^MQTT_PORT=" "$ENV_FILE" | cut -d '=' -f2)
    [ -n "$_HOST" ] && MQTT_HOST="$_HOST"
    [ -n "$_PORT" ] && MQTT_PORT="$_PORT"
fi
echo "   MQTT: $MQTT_HOST:$MQTT_PORT"

# Function to publish MQTT message with device token auth
mqtt_publish_with_token() {
    local topic=$1
    local message=$2
    local device_id=$3
    local device_token=$4
    if command -v mosquitto_pub &> /dev/null; then
        # Use device_id as username and device_token as password for MQTT auth
        mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -u "$device_id" -P "$device_token" -t "$topic" -m "$message" 2>/dev/null
        return $?
    else
        echo "   ⚠️  mosquitto_pub not installed - skipping MQTT messages"
        return 1
    fi
}

# Send device status with GPS coordinates via MQTT
# This triggers DeviceLocationUpdatedEvent which fetches forecasts/alerts via EDA

# Weather Station (Device 1): HdM Stuttgart, Germany
STATUS_MSG="{\"device_id\":\"$DEVICE1_ID\",\"ip\":\"192.168.1.100\",\"service\":\"weather\",\"latitude\":48.741102,\"longitude\":9.101742}"
if mqtt_publish_with_token "device/$DEVICE1_ID/status" "$STATUS_MSG" "$DEVICE1_ID" "$DEVICE1_TOKEN"; then
    echo "   ✓ Device 1 ($DEVICE1_ID): HdM Stuttgart (48.74, 9.10) via MQTT"
else
    echo "   ⚠️  Could not send MQTT message for Device 1"
fi

# Garden Camera (Device 2): Hamburg, Germany
STATUS_MSG="{\"device_id\":\"$DEVICE2_ID\",\"ip\":\"192.168.1.101\",\"service\":\"camera\",\"latitude\":53.539967,\"longitude\":9.955407}"
if mqtt_publish_with_token "device/$DEVICE2_ID/status" "$STATUS_MSG" "$DEVICE2_ID" "$DEVICE2_TOKEN"; then
    echo "   ✓ Device 2 ($DEVICE2_ID): Hamburg (53.54, 9.96) via MQTT"
else
    echo "   ⚠️  Could not send MQTT message for Device 2"
fi

echo ""
echo "⏳ Waiting 3 seconds for async weather fetch to complete..."
sleep 3

echo ""
echo "📊 Seeding Weather Data..."
echo "--------------------------"


# ============================================
# DEVICE 1: HdM Stuttgart (uses DEVICE1_ID)
# Cold winter climate: -6 to 2°C (January cold snap)
# ============================================
echo "   ❄️  Device 1: HdM Stuttgart (COLD winter: -6 to 2°C)"

now=$(date +%s)
count=0
for day in $(seq 0 6); do
    for hour in $(seq 0 23); do
        timestamp=$((now - (day * 86400) - ((23 - hour) * 3600)))
        
        if date -r $timestamp "+%Y-%m-%dT%H:%M:%S" > /dev/null 2>&1; then
            datetime=$(date -r $timestamp "+%Y-%m-%dT%H:%M:%S.000000")
        else
            datetime=$(date -d "@$timestamp" "+%Y-%m-%dT%H:%M:%S.000000" 2>/dev/null || echo "2024-01-01T12:00:00.000000")
        fi
        
        # COLD WINTER DATA: -6 to 2°C, higher humidity (75-90%), high pressure (winter anticyclone)
        # Coldest at night (0-6h), warmest midday (12-15h)
        if [ $hour -ge 0 ] && [ $hour -lt 6 ]; then
            base_temp=-5  # Night: very cold
        elif [ $hour -ge 6 ] && [ $hour -lt 10 ]; then
            base_temp=-3  # Morning: cold
        elif [ $hour -ge 10 ] && [ $hour -lt 15 ]; then
            base_temp=0   # Midday: warmer but still freezing
        elif [ $hour -ge 15 ] && [ $hour -lt 18 ]; then
            base_temp=-2  # Afternoon: cooling
        else
            base_temp=-4  # Evening: cold
        fi
        temp=$(awk "BEGIN {printf \"%.1f\", $base_temp + (rand() * 3 - 1.5)}")
        humidity=$((75 + RANDOM % 15))
        pressure=$((1025 + RANDOM % 10))  # Winter high pressure
        lux=$((20 + (hour >= 8 && hour <= 17 ? hour * 40 : 10) + RANDOM % 50))
        uvi=$(awk "BEGIN {printf \"%.1f\", ($hour >= 10 && $hour <= 15) ? (rand() * 1.5) : 0}")
        
        payload="{\"temperature\": $temp, \"humidity\": $humidity, \"pressure\": $pressure, \"lux\": $lux, \"uvi\": $uvi, \"timestamp\": \"$datetime\", \"deviceId\": \"$DEVICE1_ID\"}"
        api_call POST "/api/weather" "$payload" > /dev/null
        ((count++))
    done
done
echo "   ✅ HdM Stuttgart: $count readings (COLD winter: ~-3°C avg)"

# ============================================
# DEVICE 2: Hamburg (uses DEVICE2_ID)
# Even colder: -8 to 0°C (northern Germany, maritime cold)
# ============================================
echo "   🥶 Device 2: Hamburg (VERY COLD winter: -8 to 0°C)"

count2=0
for day in $(seq 0 6); do
    for hour in $(seq 0 23); do
        timestamp=$((now - (day * 86400) - ((23 - hour) * 3600)))
        
        if date -r $timestamp "+%Y-%m-%dT%H:%M:%S" > /dev/null 2>&1; then
            datetime=$(date -r $timestamp "+%Y-%m-%dT%H:%M:%S.000000")
        else
            datetime=$(date -d "@$timestamp" "+%Y-%m-%dT%H:%M:%S.000000" 2>/dev/null || echo "2024-01-01T12:00:00.000000")
        fi
        
        # VERY COLD DATA: -8 to 0°C, high humidity (80-95% maritime), high pressure
        if [ $hour -ge 0 ] && [ $hour -lt 7 ]; then
            base_temp=-7  # Night: bitterly cold
        elif [ $hour -ge 7 ] && [ $hour -lt 11 ]; then
            base_temp=-5  # Morning: very cold
        elif [ $hour -ge 11 ] && [ $hour -lt 14 ]; then
            base_temp=-2  # Midday: still below freezing
        elif [ $hour -ge 14 ] && [ $hour -lt 17 ]; then
            base_temp=-3  # Afternoon
        else
            base_temp=-6  # Evening: cold again
        fi
        temp=$(awk "BEGIN {printf \"%.1f\", $base_temp + (rand() * 3 - 1.5)}")
        humidity=$((80 + RANDOM % 15))  # Maritime high humidity
        pressure=$((1028 + RANDOM % 8))  # Strong winter high
        lux=$((15 + (hour >= 9 && hour <= 16 ? hour * 25 : 5) + RANDOM % 30))
        uvi=$(awk "BEGIN {printf \"%.1f\", ($hour >= 11 && $hour <= 14) ? (rand() * 1.0) : 0}")
        
        payload="{\"temperature\": $temp, \"humidity\": $humidity, \"pressure\": $pressure, \"lux\": $lux, \"uvi\": $uvi, \"timestamp\": \"$datetime\", \"deviceId\": \"$DEVICE2_ID\"}"
        api_call POST "/api/weather" "$payload" > /dev/null
        ((count2++))
    done
done
echo "   ✅ Hamburg: $count2 readings (VERY COLD: ~-5°C avg)"

echo "✅ Weather data seeded ($((count + count2)) total readings)"

echo ""
echo "🦊 Seeding Animal Detections..."
echo "--------------------------------"

# ============================================
# DEVICE 2: Garden Camera - BIRDS only (uses DEVICE2_ID)
# ============================================
echo "   🐦 Device 2: Garden Camera → BIRDS (Robin, Sparrow, Blue Tit...)"

bird_species=("Robin" "Sparrow" "Blue Tit" "Blackbird" "Magpie" "Great Tit" "Wren" "Starling" "Chaffinch" "Goldfinch")
bird_detections=0

for day in $(seq 0 6); do
    num_detections=$((8 + RANDOM % 12))
    
    for _ in $(seq 1 $num_detections); do
        if [ $((RANDOM % 3)) -lt 2 ]; then
            hour=$((6 + RANDOM % 14))
        else
            hour=$((RANDOM % 24))
        fi
        minute=$((RANDOM % 60))
        second=$((RANDOM % 60))
        
        timestamp=$((now - (day * 86400) - ((23 - hour) * 3600) - ((59 - minute) * 60) - second))
        
        if date -r $timestamp "+%Y-%m-%dT%H:%M:%S" > /dev/null 2>&1; then
            datetime=$(date -r $timestamp "+%Y-%m-%dT%H:%M:%S")
        else
            datetime=$(date -d "@$timestamp" "+%Y-%m-%dT%H:%M:%S" 2>/dev/null || echo "2024-01-01T12:00:00")
        fi
        
        species_idx=$((RANDOM % ${#bird_species[@]}))
        species_name="${bird_species[$species_idx]}"
        conf=$(awk "BEGIN {printf \"%.2f\", 0.75 + (rand() * 0.24)}")
        
        x=$((50 + RANDOM % 400))
        y=$((50 + RANDOM % 300))
        w=$((60 + RANDOM % 80))
        h=$((60 + RANDOM % 80))
        
        payload="{\"species\": \"$species_name\", \"animalType\": \"bird\", \"confidence\": $conf, \"x\": $x, \"y\": $y, \"width\": $w, \"height\": $h, \"timestamp\": \"$datetime\", \"deviceId\": \"$DEVICE2_ID\"}"
        api_call POST "/api/animals/detect" "$payload" > /dev/null
        ((bird_detections++))
    done
done
echo "   ✅ Garden Camera: $bird_detections bird detections"

# ============================================
# DEVICE 1: Weather Station - MAMMALS only (uses DEVICE1_ID)
# ============================================
echo "   🦊 Device 1: Weather Station → MAMMALS (Fox, Squirrel, Hedgehog...)"

mammal_species=("Fox" "Squirrel" "Hedgehog" "Rabbit" "Deer" "Badger" "Mouse" "Rat" "Mole" "Stoat")
mammal_detections=0

for day in $(seq 0 6); do
    num_detections=$((3 + RANDOM % 6))
    
    for _ in $(seq 1 $num_detections); do
        # Mammals more active at dawn/dusk
        if [ $((RANDOM % 2)) -eq 0 ]; then
            hour=$((5 + RANDOM % 3))  # Dawn: 5-7 AM
        else
            hour=$((18 + RANDOM % 4)) # Dusk: 6-9 PM
        fi
        minute=$((RANDOM % 60))
        second=$((RANDOM % 60))
        
        timestamp=$((now - (day * 86400) - ((23 - hour) * 3600) - ((59 - minute) * 60) - second))
        
        if date -r $timestamp "+%Y-%m-%dT%H:%M:%S" > /dev/null 2>&1; then
            datetime=$(date -r $timestamp "+%Y-%m-%dT%H:%M:%S")
        else
            datetime=$(date -d "@$timestamp" "+%Y-%m-%dT%H:%M:%S" 2>/dev/null || echo "2024-01-01T12:00:00")
        fi
        
        species_idx=$((RANDOM % ${#mammal_species[@]}))
        species_name="${mammal_species[$species_idx]}"
        conf=$(awk "BEGIN {printf \"%.2f\", 0.60 + (rand() * 0.35)}")
        
        x=$((100 + RANDOM % 350))
        y=$((80 + RANDOM % 250))
        w=$((100 + RANDOM % 150))
        h=$((80 + RANDOM % 120))
        
        payload="{\"species\": \"$species_name\", \"animalType\": \"mammal\", \"confidence\": $conf, \"x\": $x, \"y\": $y, \"width\": $w, \"height\": $h, \"timestamp\": \"$datetime\", \"deviceId\": \"$DEVICE1_ID\"}"
        api_call POST "/api/animals/detect" "$payload" > /dev/null
        ((mammal_detections++))
    done
done
echo "   ✅ Weather Station: $mammal_detections mammal detections"

total_detections=$((bird_detections + mammal_detections))
echo "✅ Animal detections seeded ($total_detections total)"

echo ""
echo "🎉 Mock data seeding complete!"
echo ""
echo ""
echo "Summary:"
echo "  📊 Weather readings:"
echo "     - Device 1 ($DEVICE1_ID): $count entries → HdM Stuttgart (~-3°C)"
echo "     - Device 2 ($DEVICE2_ID): $count2 entries → Hamburg (~-5°C)"
echo "  🦊 Animal detections:"
echo "     - Device 2 ($DEVICE2_ID): $bird_detections BIRD detections"
echo "     - Device 1 ($DEVICE1_ID): $mammal_detections MAMMAL detections"
echo ""
echo "🔍 To verify multi-device support:"
echo "   - Select Device 1 → HdM Stuttgart: COLD (~-3°C) + MAMMALS"
echo "   - Select Device 2 → Hamburg: VERY COLD (~-5°C) + BIRDS"
echo ""
echo "Access the frontend at: http://localhost:3000"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📡 Starting continuous heartbeat to keep devices ACTIVE..."
echo "   Press Ctrl+C to stop"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Heartbeat interval in seconds
HEARTBEAT_INTERVAL=30
heartbeat_count=0

# Trap Ctrl+C to exit gracefully
trap 'echo ""; echo "🛑 Stopping heartbeat..."; echo "   Sent $heartbeat_count heartbeats"; exit 0' INT

while true; do
    ((heartbeat_count++))
    current_time=$(date "+%H:%M:%S")
    
    # Weather Station (Device 1): HdM Stuttgart
    STATUS_MSG1="{\"device_id\":\"$DEVICE1_ID\",\"ip\":\"192.168.1.100\",\"service\":\"weather\",\"latitude\":48.741102,\"longitude\":9.101742}"
    mqtt_publish_with_token "device/$DEVICE1_ID/status" "$STATUS_MSG1" "$DEVICE1_ID" "$DEVICE1_TOKEN" 2>/dev/null
    
    # Garden Camera (Device 2): Hamburg  
    STATUS_MSG2="{\"device_id\":\"$DEVICE2_ID\",\"ip\":\"192.168.1.101\",\"service\":\"camera\",\"latitude\":53.539967,\"longitude\":9.955407}"
    mqtt_publish_with_token "device/$DEVICE2_ID/status" "$STATUS_MSG2" "$DEVICE2_ID" "$DEVICE2_TOKEN" 2>/dev/null
    
    echo "   [$current_time] 💓 Heartbeat #$heartbeat_count sent (2 devices)"
    
    sleep $HEARTBEAT_INTERVAL
done
