# HelpFlow — IT Support Ticket Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker%20Compose-Ready-blue.svg)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-75%20Passing-success.svg)]()
[![Coverage](https://img.shields.io/badge/JaCoCo%20Coverage->81%25-success.svg)]()

**HelpFlow** is a distributed IT Support Ticket Management platform engineered under **Hexagonal Architecture (Ports and Adapters)** and **Domain-Driven Design (DDD)** principles, composed of two independently executable microservices.

### Key Capabilities

- Incident reporting and lifecycle orchestration governed by a domain state machine.
- Automated SLA deadline computation based on priority policies.
- Deterministic technician allocation via independent dispatch microservice with local fallback resilience.
- Automated scheduled escalation engine for tickets approaching SLA breach.
- Stateless JWT authentication and Role-Based Access Control (RBAC).
- Full observability through structured logging with MDC correlation IDs (`traceId`) and Micrometer metrics.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Microservices Architecture](#2-microservices-architecture)
3. [Ticket Lifecycle & State Machine](#3-ticket-lifecycle--state-machine)
4. [SLA Management & Escalation Engine](#4-sla-management--escalation-engine)
5. [Database Model & Flyway Migrations](#5-database-model--flyway-migrations)
6. [API Reference & Swagger](#6-api-reference--swagger)
7. [Security & Authentication Model](#7-security--authentication-model)
8. [Observability & Metrics](#8-observability--metrics)
9. [Prerequisites & Local Execution](#9-prerequisites--local-execution)
10. [Docker Compose Deployment](#10-docker-compose-deployment)
11. [Testing & Quality Assurance](#11-testing--quality-assurance)
12. [Postman Test Collection](#12-postman-test-collection)
13. [Verified Execution Evidence](#13-verified-execution-evidence)

---

## 1. Architecture Overview

HelpFlow implements genuine **Hexagonal Architecture (Ports and Adapters)**. The domain logic is completely isolated from frameworks, persistence technologies, and communication protocols.

```text
+-----------------------------------------------------------------------------------+
|                            INFRASTRUCTURE LAYER                                   |
|                                                                                   |
|   +--------------------------+             +----------------------------------+   |
|   |   Driving Adapters       |             |        Driven Adapters           |   |
|   | (infrastructure.adapter. |             |    (infrastructure.adapter.      |   |
|   |         in.web)          |             |       out.persistence /          |   |
|   |   - TicketController     |             |          out.dispatch)           |   |
|   |   - AuthController       |             |   - TicketPersistenceAdapter     |   |
|   |   - UserController       |             |   - UserPersistenceAdapter       |   |
|   |   - TechnicianController |             |   - TechnicianPersistenceAdapter |   |
|   +------------+-------------+             |   - TechnicianDispatchRestClient |   |
|                |                           +-----------------+----------------+   |
+----------------|---------------------------------------------|--------------------+
                 | (invokes)                                   ^ (implements)
+----------------|---------------------------------------------|--------------------+
|                v                                             |                    |
|   +--------------------------+             +-----------------+----------------+   |
|   |       Input Ports        |             |          Output Ports            |   |
|   |  (application.port.in)   |             |     (application.port.out)       |   |
|   |   - CreateTicketUseCase  |             |   - TicketRepositoryPort         |   |
|   |   - UpdateTicketStatusUC |             |   - UserRepositoryPort           |   |
|   |   - AssignTechnicianUC   |             |   - TechnicianRepositoryPort     |   |
|   |   - AuthenticateUserUC   |             |   - TechnicianDispatchPort       |   |
|   |   - EscalateTicketsUC    |             |   - PasswordEncoderPort          |   |
|   +------------+-------------+             |   - TokenServicePort             |   |
|                |                           +-----------------+----------------+   |
|                v                                             ^                    |
|   +----------------------------------------------------------+----------------+   |
|   |                  Application Services (Use Case Impls)                    |   |
|   |                      (application.service)                                |   |
|   |   - TicketService, AuthService, UserService, TechnicianService, SlaService|   |
|   +---------------------------------------------------------------------------+   |
|                                      |                                            |
|                                      v                                            |
|   +---------------------------------------------------------------------------+   |
|   |                               DOMAIN LAYER                                |   |
|   |                            (domain.model / policy)                        |   |
|   |   - Pure POJOs: Ticket, User, Technician, Assignment, TicketHistory       |   |
|   |   - Enums: TicketStatus, Priority, Category, Role, Availability           |   |
|   |   - Policies: SlaPolicy, AssignmentEligibilityPolicy                      |   |
|   |   - ZERO framework or JPA dependencies                                   |   |
|   +---------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------+
```

### Directory Mapping

```text
support-ticket-service/src/main/java/com/helpflow/ticket/
|-- domain/                               # Pure business logic (NO Spring, NO JPA)
|   |-- model/                            # Ticket, User, Technician, Assignment, TicketHistory
|   |-- policy/                           # SlaPolicy, AssignmentEligibilityPolicy
|   +-- exception/                        # DomainRuleViolationException, InvalidTransitionException...
|-- application/                          # Orchestration & Use Case abstractions
|   |-- port/in/                          # Input port interfaces (CreateTicketUseCase, etc.)
|   |-- port/out/                         # Output port interfaces (TicketRepositoryPort, etc.)
|   +-- service/                          # Application services implementing input ports
+-- infrastructure/                       # Framework, delivery, and persistence implementations
    |-- adapter/in/web/                   # REST Controllers, DTOs, GlobalExceptionHandler
    |-- adapter/out/persistence/          # JPA Entities, Spring Data Repositories, Entity Mappers
    |-- adapter/out/dispatch/             # Spring RestClient adapter calling dispatch microservice
    |-- security/                         # Spring Security 6, JwtAuthenticationFilter, TokenProvider
    |-- metrics/                          # Micrometer metrics configuration & registration
    |-- scheduler/                        # @Scheduled SLA Escalation background job
    +-- config/                           # Bean wiring, Clock configuration, OpenAPI documentation
```

---

## 2. Microservices Architecture

HelpFlow consists of two microservices that operate as independent processes:

```text
                      +-----------------------------+
                      |         HTTP Client         |
                      |   (Browser / Postman / cURL) |
                      +--------------+--------------+
                                     |
                                     | Port 8080 (REST + JWT)
                                     v
                  +-----------------------------------+
                  |      support-ticket-service       |
                  |  (Hexagonal, Spring Boot, JPA)    |
                  +---------+---------------+---------+
                            |               |
         PostgreSQL Port    |               | HTTP Port 8081
         5432 (Internal)    |               | (TechnicianDispatchPort)
                            v               v
            +--------------------+    +-------------------------------+
            |  PostgreSQL 16     |    |  technician-dispatch-service  |
            |  (helpflow_db)     |    |  (Stateless, Deterministic)   |
            +--------------------+    +-------------------------------+
```

### 1. `support-ticket-service` (Port 8080)
- Manages user profiles, corporate authentication, ticket lifecycles, technician rosters, and audit trails.
- Uses PostgreSQL with Spring Data JPA and Flyway versioned migrations.
- Secures all protected routes via stateless JWT Bearer authentication and role-based access control (RBAC).
- Automatically invokes the dispatch service upon ticket creation.
- Runs a scheduled job every 60 seconds to detect tickets approaching SLA breaches and escalates their priority.

### 2. `technician-dispatch-service` (Port 8081)
- Autonomous microservice simulating technician allocation without database or security overhead.
- Exposes `POST /technician-dispatch`.
- **Deterministic selection:** Uses an MD5 digest hash combining `ticketId`, `category`, and `priority` modulo 20 to consistently return the same technician and availability rating for the same input parameters.
- Availability classification:
  - `0 - 2 hours`: `HIGH`
  - `3 - 8 hours`: `MEDIUM`
  - `> 8 hours`: `LOW`
- **Resilience:** If the dispatch service is unreachable or errors out, `support-ticket-service` falls back gracefully to local eligibility policy selection and logs a fallback assignment rather than aborting ticket creation.

---

## 3. Ticket Lifecycle & State Machine

HelpFlow enforces a strict state machine at the domain level (`Ticket.java`). Arbitrary or out-of-order transitions are rejected with HTTP 400 Bad Request.

```text
       +--------+
       | (Init) |
       +----+---+
            |
            v
        +------+
        | OPEN | <-----------------------+ (Auto-dispatch)
        +---+--+                         |
            |                            |
            +------------+---------------+
                         |
                         v
                   +----------+
     +------------>| ASSIGNED |
     |             +----+-----+
     | Reassignment     |
     | by Admin         v
     |             +-------------+
     +-------------| IN_PROGRESS |
                   +----+---+----+
                        |   |
          Cancelled     |   | Resolved
          by Admin/User |   v
          +-------------+  +----------+
          |                | RESOLVED |
          v                +----+-----+
    +-----------+               |
    | CANCELLED |               v
    +-----------+          +--------+
                           | CLOSED |
                           +--------+
```

### Permitted Transitions & Invariants

| From State | To State | Required Role | Mandatory Observations | Side Effects & Domain Invariants |
| :--- | :--- | :--- | :--- | :--- |
| *(None)* | `OPEN` | `ROLE_USUARIO`, `ROLE_ADMIN` | No | User must be active. Calculates SLA deadline based on priority. Dispatches technician. |
| `OPEN` | `ASSIGNED` | System / `ROLE_ADMIN` | Reason required on reassignment | Selected technician must be `ACTIVE` and have matching specialty. Active ticket count incremented. |
| `ASSIGNED` | `IN_PROGRESS` | `ROLE_TECNICO` (assigned), `ROLE_ADMIN` | Mandatory observations | Technician must be the one assigned to the ticket. |
| `IN_PROGRESS` | `RESOLVED` | `ROLE_TECNICO` (assigned), `ROLE_ADMIN` | Mandatory resolution notes | Records resolution timestamp. Active ticket count decremented for technician. |
| `RESOLVED` | `CLOSED` | `ROLE_ADMIN`, `ROLE_USUARIO` (ticket owner) | Mandatory closure comment | Terminal state. No further transitions allowed. |
| `OPEN` / `ASSIGNED` / `IN_PROGRESS` | `CANCELLED` | `ROLE_ADMIN`, `ROLE_USUARIO` (ticket owner) | Mandatory cancellation reason | Terminal state. Decrements technician active ticket count if assigned. |
| `ASSIGNED` | `ASSIGNED` | `ROLE_ADMIN` | Mandatory reassignment reason | Decrements previous technician count, increments new technician count, updates assignment record. |

---

## 4. SLA Management & Escalation Engine

SLA deadlines are computed at ticket creation using `SlaPolicy` and an injectable system `Clock`:

| Priority | Resolution SLA | Auto-Escalation Threshold |
| :--- | :--- | :--- |
| `CRITICAL` | **4 hours** | Already at highest priority (Alert logged) |
| `HIGH` | **8 hours** | Within 2 hours of deadline $\rightarrow$ Escalates to `CRITICAL` |
| `MEDIUM` | **24 hours** | Within 4 hours of deadline $\rightarrow$ Escalates to `HIGH` |
| `LOW` | **48 hours** | Within 8 hours of deadline $\rightarrow$ Escalates to `MEDIUM` |

### Escalation Scheduler
- Implemented in `SlaEscalationScheduler.java`.
- Runs periodically via `@Scheduled(cron = "${helpflow.sla.escalation-cron:0 * * * * *}")` (every 60 seconds by default).
- Queries active non-terminal tickets (`OPEN`, `ASSIGNED`, `IN_PROGRESS`).
- Evaluates `slaPolicy.shouldEscalate(ticket, now)`.
- If eligible, escalates ticket priority, recalculates SLA deadlines, logs a `TicketHistory` entry with `actor = SYSTEM`, and persists changes transactionally.

---

## 5. Database Model & Flyway Migrations

The database is managed with versioned Flyway migrations located in `support-ticket-service/src/main/resources/db/migration/`:

```text
db/migration/
|-- V1__schema.sql                   # DDL: users, technicians, tickets
|-- V2__assignments_history_sla.sql  # DDL: assignments, ticket_histories, indexes, FK constraints
+-- V3__initial_data.sql             # DML: 20 seed technicians, admin user, standard user, technician user
```

### Entity Relationships

- `users` (1) $\rightarrow$ (N) `tickets`
- `technicians` (1) $\rightarrow$ (N) `tickets`
- `tickets` (1) $\rightarrow$ (N) `assignments`
- `tickets` (1) $\rightarrow$ (N) `ticket_histories`

---

## 6. API Reference & Swagger

### Swagger UI (Interactive Testing)
When the application is running, open in your browser:
👉 **`http://localhost:8080/swagger-ui/index.html`**

- **OpenAPI 3 JSON Spec:** `http://localhost:8080/v3/api-docs`
- **Static Exported File:** Included in the root directory as [`swagger.json`](./swagger.json).

### Endpoints Summary

| Method | Endpoint | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Public | Register a new user (`ROLE_USUARIO`). Requires corporate `@helpflow.com` email. |
| `POST` | `/auth/login` | Public | Authenticate user and return JWT token. |
| `GET` | `/users/me` | Authenticated | Retrieve profile of authenticated principal. |
| `PUT` | `/users/me` | Authenticated | Update full name and support area of authenticated principal. |
| `GET` | `/users` | `ROLE_ADMIN` | List all system users with optional status filtering. |
| `PATCH`| `/users/{id}/status` | `ROLE_ADMIN` | Activate or deactivate user account. |
| `GET` | `/technicians` | Authenticated | List technicians with optional `?specialty=` and `?availability=` filters. |
| `GET` | `/technicians/{id}` | Authenticated | Get technician details by ID. |
| `PATCH`| `/technicians/{id}/availability` | `ROLE_ADMIN` | Update technician availability status. |
| `POST` | `/tickets` | `ROLE_USUARIO`, `ROLE_ADMIN` | Create a ticket (auto-computes SLA and auto-dispatches). |
| `GET` | `/tickets` | `ROLE_ADMIN` | List all tickets with optional filtering (`status`, `priority`, `category`, `overdue`). |
| `GET` | `/tickets/my` | `ROLE_USUARIO`, `ROLE_TECNICO` | View tickets owned by requesting user or assigned to technician. |
| `GET` | `/tickets/{id}` | Authenticated | Get ticket by ID (ownership enforced for users and technicians). |
| `POST` | `/tickets/{id}/assign`| `ROLE_ADMIN` | Explicitly assign or reassign ticket to a technician. |
| `PATCH`| `/tickets/{id}/status`| Authenticated | Transition ticket state (`IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`). |
| `POST` | `/technician-dispatch`| Public (Port 8081) | Deterministic technician dispatch algorithm service. |

### RFC 7807 Standard Error Format (`ProblemDetail`)

```json
{
  "type": "about:blank",
  "title": "Invalid Ticket Transition",
  "status": 400,
  "detail": "Cannot transition ticket 1 from status CLOSED to OPEN. Permitted target states: none",
  "instance": "/tickets/1/status",
  "timestamp": "2026-10-27T14:48:30.125Z",
  "traceId": "9b642ca8-3162-43bb-a5eb-06fceeb0ea12"
}
```

---

## 7. Security & Authentication Model

### Default Accounts for Testing

| Role | Email | Password | Profile |
| :--- | :--- | :--- | :--- |
| `ROLE_ADMIN` | `admin@helpflow.com` | `Password123!` | System Administrator |
| `ROLE_TECNICO`| `tecnico@helpflow.com` | `Password123!` | Assigned Support Technician |
| `ROLE_USUARIO`| `usuario@helpflow.com` | `Password123!` | Department User |

---

## 8. Observability & Metrics

Exposed via Actuator at `http://localhost:8080/actuator/metrics`:

- `helpflow.tickets.created`: Counter of total tickets created (tagged by `priority` and `category`).
- `helpflow.tickets.overdue`: Counter of tickets that breached SLA deadline.
- `helpflow.tickets.first_response.time`: Timer measuring latency between ticket creation and assignment to a technician.
- `helpflow.tickets.resolution.time`: Timer measuring total duration from creation to resolution.
- `helpflow.auth.failures`: Counter of invalid login attempts.
- `helpflow.auth.access_denied`: Counter of unauthorized (403) access attempts.

---

## 9. Prerequisites & Local Execution

### Prerequisites
- JDK 21
- Maven 3.8+
- Docker & Docker Compose

### Running Locally with Maven
```bash
# 1. Start PostgreSQL
docker run --name helpflow-postgres -e POSTGRES_DB=helpflow_db -e POSTGRES_USER=helpflow_user -e POSTGRES_PASSWORD=helpflow_password -p 5434:5432 -d postgres:16-alpine

# 2. Compile & package
mvn clean package

# 3. Start Dispatch Service (Port 8081)
mvn spring-boot:run -pl technician-dispatch-service

# 4. Start Support Ticket Service (Port 8080)
mvn spring-boot:run -pl support-ticket-service
```

---

## 10. Docker Compose Deployment

```bash
# Start all containers in background
DOCKER_BUILDKIT=0 COMPOSE_PROJECT_NAME=helpflow docker compose up --build -d

# Verify health
docker compose ps
```

---

## 11. Testing & Quality Assurance

```bash
mvn test
```

### JaCoCo Code Coverage Verification

| Module | Line Coverage | Branch Coverage | Method Coverage | Class Coverage |
| :--- | :---: | :---: | :---: | :---: |
| **`support-ticket-service`** | **81.7%** | 62.5% | **83.8%** | **97.7%** |
| **`technician-dispatch-service`** | **81.7%** | **91.7%** | **87.5%** | **100.0%** |
| **Required Minimum** | $\ge 80\%$ | $\ge 60\%$ | $\ge 80\%$ | $\ge 90\%$ |

---

## 12. Postman Test Collection

The complete Postman collection is included at [`HelpFlow_API.postman_collection.json`](./HelpFlow_API.postman_collection.json).

1. Import `HelpFlow_API.postman_collection.json` in Postman.
2. Run **`Login Admin`** in `1. Authentication` (automatically populates `{{adminToken}}`).
3. Execute the lifecycle and management requests in order.

---

## 13. Verified Execution Evidence

- **Database Migrations:** Flyway applied `V1__schema.sql`, `V2__assignments_history_sla.sql`, and `V3__initial_data.sql` successfully on startup.
- **Seed Data:** 20 technicians and default users seeded and verified.
- **Auto-Dispatch:** Ticket creation requests sent to `/tickets` synchronously invoke `technician-dispatch-service` (8081) and allocate technicians deterministically.
- **Traceability:** Distributed trace correlation via MDC header `X-Trace-Id`.
- **Zero Mock Policy on Persistence:** End-to-end integration tests execute against real PostgreSQL instances via Testcontainers.
