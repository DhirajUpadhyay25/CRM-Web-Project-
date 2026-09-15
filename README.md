<div align="center">
  <img src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png" alt="EduTake Logo" width="100">
  
  # EduTake LMS & Education CRM
  
  **An enterprise-grade, full-stack Learning Management System and CRM platform built with Spring Boot 3 & Thymeleaf.**
  
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-17_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
  [![Spring Security](https://img.shields.io/badge/Spring_Security-6.3-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)](https://spring.io/projects/spring-security)
  [![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
  [![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
  [![Render](https://img.shields.io/badge/Deploy_on-Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)](https://render.com)
  
</div>

<br />

## 📖 About EduTake

**EduTake** is a full-featured Learning Management System (LMS) combined with an Education Customer Relationship Management (CRM) platform. Designed for universities, online academies, and educational businesses, EduTake provides an end-to-end operational platform connecting administrators, instructors, students, and staff in a unified, modern web portal.

### Core Capabilities:
- 🎓 **Student Learning Portal:** Course enrollment, interactive lesson player, curriculum navigation, assignments, progress tracking, and certificate generation.
- 👨‍🏫 **Instructor Workstation:** Curriculum authoring, module & lesson management, assignment grading, batch assignments, and learner engagement metrics.
- 🛠️ **Unified Admin Control Center:** 33+ comprehensive operational subsystems including course catalog moderation, category management, financial reports, coupon engine, inquiry tracking, support tickets, dynamic RBAC permission matrices, and audit logging.
- 🔒 **Spring Security 6 RBAC:** Granular authorization, BCrypt password hashing, session fixation guards, HttpOnly cookie policies, and CSRF protection on all stateful forms.
- 💳 **Payment Engine:** Razorpay payments workflow with server-side amount calculation, coupon validation, and order reconciliation.
- 🚀 **Cloud-Native & Production Hardened:** Environment-variable-driven configuration, health check monitoring (`/health`), dynamic port binding for cloud platforms (Render/Heroku), container-safe file storage, and connection pool optimization.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Java 17, Spring Boot 3.3.1 (Spring MVC, Spring Data JPA, Spring Validation) |
| **Security Layer** | Spring Security 6.3.1 (Form Login, Method Security `@PreAuthorize`, BCrypt) |
| **Template Engine** | Thymeleaf 3.1 + Thymeleaf Extras Spring Security 6 |
| **Relational Database** | MySQL 8.x + Hibernate 6.5.2 ORM + HikariCP Connection Pooling |
| **Payments Integration** | Razorpay Java SDK 1.4.3 |
| **Observability** | REST Health Probe (`/health`), Database Audit Logger, System Error Registry |
| **Build & Packaging** | Apache Maven 3.9.9, Multi-stage Docker Alpine Container |
| **Styling & UI** | Tailwind CSS (Dark Mode, Glassmorphism), Bootstrap 5, Bootstrap Icons |

---

## 🏛️ Application Architecture

```text
[ Browser / Client ] ──HTTPS──> [ Render Cloud Router / Dynamic $PORT ]
                                                │
                                    [ Spring Security 6 ]
                                    (Auth, CSRF, RBAC Filter)
                                                │
                                      [ Spring MVC Layer ]
                      ┌─────────────────────────┼─────────────────────────┐
                      ▼                         ▼                         ▼
              [ Public / Student ]       [ Instructor ]             [ Admin CRM ]
              (Catalog, Learning)     (Course Authoring)        (33 Mgmt Subsystems)
                      └─────────────────────────┬─────────────────────────┘
                                                ▼
                                    [ Service Layer (@Transactional) ]
                                                │
                                    [ Spring Data JPA (52 Repos) ]
                                                │
                                    [ Hikari Connection Pool ]
                                                │
                                    [ MySQL 8.x Cloud Database ]
```

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- **JDK 17** or higher installed (`java -version`)
- **MySQL 8.x** running locally
- **Git**

### 1. Clone the Repository
```bash
git clone https://github.com/DhirajUpadhyay25/CRM-Web-Project-.git
cd CRM-Web-Project-
```

### 2. Configure Local Database
Create the MySQL database:
```sql
CREATE DATABASE CrmData CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Optionally create `application-secrets.properties` in the project root for local development (git-ignored):
```properties
spring.datasource.password=your_mysql_password
app.razorpay.key-id=rzp_test_placeholder
app.razorpay.key-secret=placeholder_secret
```

### 3. Build & Run
```bash
# Using Maven Wrapper
./mvnw clean package -DskipTests
java -jar target/EducationCrmProject-1.0.jar
```

Access the application in your browser:
- Landing Page: `http://localhost:8080`
- Health Probe: `http://localhost:8080/health`
- Course Catalog: `http://localhost:8080/courses`
- Login Portal: `http://localhost:8080/login`

---

## 🌐 Cloud Deployment (GitHub + Render)

EduTake is configured for zero-friction continuous deployment from **GitHub to Render**.

### 1. Deploy via Docker (Recommended)
1. In Render, select **New +** → **Web Service**.
2. Connect your repository.
3. Select **Docker** environment (Render automatically uses the multi-stage [Dockerfile](Dockerfile)).
4. Set **Health Check Path** to `/health`.
5. Supply environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATABASE_URL=jdbc:mysql://<host>:<port>/<dbname>?sslMode=REQUIRED`
   - `DATABASE_USERNAME=<user>`
   - `DATABASE_PASSWORD=<password>`
   - `SEED_ADMIN_PASSWORD=<strong_admin_password>`

For complete step-by-step instructions and free MySQL provisioning, refer to:
👉 **[DEPLOYMENT.md](DEPLOYMENT.md)**

---

## 📋 Comprehensive Documentation

- **[PRODUCTION_READINESS_REPORT.md](PRODUCTION_READINESS_REPORT.md)**: Deep-dive architecture audit, technology versions, resolved blockers, and security audit.
- **[DEPLOYMENT.md](DEPLOYMENT.md)**: Complete Render & cloud database deployment handbook.
- **[PRODUCTION_TEST_CHECKLIST.md](PRODUCTION_TEST_CHECKLIST.md)**: Verification checklist covering functional, RBAC, and operational testing.

---

## 👤 Author & Maintainer

Developed by **Dhiraj Upadhyay**  
- **GitHub:** [@DhirajUpadhyay25](https://github.com/DhirajUpadhyay25)  
- **Repository:** [CRM-Web-Project-](https://github.com/DhirajUpadhyay25/CRM-Web-Project-)
