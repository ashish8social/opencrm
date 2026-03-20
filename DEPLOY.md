# Deploying OpenCRM to a VPS

Step-by-step guide to deploy OpenCRM on a fresh Ubuntu VPS (tested on Ubuntu 22.04/24.04).

---

## Step 1: Upload the project to your VPS

From your **local machine**, upload the project using `scp`:

```bash
# Create a tarball (from the parent directory of opencrm/)
cd /path/to/claudePlayground
tar czf opencrm.tar.gz --exclude='node_modules' --exclude='.git' --exclude='pgdata' opencrm/

# Upload to your VPS
scp opencrm.tar.gz root@YOUR_VPS_IP:/root/
```

Replace `YOUR_VPS_IP` with your LightNode server's IP address. You'll be prompted for the root password.

## Step 2: SSH into your VPS

```bash
ssh root@YOUR_VPS_IP
```

## Step 3: Extract and deploy

```bash
cd /root
tar xzf opencrm.tar.gz
cd opencrm
sudo bash deploy.sh
```

The script will:
1. Install Docker and Docker Compose
2. Configure the firewall (ports 22, 80, 443)
3. Generate secure random passwords for the database and JWT secret
4. Build and start all containers
5. Print the URL to access OpenCRM

After it finishes, open **http://YOUR_VPS_IP** in your browser.

Login: `admin` / `admin123`

---

## Managing the Application

All commands should be run from the `/root/opencrm` directory.

### View status

```bash
docker compose -f docker-compose.prod.yml ps
```

### View logs

```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Backend only
docker compose -f docker-compose.prod.yml logs -f backend

# Just last 100 lines
docker compose -f docker-compose.prod.yml logs --tail 100 backend
```

### Stop the application

```bash
docker compose -f docker-compose.prod.yml stop
```

### Start the application

```bash
docker compose -f docker-compose.prod.yml start
```

### Restart after code changes

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Full reset (destroys all data)

```bash
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d --build
```

---

## Setting Up HTTPS with a Domain Name (Optional)

If you have a domain name pointed to your VPS IP:

### 1. Update nginx config

Edit `nginx/prod.conf` — replace `YOUR_DOMAIN` with your actual domain in the commented-out HTTPS section at the bottom, then uncomment those blocks.

### 2. Get SSL certificate

```bash
# Create certbot directories
mkdir -p nginx/certbot/conf nginx/certbot/www

# Restart nginx to pick up the challenge location
docker compose -f docker-compose.prod.yml restart nginx

# Run certbot
docker compose -f docker-compose.prod.yml run --rm certbot \
  certonly --webroot --webroot-path=/var/www/certbot \
  --email your-email@example.com --agree-tos --no-eff-email \
  -d YOUR_DOMAIN

# Restart nginx to load the certificate
docker compose -f docker-compose.prod.yml restart nginx
```

Certificates auto-renew via the certbot container.

---

## Updating the Application

When you have code changes:

### Option A: Re-upload and rebuild

```bash
# On your local machine
cd /path/to/claudePlayground
tar czf opencrm.tar.gz --exclude='node_modules' --exclude='.git' --exclude='pgdata' opencrm/
scp opencrm.tar.gz root@YOUR_VPS_IP:/root/

# On the VPS
cd /root
tar xzf opencrm.tar.gz
cd opencrm
docker compose -f docker-compose.prod.yml up -d --build
```

### Option B: Use Git (recommended for ongoing development)

```bash
# On your local machine — push to a Git repo (GitHub, GitLab, etc.)
cd opencrm
git init && git add -A && git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USER/opencrm.git
git push -u origin main

# On the VPS — clone and deploy
cd /root
git clone https://github.com/YOUR_USER/opencrm.git
cd opencrm
sudo bash deploy.sh

# For subsequent updates:
cd /root/opencrm
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

---

## Backup & Restore

### Backup the database

```bash
docker compose -f docker-compose.prod.yml exec db \
  pg_dump -U opencrm opencrm > backup_$(date +%Y%m%d).sql
```

### Restore from backup

```bash
docker compose -f docker-compose.prod.yml exec -T db \
  psql -U opencrm opencrm < backup_20260320.sql
```

---

## Troubleshooting

### Containers won't start

```bash
# Check what's running
docker compose -f docker-compose.prod.yml ps

# Check logs for errors
docker compose -f docker-compose.prod.yml logs backend
```

### Backend can't connect to database

The backend waits for the database health check. If the DB is slow to start:

```bash
# Restart just the backend
docker compose -f docker-compose.prod.yml restart backend
```

### Port 80 already in use

```bash
# Find what's using port 80
lsof -i :80

# If it's apache2, stop and disable it
systemctl stop apache2
systemctl disable apache2
```

### Out of disk space

```bash
# Check disk usage
df -h

# Clean unused Docker images
docker system prune -a
```

### Change admin password

```bash
# Connect to the database
docker compose -f docker-compose.prod.yml exec db psql -U opencrm -d opencrm

# Generate a new hash at https://bcrypt-generator.com/ and update
UPDATE users SET password_hash = '$2a$10$NEW_HASH_HERE' WHERE username = 'admin';
\q
```
