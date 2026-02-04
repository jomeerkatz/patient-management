# Patient Management - Microservice Healthcare Backend

A distributed patient management platform built with **Java 21**, **Spring Boot**, **Docker**, and **AWS CDK**.  
The project demonstrates secure API gateway routing, service-to-service communication via **gRPC**, and event-driven processing with **Kafka**.

`Spring Boot` `Java 21` `Spring Cloud Gateway` `PostgreSQL` `Kafka` `gRPC` `Docker` `AWS CDK`

## Project Overview

This repository contains a microservice system focused on patient domain workflows:

- **API Gateway** as the single entry point
- **Auth Service** for login + JWT validation
- **Patient Service** for patient CRUD
- **Billing Service** for gRPC-based account creation
- **Analytics Service** for Kafka event consumption
- **Integration Tests** for gateway/auth/patient flows
- **Infrastructure module** with AWS CDK (VPC, ECS/Fargate, RDS, MSK, ALB blueprint)

The architecture combines:
- **Synchronous communication** (REST + gRPC)
- **Asynchronous communication** (Kafka + Protobuf)
- **Containerized runtime** (Docker Compose)

---

## Architecture & Service Design

### High-Level Flow

```text
Client
  |
  v
API Gateway (:4004)
  |------------------------------> Auth Service (:4005) [login/validate]
  |
  +------------------------------> Patient Service (:4000) [CRUD]
                                     |
                                     +--> Billing Service (:9001 gRPC)
                                     |
                                     +--> Kafka Topic: "patient"
                                                |
                                                v
                                       Analytics Service (:4002)
```

### Why this architecture?

- **Centralized entrypoint**: Gateway handles routing and token checks for protected routes.
- **Service boundaries**: Auth, Patient, Billing, and Analytics stay decoupled by domain.
- **Communication fit-for-purpose**:
  - REST for external API calls
  - gRPC for efficient internal sync calls
  - Kafka for async event fan-out
- **Scalable foundation**: Each service can be deployed and scaled independently.

---

## Core Modules

### 1) API Gateway (`api-gateway`)
- Built with **Spring Cloud Gateway (WebFlux)**.
- Routes:
  - `/auth/**` -> Auth Service
  - `/api/patients/**` -> Patient Service
- Includes custom `JwtValidationGatewayFilterFactory`:
  - checks `Authorization: Bearer <token>`
  - calls Auth Service `/validate`
  - blocks unauthorized requests with `401`

### 2) Auth Service (`auth-service`)
- Handles login and token validation.
- Uses **Spring Security**, **JPA**, **PostgreSQL**, **JWT (jjwt)**.
- Endpoints:
  - `POST /login` -> issues JWT on valid credentials
  - `GET /validate` -> validates JWT integrity/expiration
- Seeds demo user via `data.sql`.

### 3) Patient Service (`patient-service`)
- Main domain service for patient records.
- Tech: **Spring Web**, **Spring Data JPA**, **Validation**, **PostgreSQL/H2**.
- Endpoints:
  - `GET /patients`
  - `POST /patients`
  - `PUT /patients/{id}`
  - `DELETE /patients/{id}`
- Business behavior on patient creation:
  1. Persist patient
  2. Trigger billing account creation via gRPC
  3. Publish `PATIENT_CREATED` event to Kafka topic `patient`

### 4) Billing Service (`billing-service`)
- gRPC server providing `CreateBillingAccount`.
- Receives `BillingRequest` with patient data and returns `BillingResponse`.
- Contract defined with Protocol Buffers.

### 5) Analytics Service (`analytics-service`)
- Kafka consumer (`@KafkaListener`) for topic `patient`.
- Deserializes protobuf payload (`PatientEvent`) and processes/logs events.
- Designed as extension point for reporting, BI, monitoring pipelines.

### 6) Integration Tests (`integration-tests`)
- Uses **RestAssured + JUnit 5**.
- Verifies:
  - successful and failing auth login flow
  - protected patient endpoint access via bearer token through gateway
- Tests run against a live stack on `http://localhost:4004`.

### 7) Infrastructure (`infrastructure`)
- Java-based **AWS CDK** stack definition.
- Provisions blueprint resources like:
  - VPC
  - ECS/Fargate services
  - RDS databases
  - MSK cluster
  - Application Load Balanced gateway service
- Synth output available under `infrastructure/cdk.out`.

---

## Data & Messaging Contracts

### Patient Event (Kafka + Protobuf)

```proto
message PatientEvent {
  string patientId = 1;
  string name = 2;
  string email = 3;
  string event_type = 4;
}
```

### Billing gRPC Contract

```proto
service BillingService {
  rpc CreateBillingAccount (BillingRequest) returns (BillingResponse);
}
```

---

## Security Model

- Authentication is token-based (**JWT**).
- Gateway protects patient routes using a custom filter.
- Auth Service provides token issuance + validation endpoints.
- Services are configured for container network communication in Docker Compose.

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.9
- Spring Cloud Gateway
- Spring Data JPA / Hibernate
- Spring Security
- gRPC + Protocol Buffers
- Apache Kafka
- PostgreSQL 17
- Maven

### DevOps / Infra
- Docker / Docker Compose
- AWS CDK (Java)

### Testing
- JUnit 5
- RestAssured

---

## Local Setup

### Prerequisites

- Docker + Docker Compose
- Java 21 (for local service runs/tests)
- Maven (or `./mvnw` per module)

### 1) Start the full stack (recommended)

```bash
docker compose up --build
```

### 2) Main exposed ports

- `4004` API Gateway
- `4005` Auth Service
- `4000` Patient Service (internal in compose, reachable through gateway)
- `4001` Billing HTTP
- `9001` Billing gRPC
- `4002` Analytics Service
- `5001` Patient PostgreSQL
- `5002` Auth PostgreSQL
- `9092/9094` Kafka
- `8080` Kafka UI

### 3) Quick API check

```bash
# Login
curl -s -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}'
```

```bash
# Example protected request
curl -X GET http://localhost:4004/api/patients \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## API Documentation

Gateway provides doc routes:

- `GET /api-docs/auth` -> proxied OpenAPI spec from Auth Service
- `GET /api-docs/patients` -> proxied OpenAPI spec from Patient Service

Core gateway endpoints:

- `POST /auth/login`
- `GET /auth/validate`
- `GET /api/patients`
- `POST /api/patients`
- `PUT /api/patients/{id}`
- `DELETE /api/patients/{id}`

---

## Running Tests

### Integration tests

Start services first (`docker compose up`), then:

```bash
cd integration-tests
mvn test
```

### Service-level tests

Each service contains its own Maven test setup, e.g.:

```bash
cd patient-service
./mvnw test
```

---

## What This Project Demonstrates

### Engineering Skills
- Microservice architecture with clear service boundaries
- Secure API gateway pattern with centralized auth checks
- Mixed communication patterns (REST + gRPC + Kafka)
- Contract-first messaging with Protocol Buffers
- Container-first local environment with Docker Compose
- Infrastructure as Code fundamentals with AWS CDK

### Professional Practices
- Separation of concerns across controller/service/repository layers
- Input validation and global exception handling
- Integration testing of real HTTP flows
- Reproducible local setup with seeded data

---

## Potential Next Improvements

- Expand automated test coverage (service logic + gateway filter edge cases)
- Add centralized observability (OpenTelemetry + tracing + metrics dashboards)
- Add role-based authorization rules beyond token validity checks
- Implement retry/dead-letter handling for Kafka consumer failures
- Introduce CI pipeline (build, test, container scan, deployment checks)

---

## Notes

This is a portfolio project and actively evolving.  
Feedback, ideas, and architectural discussions are welcome.
