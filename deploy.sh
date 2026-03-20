#!/bin/bash
# ============================================================
# OpenCRM — VPS Deployment Script
# Run this ON your VPS after uploading the project files
# Usage: sudo bash deploy.sh
# ============================================================

set -e

echo "========================================="
echo "  OpenCRM VPS Deployment"
echo "========================================="

# ---- 1. System updates & Docker install ----
echo ""
echo "[1/5] Installing Docker..."
apt-get update -qq
apt-get install -y -qq ca-certificates curl gnupg lsb-release > /dev/null

# Add Docker GPG key and repo
install -m 0755 -d /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
fi

if [ ! -f /etc/apt/sources.list.d/docker.list ]; then
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
fi

apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin > /dev/null
echo "  Docker installed: $(docker --version)"

# ---- 2. Firewall ----
echo ""
echo "[2/5] Configuring firewall..."
if command -v ufw > /dev/null 2>&1; then
    ufw allow 22/tcp   > /dev/null 2>&1 || true
    ufw allow 80/tcp   > /dev/null 2>&1 || true
    ufw allow 443/tcp  > /dev/null 2>&1 || true
    ufw --force enable  > /dev/null 2>&1 || true
    echo "  Firewall: ports 22, 80, 443 open"
else
    echo "  ufw not found, skipping firewall config"
fi

# ---- 3. Environment file ----
echo ""
echo "[3/5] Setting up environment..."
ENV_FILE="$(dirname "$0")/.env"

if [ ! -f "$ENV_FILE" ]; then
    DB_PASS=$(openssl rand -base64 24 | tr -d '=+/' | head -c 32)
    JWT_SEC=$(openssl rand -base64 48 | tr -d '=+/' | head -c 64)

    cat > "$ENV_FILE" <<EOF
DB_PASSWORD=${DB_PASS}
JWT_SECRET=${JWT_SEC}
EOF
    echo "  Created .env with random secrets"
else
    echo "  .env already exists, keeping existing secrets"
fi

# ---- 4. Build and start ----
echo ""
echo "[4/5] Building and starting containers..."
cd "$(dirname "$0")"
docker compose -f docker-compose.prod.yml build --quiet
docker compose -f docker-compose.prod.yml up -d

echo "  Waiting for backend to start..."
sleep 15

# Check health
if curl -sf http://localhost/api/auth/login > /dev/null 2>&1 || curl -sf http://localhost:80 > /dev/null 2>&1; then
    echo "  Application is running!"
else
    echo "  Backend may still be starting. Check: docker compose -f docker-compose.prod.yml logs backend"
fi

# ---- 5. Summary ----
echo ""
echo "========================================="
echo "  Deployment complete!"
echo "========================================="
echo ""
echo "  URL:      http://$(hostname -I | awk '{print $1}')"
echo "  Login:    admin / admin123"
echo ""
echo "  Useful commands:"
echo "    View logs:     docker compose -f docker-compose.prod.yml logs -f"
echo "    Stop:          docker compose -f docker-compose.prod.yml stop"
echo "    Start:         docker compose -f docker-compose.prod.yml start"
echo "    Rebuild:       docker compose -f docker-compose.prod.yml up -d --build"
echo ""
echo "  To enable HTTPS with a domain name, see DEPLOY.md"
echo "========================================="
