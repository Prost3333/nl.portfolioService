# Portfolio Tracking Platform

![CI](https://github.com/Prost3333/nl.portfolioService/actions/workflows/ci.yml/badge.svg)

A microservice platform for tracking an investment portfolio: positions, live
prices, value snapshots over time, aggregated cross-user statistics, and an
LLM-built behavioural profile of the investor from their own trade journal.
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
- [CI](#ci)
- [Repository Layout](#repository-layout)
- [Deployment](#deployment)

---

## Architecture

```
                              ┌──────────────┐
                   register / │ auth-service │  issues JWT
                   login      │   :8081      │
                              └──────┬───────┘
                                     │ JWT (Bearer)
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
┌────────▼─────────┐       ┌─────────▼────────┐        ┌─────────▼────────┐
│ portfolio-service│       │  report-service  │        │    ai-service    │
│      :8084       │       │      :8083       │        │      :8085       │
│ positions, live  │       │ per-user stats   │        │ trade journal,   │
│ prices, snapshots│       │ aggregation      │        │ behaviour profile│
│ trade journal    │       │                  │        │ via Claude API   │
└────────┬─────────┘       └─────────▲────────┘        └─────────▲────────┘
         │                           │                           │
         │  snapshot-events (Kafka)  │                           │
         ├───────────────────────────┘                           │
         │                                                       │
         │  trade-events (Kafka)                                 │
         └───────────────────────────────────────────────────────┘
```

- Each service is self-contained and owns its own PostgreSQL database.
- `auth-service` is the only issuer of JWTs; the other services merely validate
  the token using a shared secret (`JWT_SECRET`).
- `portfolio-service` publishes a `SnapshotCreatedEvent` to the Kafka topic
  `snapshot-events` whenever a portfolio snapshot is taken; `report-service`
  consumes them and maintains aggregated per-user statistics (idempotently).
- `portfolio-service` also publishes a `TradeCreatedEvent` to `trade-events` on
  every trade; `ai-service` consumes those into a local read model and never
  touches the portfolio database directly.

> Note: an earlier `finance-service` (income/expense tracking) was removed — the
> project is now focused solely on portfolio tracking.

---

## Services

| Service             | Port | Database (port)      | Purpose |
|---------------------|------|----------------------|---------|
| `auth-service`      | 8081 | `auth_db` (5433)     | Registration, login, JWT issuance, USER/ADMIN roles |
| `portfolio-service` | 8084 | `portfolio_db` (5436)| Investment positions, Yahoo Finance quotes, snapshots, performance; publishes Kafka events |
| `report-service`    | 8083 | `report_db` (5435)   | Consumes snapshot events, aggregated per-user statistics |
| `ai-service`        | 8085 | `ai_db` (5437)       | Consumes trade events, builds an LLM behavioural profile of the investor |

---

## Tech Stack

- **Java 17**, **Spring Boot 3.5**, Gradle (wrapper in each service)
- **Spring Security** + **JWT** (jjwt), roles and `@PreAuthorize`
- **Spring Data JPA** + **PostgreSQL 15**
- **Liquibase** — schema migrations (auth, portfolio, ai); `report-service` uses
  JPA `ddl-auto: update`
- **Apache Kafka** (Confluent 7.6) — event-driven communication
- **Anthropic Java SDK** (`com.anthropic:anthropic-java`) — Claude API with
  structured outputs, used by `ai-service`
- **Caffeine Cache** — Yahoo Finance quote cache (5 min TTL)
- **Testcontainers**, JUnit 5 — integration tests
- **Docker / Docker Compose** — full stack: all four services (built from
  per-service `Dockerfile`s), the four PostgreSQL databases, Kafka, Zookeeper

---

## Quick Start

### 1. Prerequisites

- Docker and Docker Compose (enough to run the whole stack)
- JDK 17 — only needed if you want to run a service outside of Docker

### 2. Configure environment variables

```bash
cp .env.example .env
# edit .env: set the database passwords and JWT_SECRET
# you can generate a secret with: openssl rand -base64 32
```

### 3. Start the full stack

```bash
docker compose up --build
```

This builds and starts everything: the four services (`auth-service`,
`portfolio-service`, `report-service`, `ai-service`), their databases
(`postgres-auth`, `postgres-report`, `postgres-portfolio`, `postgres-ai`), plus
`zookeeper` and `kafka`. Add `-d` to run detached.

Once up, the services are reachable on `localhost:8081` (auth), `localhost:8084`
(portfolio), `localhost:8083` (report), and `localhost:8085` (ai).

### Running a service outside Docker (optional)

Each service is also a standalone Gradle project. To run one locally against the
Dockerized infrastructure, start the databases/Kafka only and use `bootRun`:

```bash
docker compose up -d postgres-auth postgres-portfolio postgres-report postgres-ai kafka zookeeper
cd portfolio-service && ./gradlew bootRun
```

> On Windows use `gradlew.bat` instead of `./gradlew`.

---

## Environment Configuration

The `.env` file (see `.env.example`):

| Variable | Description |
|----------|-------------|
| `AUTH_DB_NAME` / `AUTH_DB_USER` / `AUTH_DB_PASSWORD` | auth-service database |
| `PORTFOLIO_DB_NAME` / `PORTFOLIO_DB_USER` / `PORTFOLIO_DB_PASSWORD` | portfolio-service database |
| `REPORT_DB_NAME` / `REPORT_DB_USER` / `REPORT_DB_PASSWORD` | report-service database |
| `AI_DB_NAME` / `AI_DB_USER` / `AI_DB_PASSWORD` | ai-service database |
| `ANTHROPIC_API_KEY` | Claude API key. Without it `ai-service` starts but `analyze` returns `503` |
| `ANTHROPIC_MODEL` | Model id (default `claude-opus-5`) |
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

### portfolio-service (`:8084`)

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/trade/trades` | Record a trade (optionally with journal fields: `rationale`, `conviction` 1–5, `emotion`) |
| `GET`    | `/trade/trades` | The user's trade journal, newest first |
| `POST`   | `/positions` | Add a position |
| `GET`    | `/positions` | List the user's positions |
| `DELETE` | `/positions/{id}` | Delete a position |
| `GET`    | `/portfolio/summary` | Portfolio summary with live prices |
| `GET`    | `/portfolio/history?period=all` | Snapshot history |
| `POST`   | `/portfolio/snapshot` | Take a portfolio snapshot (publishes a Kafka event) |
| `GET`    | `/portfolio/performance?period=month` | Per-period performance items |
| `GET`    | `/portfolio/getPercent?period=all` | Percent change over the period |
| `GET`    | `/portfolio/admin/snapshots/{userId}` | A user's snapshots (`ADMIN` only) |

Quotes are fetched from Yahoo Finance and cached (Caffeine, 5 min).
Positions with the same ticker are merged rather than duplicated, and names are
resolved automatically from Yahoo. Snapshots are also created automatically on a
schedule — weekdays at 18:00 (`SnapshotScheduler`).

### report-service (`:8083`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/report/stats` | Aggregated statistics across all users (`ADMIN` only) |

`/report/stats` returns `totalUsers`, `totalValueAllUsers`, and a per-user list
(last value, last snapshot date, snapshot count) — built up from the
`snapshot-events` stream.

### ai-service (`:8085`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/ai/behavior/analyze` | Queue a behavioural profile build; returns `202` with a `PENDING` record |
| `GET`  | `/ai/behavior/profile` | Latest profile: `PENDING`, `READY` or `FAILED` (`204` if none yet) |

**How the profile is built.** Everything numeric is computed in Java, not by the
model — for every trade `ai-service` pulls five years of daily closes from Yahoo
and derives:

| Metric | Meaning |
|--------|---------|
| `pricePercentile90d` | Where the trade price sat in the 90-day range, 0–100. A buy near 100 is chasing a rally. |
| `priorReturn30d` | Price move in the 30 days *before* the trade. A sell after a deep drop is a drawdown exit. |
| `forwardReturn30d` | Price move in the 30 days *after* the trade. |
| `timingScore` | Whether the decision worked out: equals `forwardReturn30d` for buys, negated for sells. |

Those numbers, plus the user's own `rationale` / `conviction` / `emotion`, go to
Claude, which only interprets them — looking for the gap between what the
investor said and what they actually did. The response comes back through
structured outputs as summary, patterns, strengths and recommendations.

Generation runs asynchronously (`@Async`) because a call takes tens of seconds;
the frontend polls `/ai/behavior/profile` until the status is final. Without
`ANTHROPIC_API_KEY` the service still starts, and `analyze` returns `503`.

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

- Topic: **`snapshot-events`**
- Producer: `portfolio-service` (`SnapshotEventProducer`) — published whenever a
  snapshot is taken (manually via `POST /portfolio/snapshot` or by the scheduler)
- Consumer: `report-service`, group `report-service` (`SnapshotEventListener`)

Event payload (`SnapshotCreatedEvent`): `eventId`, `userId`, `snapshotDate`,
`totalValue`. Messages are keyed by `userId`.

`report-service` processes events idempotently: each event carries an `eventId`,
and processed ids are stored in a processed-events table, which prevents double
counting if an event is redelivered.

- Topic: **`trade-events`**
- Producer: `portfolio-service` (`TradeEventProducer`) — published on every trade
- Consumer: `ai-service`, group `ai-service` (`TradeEventListener`)

Event payload (`TradeCreatedEvent`): `tradeId`, `userId`, `ticker`, `type`,
`quantity`, `price`, `tradeDate`, plus the journal fields `rationale`,
`conviction` and `emotion`. `ai-service` stores these in `trade_records` keyed by
`tradeId`, so a redelivered event simply overwrites the row with identical data.

---

## Tests

In each service:

```bash
./gradlew test
```

Integration tests use **Testcontainers** (PostgreSQL), so no pre-running
database is required.

---

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on every push and pull request
to `main` on JDK 17, in two jobs:

- **`build-portfolio`** — builds **and tests** `portfolio-service`
  (`./gradlew build`, Testcontainers integration tests included).
- **`build-others`** — a matrix over `auth-service`, `report-service` and
  `ai-service` that builds them without tests (`./gradlew build -x test`).

---

## Repository Layout

```
.
├── docker-compose.yml          # full stack: 4 services + PostgreSQL × 4 + Kafka + Zookeeper
├── .env.example                # environment variable template
├── .github/workflows/ci.yml    # CI (test portfolio, build auth/report/ai)
├── docker-compose.prod.yml     # production stack for a server (restart policies, no exposed infra ports)
├── DEPLOY.md                   # free hosting guide (Oracle Cloud Always Free)
├── auth-service/               # authentication service (+ Dockerfile)
├── portfolio-service/          # investment portfolio service, Kafka producer (+ Dockerfile)
├── report-service/             # aggregation service, Kafka consumer (+ Dockerfile)
└── ai-service/                 # behavioural profile via Claude, Kafka consumer (+ Dockerfile)
```

---

## Deployment

The stack can be hosted for free on an Oracle Cloud Always Free ARM VM using
`docker-compose.prod.yml` (restart policies, capped JVM heaps, infra ports not
exposed publicly). See [DEPLOY.md](DEPLOY.md) for the step-by-step guide.
