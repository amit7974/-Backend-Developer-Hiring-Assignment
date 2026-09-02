# Product REST API

A fully-featured **RESTful API** for Product CRUD operations built with **Java 17**, **Spring Boot 3**, **Spring Security + JWT**, **PostgreSQL**, and **Docker**.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP/HTTPS (Bearer JWT)
┌─────────────────────▼───────────────────────────────────┐
│              JwtAuthenticationFilter                     │
│                  SecurityFilterChain                     │
├─────────────────────────────────────────────────────────┤
│   AuthController       │      ProductController          │
│   /api/v1/auth/**      │      /api/v1/products/**        │
├─────────────────────────────────────────────────────────┤
│   AuthServiceImpl      │      ProductServiceImpl         │
├─────────────────────────────────────────────────────────┤
│   JPA Repositories (Spring Data)                        │
├─────────────────────────────────────────────────────────┤
│   PostgreSQL (prod) │ H2 in-memory (test)               │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.example.productapi/
├── config/          # SecurityConfig, OpenApiConfig, AsyncConfig
├── controller/      # ProductController, AuthController
├── dto/
│   ├── request/     # ProductRequest, LoginRequest, RefreshTokenRequest, ItemRequest
│   └── response/    # ProductResponse, ItemResponse, AuthResponse, ApiErrorResponse
├── entity/          # Product, Item, User, Role, RefreshToken
├── exception/       # GlobalExceptionHandler, ResourceNotFoundException, TokenRefreshException
├── mapper/          # ProductMapper (MapStruct)
├── repository/      # JPA repositories
├── security/        # JwtUtils, JwtAuthenticationFilter, CustomUserDetailsService
└── service/         # Interfaces + impl/
```

---

## 🚀 Quick Start

### Prerequisites
- **Docker** & **Docker Compose** (recommended)
- **OR**: Java 17+, Maven 3.9+, PostgreSQL 15+

---

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repo
git clone <YOUR_REPO_URL>
cd product-api

# Build and start all services
docker-compose up --build

# API available at:
# http://localhost:8080/swagger-ui.html
```

---

### Option 2: Run Locally

#### 1. Start PostgreSQL
```bash
docker run -d \
  --name productdb \
  -e POSTGRES_DB=productdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

#### 2. Configure environment variables (optional — defaults already set)
```bash
export DB_URL=jdbc:postgresql://localhost:5432/productdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

#### 3. Run
```bash
mvn clean package -DskipTests
java -jar target/product-api-1.0.0.jar
```

---

## 🔐 Authentication

The API uses **JWT (Bearer) authentication** with **refresh token rotation**.

### Default Credentials (seeded by Flyway)

| Username | Password   | Role        |
|----------|------------|-------------|
| `admin`  | `Admin@123`| ADMIN, USER |
| `user`   | `User@123` | USER        |

### Auth Flow

```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'

# Response: { "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer", "username": "admin" }

# 2. Use the token
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <accessToken>"

# 3. Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# 4. Logout
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>"
```

---

## 📋 API Endpoints

| Method   | Endpoint                          | Role          | Description              |
|----------|-----------------------------------|---------------|--------------------------|
| `POST`   | `/api/v1/auth/login`              | Public        | Login                    |
| `POST`   | `/api/v1/auth/refresh`            | Public        | Refresh token            |
| `POST`   | `/api/v1/auth/logout`             | Authenticated | Logout                   |
| `GET`    | `/api/v1/products`                | USER, ADMIN   | Get all products (paged) |
| `GET`    | `/api/v1/products/{id}`           | USER, ADMIN   | Get product by ID        |
| `POST`   | `/api/v1/products`                | ADMIN         | Create product           |
| `PUT`    | `/api/v1/products/{id}`           | ADMIN         | Update product           |
| `DELETE` | `/api/v1/products/{id}`           | ADMIN         | Delete product           |
| `GET`    | `/api/v1/products/{id}/items`     | USER, ADMIN   | Get items for product    |

### Pagination Parameters (for GET list endpoints)

| Param     | Default | Description                        |
|-----------|---------|------------------------------------|
| `page`    | `0`     | Page number (0-indexed)            |
| `size`    | `10`    | Page size                          |
| `sortBy`  | `id`    | Field to sort by                   |
| `sortDir` | `ASC`   | Sort direction (`ASC` or `DESC`)   |

### Standardized Error Response

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "fieldErrors": [
    { "field": "productName", "message": "Product name must not be blank" }
  ]
}
```

---

## 📊 Database Schema

```sql
product (id, product_name, created_by, created_on, modified_by, modified_on)
item    (id, product_id FK → product.id, quantity)
users   (id, username, email, password, enabled)
roles   (id, name)
user_roles (user_id, role_id)
refresh_tokens (id, user_id, token, expiry_date)
```

Migrations managed by **Flyway** (`src/main/resources/db/migration/V1__init.sql`).

---

## 🧪 Running Tests

```bash
# All tests (uses H2 in-memory via application-test.yml)
mvn test

# Unit tests only
mvn test -Dtest="ProductServiceTest,AuthServiceTest"

# Integration tests only
mvn test -Dtest="ProductIntegrationTest"
```

### Test Coverage

| Test Class                | Type        | What it tests                               |
|---------------------------|-------------|---------------------------------------------|
| `ProductServiceTest`      | Unit        | CRUD operations, exception handling         |
| `AuthServiceTest`         | Unit        | Login, refresh rotation, logout, expiry     |
| `ProductControllerTest`   | Slice test  | HTTP status codes, validation, role access  |
| `ProductIntegrationTest`  | Integration | Full request cycle with real Spring context |

---

## 🔧 Configuration Reference

| Property                         | Default / Env Var     | Description                  |
|----------------------------------|-----------------------|------------------------------|
| `DB_URL`                         | jdbc:postgresql://... | PostgreSQL JDBC URL          |
| `DB_USERNAME`                    | `postgres`            | DB username                  |
| `DB_PASSWORD`                    | `postgres`            | DB password                  |
| `JWT_SECRET`                     | (hex string)          | HS256 signing key            |
| `app.jwt.expiration-ms`          | `900000` (15 min)     | Access token TTL             |
| `app.jwt.refresh-expiration-ms`  | `604800000` (7 days)  | Refresh token TTL            |

---

## 📖 Swagger UI

Once running, access the interactive API documentation at:

> **http://localhost:8080/swagger-ui.html**

Click **"Authorize"**, enter your **Bearer token**, and test all endpoints directly.

---

## 🐳 Docker Reference

```bash
# Build image only
docker build -t product-api .

# Start all services
docker-compose up --build

# Stop all services
docker-compose down

# Stop and remove volumes (reset DB)
docker-compose down -v
```

---

## 🛡️ Security Features

- **Stateless JWT** (no server-side sessions)
- **Refresh token rotation** (old token invalidated on each refresh)
- **BCrypt password encoding** (strength 12)
- **Role-based authorization** (`ROLE_ADMIN`, `ROLE_USER`)
- **CORS** configured for all origins (restrict in production)
- **Input validation** via Jakarta Validation annotations
- **Global exception handler** with standardized error responses

---

## 📁 Tech Stack

| Layer         | Technology                             |
|---------------|----------------------------------------|
| Language      | Java 17                                |
| Framework     | Spring Boot 3.2.5                      |
| Security      | Spring Security 6 + JJWT 0.12.5       |
| ORM           | Spring Data JPA (Hibernate 6)          |
| Database      | PostgreSQL 15 (H2 for tests)           |
| Migration     | Flyway                                 |
| Mapping       | MapStruct                              |
| Documentation | SpringDoc OpenAPI 3 (Swagger UI)       |
| Build         | Maven 3.9                              |
| Container     | Docker + Docker Compose                |
| Testing       | JUnit 5, Mockito, Spring Boot Test     |
