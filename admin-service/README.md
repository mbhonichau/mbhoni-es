# MBHONI Enterprise Admin Service

> **Centralizing enterprise administration — connecting tenant management, identity, billing, and workforce operations in one platform.**

---

## 🌍 Problem Statement

Managing multiple organizations within a shared software platform introduces challenges around user access, business configuration, subscriptions, and operational oversight. Each organization needs its own workspace, branding, permissions, and employee records, while platform administrators need a central view of the system.

When these functions are spread across disconnected tools, administrators repeat tasks, maintain inconsistent records, and struggle to manage access across teams and applications.

**MBHONI Enterprise Admin Service addresses these challenges** through a centralized, multi-tenant administration platform. It brings tenant onboarding, identity management, subscriptions, billing, and employee administration into a single web interface, with REST APIs for integration into a broader microservices ecosystem.

---

## 🚀 Core Capabilities

| Feature | Description |
| --- | --- |
| **Tenant Management** | Onboard and manage organizations with tenant-specific settings, quotas, and configurations |
| **Identity & Access Control** | Manage users, roles, and permissions for platform and tenant administration |
| **Authentication** | Form-based login, Google OAuth integration, and password reset workflows |
| **Custom Branding** | Configure tenant logos, colors, and visual preferences |
| **API Key Administration** | Create, rotate, and revoke integration keys with scoped permissions |
| **Subscriptions & Modules** | Manage subscription plans and configure tenant access to platform modules |
| **Billing & Finance** | Administer billing accounts, expenses, invoices, and quotations |
| **Employee Management** | Maintain employee records, configurable fields, and completeness reports |
| **Payslips & Documents** | Generate payslips and export business documents |
| **Organization Management** | Structure organizational units and maintain shared reference data |
| **Contracts & Services** | Manage contracts and service catalog records |
| **Integration APIs** | Expose user profiles and administrative data to connected applications |

---

## 🛠️ Tech Stack

| Layer | Technology |
| --- | --- |
| **Backend** | Java 25, Spring Boot 3.5 |
| **Web Interface** | Thymeleaf, Bootstrap, JavaScript, CSS |
| **Security** | Spring Security, OAuth 2.0 client integration, role-based permissions |
| **Persistence** | Spring Data JPA, Hibernate |
| **Database** | MySQL; H2 for tests |
| **Database Migrations** | Flyway |
| **Service Discovery** | Spring Cloud Netflix Eureka Client |
| **Document Processing** | Apache POI |
| **Testing** | JUnit, Mockito, Spring Boot Test, Spring Security Test |
| **Build & Packaging** | Maven, Docker |

---

## 🏗️ Application Design

The service uses a layered architecture to separate web requests, business rules, and data persistence.

- **Controllers** handle administration pages and REST endpoints.
- **Services** implement tenant, identity, billing, and employee workflows.
- **Repositories and entities** manage database access and domain relationships.
- **Security components** establish tenant context and enforce access rules.
- **Thymeleaf templates** deliver the administration interface.

Within the wider MBHONI ES project, admin-service provides the central administration capabilities and registers with Eureka for service discovery.

---

## 📁 Project Structure

```text
admin-service/
├── src/
│   ├── main/
│   │   ├── java/com/mbhoni_creative/
│   │   │   ├── admincontroller/    # Web controllers and REST endpoints
│   │   │   ├── admindto/           # Request and response objects
│   │   │   ├── adminentity/        # Domain entities
│   │   │   ├── adminrepository/    # Database repositories
│   │   │   ├── adminservice/       # Business logic and implementations
│   │   │   ├── config/             # Application and security configuration
│   │   │   └── security/           # Tenant context and validation
│   │   └── resources/
│   │       ├── templates/          # Administration pages
│   │       ├── static/             # Styles, scripts, and assets
│   │       ├── db/migration/       # Flyway migrations
│   │       └── application.yml     # Runtime configuration
│   └── test/                      # Controller, service, and repository tests
├── .env.example                   # Environment configuration template
├── Dockerfile                     # Container packaging
└── pom.xml                        # Dependencies and build configuration
```

---

## 💡 Engineering Highlights

- **Multi-tenant application design:** tenant context, scoped administration, and configurable organization settings.
- **Permission-based workflows:** user roles and permissions integrated into administrative operations.
- **Configurable business data:** dynamic employee fields and tenant-specific onboarding requirements.
- **Schema evolution:** versioned Flyway migrations alongside existing database structures.
- **Automated verification:** tests covering tenant access, repository isolation, controllers, and business services.
- **Integration readiness:** REST endpoints and service discovery for communication with other applications.

---

## ⚙️ Getting Started

See the [project setup guide](../README.md#local-development) for prerequisites, environment variables, database setup, build commands, and local startup instructions. The [configuration and Docker notes](../README.md#configuration-and-docker) describe the current deployment requirements.

---

> *Built to simplify enterprise administration and give organizations a configurable foundation for managing their people, access, and business operations.*
