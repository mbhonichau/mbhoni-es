# MBHONI Enterprise Admin Service

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3](https://img.shields.io/badge/Spring_Boot-3.3-brightgreen.svg)
![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)
![License](https://img.shields.io/badge/License-Proprietary-blue.svg)

**MBHONI Enterprise Admin Service** is a high-performance, multi-tenant administrative engine and central Identity & Integration API Gateway built for enterprise SaaS applications. It powers tenant onboarding, white-label branding, role-based access control (RBAC), subscription billing, microservice API key entitlements, and real-time security process enforcement across downstream microservices (e.g., HCM, Payroll, Finance, Service Catalog).

---

## Key Features

### 🏢 1. Multi-Tenant Isolation & Workspace Engine
- **Thread-Local Context Isolation**: Thread-safe `TenantContext` filter guaranteeing zero data bleed across tenant workspaces.
- **White-Label Customization**: Per-tenant primary/secondary color schemes, brand logos, custom domain aliases, and custom CSS overrides.
- **10-Step Transactional Cascade Tenant Purge**: Deleting a tenant safely purges all dependent records in exact relational order:
  1. Employee records & manager FK disassociation
  2. `user_roles` join entries & user accounts
  3. Contract milestones & agreements
  4. Payments, invoices & billing accounts
  5. Parent organization unit hierarchy links & org units
  6. Lookup codes & lookup categories
  7. `tenant_api_key_permissions` join records & API keys
  8. Tenant subscriptions, quotas, customizations, modules, content & metrics
  9. `role_permissions` join records & tenant custom roles
  10. Primary tenant workspace record

### 🔐 2. Access Control (RBAC) & Categorized Permission Matrix
- **Immutable System & Tenant Roles**: Pre-seeded system roles (`GLOBAL_ADMIN`, `SYSTEM_ADMIN`, `TENANT_ADMIN`) alongside tenant-customized roles.
- **Domain-Grouped Permission Cards**: Categorized permission sets across Security, Users, Roles, API Keys, Subscriptions, HCM, Finance, and Service Catalog.
- **Modal Permission Matrix**: Non-cluttered modal dialog popups (`#permModal-{id}`) for inspecting assigned permissions.

### 👤 3. Identity Administration & Dynamic Tenant Filtering
- **Real-Time Dynamic Tenant Role Filtering**: Selecting a tenant workspace dynamically filters role cards on user creation/editing forms to show only applicable roles (Global System roles + selected Tenant custom roles).
- **Credential Self-Service**: Automatic 12-character high-entropy temporary password generator (`/users/generate-password`) and 24-hour token-based email reset links (`/users/reset-password`).
- **Integration Profile Controls (Admin Process Toggles)**:
  - `MFA Enforced`: Mandatory 2FA passcode requirement flag.
  - `SSO Enforced`: Mandatory Single Sign-On requirement flag.
  - `API Access Allowed`: REST/GraphQL API integration privilege.
  - `Audit Extended`: Granular payload audit logging flag.
  - `Password Change Required`: Mandatory password rotation flag.

### 🔑 4. API Key & Microservice Integration Gateway
- **High-Entropy Key Generation**: 64-character secure tokens (`mb_live_...`).
- **Scope-Restricted Entitlements**: Microservice scopes (`READ_ONLY`, `WRITE`, `ADMIN`, `HCM_SYNC`, `FINANCE_WRITE`).
- **Instant Rotation & Revocation**: Instant token deactivation and scope inspection popups.

### 💳 5. Subscriptions, Module Hub & Billing Engine
- **Tiered Plans**: Starter, Growth, and Enterprise plans with configurable max user limits, custom domain flags, and API access privileges.
- **Creative Module Hub**: Domain-categorized module matrix (Core HCM, Payroll, Performance, Operations, Service Catalog) with iOS-style toggle switches.
- **Billing & Invoicing**: Automated invoice generation, due-date tracking, payment reference recording, and revenue analytics.

### 🎨 6. Modern Visual Aesthetics & Design System
- **Glassmorphic 3D Canvas Login**: Interactive WebGL 3D canvas background powered by Three.js reacting to cursor movements.
- **Pure White Surface Theme**: Crisp `#FFFFFF` surface backgrounds for optimal data readability.
- **UI/UX Color Palette**:
  - **Primary / Brand**: Dark Olive (`#262A1D`) & Terracotta Rust (`#713519`)
  - **Success / Active**: Sage Emerald (`#454F2C`)
  - **Warning / Security**: Warm Amber Gold (`#D97706`)
  - **Danger / Destructive**: Crimson Red (`#991B1B`)
- **Global CRUD Confirmation Interceptor**: Custom modal warning interceptor (`initGlobalCrudConfirmations`) for safe entity deletion.

---

## System Architecture & Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language & JDK** | Java 21 LTS |
| **Framework** | Spring Boot 3.3.x (Spring Security, Spring Data JPA, Web) |
| **Database** | PostgreSQL / H2 Database (Hibernate ORM) |
| **Template Engine** | Thymeleaf 3 with HTML5 & Bootstrap 5 |
| **3D Graphics** | Three.js WebGL (Interactive Canvas Background) |
| **Build Tool** | Apache Maven 3.9+ |

---

## REST API Integration Specifications

### 1. Retrieve User Integration Profile
Returns complete identity metadata, roles, permissions, and active process toggles for downstream microservices.

```http
GET /api/user/profile/{id} HTTP/1.1
Host: admin.mbhoni.com
Authorization: Bearer <API_KEY_OR_JWT>
Accept: application/json
```

#### Sample Response:
```json
{
  "id": 42,
  "username": "mwilson_apex",
  "email": "mwilson@apexlogistics.com",
  "department": "Human Resources",
  "jobTitle": "HR Director",
  "timeZone": "Africa/Johannesburg",
  "authProvider": "LOCAL",
  "globalAdmin": false,
  "active": true,
  "passwordChangeRequired": false,
  "mfaEnforced": true,
  "ssoEnforced": false,
  "apiAccessAllowed": true,
  "auditExtended": true,
  "tenantId": 2,
  "tenantName": "Apex Logistics",
  "roles": ["HR_DIRECTOR"],
  "permissions": ["USER_VIEW", "USER_CREATE", "USER_EDIT", "EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_EDIT"],
  "processToggles": {
    "mfaEnforced": true,
    "ssoEnforced": false,
    "apiAccessAllowed": true,
    "auditExtended": true,
    "passwordChangeRequired": false,
    "active": true
  }
}
```

### 2. Dynamically Update Admin Process Toggles
Allows authorized admins or compliance services to update user security enforcement rules over REST.

```http
PUT /api/user/profile/{id}/process-toggles HTTP/1.1
Host: admin.mbhoni.com
Content-Type: application/json

{
  "mfaEnforced": true,
  "ssoEnforced": true,
  "apiAccessAllowed": true,
  "auditExtended": true
}
```

---

## Project Directory Structure

```text
admin-service/
├── src/
│   ├── main/
│   │   ├── java/com/mbhoni_creative/
│   │   │   ├── admincontroller/        # Spring MVC & REST API Controllers
│   │   │   │   └── api/                # Integration REST Endpoints
│   │   │   ├── admindto/               # Data Transfer Objects
│   │   │   ├── adminentity/            # JPA Data Entities
│   │   │   ├── adminrepository/        # Data Repositories
│   │   │   ├── adminservice/          # Service Layer Interfaces & Implementations
│   │   │   ├── config/                 # Security, Filter & Context Configuration
│   │   │   └── security/               # Tenant Context Holder & Security Providers
│   │   └── resources/
│   │       ├── static/                 # CSS (admin.css), JS (admin.js), WebGL scripts
│   │       ├── templates/              # Thymeleaf Views (auth, tenants, users, roles, modules)
│   │       └── application.yml         # Application Configurations
│   └── test/                           # Automated JUnit 5 & Mockito Test Suite
├── pom.xml                             # Maven Dependencies & Build Configuration
└── README.md                           # GitHub Documentation
```

---

## Getting Started & Local Setup

### Configuration and secrets

The application reads database, SMTP, admin-email, and Google OAuth credentials from environment variables. For local development, copy `.env.example` to a local `.env` file in the repository root and supply valid values; Spring Boot loads that file automatically. Alternatively, configure the variables in your IDE/runtime. Never commit actual credentials. Existing credentials that were previously stored in source control should be rotated before the next deployment.

### Database migrations

Flyway executes versioned migrations from `src/main/resources/db/migration`. On an existing database, it creates a Flyway history table at baseline version `0` and then applies the idempotent baseline migration. During the transition from legacy schema initialization, `JPA_DDL_AUTO` defaults to `update`; set it to `validate` in staging first, then production, once the live schema has been reconciled and every schema change is delivered through a Flyway migration.

If a migration fails during local development, correct the migration before retrying and remove only its failed row from `flyway_schema_history` after taking a database backup. Flyway blocks startup by design until this repair is completed; never delete successful migration-history entries.

### Prerequisites
- **JDK 21** or later installed
- **Apache Maven 3.9+** installed
- **Git**

### Installation Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/mbhoni-es/admin-service.git
   cd admin-service
   ```

2. **Build the Application**:
   ```bash
   mvn clean package -DskipTests=false
   ```

3. **Run the Automated Test Suite**:
   ```bash
   mvn test
   ```

4. **Start the Application Server**:
   ```bash
   mvn spring-boot:run
   ```

5. **Access the Portal**:
   Open your browser and navigate to: `http://localhost:8080`
   - **Default Admin Login**: `admin` / `admin123`

---

## Verification & Testing

The project maintains comprehensive test coverage across controllers, services, and tenant context isolation filters:

```text
[INFO] Results:
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## License

Copyright © 2026 MBHONI Creative Enterprise. All rights reserved.
