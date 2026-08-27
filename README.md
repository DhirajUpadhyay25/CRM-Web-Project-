<div align="center">
  <img src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png" alt="EduTake Logo" width="120">
  
  # EduTake CRM & LMS Platform
  
  **A powerful, modern Education Platform + LMS + CRM built with Spring Boot.**
  
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
  [![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
  
</div>

<br />

## 📖 About The Project

**EduTake** is a comprehensive Customer Relationship Management (CRM) and Learning Management System (LMS) web application. Designed for educational businesses, it provides a centralized platform for managing courses, tracking sales, handling customer inquiries, and delivering a premium learning experience to students.

The platform is architected with a robust **Controller-Service-Repository** pattern and secured with a unified **Spring Security 6** role-based authentication system, ensuring that Admins, Employees, and Students experience tailored, secure workflows.

---

## ✨ Key Features

- 🔐 **Unified Role-Based Security**: Seamless authentication utilizing Spring Security 6 with `CustomUserDetails`, seamlessly routing `ADMIN`, `EMPLOYEE`, and `STUDENT` roles to their respective dashboards.
- 💳 **Razorpay Integration**: End-to-end secure online payment processing for seamless course purchasing and transaction tracking.
- 🎨 **Premium UI/UX**: A state-of-the-art frontend crafted with **Tailwind CSS**, featuring dark mode, glassmorphism, responsive navigation, and dynamic visual feedback.
- 📊 **Admin Dashboard**: Real-time business intelligence metrics tracking sales, course distributions, customer registrations, and overall performance.
- 👥 **Employee Management**: Dedicated workflows for staff to handle customer follow-ups, inquiries, and offline sales.
- 📚 **Course Management**: Complete CRUD operations for courses, including thumbnail uploads, pricing management, and descriptive metadata.

---

## 🛠️ Technology Stack

**Backend System**
- **Core**: Java
- **Framework**: Spring Boot 3.3.1 (Spring MVC, Spring Web)
- **Security**: Spring Security 6 (BCrypt Password Encoding, CSRF Protection)
- **Data Persistence**: Spring Data JPA / Hibernate
- **Database**: MySQL 8+
- **Build Tool**: Maven

**Frontend System**
- **Templating Engine**: Thymeleaf (with Spring Security Dialect)
- **Styling**: Tailwind CSS & Bootstrap 5
- **Icons**: Bootstrap Icons
- **Interactive UI**: Custom Vanilla JavaScript

**Third-Party Integrations**
- **Payments**: Razorpay API

---

## 🚀 Installation & Setup

Follow these steps to run EduTake locally on your machine.

### Prerequisites
- JDK 21 or higher installed
- MySQL Server installed and running
- Maven installed

### 1. Database Configuration
Create a new MySQL database named `CrmData`:
```sql
CREATE DATABASE CrmData;
```

### 2. Clone the Repository
```bash
git clone https://github.com/DhirajUpadhyay25/CRM-Web-Project-.git
cd CRM-Web-Project-
```

### 3. Application Properties
Configure your `src/main/resources/application.properties` with your MySQL credentials and Razorpay keys:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/CrmData
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# Admin Default Credentials
app.admin.email=admin@gmail.com
app.admin.password=admin123

# Razorpay Keys
app.razorpay.key-id=YOUR_RAZORPAY_KEY
app.razorpay.key-secret=YOUR_RAZORPAY_SECRET
```

### 4. Build and Run
Use Maven to build and run the Spring Boot application:
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080/`.

---

## 🏗️ Architecture overview

The application strictly follows a modular, layered MVC architecture:
- **`controllers/`**: Handles HTTP requests, manages model data, and returns Thymeleaf views.
- **`services/`**: Encapsulates core business logic and transaction management.
- **`repositories/`**: Interfaces extending `JpaRepository` for seamless database interaction.
- **`entities/`**: JPA data models (User, Course, Orders, Inquiry, etc.).
- **`security/`**: Houses the `SecurityConfig`, custom success handlers, and dynamic password encoders bridging legacy plaintext with secure BCrypt hashes.

---

## 📸 Screenshots

*(Replace these placeholder links with actual screenshots of your application)*

| Unified Login Page | Admin Dashboard |
|:---:|:---:|
| <img src="https://via.placeholder.com/600x400.png?text=Login+Page" alt="Login Page" width="400"/> | <img src="https://via.placeholder.com/600x400.png?text=Admin+Dashboard" alt="Admin Dashboard" width="400"/> |

| Course Catalog | Student Profile |
|:---:|:---:|
| <img src="https://via.placeholder.com/600x400.png?text=Course+Catalog" alt="Course Catalog" width="400"/> | <img src="https://via.placeholder.com/600x400.png?text=Student+Profile" alt="Student Profile" width="400"/> |

---

## 📝 License & Contact

Developed by **Dhiraj Upadhyay**.

- **GitHub**: [@DhirajUpadhyay25](https://github.com/DhirajUpadhyay25)
- **Project Link**: [https://github.com/DhirajUpadhyay25/CRM-Web-Project-](https://github.com/DhirajUpadhyay25/CRM-Web-Project-)
