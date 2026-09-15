# EduTake LMS — Production Test Checklist

This checklist is designed for developers, interviewers, and evaluators to systematically verify all core capabilities of **EduTake LMS** after deployment.

---

## 1. Infrastructure & Platform Health

- [ ] **Application Startup:** Service deploys without fatal exceptions in Render logs.
- [ ] **Port Binding:** Tomcat successfully binds to `${PORT}` dynamically allocated by Render.
- [ ] **Database Connectivity:** HikariCP establishes connections to the managed MySQL database.
- [ ] **HTTPS Verification:** The application loads securely over `https://<service-name>.onrender.com`.
- [ ] **Health Endpoint:** `GET /health` returns HTTP 200 with JSON payload:
  ```json
  {
    "service": "EduTake LMS/CRM",
    "status": "UP"
  }
  ```
- [ ] **Static Assets Delivery:** CSS (`/css/**`), JS (`/js/**`), and icons load without 404s or MIME type warnings.
- [ ] **Custom Error Pages:** Navigating to an invalid URL (e.g. `/not-a-real-page`) renders the styled `404.html` error page without exposing stack traces.

---

## 2. Authentication & Account Security

- [ ] **Student Registration:**
  - Visit `/register`.
  - Fill out name, email, phone, and password.
  - Submit form; verify user is registered and redirected to `/login`.
  - Verify password in database is hashed with BCrypt (starts with `$2a$` or `$2b$`).
- [ ] **Student Login:**
  - Enter student credentials at `/login`.
  - Verify successful authentication and redirection to `/student/dashboard`.
- [ ] **Admin Login:**
  - Enter `admin@edutake.com` and the `SEED_ADMIN_PASSWORD` at `/login`.
  - Verify successful authentication and redirection to `/admin/dashboard`.
- [ ] **Failed Login Handling:**
  - Attempt login with invalid password.
  - Verify error message displays gracefully on the login page.
- [ ] **Logout Flow:**
  - Click Logout button.
  - Verify session is invalidated and user is redirected to `/login?logout`.

---

## 3. Role-Based Access Control (RBAC)

- [ ] **Unauthenticated Access Denial:**
  - Open an incognito window.
  - Navigate to `/admin/dashboard` or `/student/dashboard`.
  - Verify automatic redirect to `/login`.
- [ ] **Student Access Boundary:**
  - Log in as a Student.
  - Attempt to navigate to `/admin/dashboard` or `/admin/courses`.
  - Verify access is blocked with HTTP 403 / styled `403.html` Access Denied page.
- [ ] **Instructor Access Boundary:**
  - Attempt to access administrative controls (`/admin/settings`, `/admin/roles`) from an Instructor account.
  - Verify unauthorized access is blocked.
- [ ] **CSRF Protection:**
  - Inspect any POST form (login, course create, feedback).
  - Verify `_csrf` token is present in the form payload.

---

## 4. Core LMS Functional Workflows

### Course Catalog & Discovery
- [ ] Public catalog loads at `/courses`.
- [ ] Course cards display title, thumbnail, instructor name, and price badge.
- [ ] Search and category filter work on the courses catalog.
- [ ] Course details page loads at `/courses/{slug}` or `/courses/{id}`.

### Enrollment & Learning Experience
- [ ] Logged-in student can enroll in a course.
- [ ] Enrolled courses appear on the student dashboard (`/student/dashboard` or `/myCourses`).
- [ ] Student can open the learning room (`/student/learning/{courseId}`).
- [ ] Video/lesson curriculum navigation renders lessons and allows progress updates.

### Admin Dashboard & Management
- [ ] Admin dashboard displays live system statistics (total courses, users, inquiries).
- [ ] Course management (`/admin/courses`) lists existing courses with status badges.
- [ ] Category management (`/admin/categories`) allows creating and listing categories.
- [ ] Student management (`/admin/students`) lists active students with search filters.
- [ ] Role management (`/admin/roles`) displays system roles and permission matrices.
- [ ] Audit logs (`/admin/audit-logs`) record system login and administration activities.

---

## 5. Security & Error Handling Smoke Test

- [ ] **SQL Injection Guard:** Repositories use parameterized JPA/Spring Data queries.
- [ ] **XSS Sanitization:** Thymeleaf escapes HTML expressions (`th:text`) by default.
- [ ] **Cookie Flags:** Inspect cookie `JSESSIONID` in browser DevTools:
  - `HttpOnly`: Checked
  - `SameSite`: Lax
- [ ] **Zero Secret Leakage:** `curl https://<app-url>/health` and public endpoints do not expose environment variables, passwords, or connection strings.
