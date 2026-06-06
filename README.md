<div align="center">

<img width="900" height="300" alt="banner" src="https://github.com/user-attachments/assets/43aca63a-1240-438f-8c08-ed618d07beb7" />

<br/>

[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-7.6.1-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)

<br/>

> **Hospital Pharmacy & Drug Inventory Management System** — A production-ready Spring Boot application
> with real-time Kafka event streaming, Claude AI demand forecasting, JWT security, and a full observability stack.

<br/>

[Features](#-what-it-does) · [Architecture](#-system-architecture) · [Tech Stack](#%EF%B8%8F-tech-stack) · [Quick Start](#-quick-start) · [API Docs](#-api-reference) · [Monitoring](#-observability) · [Security](#-security-model)

</div>

---

## What It Does

MediChain solves a specific, expensive problem — hospital pharmacies losing drugs to expiry, running out of critical stock mid-shift, and spending hours on manual reorder paperwork. It replaces all of that with a system that watches your inventory in real time, fires Kafka events the moment stock dips below threshold, and uses Claude AI to forecast demand before the shortage even happens.

It's not a toy CRUD app. It ships with a full production stack: Nginx reverse proxy, PostgreSQL with Flyway migrations, Redis caching, Kafka event streaming, Prometheus metrics, Grafana dashboards, iText PDF report generation, and Spring Batch for overnight inventory reconciliation jobs.

---

## System Architecture

<img width="900" height="500" alt="architecture" src="https://github.com/user-attachments/assets/d07eaead-89b5-4086-a017-8748816673a8" />

The system runs as 9 Docker services, all orchestrated by a single `docker-compose.yml`. Here's what each layer does:

```
Browser / Client
     │
     ▼
[ Nginx 1.26 ]          — SSL termination, rate limiting, reverse proxy
     │
     ▼
[ Spring Boot App ]     — REST API, JWT auth, business logic, batch jobs
     ├─────────────────► [ PostgreSQL 16 ]   — source of truth, Flyway migrations
     ├─────────────────► [ Redis 7 ]          — L2 cache (Ehcache passthrough), sessions
     ├─────────────────► [ Apache Kafka ]     — inventory events, reorder triggers
     ├─────────────────► [ Claude AI API ]    — demand forecasting via WebClient
     └─────────────────► [ Spring Mail ]      — expiry & low-stock alert emails
          │
          ▼
[ Prometheus ]  ◄──  /actuator/prometheus
     │
     ▼
[ Grafana ]     — dashboards, alerting rules
```

**Key design decisions worth calling out:**

- The app runs on **Java 21 with ZGC** (`-XX:+UseZGC`). ZGC is a sub-millisecond garbage collector — pharmacy staff shouldn't wait for GC pauses when a dispensing request hits.
- **Flyway** manages every schema change. No hand-edited migrations, no "works on my machine" schema drift.
- **MapStruct** generates all DTO ↔ Entity mappings at compile time — zero reflection, full type safety.
- **Spring Batch** handles overnight inventory reconciliation and expiry scans without blocking the API.
- **Ehcache + Redis** forms a two-tier cache: Ehcache is in-process (nanosecond reads), Redis is the distributed fallback when Ehcache misses.

---

## Request Lifecycle

<img width="900" height="380" alt="request_lifecycle" src="https://github.com/user-attachments/assets/8d776138-d9f0-496a-a635-323bce6bea6d" />

Every HTTP request goes through this exact pipeline:

```
1. Nginx          — rate limit check, SSL termination, upstream routing
2. JWT Filter     — token extraction, signature verification (HS256), claims parsing
3. Auth Context   — SecurityContextHolder population, role extraction
4. Controller     — @RestController, @Valid input validation (Bean Validation 3.0)
5. Service Layer  — business logic, Kafka publishing for side effects
6. Repository     — Spring Data JPA, Ehcache L2 intercept, Redis fallback
7. PostgreSQL     — actual database read/write when cache misses
```

The return path is the same pipeline in reverse. Errors at any layer surface as structured `ProblemDetail` responses (RFC 7807) — not naked stack traces.

---

## Dashboard Preview

<img width="900" height="520" alt="dashboard" src="https://github.com/user-attachments/assets/73d272e8-d8bb-491e-bd7c-c2fcbc677ea3" />

The Thymeleaf frontend gives pharmacists a single-screen view of what matters:

- **Live stock levels** with color-coded status (green → yellow → red as stock approaches minimum)
- **Expiry tracker** — drugs within 30 days highlighted automatically
- **Dispensing log** — every unit dispensed tied to prescribing doctor and patient
- **AI Forecast panel** — Claude's demand prediction for the next 7 days shown inline
- **Batch job status** — last run time, records processed, any failures
- **PDF export** — one click generates an iText7 PDF report of current inventory

---

## ⚙️ Tech Stack

<img width="600" height="600" alt="tech_stack" src="https://github.com/user-attachments/assets/47d6fff1-e201-4006-bfcb-82e96615cdaf" />

### Core

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 (LTS) | Virtual threads, pattern matching, ZGC |
| Framework | Spring Boot 3.2.4 | Production autoconfiguration, actuator |
| Build | Maven 3.x | Dependency management, JaCoCo coverage |
| ORM | Spring Data JPA + Hibernate | Repository pattern, L2 cache integration |
| Migrations | Flyway | Versioned, repeatable schema management |
| Mapping | MapStruct 1.5.5 | Compile-time, reflection-free DTO mapping |

### Infrastructure

| Service | Image | Role |
|---|---|---|
| PostgreSQL | `postgres:16-alpine` | Primary relational database |
| Redis | `redis:7-alpine` | Cache + JWT blacklist store |
| Apache Kafka | `cp-kafka:7.6.1` | Async inventory event bus |
| ZooKeeper | `cp-zookeeper:7.6.1` | Kafka coordination |
| Nginx | `nginx:1.26-alpine` | Reverse proxy, SSL, rate limiting |
| Prometheus | `prom/prometheus:latest` | Metrics scraping + alerting |
| Grafana | `grafana/grafana:latest` | Metrics visualization |
| pgAdmin | `dpage/pgadmin4:latest` | DB administration (dev only) |

### Libraries

| Library | Version | Used For |
|---|---|---|
| JJWT | 0.12.5 | JWT creation, parsing, validation |
| iText7 Core | 8.0.4 | PDF report generation |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI, API documentation |
| Spring Batch | 3.x | Overnight inventory reconciliation |
| Ehcache | 3.x (jakarta) | In-process L2 cache |
| WebFlux (WebClient) | 3.2.4 | Non-blocking Claude AI API calls |
| Testcontainers | 1.19.7 | Integration tests with real Docker |
| JaCoCo | 0.8.12 | Code coverage reporting |

---

## Kafka Event Streaming

<img width="900" height="380" alt="kafka_flow" src="https://github.com/user-attachments/assets/2bde9d0d-acca-4626-bba6-b8a8e7669c09" />

Inventory operations publish to four Kafka topics. Consumers react asynchronously — the HTTP response doesn't wait for email delivery or PDF generation.

| Topic | Producer | Consumers | What Triggers It |
|---|---|---|---|
| `inventory.low-stock` | `InventoryService` | `AlertService`, `ReorderService` | Stock falls below `minimum_quantity` |
| `inventory.expiry-alert` | `BatchJobService` | `AlertService` | Drug expires within 30 days |
| `inventory.reorder` | `ReorderService` | `AuditService` | Auto-reorder initiated |
| `audit.dispensing` | `DispensingController` | `AuditService` | Any unit dispensed |

**Kafka configuration highlights from `docker-compose.yml`:**
```yaml
KAFKA_LOG_RETENTION_HOURS: 168          # 7-day event retention
KAFKA_AUTO_CREATE_TOPICS_ENABLE: true   # topics auto-created on first publish
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1  # single-broker dev setup
```

For production multi-broker, bump `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` to 3 and set `min.insync.replicas=2`.

---

## Security Model

<img width="900" height="380" alt="security" src="https://github.com/user-attachments/assets/6e78d09b-ff71-4259-a8cd-e92729e10c67" />

### Authentication

Stateless JWT authentication using JJWT 0.12.5 with HS256 signing:

```
POST /api/auth/login  →  returns { token, expiresIn }
Authorization: Bearer <token>  →  every subsequent request
```

Token lifetime is configurable via `MEDICHAIN_JWT_EXPIRATION_MS` (default: 86400000 = 24h). Revoked tokens are stored in Redis with TTL matching their remaining validity — so logout actually works, even though JWTs are stateless by design.

### Authorization

Four roles, enforced at both the method and URL level via Spring Security:

```
ADMIN        — full system access, user management, batch job triggers
PHARMACIST   — inventory CRUD, dispensing operations, PDF export
DOCTOR       — read-only inventory view, dispensing requests
VIEWER       — read-only dashboards and reports only
```

### Transport

Nginx handles TLS termination. Mount your certificates at `./nginx/ssl/` and they're picked up automatically. The Spring Boot app only ever sees plain HTTP on port 8080 inside the Docker network — TLS never touches the JVM.

---

## Observability

The full observability stack runs out of the box:

| Service | Port | What's There |
|---|---|---|
| Spring Actuator | `:8080/actuator` | Health, metrics, info, env |
| Prometheus | `:9090` | Metrics storage, alert rules |
| Grafana | `:3000` | Pre-provisioned dashboards |
| pgAdmin | `:5050` | PostgreSQL query/inspection |
| Swagger UI | `:8080/swagger-ui.html` | Full API documentation |

**Prometheus scrapes `/actuator/prometheus` every 15 seconds.** The `alert.rules.yml` fires alerts for:
- App heap usage > 80%
- API error rate > 5% over 5 minutes
- Kafka consumer lag > 1000 messages
- PostgreSQL connection pool saturation

---

## Project Structure

```
MediChain/
│
├── src/
│   └── main/
│       ├── java/com/medichain/
│       │   ├── config/           # Spring Security, Kafka, Redis, Flyway config
│       │   ├── controller/       # REST controllers (@RestController)
│       │   ├── service/          # Business logic layer
│       │   ├── repository/       # Spring Data JPA repositories
│       │   ├── entity/           # JPA entities (@Entity)
│       │   ├── dto/              # Request/response DTOs
│       │   ├── mapper/           # MapStruct mappers
│       │   ├── security/         # JWT filter, UserDetailsService
│       │   ├── kafka/            # Producers, consumers, event models
│       │   ├── batch/            # Spring Batch jobs (expiry scan, reconciliation)
│       │   ├── ai/               # Claude API WebClient integration
│       │   └── pdf/              # iText7 PDF report builders
│       └── resources/
│           ├── db/migration/     # Flyway SQL migrations (V1__, V2__, ...)
│           ├── templates/        # Thymeleaf HTML templates
│           └── application.yml   # Main config (env var substitution)
│
├── grafana/
│   └── provisioning/             # Auto-provisioned datasources + dashboards
│
├── nginx/
│   └── nginx.conf                # Reverse proxy, rate limiting, SSL
│
├── prometheus/
│   ├── prometheus.yml            # Scrape configs
│   └── alert.rules.yml          # Alerting rules
│
├── scripts/
│   └── seed.sql                  # Demo data seeding script
│
├── Dockerfile                    # eclipse-temurin:21-jre, ZGC flags, health check
├── docker-compose.yml            # Full 9-service production stack
├── pom.xml                       # Maven build, all deps declared
└── .env.example                  # Every env var documented and explained
```

---

## Deployment Pipeline

<img width="900" height="320" alt="deployment" src="https://github.com/user-attachments/assets/b3133e0d-2405-4e22-9395-6487f9030368" />

### Local Development

```bash
# Clone and enter
git clone https://github.com/sat1828/MediChain.git
cd MediChain

# Configure environment
cp .env.example .env
# Edit .env — at minimum set MEDICHAIN_DB_PASSWORD and MEDICHAIN_JWT_SECRET
# Optionally add MEDICHAIN_AI_CLAUDE_API_KEY for demand forecasting

# Build the JAR
./mvnw clean package -DskipTests

# Bring up all 9 services
docker compose up --build -d

# Tail logs
docker compose logs -f app
```

### Health Check Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Metrics (Prometheus format)
curl http://localhost:8080/actuator/prometheus

# API Documentation
open http://localhost:8080/swagger-ui.html

# Grafana
open http://localhost:3000   # admin / admin

# pgAdmin
open http://localhost:5050   # admin@medichain.local / admin
```

### Generating a JWT Secret

```bash
# Minimum 64-char random key for HS256
openssl rand -base64 64
```

---

## API Reference

Full API documentation lives at `/swagger-ui.html` when running. Key endpoint groups:

```
POST   /api/auth/login                  — Authenticate, get JWT
POST   /api/auth/logout                 — Revoke token (Redis blacklist)

GET    /api/drugs                       — List all drugs (paginated, filterable)
POST   /api/drugs                       — Register new drug [PHARMACIST+]
PUT    /api/drugs/{id}                  — Update drug details [PHARMACIST+]
DELETE /api/drugs/{id}                  — Remove drug [ADMIN]

GET    /api/inventory                   — Current stock levels
POST   /api/inventory/dispense          — Dispense units (fires audit event)
GET    /api/inventory/low-stock         — Drugs below minimum threshold
GET    /api/inventory/expiring          — Drugs expiring within N days

GET    /api/reports/inventory/pdf       — Download iText7 PDF inventory report
GET    /api/reports/dispensing/pdf      — Download dispensing history PDF

GET    /api/ai/forecast/{drugId}        — Claude AI demand forecast (7-day)

GET    /api/admin/jobs                  — List Spring Batch jobs [ADMIN]
POST   /api/admin/jobs/{jobName}/run    — Manually trigger batch job [ADMIN]
```

All endpoints return RFC 7807 `ProblemDetail` on errors — no naked stack traces, no inconsistent error shapes.

---

## Configuration Reference

Every environment variable is documented in `.env.example`. The critical ones:

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_PASSWORD` | `changeme_in_production` | PostgreSQL password — change this |
| `MEDICHAIN_JWT_SECRET` | `changeme_generate...` | JWT signing key — use `openssl rand -base64 64` |
| `MEDICHAIN_JWT_EXPIRATION_MS` | `86400000` | Token lifetime (ms) — 24h default |
| `MEDICHAIN_AI_CLAUDE_API_KEY` | _(empty)_ | Claude API key — forecasting disabled if absent |
| `MEDICHAIN_AI_CLAUDE_MODEL` | `claude-sonnet-4-20250514` | Claude model to use |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile — `dev` enables extra logging |
| `MEDICHAIN_SEED_DEMO_DATA` | `false` | Load demo drugs/inventory on startup |

---

## Running Tests

```bash
# Unit + integration tests (Testcontainers spins up real Postgres, Kafka)
./mvnw verify

# Coverage report (JaCoCo)
open target/site/jacoco/index.html

# Just unit tests (fast, no Docker needed)
./mvnw test -Dspring.profiles.active=test
```

The test suite uses **Testcontainers** — no mocking of Postgres or Kafka. If it passes in `./mvnw verify`, it works against real infrastructure.

---

## Claude AI Integration

The `MEDICHAIN_AI_CLAUDE_API_KEY` powers the demand forecasting feature. When set, the application makes WebClient calls to `claude-sonnet-4-20250514` with structured prompts built from:

- Last 90 days of dispensing history for the drug
- Seasonal patterns from the same period in prior years
- Current stock level and pending reorder status

The response is a 7-day demand forecast with confidence bands, displayed in the dashboard and used to pre-trigger reorders before stock actually runs out.

If the API key is not set, forecasting is silently disabled and the dashboard shows a "configure AI key" placeholder — no errors, no crashes.

---

## Docker Image Details

The Dockerfile uses a minimal `eclipse-temurin:21-jre-jammy` base — no full JDK in production, no build tools, no shell exploits. The JVM flags are not arbitrary:

```dockerfile
ENTRYPOINT ["java",
  "-XX:+UseZGC",              # Sub-millisecond GC — critical for pharmacy latency
  "-XX:MaxRAMPercentage=75.0", # Container-aware heap — leaves 25% for OS/Kafka client
  "-XX:+ExitOnOutOfMemoryError", # Crash fast on OOM rather than limping broken
  "-Djava.security.egd=file:/dev/./urandom",  # Faster /dev/random on Linux
  "-jar", "app.jar"]
```

The image runs as a non-root user (`medichain`, UID 1000). The health check polls `/actuator/health` every 30 seconds — Docker Compose won't start dependent services until the app is actually healthy.

---

<div align="center">

Built with Java 21, Spring Boot 3.2, and too much caffeine.

**[⭐ Star this repo](https://github.com/sat1828/MediChain)** if it saved you time.

</div>
