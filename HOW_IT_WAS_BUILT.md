# How OpenCRM Was Built — From Zero to Deployed

A beginner-friendly walkthrough of every decision, design choice, and step taken to build and deploy OpenCRM — a lightweight Salesforce clone.

---

## Table of Contents

1. [The Starting Point — What Problem Are We Solving?](#1-the-starting-point)
2. [Choosing the Tech Stack — Why These Tools?](#2-choosing-the-tech-stack)
3. [The Big Architectural Insight — Metadata-Driven Design](#3-the-big-architectural-insight)
4. [Database Design — The Foundation](#4-database-design)
5. [Setting Up the Project Structure](#5-setting-up-the-project-structure)
6. [Docker — Making Everything Reproducible](#6-docker)
7. [Backend Development — Layer by Layer](#7-backend-development)
8. [Frontend Development — Building the UI](#8-frontend-development)
9. [Connecting Frontend to Backend](#9-connecting-frontend-to-backend)
10. [Adding Sales Features](#10-adding-sales-features)
11. [Production Deployment to a VPS](#11-production-deployment)
12. [Bugs We Hit and How We Fixed Them](#12-bugs-and-fixes)
13. [Key Takeaways](#13-key-takeaways)

---

## 1. The Starting Point

### What is Salesforce?

Salesforce is a Customer Relationship Management (CRM) platform. At its core, it helps businesses track:

- **Accounts** — Companies you do business with
- **Contacts** — People at those companies
- **Opportunities** — Potential deals you're working on
- **Quotes** — Price proposals you send to customers
- **Orders** — Confirmed purchases

But what makes Salesforce special isn't just these built-in objects — it's that users can **create their own custom objects** (entities) with custom fields, without writing code. A hospital could add a "Patient" entity; a school could add a "Student" entity.

### Our Goal

Build a web application that:
1. Has the core Sales Cloud entities (Account, Contact, Opportunity, etc.)
2. Lets users create **custom entities** at runtime (no code changes needed)
3. Runs entirely in Docker containers
4. Can be deployed to a single cheap VPS

### The Core Challenge

The hard part isn't building a CRUD app for Accounts and Contacts — that's straightforward. The hard part is: **how do you let users create entirely new database "tables" at runtime without actually modifying the database schema?**

This question drove every major architectural decision.

---

## 2. Choosing the Tech Stack

### The Three-Tier Architecture

Every web application has three layers. Think of it like a restaurant:

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│   FRONTEND  (React)          ← The dining room          │
│   What the user sees           (menu, plates, decor)    │
│   and interacts with                                    │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   BACKEND  (Java/Spring Boot) ← The kitchen             │
│   Business logic, validation,   (chefs, recipes,        │
│   API endpoints                  food safety rules)     │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   DATABASE  (PostgreSQL)      ← The pantry/storage      │
│   Where all data lives          (ingredients, inventory) │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Why These Specific Technologies?

| Choice | Why |
|--------|-----|
| **PostgreSQL** | Has a special feature called JSONB that lets us store flexible data (critical for our custom entities). It's also free, reliable, and battle-tested. |
| **Java + Spring Boot** | Mature ecosystem with built-in security, database access, and validation. Spring Boot auto-configures most things so we write less boilerplate. Java 21 adds virtual threads for better performance. |
| **React + TypeScript** | The most popular frontend framework. TypeScript catches bugs at compile time (like spelling a variable wrong). React's component model maps perfectly to our dynamic UI needs. |
| **Vite** | Super fast build tool for React. Hot-reloads changes instantly during development. |
| **Tailwind CSS** | Utility-first CSS framework. Instead of writing separate CSS files, you add classes directly: `className="text-sm font-bold text-blue-600"`. Much faster for building UIs. |
| **Docker** | Packages each part (database, backend, frontend) into isolated containers. "Works on my machine" becomes "works on every machine." |

---

## 3. The Big Architectural Insight

### The Problem with Traditional Database Design

Normally, each entity gets its own database table:

```
Traditional approach:
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│   accounts   │  │   contacts   │  │  opportunities   │
├──────────────┤  ├──────────────┤  ├──────────────────┤
│ id           │  │ id           │  │ id               │
│ name         │  │ first_name   │  │ name             │
│ industry     │  │ last_name    │  │ stage            │
│ website      │  │ email        │  │ amount           │
│ phone        │  │ phone        │  │ close_date       │
└──────────────┘  └──────────────┘  └──────────────────┘
```

This works great... until a user says "I want to create a Support Case entity with Priority, Description, and Resolution fields." Now you'd need to:

1. Run a `CREATE TABLE` SQL command at runtime
2. Write new API endpoints
3. Build new UI forms

That's complex, risky, and fragile.

### Our Solution: The Metadata-Driven Approach

Instead of one table per entity, we use **one universal table for ALL records** and separate **metadata tables** that describe what entities and fields exist:

```
Our approach:
┌─────────────────────────────────────────────────────────┐
│                    entity_defs                          │
│  (describes WHAT entities exist)                        │
├─────────────────────────────────────────────────────────┤
│ id: abc-123     │ api_name: "Account"  │ label: "Account"
│ id: def-456     │ api_name: "Contact"  │ label: "Contact"
│ id: ghi-789     │ api_name: "Case__c"  │ label: "Support Case"  ← user-created!
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    field_defs                            │
│  (describes WHAT fields each entity has)                │
├─────────────────────────────────────────────────────────┤
│ entity: abc-123 │ api_name: "Name"     │ type: TEXT
│ entity: abc-123 │ api_name: "Industry" │ type: PICKLIST
│ entity: ghi-789 │ api_name: "Priority" │ type: PICKLIST    ← user-created!
│ entity: ghi-789 │ api_name: "Desc"     │ type: TEXTAREA    ← user-created!
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                      records                            │
│  (ALL data for ALL entities lives here)                 │
├─────────────────────────────────────────────────────────┤
│ id │ entity_def_id │ name        │ data (JSONB)         │
│ 1  │ abc-123       │ "Acme Corp" │ {"Industry":"Tech"}  │
│ 2  │ def-456       │ "John Doe"  │ {"Email":"j@acme"}   │
│ 3  │ ghi-789       │ "Case #1"   │ {"Priority":"High"}  │ ← custom entity data!
└─────────────────────────────────────────────────────────┘
```

### Why This Works

The magic is the **JSONB** column. JSONB is a PostgreSQL feature that stores JSON data efficiently, with indexing support. Each record stores its field values as a JSON object in the `data` column.

When a user creates a new "Support Case" entity:
1. We insert a row into `entity_defs` (name, label, etc.)
2. We insert rows into `field_defs` (Priority, Description, etc.)
3. When they create a Support Case record, we insert into `records` with the data as JSONB

**No schema changes. No new tables. No new API endpoints.** The same code handles every entity.

### The Tradeoff

```
Traditional (one table per entity):
  ✅ Maximum query performance
  ✅ Strong database-level constraints
  ❌ Can't add entities at runtime
  ❌ Requires code changes for each entity

Our approach (universal records + metadata):
  ✅ Users can create entities at runtime
  ✅ Same code handles everything
  ✅ Very flexible
  ❌ Slightly less query performance (still fast with JSONB indexes)
  ❌ Validation must be done in application code, not database constraints
```

For a CRM, the flexibility tradeoff is absolutely worth it. Salesforce itself uses a similar approach internally.

---

## 4. Database Design

### The Migration Strategy

We don't create database tables manually. We use **Flyway** — a database migration tool. You write numbered SQL files, and Flyway runs them in order, tracking which ones have already been applied.

```
db/migration/
├── V1__create_auth_tables.sql       ← Creates users table
├── V2__create_metadata_tables.sql   ← Creates entity_defs, field_defs, etc.
├── V3__create_record_tables.sql     ← Creates the universal records table
└── V4__seed_sales_entities.sql      ← Pre-populates the 11 sales entities
```

Why "V1", "V2"? Flyway runs them in version order. If V1 and V2 have already run, and you add V5, only V5 runs next time. This makes database changes safe and repeatable.

### Table Relationships

```
┌──────────────┐
│    users     │
│──────────────│
│ id (PK)      │──────────────────────────────────────────┐
│ username     │                                          │
│ password_hash│                                          │
│ email        │                                          │
│ full_name    │                                          │
└──────────────┘                                          │
                                                          │
┌──────────────┐      ┌──────────────┐                    │
│ entity_defs  │      │  field_defs  │                    │
│──────────────│      │──────────────│                    │
│ id (PK)      │◄────▶│ entity_def_id│(FK)                │
│ api_name     │      │ id (PK)      │                    │
│ label        │      │ api_name     │                    │
│ plural_label │      │ label        │                    │
│ is_custom    │      │ field_type   │ ← TEXT, NUMBER,    │
│ name_field   │      │ required     │   PICKLIST,        │
│ icon         │      │ ref_entity_id│ ← points to        │
│              │      │ relation_type│   another entity    │
│              │      │ picklist_vals│   for LOOKUP fields │
└──────────────┘      └──────────────┘                    │
       │                                                  │
       │              ┌──────────────┐                    │
       │              │   records    │                    │
       │              │──────────────│                    │
       └─────────────▶│ entity_def_id│(FK)                │
                      │ id (PK)      │                    │
                      │ name         │ ← denormalized     │
                      │ data (JSONB) │ ← ALL field values │
                      │ owner_id     │────────────────────┘
                      │ is_deleted   │ ← soft delete
                      │ created_at   │
                      │ updated_at   │
                      └──────────────┘

   PK = Primary Key (unique identifier)
   FK = Foreign Key (reference to another table)
```

### Key Design Decisions

**Soft Delete**: We never actually DELETE records from the database. Instead, we set `is_deleted = true`. This prevents accidental data loss and enables an "undo" or "recycle bin" feature later.

**Denormalized Name**: The `name` field is stored directly on the `records` table even though it's also in the JSONB `data`. This makes sorting and searching by name much faster since PostgreSQL can use a regular index on it.

**GIN Index on JSONB**: We created a GIN (Generalized Inverted Index) on the `data` column. This lets PostgreSQL efficiently search inside the JSON data.

### Seeding Sales Entities

Migration V4 is special — it doesn't create tables, it **inserts data**. It pre-populates the `entity_defs` and `field_defs` tables with all 11 standard sales entities:

```
Account ──┐
           ├── Contact (has AccountId lookup)
           ├── Opportunity (has AccountId lookup)
           │    ├── OpportunityLineItem (master-detail to Opportunity)
           │    └── Quote (has OpportunityId lookup)
           │         ├── QuoteLineItem (master-detail to Quote)
           │         └── Order (converted from Quote)
           │              └── OrderItem (master-detail to Order)
           │
Product ──── PriceBook
              └── PriceBookEntry (links Product + PriceBook)
```

The `master-detail` vs `lookup` distinction matters:
- **Lookup**: A loose reference. Deleting an Account doesn't delete its Contacts.
- **Master-Detail**: A tight parent-child bond. Deleting a Quote automatically deletes all its QuoteLineItems (cascade delete).

---

## 5. Setting Up the Project Structure

### Directory Layout

```
opencrm/
│
├── docker-compose.yml           ← Dev environment definition
├── docker-compose.prod.yml      ← Production environment
├── deploy.sh                    ← One-click VPS deployment script
├── .env.example                 ← Template for environment variables
│
├── backend/                     ← Java Spring Boot application
│   ├── Dockerfile               ← How to build the backend container
│   ├── pom.xml                  ← Java dependencies (like package.json for Java)
│   └── src/main/
│       ├── java/com/opencrm/
│       │   ├── OpenCrmApplication.java    ← Entry point
│       │   ├── config/          ← Security, JWT, CORS configuration
│       │   ├── auth/            ← Login, tokens, user management
│       │   ├── metadata/        ← Entity & field definition CRUD
│       │   ├── data/            ← The universal record engine
│       │   ├── reporting/       ← Dashboard analytics APIs
│       │   ├── sales/           ← Quote PDF, order conversion
│       │   └── common/          ← Shared utilities, error handling
│       └── resources/
│           ├── application.yml  ← App configuration
│           └── db/migration/    ← Flyway SQL files
│
├── frontend/                    ← React application
│   ├── Dockerfile               ← Multi-stage: dev server OR production build
│   ├── package.json             ← JavaScript dependencies
│   ├── tailwind.config.js       ← Tailwind CSS configuration
│   └── src/
│       ├── App.tsx              ← Route definitions
│       ├── main.tsx             ← React entry point
│       ├── api/                 ← HTTP client and API functions
│       ├── auth/                ← Login page, auth context
│       ├── layout/              ← Sidebar, TopBar, AppShell
│       ├── records/             ← Generic CRUD views (list, detail, form)
│       ├── metadata/            ← Entity builder UI
│       ├── reporting/           ← Dashboard with charts
│       ├── sales/               ← Pipeline Kanban board
│       └── types/               ← TypeScript type definitions
│
└── nginx/                       ← Web server config for production
    └── prod.conf                ← Reverse proxy rules
```

### Why This Structure?

The backend is organized by **domain** (auth, metadata, data, sales), not by **layer** (controllers, services, repositories). This means everything related to "authentication" is in one folder. When you need to fix a login bug, you look in `auth/`, not across three different folders.

---

## 6. Docker

### What Is Docker?

Think of Docker as a shipping container for software. Just like a shipping container can hold anything and fits on any ship, a Docker container packages your app with everything it needs and runs on any machine.

### Our Docker Setup

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Compose                          │
│                                                             │
│  ┌─────────┐    ┌──────────┐    ┌──────────┐               │
│  │   db    │    │ backend  │    │ frontend │               │
│  │ Postgres│◄───│ Java/    │    │ React/   │               │
│  │  :5432  │    │ Spring   │    │ Vite     │               │
│  │         │    │  :8080   │    │  :5173   │               │
│  └─────────┘    └──────────┘    └──────────┘               │
│       ▲              ▲               ▲                      │
│       │              │               │                      │
│   volume:pgdata   builds from     builds from              │
│   (data persists  backend/        frontend/                │
│    across          Dockerfile      Dockerfile              │
│    restarts)                                               │
└─────────────────────────────────────────────────────────────┘

Your browser connects to frontend (:5173)
Frontend makes API calls to backend (:8080)
Backend reads/writes data to PostgreSQL (:5432)
```

### docker-compose.yml Explained

```yaml
services:
  db:                              # Service name (other containers use this as hostname)
    image: postgres:16-alpine      # Use official PostgreSQL 16 image (Alpine = small Linux)
    environment:
      POSTGRES_DB: opencrm         # Create a database called "opencrm" on first start
      POSTGRES_USER: opencrm       # Database username
      POSTGRES_PASSWORD: secret    # Database password
    volumes:
      - pgdata:/var/lib/postgresql/data   # Persist data even if container is destroyed
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U opencrm"]  # Docker checks if DB is ready
      interval: 10s               # Check every 10 seconds
      retries: 5                   # Give up after 5 failures

  backend:
    build: ./backend               # Build from the backend/Dockerfile
    environment:                   # Pass config as environment variables
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/opencrm
      #                                       ^^
      #                    "db" = the service name above. Docker networking
      #                    lets containers find each other by service name.
    depends_on:
      db:
        condition: service_healthy  # Don't start until DB is healthy

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"                # Map container port to host port
    depends_on:
      - backend
```

### Multi-Stage Dockerfiles

The frontend Dockerfile has a clever trick — **multi-stage builds**:

```dockerfile
# Stage 1: "dev" — for local development
FROM node:20-alpine AS dev
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
CMD ["npm", "run", "dev", "--", "--host"]   # Runs Vite dev server

# Stage 2: "build" — compiles production assets
FROM dev AS build
RUN npm run build                           # Creates optimized static files

# Stage 3: "prod" — tiny production image
FROM nginx:alpine AS prod
COPY --from=build /app/dist /usr/share/nginx/html  # Copy ONLY the built files
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

- In development: `docker compose build` uses the `dev` stage → runs Vite with hot reload
- In production: `docker compose -f docker-compose.prod.yml build` uses the `prod` stage → a tiny nginx image serving static files

The production image is ~30MB instead of ~500MB because it doesn't include Node.js, npm, or source code.

---

## 7. Backend Development

We built the backend in layers, from bottom to top:

### Layer 1: Authentication (JWT)

**What is JWT?** JSON Web Token — a secure way to prove "I am user X" without the server keeping a session. After login, the server gives you a signed token. You send it with every request.

```
Login Flow:
┌────────┐                          ┌────────┐                    ┌────────┐
│Browser │                          │Backend │                    │Database│
└───┬────┘                          └───┬────┘                    └───┬────┘
    │  POST /api/auth/login             │                             │
    │  { username, password }           │                             │
    │──────────────────────────────────▶│                             │
    │                                   │  Find user by username      │
    │                                   │────────────────────────────▶│
    │                                   │◀────────────────────────────│
    │                                   │                             │
    │                                   │  Compare password with      │
    │                                   │  BCrypt hash                │
    │                                   │  (stored hash, not plain!)  │
    │                                   │                             │
    │                                   │  Generate JWT token         │
    │                                   │  (signed with secret key)   │
    │                                   │                             │
    │  { accessToken: "eyJhbG..." }     │                             │
    │◀──────────────────────────────────│                             │
    │                                   │                             │
    │  GET /api/data/Account            │                             │
    │  Header: Authorization:           │                             │
    │    Bearer eyJhbG...               │                             │
    │──────────────────────────────────▶│                             │
    │                                   │  Verify JWT signature       │
    │                                   │  Extract user ID            │
    │                                   │  Process request...         │
```

**Key files:**
- `JwtService.java` — Generates and validates tokens
- `JwtAuthFilter.java` — Intercepts every request, checks for valid token
- `SecurityConfig.java` — Defines which URLs require authentication

**BCrypt** is a one-way hashing algorithm. We never store the actual password — we store a hash. When someone logs in, we hash their input and compare hashes. Even if the database is stolen, passwords can't be recovered.

### Layer 2: Metadata Engine

This is the CRUD for entity and field definitions — the "schema" of our dynamic system.

```
Creating a custom entity:

POST /api/metadata/entities
{
  "apiName": "SupportCase",
  "label": "Support Case",
  "pluralLabel": "Support Cases"
}
                    │
                    ▼
         ┌──────────────────┐
         │ EntityDefService  │
         │                  │
         │ 1. Validate name │
         │ 2. Set isCustom  │
         │    = true        │
         │ 3. Save to DB    │
         └──────────────────┘
                    │
                    ▼
POST /api/metadata/entities/SupportCase/fields
{
  "apiName": "Priority",
  "label": "Priority",
  "fieldType": "PICKLIST",
  "required": true,
  "picklistValues": [
    {"value": "Low", "label": "Low"},
    {"value": "Medium", "label": "Medium"},
    {"value": "High", "label": "High"}
  ]
}
                    │
                    ▼
         ┌──────────────────┐
         │ FieldDefService   │
         │                  │
         │ 1. Find parent   │
         │    entity        │
         │ 2. Validate      │
         │    field config   │
         │ 3. Save to DB    │
         └──────────────────┘
```

After this, the entity is immediately usable — the record engine and UI automatically pick it up.

### Layer 3: The Universal Record Engine

This is the heart of the application. One service (`RecordService.java`) handles CRUD for **every** entity.

```
Creating a record:

POST /api/data/SupportCase
{ "Name": "Login broken", "Priority": "High", "Description": "Users can't log in" }
              │
              ▼
    ┌─────────────────────────────────────────────────────┐
    │                  RecordService.create()              │
    │                                                     │
    │  1. Look up EntityDef by apiName ("SupportCase")    │
    │     → Gets entity ID, name field, etc.              │
    │                                                     │
    │  2. Look up FieldDefs for this entity               │
    │     → Gets list of fields with types & rules        │
    │                                                     │
    │  3. Validate data against field definitions:         │
    │     - Is "Priority" a valid picklist value? ✓       │
    │     - Is "Name" present (required)? ✓               │
    │     - Is "Priority" the right type? ✓               │
    │                                                     │
    │  4. Extract name from the designated name field      │
    │     → "Login broken" goes into records.name         │
    │                                                     │
    │  5. Serialize all data to JSON string               │
    │     → Goes into records.data as JSONB               │
    │                                                     │
    │  6. Save to the `records` table                     │
    └─────────────────────────────────────────────────────┘
```

**The beauty**: this exact same code path handles Account, Contact, Opportunity, and any custom entity. The field definitions drive everything.

### Layer 4: Reporting APIs

The reporting layer reads from the same `records` table and aggregates data:

```
Pipeline Summary:
1. Find all records where entity = "Opportunity"
2. Group by the "Stage" field in their JSONB data
3. For each stage, count records and sum the "Amount" field
4. Return: [{ stage: "Prospecting", count: 5, totalAmount: 250000 }, ...]

Revenue by Month:
1. Find Opportunities where Stage = "Closed Won"
2. Group by month from "CloseDate" field
3. Sum amounts per month

Top Accounts:
1. Find all Opportunities
2. Group by "AccountId" field
3. Sum amounts, resolve Account names
4. Sort by total, return top 10
```

### Layer 5: Sales-Specific Services

**Quote PDF Generation** — Uses the OpenPDF library to programmatically create a PDF document:

```
QuotePdfService.generateQuotePdf(quoteId):
1. Load the Quote record
2. Load all QuoteLineItem records linked to this Quote
3. Resolve Account name and Opportunity name
4. Build PDF:
   ┌──────────────────────────────────┐
   │                          QUOTE   │
   │                    Acme Proposal │
   │                                  │
   │ Quote Details    │ Bill To       │
   │ Status: Approved │ Acme Corp     │
   │ Expires: 2026-04 │               │
   │                                  │
   │ Line Items                       │
   │ ┌────────┬───┬───────┬─────────┐ │
   │ │Product │Qty│Price  │Total    │ │
   │ ├────────┼───┼───────┼─────────┤ │
   │ │Widget A│ 10│$50.00 │$500.00  │ │
   │ │Widget B│  5│$100.00│$500.00  │ │
   │ └────────┴───┴───────┴─────────┘ │
   │                                  │
   │              Grand Total: $1,000 │
   └──────────────────────────────────┘
5. Return as byte array (browser downloads it)
```

**Quote-to-Order Conversion**:
```
OrderConversionService.convertQuoteToOrder(quoteId):
1. Load the Quote — verify status is "Approved"
2. Create a new Order record with data copied from Quote
3. For each QuoteLineItem:
   → Create a corresponding OrderItem
4. Update Quote status to "Converted"
5. Return the new Order
```

### The Spring Boot Request Lifecycle

Every HTTP request flows through several layers:

```
HTTP Request
    │
    ▼
┌─────────────────┐
│  JwtAuthFilter   │  ← Checks Authorization header, validates token
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  SecurityConfig  │  ← Is this URL allowed? Is user authenticated?
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Controller     │  ← Parses URL params, request body
│  @RestController │     Calls the right service method
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    Service       │  ← Business logic, validation
│   @Service       │     Calls repository methods
│ @Transactional   │  ← Wraps everything in a DB transaction
└────────┬────────┘     (all-or-nothing: if step 3 fails, steps 1-2 are rolled back)
         │
         ▼
┌─────────────────┐
│   Repository     │  ← Translates to SQL queries
│ JpaRepository    │     Talks to PostgreSQL
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL     │
└─────────────────┘
```

### API Response Format

Every API response follows the same structure:

```json
{
  "success": true,
  "data": { ... },      // The actual payload (record, list, etc.)
  "error": null          // Error message if success is false
}
```

This consistency makes the frontend simpler — it always knows where to find the data and how to check for errors.

---

## 8. Frontend Development

### Component Architecture

React apps are built from **components** — reusable pieces of UI. Our key insight: since entities are dynamic, our components must be dynamic too.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ App                                                                     │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ BrowserRouter (handles URL → component mapping)                     │ │
│ │                                                                     │ │
│ │ /login → LoginPage                                                  │ │
│ │ /      → ProtectedRoute → AppShell                                 │ │
│ │          ┌──────────┬───────────────────────────────────────┐       │ │
│ │          │ Sidebar  │  TopBar (with Global Search)          │       │ │
│ │          │          ├───────────────────────────────────────┤       │ │
│ │          │ Dashboard│                                       │       │ │
│ │          │ Pipeline │  Route-dependent content:              │       │ │
│ │          │ ──────── │                                       │       │ │
│ │          │ Sales    │  /              → DashboardPage        │       │ │
│ │          │  Account │  /pipeline      → PipelineBoard       │       │ │
│ │          │  Contact │  /o/Account     → ListView             │       │ │
│ │          │  Opport..│  /o/Account/new → RecordForm           │       │ │
│ │          │ ──────── │  /o/Account/123 → DetailView           │       │ │
│ │          │ Custom   │  /o/Account/123/edit → RecordForm      │       │ │
│ │          │  Case    │  /setup/entities → EntityBuilderPage   │       │ │
│ │          │ ──────── │                                       │       │ │
│ │          │ Setup    │                                       │       │ │
│ │          │  Entities│                                       │       │ │
│ │          └──────────┴───────────────────────────────────────┘       │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### The Dynamic Rendering Pattern

The most important pattern: **one component handles ALL entities**.

`ListView` doesn't know about "Account" or "Contact". It:
1. Reads the entity name from the URL (`/o/Account` → `entityApiName = "Account"`)
2. Fetches that entity's metadata (fields, labels)
3. Fetches records for that entity
4. Renders a table with columns based on the field definitions

```
URL: /o/Account
         │
         ▼
    ┌──────────┐
    │ ListView │
    └────┬─────┘
         │
    ┌────┴──────────────────────────────────────────┐
    │                                               │
    ▼                                               ▼
GET /api/metadata/entities/Account          GET /api/data/Account
    │                                               │
    ▼                                               ▼
Entity metadata:                            Records:
{                                           [
  label: "Account",                           { name: "Acme", data: {Industry: "Tech"} },
  fields: [                                   { name: "Globex", data: {Industry: "Mfg"} },
    { apiName: "Name", label: "Name" },     ]
    { apiName: "Industry", label: "Industry" },
    { apiName: "Phone", label: "Phone" },
  ]
}
    │                                               │
    └──────────────┬────────────────────────────────┘
                   │
                   ▼
    Dynamically rendered table:
    ┌─────────┬──────────┬─────────────┐
    │ Name    │ Industry │ Phone       │
    ├─────────┼──────────┼─────────────┤
    │ Acme    │ Tech     │ 555-1234    │
    │ Globex  │ Mfg      │ 555-5678    │
    └─────────┴──────────┴─────────────┘
```

### DynamicField — The Universal Input

`DynamicField.tsx` is a single component that renders the correct input type based on the field definition:

```
field.fieldType = "TEXT"        →  <input type="text" />
field.fieldType = "NUMBER"      →  <input type="number" />
field.fieldType = "DATE"        →  <input type="date" />
field.fieldType = "BOOLEAN"     →  <input type="checkbox" />
field.fieldType = "PICKLIST"    →  <select> with options from field.picklistValues
field.fieldType = "TEXTAREA"    →  <textarea />
field.fieldType = "LOOKUP"      →  <LookupField /> (search-as-you-type dropdown)
field.fieldType = "FORMULA"     →  Read-only display
```

### LookupField — Connecting Records

Lookup fields are how records reference each other (e.g., a Contact's AccountId points to an Account). The challenge: the database stores a UUID, but users want to see the Account **name**, not `a1b2c3d4-e5f6-...`.

```
LookupField component:

┌──────────────────────────────────────┐
│ Account                              │
│ ┌──────────────────────────────────┐ │
│ │ Acme Corp                    ×   │ │  ← Shows resolved name, not UUID
│ └──────────────────────────────────┘ │
│                                      │
│  When focused + typing:              │
│ ┌──────────────────────────────────┐ │
│ │ Search Accounts...               │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Acme Corp                        │ │  ← Search results from API
│ │ Globex Inc                       │ │
│ │ Initech                          │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘

Internally:
- Stores the UUID as the value (sent to backend)
- Resolves UUID → name for display (separate API call)
- Searches by name as user types (debounced API call)
```

### RelatedList — Showing Child Records

On a detail page, RelatedList shows records from other entities that point to this record:

```
Account Detail: "Acme Corp"
┌────────────────────────────────────┐
│ Details                            │
│ Name: Acme Corp                    │
│ Industry: Technology               │
│ Phone: 555-1234                    │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ Contacts (3)          + New Contact│  ← RelatedList component
├────────────────────────────────────┤
│ John Doe  │ john@acme.com          │
│ Jane Smith│ jane@acme.com          │
│ Bob Jones │ bob@acme.com           │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ Opportunities (2)  + New Opportun..│  ← Another RelatedList
├────────────────────────────────────┤
│ Enterprise Deal  │ $500K │ Qual.   │
│ SMB Deal         │ $50K  │ Prosp.  │
└────────────────────────────────────┘
```

How does it know which entities to show? It:
1. Fetches ALL entity definitions with their fields
2. Finds entities that have a LOOKUP or MASTER_DETAIL field pointing to the current entity
3. For each one, queries the related records API
4. Renders a mini-table for each

### State Management with TanStack Query

Instead of manually managing loading states, error states, and caching, we use TanStack Query:

```typescript
// Without TanStack Query (manual approach):
const [records, setRecords] = useState([]);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  setLoading(true);
  fetch('/api/data/Account')
    .then(r => r.json())
    .then(data => { setRecords(data); setLoading(false); })
    .catch(err => { setError(err); setLoading(false); });
}, []);

// With TanStack Query (declarative approach):
const { data: records, isLoading, error } = useQuery({
  queryKey: ['records', 'Account'],      // Cache key
  queryFn: () => recordApi.list('Account'),  // How to fetch
});
```

TanStack Query also:
- **Caches** results (navigating back doesn't re-fetch)
- **Deduplicates** requests (two components asking for the same data = one API call)
- **Refetches** stale data automatically
- **Invalidates** cache when you mutate data (create/update/delete)

---

## 9. Connecting Frontend to Backend

### The API Client

`client.ts` creates an Axios HTTP client with two key features:

**1. Automatic token injection:**
Every request automatically includes the JWT token from localStorage.

```
Request: GET /api/data/Account
         ↓ Interceptor adds header
Request: GET /api/data/Account
         Authorization: Bearer eyJhbG...
```

**2. Automatic token refresh:**
When a token expires (401 response), the client automatically tries to refresh it before failing.

```
Request → 401 Unauthorized → Refresh token → Retry original request
                                    ↓
                              If refresh also fails → Redirect to /login
```

### Development vs Production Networking

```
DEVELOPMENT (docker-compose.yml):
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Browser  │────▶│ Frontend │     │ Backend  │
│          │:5173│ Vite Dev │     │ Spring   │
│          │     │ Server   │     │ Boot     │
│          │────────────────────▶│          │
│          │:8081               │  :8080   │
└──────────┘                    └──────────┘
Browser talks directly to both containers on different ports.
VITE_API_URL=http://localhost:8081 tells frontend where backend is.

PRODUCTION (docker-compose.prod.yml):
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ Browser  │────▶│  nginx   │────▶│ Frontend │     │ Backend  │
│          │:80  │ reverse  │     │ static   │     │ Spring   │
│          │     │ proxy    │────▶│ files    │     │ Boot     │
│          │     │          │     └──────────┘     │          │
│          │     │  /api/*  │────────────────────▶│  :8080   │
│          │     │  /*      │──▶ frontend:80      └──────────┘
└──────────┘     └──────────┘
Browser only talks to nginx on port 80.
nginx routes /api/* to backend, everything else to frontend.
VITE_API_URL is empty (relative paths go through nginx).
```

---

## 10. Adding Sales Features

### Dashboard with Charts (Recharts)

The dashboard makes API calls to the reporting endpoints and renders charts:

```
DashboardPage
├── Record count cards (6 key entities)
│   └── Calls: GET /api/reports/record-counts
│
├── Pipeline bar chart
│   └── Calls: GET /api/reports/pipeline-summary
│   └── Renders: Horizontal bar chart with one bar per stage
│
├── Revenue line chart
│   └── Calls: GET /api/reports/revenue-by-month
│   └── Renders: Line chart with monthly Closed Won revenue
│
├── Top Accounts table
│   └── Calls: GET /api/reports/top-accounts
│   └── Renders: Table sorted by total deal value
│
└── Quick action buttons
    └── Links to create new Account, Contact, Opportunity, Quote
```

### Pipeline Kanban Board

The Kanban board uses the `@dnd-kit` library for drag-and-drop:

```
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│Prospect.│ │Qualific.│ │Needs An.│ │Proposal │ │Closed W.│
│ 3 deals │ │ 2 deals │ │ 1 deal  │ │ 2 deals │ │ 4 deals │
│ $150K   │ │ $200K   │ │ $75K    │ │ $300K   │ │ $1.2M   │
│┌───────┐│ │┌───────┐│ │┌───────┐│ │         │ │         │
││Acme   ││ ││BigCo  ││ ││MedCo  ││ │         │ │         │
││$50K   ││ ││$150K  ││ ││$75K   ││ │         │ │         │
│└───────┘│ │└───────┘│ │└───────┘│ │         │ │         │
│┌───────┐│ │┌───────┐│ │         │ │         │ │         │
││Widget ││ ││SmallCo││ │         │ │         │ │         │
││$40K   ││ ││$50K   ││ │         │ │         │ │         │
│└───────┘│ │└───────┘│ │         │ │         │ │         │
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
     ◄─── Drag a card from one column to another ───►
     This updates the Opportunity's Stage field via API
```

When you drop a card on a different column:
1. `@dnd-kit` fires a `DragEnd` event
2. We extract the new stage name from the drop target
3. We call `PUT /api/data/Opportunity/{id}` with `{ Stage: "new stage" }`
4. TanStack Query invalidates the cache and the board re-renders

### Global Search

The TopBar search field searches across ALL entities:

```
User types: "acm"
                │
                ▼
   Debounced API call after 2+ characters:
   GET /api/search?q=acm
                │
                ▼
   Backend searches records table:
   WHERE name ILIKE '%acm%' OR data::text ILIKE '%acm%'
                │
                ▼
   Results dropdown:
   ┌──────────────────────┐
   │ Acme Corp            │
   │ Account              │
   ├──────────────────────┤
   │ Acme Deal 2026       │
   │ Opportunity           │
   ├──────────────────────┤
   │ Acme Contact         │
   │ Contact              │
   └──────────────────────┘
   Click → navigates to that record's detail page
```

---

## 11. Production Deployment

### The Production Stack

```
Internet → VPS (Ubuntu 22.04) → Docker
                │
                ▼
    ┌───────────────────────────────────────────────────────┐
    │                        nginx                          │
    │                     (port 80/443)                     │
    │                                                       │
    │   Request URL starts with /api/ ?                     │
    │         │                                             │
    │    YES  │  NO                                         │
    │    │    │                                             │
    │    ▼    ▼                                             │
    │ ┌──────────┐  ┌──────────────┐                       │
    │ │ backend  │  │   frontend   │                       │
    │ │ (Spring  │  │ (nginx with  │                       │
    │ │  Boot)   │  │  static HTML/│                       │
    │ │          │  │  JS/CSS)     │                       │
    │ └────┬─────┘  └──────────────┘                       │
    │      │                                               │
    │      ▼                                               │
    │ ┌──────────┐                                         │
    │ │   db     │                                         │
    │ │(Postgres)│                                         │
    │ └──────────┘                                         │
    │                                                       │
    │ + certbot (auto-renews TLS certificates)             │
    └───────────────────────────────────────────────────────┘
```

### The Deployment Script (deploy.sh)

The script automates everything needed on a fresh Ubuntu VPS:

```
deploy.sh flow:
│
├── [1/5] Install Docker
│   └── Adds Docker's APT repository and installs docker-ce
│
├── [2/5] Configure firewall
│   └── Opens ports 22 (SSH), 80 (HTTP), 443 (HTTPS)
│   └── Blocks everything else
│
├── [3/5] Generate secrets
│   └── Creates .env file with random DB_PASSWORD and JWT_SECRET
│   └── Uses `openssl rand` for cryptographic randomness
│   └── Only runs if .env doesn't already exist (idempotent)
│
├── [4/5] Build and start containers
│   └── docker compose -f docker-compose.prod.yml build
│   └── docker compose -f docker-compose.prod.yml up -d
│   └── Waits 15 seconds for services to start
│   └── Health check: curl localhost
│
└── [5/5] Print summary
    └── URL: http://<VPS-IP>
    └── Login: admin / admin123
```

### Environment Variables and Secrets

We **never** hardcode passwords or secret keys in code. Instead:

```
.env file (generated by deploy.sh, never committed to Git):
DB_PASSWORD=xK9mP2nQ7rT4vW8y...    ← Random 32-char password
JWT_SECRET=aB3cD5eF7gH9iJ1kL...    ← Random 64-char secret for signing JWTs

docker-compose.prod.yml references them:
  environment:
    SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}    ← Docker substitutes the value
    JWT_SECRET: ${JWT_SECRET}
```

### nginx Reverse Proxy

nginx serves two purposes in production:

1. **Routing**: Sends `/api/*` requests to the backend, everything else to the frontend
2. **TLS Termination**: Handles HTTPS certificates (via certbot/Let's Encrypt)

```nginx
# nginx/prod.conf (simplified)
server {
    listen 80;

    # API requests → backend Java application
    location /api/ {
        proxy_pass http://backend:8080;
    }

    # Everything else → frontend static files
    location / {
        proxy_pass http://frontend:80;
    }
}
```

---

## 12. Bugs We Hit and How We Fixed Them

Real-world development isn't smooth. Here are the bugs we encountered and how we solved them:

### Bug 1: Port 8080 Already in Use

**Symptom**: Docker couldn't start the backend container.
**Cause**: Another process on the dev machine was using port 8080.
**Fix**: Changed the port mapping in `docker-compose.yml` from `8080:8080` to `8081:8080`. The backend still listens on 8080 *inside* the container, but it's exposed as 8081 on the host.

**Lesson**: Container ports and host ports are independent. You can map any host port to any container port.

### Bug 2: BCrypt Hash Couldn't Be Generated Without Tools

**Symptom**: Needed to seed an admin user with a hashed password, but didn't have BCrypt tooling installed locally.
**Cause**: The migration SQL needed a pre-computed BCrypt hash.
**Fix**: Removed the SQL seed and created a `DataInitializer.java` — a Spring Boot `CommandLineRunner` that runs on startup, checks if the admin user exists, and creates it using the app's own `PasswordEncoder` bean.

**Lesson**: Let the application manage its own concerns. If the app has a password encoder, use it — don't try to replicate its behavior externally.

### Bug 3: LazyInitializationException

**Symptom**: `GET /api/metadata/entities/Account` returned a 500 error with "could not initialize proxy - no Session".
**Cause**: JPA loads related collections (like `fields` on an `EntityDef`) lazily by default. By the time the JSON serializer tried to access `fields`, the database session was already closed.
**Fix**: Added a `findByApiNameWithFields` method that uses `LEFT JOIN FETCH` to eagerly load fields in a single query.

**Lesson**: JPA lazy loading is a common source of bugs. When you know you'll need a related collection, use `JOIN FETCH`.

### Bug 4: CORS 403 on Preflight Requests

**Symptom**: Browser showed "CORS error" on API calls. The browser's preflight OPTIONS request was getting a 403.
**Cause**: Spring Security was blocking OPTIONS requests because they don't carry an Authorization header.
**Fix**: Added `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` to the security config, and configured CORS properly with `allowedOriginPatterns("*")`.

**Lesson**: Browsers send an OPTIONS preflight request before cross-origin API calls. Your security layer must allow these unauthenticated.

```
Browser                        Backend
   │                              │
   │ OPTIONS /api/data/Account    │  ← "Can I make this request?"
   │──────────────────────────────▶│
   │ 200 OK + CORS headers        │  ← "Yes, go ahead"
   │◀──────────────────────────────│
   │                              │
   │ GET /api/data/Account         │  ← Actual request
   │ Authorization: Bearer ...     │
   │──────────────────────────────▶│
   │ 200 OK + data                 │
   │◀──────────────────────────────│
```

### Bug 5: TypeScript Build Error in Production

**Symptom**: Production Docker build failed with `Property 'env' does not exist on type 'ImportMeta'`.
**Cause**: The code used `import.meta.env.VITE_API_URL` (a Vite feature), but TypeScript didn't know about Vite's type extensions.
**Fix**: Created `vite-env.d.ts` with `/// <reference types="vite/client" />` which tells TypeScript about Vite's special types.

**Lesson**: TypeScript needs type declarations for non-standard features. A single line in a `.d.ts` file fixed a broken production build.

### Bug 6: VPS Database Container "Unhealthy"

**Symptom**: `docker compose up` showed "dependency failed to start: container opencrm-db-1 is unhealthy".
**Cause**: The `.env` file had an empty `DB_PASSWORD`, or a previous volume existed with a different password.
**Fix**: `docker compose down -v` (remove old volume) then `docker compose up -d` (start fresh with correct password from `.env`).

**Lesson**: PostgreSQL sets the password on first initialization. If you change the password later, the existing data volume still has the old password. You must either remove the volume or change the password inside the database.

---

## 13. Key Takeaways

### Architecture Principles

1. **Metadata-driven design** can eliminate enormous amounts of code. Instead of writing CRUD for each entity, we wrote it once and let metadata drive behavior.

2. **JSONB is a superpower**. PostgreSQL's JSONB gives you the flexibility of NoSQL (schema-less documents) with the reliability of a relational database (ACID transactions, joins, indexes).

3. **Convention over configuration**. Spring Boot auto-configures most things. We only configure what's different from defaults.

4. **Separation of concerns**. Each layer has one job: Controllers handle HTTP, Services handle logic, Repositories handle data access.

### Development Process

```
The order we built things (and why):

Phase 1: Docker + Database + Auth
   │      "Can the containers talk to each other?
   │       Can a user log in?"
   │
Phase 2: Metadata engine
   │      "Can we define what entities and fields exist?"
   │
Phase 3: Record engine
   │      "Can we create, read, update, delete records
   │       for ANY entity, driven by metadata?"
   │
Phase 4: Frontend CRUD
   │      "Can a user see lists, create records, edit them?"
   │
Phase 5: Sales entity seed data
   │      "Pre-populate Account, Contact, Opportunity, etc."
   │
Phase 6: Lookup fields + Related lists
   │      "Can records reference each other?
   │       Can I see Contacts on an Account?"
   │
Phase 7: Sales features
   │      "Quote PDF, Order conversion, Pipeline board"
   │
Phase 8: Dashboard + Search
   │      "Charts, analytics, global search"
   │
Phase 9: Production + Deployment
         "nginx, Docker prod config, deploy script, VPS"
```

Each phase builds on the previous one. We always verified each phase worked before moving to the next. This prevents "building a house of cards" where a foundation bug breaks everything above it.

### The 80/20 Rule in Practice

We focused on the 20% of features that deliver 80% of the value:
- **Built**: CRUD, lookups, related lists, pipeline, PDF, search, dashboard
- **Didn't build** (yet): Workflow automation, email integration, field-level security, audit trail, approval processes, complex formula engine

A working app with core features beats a half-built app with everything.

### Security Basics We Applied

1. **Never store plain passwords** → BCrypt hashing
2. **Never hardcode secrets** → Environment variables from `.env`
3. **Validate at system boundaries** → Backend validates all input
4. **Stateless auth** → JWT tokens (no server-side sessions to hijack)
5. **HTTPS ready** → nginx + certbot for TLS certificates
6. **Firewall** → Only ports 22, 80, 443 open on VPS

---

## Appendix: Full Data Flow Example

Let's trace what happens when a user creates a new Contact:

```
1. User clicks "New Contact" in the sidebar
   → React Router navigates to /o/Contact/new

2. RecordForm component mounts
   → useQuery fetches GET /api/metadata/entities/Contact
   → Response: { fields: [Name (TEXT), Email (EMAIL), AccountId (LOOKUP), ...] }

3. RecordForm renders DynamicField for each field
   → Name: <input type="text" />
   → Email: <input type="email" />
   → AccountId: <LookupField /> (shows search-as-you-type for Accounts)

4. User fills in: Name="Jane", Email="jane@acme.com", selects Account="Acme Corp"
   → LookupField stores the UUID of "Acme Corp", displays the name

5. User clicks "Save"
   → RecordForm calls POST /api/data/Contact
   → Body: { "Name": "Jane", "Email": "jane@acme.com", "AccountId": "abc-123-uuid" }

6. Request hits backend:
   JwtAuthFilter → validates token → extracts user ID
   RecordController.create() → extracts body
   RecordService.create("Contact", data, userId):
     a. Looks up EntityDef for "Contact" → entity ID = "def-456"
     b. Looks up FieldDefs → Name (TEXT, required), Email (EMAIL), AccountId (LOOKUP)
     c. Validates:
        - Name is present and is text ✓
        - Email is a string ✓
        - AccountId is a string ✓
        - No unknown fields ✓
     d. Extracts name field: "Name" → "Jane"
     e. Serializes data to JSON: '{"Name":"Jane","Email":"jane@acme.com","AccountId":"abc-123"}'
     f. Creates Record entity: entityDefId="def-456", name="Jane", data=<json>, ownerId=<user>
     g. recordRepository.save(record) → INSERT into records table
     h. Returns RecordDTO

7. Frontend receives response:
   → TanStack Query invalidates ['records', 'Contact'] cache
   → React Router navigates to /o/Contact/<new-id>
   → DetailView loads and shows the record

8. On the DetailView, RelatedList checks for child entities
   → No entities have a LOOKUP pointing to Contact → no related lists shown

9. On Acme Corp's DetailView, RelatedList finds Contact entity
   → Contact has "AccountId" LOOKUP pointing to Account
   → Calls GET /api/data/Account/abc-123/related/Contact
   → Shows "Jane" in the Contacts related list
```

---

*This document reflects the actual development process of OpenCRM, including the real bugs encountered and the order in which features were built.*
