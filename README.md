# Personal Finance & Portfolio Platform

A microservice platform for tracking personal finances and an investment portfolio.
Built with **Spring Boot 3 / Java 17**, JWT authentication, asynchronous event
exchange over **Kafka**, and a dedicated PostgreSQL database per service.

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Environment Configuration](#environment-configuration)
- [API](#api)
- [Authentication](#authentication)
- [Events (Kafka)](#events-kafka)
- [Tests](#tests)
- [Repository Layout](#repository-layout)

---

## Architecture

```
                         ┌──────────────┐
              register / │ auth-service │  issues JWT
              login      │   :8081      │
                         └──────┬───────┘
                                │ JWT (Bearer)
        ┌───────────────────────┼────────────────────────┐
        │                       │                         │
┌───────▼────────┐    ┌─────────▼────────┐      ┌─────────▼────────┐
│ finance-service │    │ portfolio-service│      │  report-service  │
│     :8082       │    │      :8084       │      │      :8083       │
│ income/expenses │    │ positions, prices│      │ balance aggregate│
└───────┬─────────┘    │ Yahoo Finance    │      └─────────▲────────┘
        │              └──────────────────┘                │
        │  transaction-events (Kafka)                      │
        └─────────────────────────────────────────────────┘
```

- Each service is self-contained and owns its own PostgreSQL database.
- `auth-service` is the only issuer of JWTs; the other services merely validate
  the token using a shared secret (`JWT_SECRET`).
- `finance-service` publishes transaction events to the Kafka topic
  `transaction-events`; `report-service` consumes them and maintains an
  aggregated per-user balance (idempotently).

---

## Services

| Service             | Port | Database (port)      | Purpose |
|---------------------|------|----------------------|---------|
| `auth-service`      | 8081 | `auth_db` (5433)     | Registration, login, JWT issuance, USER/ADMIN roles |
| `finance-service`   | 8082 | `finance_db` (5434)  | Income & expenses, balance, search, publishes Kafka events |
| `report-service`    | 8083 | `report_db` (5435)   | Consumes Kafka events, aggregated balance |
| `portfolio-service` | 8084 | `portfolio_db` (5436)| Investment positions, Yahoo Finance quotes, portfolio snapshots |

---

## Tech Stack

- **Java 17**, **Spring Boot 3.5**, Gradle (wrapper in each service)
- **Spring Security** + **JWT** (jjwt 0.11.5), roles and `@PreAuthorize`
- **Spring Data JPA** + **PostgreSQL 15**
- **Liquibase** — schema migrations (auth, portfolio)
- **Apache Kafka** (Confluent 7.6) — event-driven communication
- **Caffeine Cache** — Yahoo Finance quote cache (5 min TTL)
- **Testcontainers**, JUnit 5 — integration tests
- **Docker Compose** — databases, Kafka, Zookeeper

---

## Quick Start

### 1. Prerequisites

- Docker and Docker Compose
- JDK 17 (to run the services locally)

### 2. Configure environment variables

```bash
cp .env.example .env
# edit .env: set the database passwords and JWT_SECRET
# you can generate a secret with: openssl rand -base64 32
```

### 3. Start the infrastructure (databases + Kafka)

```bash
docker compose up -d
```

This starts: `postgres-auth`, `postgres-finance`, `postgres-report`,
`postgres-portfolio`, `zookeeper`, and `kafka`.

### 4. Run the services

Each service is a standalone Gradle project. From the service directory:

```bash
# auth-service
cd auth-service/auth-service && ./gradlew bootRun

# finance-service
cd finance-service/finance-service && ./gradlew bootRun

# report-service
cd report-service/report-service-app && ./gradlew bootRun

# portfolio-service
cd portfolio-service/portfolio-service && ./gradlew bootRun
```

> On Windows use `gradlew.bat` instead of `./gradlew`.

---

## Environment Configuration

The `.env` file (see `.env.example`):

| Variable | Description |
|----------|-------------|
| `AUTH_DB_NAME` / `AUTH_DB_USER` / `AUTH_DB_PASSWORD` | auth-service database |
| `FINANCE_DB_NAME` / `FINANCE_DB_USER` / `FINANCE_DB_PASSWORD` | finance-service database |
| `REPORT_DB_NAME` / `REPORT_DB_USER` / `REPORT_DB_PASSWORD` | report-service database |
| `PORTFOLIO_DB_NAME` / `PORTFOLIO_DB_USER` / `PORTFOLIO_DB_PASSWORD` | portfolio-service database |
| `JWT_SECRET` | Shared secret for signing/verifying JWTs (base64) |
| `JWT_EXPIRATION` | Token lifetime in ms (default `3600000` — 1 hour) |

All services read their database, port, and JWT settings from environment
variables (defaults are defined in `application.yml`).

---

## API

> All requests except `/auth/**` require the
> `Authorization: Bearer <token>` header.

### auth-service (`:8081`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register (`email`, `password`) |
| `POST` | `/auth/login` | Log in, returns a JWT |
| `GET`  | `/auth/me` | UUID of the current user |

### finance-service (`:8082`)

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/transactions` | Create a transaction (income/expense) |
| `PUT`    | `/transactions/{id}` | Update a transaction |
| `DELETE` | `/transactions/{id}` | Delete a transaction |
| `GET`    | `/transactions/search` | Search with filters (`from`, `to`, `type`, `category`) and pagination |
| `GET`    | `/transactions/summary` | Transaction summary |
| `GET`    | `/transactions/getBalance` | Current balance |

### portfolio-service (`:8084`)

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/positions` | Add a position |
| `GET`    | `/positions` | List the user's positions |
| `DELETE` | `/positions/{id}` | Delete a position |
| `GET`    | `/portfolio/summary` | Portfolio summary with live prices |
| `GET`    | `/portfolio/history?period=all` | Snapshot history |
| `POST`   | `/portfolio/snapshot` | Take a portfolio snapshot |
| `GET`    | `/portfolio/admin/snapshots/{userId}` | A user's snapshots (`ADMIN` only) |

Quotes are fetched from Yahoo Finance and cached (Caffeine, 5 min).
Snapshots are also created automatically on a schedule — weekdays at 18:00
(`SnapshotScheduler`).

---

## Authentication

1. Register: `POST /auth/register`.
2. Log in: `POST /auth/login` → receive a JWT.
3. Pass the token to the other services:
   `Authorization: Bearer <token>`.

The JWT carries a role claim (`USER` / `ADMIN`); admin endpoints are protected
with `@PreAuthorize("hasRole('ADMIN')")`. All services share the same
`JWT_SECRET`, so a token issued by auth-service is accepted everywhere.

---

## Events (Kafka)

- Topic: **`transaction-events`**
- Producer: `finance-service` (transaction created/deleted)
- Consumer: `report-service`, group `report-service`

`report-service` processes events idempotently: each event carries an
`eventId`, and processed ids are stored in a processed-events table, which
prevents double counting.

---

## Tests

In each service:

```bash
./gradlew test
```

Integration tests use **Testcontainers** (PostgreSQL), so no pre-running
database is required.

---

## Repository Layout

```
.
├── docker-compose.yml          # PostgreSQL × 4, Kafka, Zookeeper
├── .env.example                # environment variable template
├── auth-service/               # authentication service
├── finance-service/            # income/expense service
├── report-service/             # aggregation service (Kafka consumer)
└── portfolio-service/          # investment portfolio service
```
