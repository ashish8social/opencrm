# OpenCRM

A lightweight, open-source Salesforce clone built with PostgreSQL, Java Spring Boot, and React. Features a metadata-driven dynamic entity engine — the same engine that powers pre-built Sales entities also lets you create custom entities at runtime.

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│  React/Vite │────▶│  Spring Boot API  │────▶│  PostgreSQL  │
│  Port 5173  │     │  Java 21          │     │  Port 5432   │
└─────────────┘     │  Port 8081        │     └──────────────┘
                    └──────────────────┘
         All services run in Docker via Docker Compose
```

## Quick Start

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose

### Start the application

```bash
cd opencrm
docker compose up -d
```

This starts three containers:
- **db** — PostgreSQL 16 (port 5432)
- **backend** — Spring Boot API (port 8081)
- **frontend** — React/Vite dev server (port 5173)

First startup takes a few minutes to download images and build. Subsequent starts are fast.

Once running, open **http://localhost:5173** in your browser.

### Stop the application

```bash
docker compose stop
```

### Restart the application

```bash
docker compose start
```

### Full teardown (removes containers and data)

```bash
docker compose down -v
```

> **Warning:** The `-v` flag deletes the database volume. All data will be lost. Omit `-v` to keep your data.

### Rebuild after code changes

```bash
docker compose build
docker compose up -d
```

### View logs

```bash
# All services
docker compose logs -f

# Backend only
docker compose logs -f backend

# Frontend only
docker compose logs -f frontend
```

---

## Default Login

| Username | Password   |
|----------|-----------|
| `admin`  | `admin123` |

The default admin user is created automatically on first startup.

---

## Creating a New User

There is no user registration UI yet. Create users via the database:

```bash
# Connect to the PostgreSQL container
docker compose exec db psql -U opencrm -d opencrm

# Insert a new user (password must be a BCrypt hash)
# You can generate a hash at https://bcrypt-generator.com/
INSERT INTO users (username, email, password_hash, full_name, active)
VALUES ('john', 'john@example.com', '$2a$10$YOUR_BCRYPT_HASH_HERE', 'John Doe', true);

# Exit psql
\q
```

Or use the API directly:

```bash
# Generate a BCrypt hash using the running backend
docker compose exec backend java -cp app.jar \
  -Dloader.main=org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder \
  org.springframework.boot.loader.launch.PropertiesLauncher
```

---

## Features

### Dynamic Entity Engine

The core of OpenCRM is a metadata-driven engine. Every entity — whether built-in (Account, Opportunity) or user-created — is stored as metadata and rendered dynamically.

- **Create custom entities** at runtime via the Entity Builder (Setup > Entity Builder)
- **Add fields of any type** to any entity — no code changes or restarts needed
- **All data stored in a single `records` table** using PostgreSQL JSONB for maximum flexibility
- **No schema migrations** when creating custom entities

### Supported Field Types

| Type | Description |
|------|-------------|
| TEXT | Single-line text |
| NUMBER | Whole numbers |
| DECIMAL | Decimal numbers |
| CURRENCY | Money values |
| DATE | Date picker |
| DATETIME | Date and time |
| BOOLEAN | Checkbox / toggle |
| PICKLIST | Single-select dropdown |
| MULTI_PICKLIST | Multi-select dropdown |
| EMAIL | Email address |
| PHONE | Phone number |
| URL | Web address |
| TEXTAREA | Multi-line text |
| RICH_TEXT | Rich text editor |
| LOOKUP | Foreign key to another entity |
| MASTER_DETAIL | Parent-child relationship (cascade delete) |
| FORMULA | Computed field |
| AUTO_NUMBER | Auto-incrementing number |

### Pre-built Sales Cloud Entities

OpenCRM ships with 11 pre-configured sales entities, ready to use out of the box:

| Entity | Description |
|--------|-------------|
| **Account** | Companies and organizations |
| **Contact** | People linked to accounts |
| **Opportunity** | Sales deals and pipeline stages |
| **Product** | Product catalog |
| **Price Book** | Price lists |
| **Price Book Entry** | Product pricing within price books |
| **Opportunity Line Item** | Products on opportunities (master-detail) |
| **Quote** | Sales quotes linked to opportunities (master-detail) |
| **Quote Line Item** | Line items on quotes (master-detail) |
| **Order** | Sales orders linked to accounts |
| **Order Item** | Line items on orders (master-detail) |

### Sales Workflows You Can Do Today

1. **Account & Contact Management** — Create companies, add contacts linked to them, view relationships
2. **Sales Pipeline** — Create opportunities, assign stages (Prospecting → Qualification → Proposal → Negotiation → Closed Won/Lost), track amounts and probability
3. **Product Catalog** — Set up products with families (Software, Hardware, Services, Support)
4. **Pricing** — Create price books and assign unit prices to products
5. **Quoting** — Create quotes from opportunities, add line items with quantities and discounts
6. **Order Management** — Create orders linked to accounts, add order items
7. **Custom Entities** — Build your own entities (e.g., Support Cases, Projects) with any field types

### UI Features

- **Dynamic List Views** — Sortable, paginated tables for any entity
- **Dynamic Detail Views** — All fields rendered based on entity metadata
- **Dynamic Forms** — Create/edit forms with correct input types per field
- **Sidebar Navigation** — All entities organized by Sales / Custom / Setup
- **Global Search Bar** — Search across entities
- **Entity Builder** — Create and manage custom entities and fields

---

## API Reference

All APIs require a JWT token in the `Authorization: Bearer <token>` header (except login/refresh).

### Authentication

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Refresh token
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your-refresh-token>"}'

# Get current user
curl http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer <token>"
```

### Metadata (Entity & Field Management)

```bash
# List all entities
GET /api/metadata/entities

# Get entity with fields
GET /api/metadata/entities/{apiName}

# Create custom entity
POST /api/metadata/entities
  {"apiName":"Project__c","label":"Project","pluralLabel":"Projects"}

# Add field to entity
POST /api/metadata/entities/{apiName}/fields
  {"apiName":"Budget","label":"Budget","fieldType":"CURRENCY","required":true}

# Delete custom entity
DELETE /api/metadata/entities/{apiName}
```

### Records (Works for ANY Entity)

```bash
# List records (paginated)
GET /api/data/{entityApiName}?page=0&size=25&sort=Name,asc

# Get single record
GET /api/data/{entityApiName}/{id}

# Create record
POST /api/data/{entityApiName}
  {"Name":"Acme Corp","Industry":"Technology","Phone":"555-0100"}

# Update record
PUT /api/data/{entityApiName}/{id}
  {"Industry":"Finance"}

# Delete record (soft delete)
DELETE /api/data/{entityApiName}/{id}

# Get related records
GET /api/data/{entityApiName}/{id}/related/{relatedEntityApiName}

# Search within entity
GET /api/data/{entityApiName}/search?q=acme

# Global search
GET /api/search?q=acme&entities=Account,Contact
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Database | PostgreSQL 16 with JSONB |
| Backend | Java 21, Spring Boot 3.3, Spring Security, Flyway |
| Auth | JWT (jjwt library) + BCrypt |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| State | TanStack Query (React Query) |
| Routing | React Router v6 |
| Containers | Docker, Docker Compose |

---

## Project Structure

```
opencrm/
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/opencrm/
│       │   ├── auth/          # JWT auth, login, user management
│       │   ├── config/        # Security, CORS, data initializer
│       │   ├── metadata/      # Entity & field definition CRUD
│       │   ├── data/          # Dynamic record CRUD engine
│       │   ├── sales/         # Sales-specific services
│       │   ├── reporting/     # Dashboard & reports
│       │   └── common/        # Shared DTOs, exceptions
│       └── resources/
│           ├── application.yml
│           └── db/migration/  # Flyway SQL migrations
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/
        ├── api/               # Axios API client with JWT interceptor
        ├── auth/              # Login, AuthContext, ProtectedRoute
        ├── layout/            # AppShell, Sidebar, TopBar
        ├── metadata/          # Entity Builder
        ├── records/           # ListView, DetailView, RecordForm, DynamicField
        ├── reporting/         # Dashboard
        └── types/             # TypeScript type definitions
```

---

## Environment Variables

Copy `.env.example` to `.env` and configure:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PASSWORD` | `opencrm_dev` | PostgreSQL password |
| `JWT_SECRET` | (dev default) | JWT signing secret (change in production!) |

---

## Roadmap

- [ ] Quote PDF generation (download quote as PDF)
- [ ] Quote-to-Order conversion (one-click)
- [ ] Lookup field with search-as-you-type
- [ ] Sales pipeline Kanban board
- [ ] Dashboard charts (pipeline funnel, revenue by month)
- [ ] Page layout editor (drag-and-drop field arrangement)
- [ ] Validation rules UI
- [ ] Record types (different layouts per record type)
- [ ] Production deployment config (nginx + TLS for VPS)
- [ ] User management UI
