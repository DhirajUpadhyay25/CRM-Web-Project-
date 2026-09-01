# EduTake — Admin Control Panel Audit

**Date:** 2026-09-01
**Scope:** All 33 admin routes reachable from `templates/admin/layout/admin-layout.html`, plus authentication, authorization, the purchase flow, and operational configuration.
**Method:** Static analysis of controllers, services, repositories, entities, templates, and Spring Security configuration. Every finding below was traced to a specific file and line.

---

## 1. Stack reality check

The project is Spring Boot 3.3.1 on Java 17, Maven, Thymeleaf server-side rendering, Spring Security, Spring Data JPA against MySQL, and Razorpay for payments. There is no React, no TypeScript, and no separate frontend build. Roughly 171 Java files and 120 templates. Guidance framed around SPA frontend state, DTO typing, `any` elimination, or npm lint and typecheck does not apply here; the equivalent risks in this codebase are Thymeleaf/SpEL expressions that fail only at render time, and model-attribute names that nothing verifies at compile time.

Two admin layouts coexist. The current one, `templates/admin/layout/admin-layout.html`, exposes 33 sidebar routes and is used by about 45 templates. A legacy `templates/fragments/admin-sidebar.html` exposes only 10 routes and is still used by about 9 templates. The 33-route layout is treated as the source of truth throughout this audit.

---

## 2. Module status matrix

`Data` means the page renders values actually queried from the database. `CRUD` means create, update, and delete are reachable from the UI *and* persist.

| Module | Route | Page renders | Data | CRUD | Search / filter | Authz | Status |
|---|---|---|---|---|---|---|---|
| Dashboard | `/admin/dashboard` | Yes | Real | n/a | n/a | OK | **Working** |
| Users (staff) | `/admin/users` | **500** | Real | Partial | None | OK | **Broken** |
| Students | `/admin/students` | Yes | Real | Yes | Search+filter OK, sort dead | OK | **Partial** |
| Instructors | `/admin/instructors` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Profile | `/admin/profile` | Redirect | None | None | n/a | OK | **Missing** |
| Courses | `/admin/courses` | Yes | Real | Yes | Yes | **Bypassable** | **Partial** |
| Categories | `/admin/categories` | Yes | Real | Yes | Partial | OK | **Partial** |
| Lessons | `/admin/lessons` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Batches | `/admin/batches` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Enrollments | `/admin/enrollments` | **500** | Real | **No write path** | Yes | OK | **Broken** |
| Assignments | `/admin/assignments/submissions` | Yes | Real | Grading only | Status only | OK | **Partial** |
| Certificates | `/admin/certificates` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Quizzes | — | **No route** | — | — | — | — | **Missing** |
| Orders | `/admin/orders` | Yes | Real | Refund approve/reject | Search only | OK | **Partial** |
| Payments | `/admin/payments` | Stub | Real, discarded | None | None | OK | **Broken** |
| Refunds | `/admin/refunds` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Coupons | `/admin/coupons` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Leads | `/admin/leads` | **500** | Real | Add route mismatched | Broken | OK | **Broken** |
| Enquiries | `/admin/enquiries` | **500** | Real | Respond route mismatched | Partial | OK | **Broken** |
| Follow-ups | `/admin/follow-ups` | **500** | Real | Add route mismatched | No UI | OK | **Broken** |
| Feedback | `/admin/feedback` | Yes | Real | Blocked by CSRF | None | Weak | **Partial** |
| Notifications | `/admin/notifications` | **500** | Real | **No send capability** | None | OK | **Broken** |
| Announcements | `/admin/announcements` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Messages | `/admin/messages` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Blogs | `/admin/blogs` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| FAQs | `/admin/faqs` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Pages | `/admin/pages` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Testimonials | `/admin/testimonials` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Media | `/admin/media` | Stub | Real, discarded | **No upload exists** | None | OK | **Broken** |
| Roles | `/admin/roles` | Stub | Real, discarded | Handlers unreachable | None | OK | **Broken** |
| Settings | `/admin/settings` | **Error page** | **Hardcoded** | **No persistence** | n/a | OK | **Broken** |
| Audit logs | `/admin/audit-logs` | **500** | Real but always empty | n/a | Half-wired | OK | **Broken** |
| Reports | `/admin/reports` | Yes | Real | n/a | **No filters exist** | OK | **Partial** |

Summary: 1 module working, 5 partial, 26 broken or missing.

Three modules are genuinely well-built and should be treated as the reference pattern for the rest: **Courses** (`CourseService` with `@Transactional`, `@Valid` plus `BindingResult`, publish-readiness validation, slug uniquification, six-mode server-side sort), **Students** (`CustomerService`, server-side search and filter, real statistics, safe delete guarded by enrollment existence), and **Reports** (every figure traced to a live query, zero fabricated numbers).

---

## 3. Critical findings

### 3.1 Payment verification can be tricked into granting any course

In `api/OrdersApi.java`, `verifyPayment` resolves the course from the **client-supplied** `courseName` while loading the database order **by `orderId` alone**. Nothing asserts that the two agree, and the enrollment is created against the client-supplied course. A buyer can purchase a ₹1 course legitimately, then replay the same valid `{orderId, paymentId, signature}` triple with `courseName` set to a ₹50,000 course. The HMAC signature check passes, because the signature covers only `orderId|paymentId`, and an ACTIVE/PAID enrollment is granted for the expensive course. The `Payment` row still records ₹1, so the ledger silently disagrees with the access granted.

A second path is worse. When an `orderId` is absent from the local table, a fallback branch fabricates a `COMPLETED` order at the course's list price. Because `/api/verifyPayment` is `permitAll` and CSRF-exempt, `principal` may be null, in which case the user email is taken from the request body. Any valid signature triple for an order not in the database therefore yields a completed order plus an enrollment for an attacker-chosen course and an attacker-chosen email.

Supporting weaknesses: the Razorpay payment is never fetched server-side, so nothing confirms the captured amount, currency, order linkage, or `captured` status; there is no webhook anywhere in the codebase, so a user who closes the tab after paying leaves a permanently `PENDING` order with no reconciliation path; the signature is compared with `String.equals` rather than a constant-time comparison; and no `@Transactional` wraps the order, payment, and enrollment writes, so a mid-sequence failure leaves a paid order with no course access that the idempotency guard then permanently blocks from retry.

Worth noting on the positive side: the charged amount and the coupon discount **are** correctly recomputed server-side from `course.getEffectivePrice()`. The client never sends a price. That part of the design is sound.

### 3.2 Any logged-in student can create, edit, and delete courses

`CourseController` maps legacy aliases alongside the admin paths: `/deleteCourseDetails`, `/addCourseForm`, `/updateCourse`, `/courseManagement`, and `/editCourse`. Only `/admin/**` is role-restricted in `SecurityConfig`, so these aliases fall through to `anyRequest().authenticated()` and are reachable by any authenticated student. `/deleteCourseDetails?id=N` is additionally a `GET`, so it deletes on navigation and bypasses CSRF entirely.

The root cause is that authorization is 100% URL-pattern based. There is no `@PreAuthorize`, `@Secured`, or `@RolesAllowed` anywhere in the codebase, and method security is not enabled, so any handler mapped outside `/admin/**` silently loses protection.

Related exposures in the same class of defect: `GET /api/searchInquiries` and `GET /api/myFollowUps` return raw CRM entities as JSON to any authenticated user, and `empEmail` is taken from the query string with no ownership check. `POST /api/failOrder` lets any authenticated user flip any pending order to failed by ID.

### 3.3 The data seeder corrupts real production data, over GET

`/admin/seed-test-data` and `/admin/seed-enrollments` are `GET` routes with no `@Profile` guard, so they are live in production, and both are linked as plain anchors from the settings page — a mis-click or a browser link prefetcher can fire them.

The seeder does not delete anything, and most steps are guarded by an idempotency check. Three steps are not. `seedEnrollmentsAndOrders`, `seedStudentProgress`, and `seedStudentNotifications` all iterate `userRepo.findAll()` — every real user — rather than only the fake ones. For each genuine customer with fewer than two enrollments, the seeder fabricates two to four random enrollments granting free access to courses they never bought, plus matching fake orders carrying `signature = "sig_verified"` at the real list price. Those fake orders then feed the revenue table in `/admin/reports`, which reads `ordersRepo.findAll()` and cannot distinguish them. `seedStudentProgress` goes further and issues real `Certificate` rows to users who never completed the course.

Separately, `seedAdmin()` installs `admin@edutake.com` with the password `admin123` and the ADMIN role, and `seedAll()` also triggers a bulk password rewrite across both the `Employee` and `User` tables.

### 3.4 Self-registration is silently broken

`UserController` encodes the submitted password, then hands the user to `UserService.registerUserService`, which encodes it a second time. The stored value is BCrypt applied to a BCrypt hash, so **no user who self-registers can ever log in**. Admin-created students are unaffected, because that path encodes once.

Despite its name, `LegacyPlaintextDelegatingPasswordEncoder` is BCrypt-only and never accepts or stores plaintext. The behaviour is correct; the name is misleading.

### 3.5 Committed secrets

`application.properties` commits the MySQL root password `2060` and the Razorpay key **secret** as environment-variable defaults. Because they are defaults in the base file, a production deploy that forgets to set `DB_PASSWORD` silently falls back to `root/2060`. Additionally `cookies.txt` is tracked in git and contains a live `JSESSIONID`, and `app.log`, `app_err.log`, `run_output.log`, and `run_error.log` are all tracked, with `app.log` containing lines matching `password`.

---

## 4. High-severity findings

### 4.1 Eight pages fail on every request

Each of these is a name mismatch between what the controller puts in the model and what the template reads, so the page throws during render on any non-empty result set. `GlobalExceptionHandler` catches the failure and shows a friendly error page, which is why these read as "empty" or "not working" rather than as loud 500s.

Controllers add `leads`, `enquiries`, `followUps`, `notifications`, and `auditLogs`, while the templates read `leadPage.content`, `enquiryPage.content`, `followUpPage.content`, `notificationPage.content`, and `auditLogPage.content`. The `/admin/users` template reads `emp.department`, which does not exist on `Employee`, and `emp.empRole`, where the getter is `getRole()`. The `/admin/enrollments` template reads `studentName`, `studentEmail`, and `courseName`, none of which exist on `Enrollment` — it exposes `getUser()` and `getCourse()`. And `/admin/settings` returns the view name `admin/system/settings` when the only file on disk is `admin/system/settings/list.html`.

The follow-ups template additionally contains an unbalanced SpEL expression that will not parse even once the data binding is fixed.

### 4.2 Roughly sixteen pages are generated stubs with dead buttons

Instructors, lessons, batches, certificates, payments, refunds, coupons, announcements, messages, blogs, FAQs, pages, testimonials, media, roles, and systemroles all share a near-identical generated template that renders `${item.id}` and then the literal string `Item Details Here` for every row. None of the entity fields are displayed. The Add, Edit, and Delete controls are bare `<button>` elements with no form, no `href`, no `onclick`, and no modal target.

The significant consequence is that many of these modules have **working, persisting POST handlers that are simply unreachable from the UI**. Coupons, refunds, certificates, lessons, batches, announcements, and roles all fall into this category. Wiring the templates is a larger share of the remaining work than writing backend logic.

### 4.3 Enum values compared against strings in templates

Several templates compare a JPA enum to a string literal, which always evaluates false in SpEL, so status badges never colourise. This affects `/admin/enrollments` (four comparisons) and the order detail page's enrollment badge. The correct pattern already exists in the codebase at `admin/courses/list.html`, which uses `${course.status.name() == 'PUBLISHED'}`.

### 4.4 Coupons are exploitable and uncapped

`expiryDate` is stored as a `String` and **never checked** — only `isActive` is tested. The two seeded coupons, `WELCOME50` and `FLAT500`, expired in 2024 and still grant 50% and ₹500 off today. `discountValue` is an unvalidated string, so a negative value *increases* the price, and a non-numeric value throws a 500 at checkout. There are no `usageLimit`, `usedCount`, or `minOrderAmount` fields anywhere, so a code is infinitely reusable by every user and usage is never recorded. `code` has no unique constraint, and `CouponRepository.findByCode` returns a single entity, so two rows with the same code make checkout throw. `getIsActive()` is a nullable `Boolean` that is auto-unboxed, so a NULL row throws an NPE.

Separately, `POST /api/coupons/apply` is not in the CSRF ignore list and the jQuery call attaches no token, so **every coupon application currently returns 403** and the UI reports "Invalid or expired coupon code". Coupons are effectively unusable through the real interface.

### 4.5 Revenue figures count money that was never collected

`OrdersRepository.calculateTotalRevenue()` sums `course_amount` with no status predicate, so `PENDING`, `FAILED`, and `REFUNDED` orders are all counted as revenue on both the dashboard and the orders page. `totalOrders` uses a bare `count()` with the same problem. This is compounded by `/api/failOrder` being broken by the same missing CSRF token as coupons, so abandoned checkouts are never transitioned to `FAILED` and linger as `PENDING` revenue.

The reports module joins orders to courses **by course name string**, so renaming a course orphans its historical revenue into a separate row, and malformed amounts are silently swallowed by an empty catch block.

### 4.6 Refund approval moves no money

`AdminOrderController.approveRefund` flips four database rows to refunded and cancels the enrollment, but never calls the Razorpay refund API — no `payments.refund` call exists anywhere in the repository. It also has no `@Transactional` across those four writes, and does not validate current refund state, so an already-approved refund can be re-approved repeatedly. Meanwhile `AdminRefundController.add` accepts `amount` and `status` as raw unvalidated strings with no order-existence check, and can create a second refund for an order that already has one — after which the order detail pages throw, because `findByOrderId` returns a single entity.

### 4.7 Features that appear to exist but reach nobody

The audit log infrastructure is well-built — proper columns, three indexes, a `@PrePersist` timestamp, and a service that explicitly commits to never storing secrets — and `AuditLogService.log(...)` **has zero callers**. Roughly fifty admin write handlers, including student ban and delete, course publish and delete, refund approval, and the seeder itself, record nothing. The table will always be empty.

Role management is cosmetic. `SystemRole` holds only an ID, a name, and a description, with no permissions collection and no relationship to any user. Nothing reads it at request time. It is entirely disjoint from the `Role` enum that actually drives authorization, so creating a `SystemRole` grants nothing and cannot be assigned to anyone.

Settings have no entity, no repository, and no POST handler. Every value on the page is a hardcoded Thymeleaf fallback that always fires, and nothing is ever read back or applied.

Announcement targeting is stored but never honoured — `getTargetAudience()` has no readers, and no student, instructor, or public template renders announcements at all. The same pattern holds for testimonial approval, FAQ activation, and blog and page publishing, none of which have a public renderer. `Blog` and `Page` have no body or content column, so their content can never be authored in the first place.

Notifications have no send capability. There is no admin handler that creates a notification, no audience targeting, and no fan-out. The recipient shown on the page is a hardcoded constant, `admin@edutake.com`, regardless of who is logged in. Media has no upload at all — the controller takes four string parameters, and no `MultipartFile` appears in the class.

### 4.8 Destructive actions over GET

Five routes mutate state on `GET`, which means CSRF protection does not apply and a single `<img src>` on any page an admin visits is enough to trigger them: course delete (two aliases), staff account delete, the password migration, and both seeder routes. `/logout` is also GET-mapped and therefore forcible by any third-party page.

### 4.9 Production cannot boot on a fresh database

There is no Flyway or Liquibase and no `db/migration` directory, while the prod profile sets `ddl-auto=validate`. Schema only ever exists because someone previously ran the dev profile with `ddl-auto=update` against that database. Any new entity or column shipped to prod fails schema validation at startup until the DDL is applied by hand.

### 4.10 File upload path traversal

`CourseService.saveImage` does this correctly: it validates content type against an allow-list and renames to a UUID plus a safe extension. `UserController` does not. It builds the filename as a timestamp concatenated with the original filename, with no MIME or extension check and no stripping of `/`, `\`, or `..`, then writes it directly — a path-traversal write primitive. The same pattern appears twice more in `UserController` and once in `StudentDashboardController`. Because `/upload/**` and `/uploads/**` are `permitAll`, every uploaded file is also world-readable without authentication, and an uploaded `.html` or `.svg` is served same-origin as stored XSS.

---

## 5. Medium-severity and structural findings

`GlobalExceptionHandler.handleRuntimeException` puts `ex.getMessage()` straight into the model and the error template renders it verbatim, leaking internal entity names, emails, and SQL constraint detail to the browser. The catch-all handler is also what converts the settings and audit-log defects into quiet "Something went wrong" pages instead of loud failures that would have been noticed.

`SecurityConfig` sets `accessDeniedPage("/403")` but no controller maps `/403`, so access-denied renders the 404 page and `templates/403.html` is dead.

The layout renders flash messages from `successMsg` and `errorMsg`, but the instructors, lessons, batches, and certificates controllers emit `success` and `error`, so all of their CRUD feedback is silently discarded.

Money and dates are stored as `String` across `Orders`, `Payment`, `Refund`, and `Coupon`, with zero bean-validation annotations. Every arithmetic operation requires a runtime parse or a SQL `CAST`, which is the direct root cause of the revenue-sum defect and the coupon 500s. The same untyped pattern appears in `Batch`, `Certificate`, `Announcement`, `Blog`, and `Media`. `Lesson.courseId` is a `String` while `Assignment.courseId` and `Quiz.courseId` are `Long`, forcing `String.valueOf` conversions in `LearningService`.

Validation coverage is thin. `@Valid` with `BindingResult` appears only in `CourseController`. The students and categories templates contain `#fields.hasErrors` and `th:errors` blocks that are unreachable dead code because their handlers never declare `BindingResult`, so invalid phone numbers, cities, emails, and one-character category names are persisted silently.

`@Transactional` is present throughout `CourseService` and `CategoryService` and absent from the entire purchase path, refund approval, assignment grading (three writes), and the lessons, batches, and certificates controllers.

Course delete only checks whether an order references the course *by name string*, and never consults enrollments, lessons, assignments, or quizzes, so deleting a course orphans all of its child content.

Categories cannot be deactivated, because the active checkbox is a plain `<input>` rather than `th:field`, so unchecking submits nothing and the DTO default re-activates. Category names and descriptions are interpolated raw into JavaScript `onclick` string literals, so an apostrophe breaks the page and a script tag injects.

The student sort dropdown is entirely dead: the JPQL query hardcodes `ORDER BY u.id DESC`, and Spring Data appends the `Pageable` sort *after* it, so the unique ID always wins. A sibling repository method without the hardcoded ordering exists and is never called.

Student avatars never load, because `imageName` is stored already prefixed with `/upload/` and the admin templates prepend `/uploads/` again.

Reports aggregate in application memory with a severe N+1 fan-out — `findAll()` on users, courses, orders, and quizzes, then roughly three queries per row plus one progress calculation per enrolled student per course. Revenue is summed in Java rather than with `SUM()`. The dashboard fetches ten rows via `findTop10` to display five, and swallows query failures in ten empty catch blocks, so a broken query is indistinguishable from genuinely zero data.

Quiz management has no admin surface at all: no controller, no route, no template, no sidebar entry. Instructors can create quizzes and questions, and students can attempt them, but an admin can only view aggregate quiz statistics in reports.

Orphaned and duplicate code worth removing or consolidating: `OrdersChartController` returns the dashboard view with an empty model, and `OrdersChartService` plus `OrdersChartRepository` have zero callers; `templates/admin/orders/*` are stale duplicates of `templates/admin/commerce/orders/*`; `templates/admin-profile.html` is orphaned mock data showing ₹2,00,000 in fake sales; `templates/view-feedbacks.html` and `templates/403.html` are orphaned; `/admin/systemroles` duplicates `/admin/roles` with no sidebar link. The Enquiry and Inquiry entities are parallel CRM systems where the sidebar reaches only one, as are `AdminFollowUp` and `FollowUps`.

Finally, the public site cannot feed the CRM: the contact page has no POST handler, so the enquiry inbox only ever fills from the seeder, and the public FAQ page is four hardcoded accordion items that ignore the FAQ table entirely.

---

## 6. Verification constraint

This audit is static. The sandbox used to perform it cannot build or run the project: Maven is not installed, the Maven wrapper is script-only with no jar, the local repository is empty, Maven Central is blocked by the network allow-list, and the available JDK is 11 against a project targeting 17. No dependency jars exist on disk, so even `javac` cannot resolve the Spring imports.

Every finding above is therefore derived from reading the code, cross-checking templates against controllers, and correlating with the committed `app.log`, which independently confirms the leads, enquiries, and notifications render failures. Compilation and runtime verification must happen on the Windows host with `mvnw.cmd`.

---

## 7. Recommended sequencing

**Wave 1 — stop the bleeding.** Bind the verified payment to its own order and delete the fallback order-minting branch; require authentication on `verifyPayment`; wrap order, payment, and enrollment in one transaction. Remove the non-admin course aliases and convert the GET mutations to POST. Guard the seeder behind `@Profile("dev")` and scope its three unguarded steps to seeded users only. Fix the registration double-hash. Purge secrets, `cookies.txt`, and logs from git and rotate the Razorpay key and database password.

**Wave 2 — make the panel usable.** Fix the eight render failures, which are mostly one-line model-attribute renames and yield a large visible improvement for very little risk. Correct the enum-versus-string comparisons and the flash-message key mismatches. Add the missing status filter to orders and filter revenue by status.

**Wave 3 — complete the stub modules.** Replace the sixteen `Item Details Here` templates with real tables and wire the Add, Edit, and Delete controls to the handlers that already exist. Prioritise coupons, refunds, payments, certificates, and enrollments, since those carry business consequences. Add coupon expiry enforcement, usage limits, and validation. Give enrollments a write path.

**Wave 4 — close the honest gaps.** Call `AuditLogService.log` from admin write operations. Give settings an entity and a POST handler, or remove the page. Either build a real permission model or delete the cosmetic roles UI. Add notification sending with audience targeting, or remove the claim. Introduce Flyway before the next production deploy. Add report filters and replace the N+1 aggregation with database-side queries.
