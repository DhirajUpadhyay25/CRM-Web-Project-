# EduTake LMS — Deployment Guide (GitHub + Render)

This guide provides step-by-step instructions to deploy the **EduTake LMS** to **Render** using a free managed database.

---

## 1. Prerequisites

- **Git & GitHub Account:** Repository hosted on GitHub.
- **Render Account:** [https://render.com](https://render.com) (Free tier available).
- **Free Cloud MySQL Database:** Any MySQL 8.x cloud instance:
  - **Aiven for MySQL:** Free 5GB plan ([https://aiven.io](https://aiven.io)) — *Recommended*
  - **Clever Cloud:** Free MySQL plan ([https://clever-cloud.com](https://clever-cloud.com))
  - **Railway:** MySQL template ([https://railway.app](https://railway.app))
- **Local Tools (Optional for local testing):** Java 17+ and Maven.

---

## 2. Step 1: Provision Free Cloud MySQL Database

1. Sign up at [Aiven.io](https://aiven.io) or your preferred MySQL provider.
2. Create a new service: select **MySQL** (Version 8.x), Free plan, and your preferred region.
3. Once running, note your connection parameters:
   - **Host:** e.g., `mysql-xxxx.aivencloud.com`
   - **Port:** e.g., `12345`
   - **Database Name:** e.g., `defaultdb` or `CrmData`
   - **User:** e.g., `avnadmin`
   - **Password:** e.g., `your_secure_password`
4. Formulate your JDBC URL:
   ```text
   jdbc:mysql://<host>:<port>/<dbname>?sslMode=REQUIRED&createDatabaseIfNotExist=true
   ```
   *(For Aiven, SSL is required; for standard instances, `jdbc:mysql://<host>:<port>/<dbname>?createDatabaseIfNotExist=true` is standard).*

---

## 3. Step 2: Push Code to GitHub

From your local project directory:

```bash
git add .
git commit -m "chore: prepare Edutake for Render deployment and production hardening"
git push origin main
```

---

## 4. Step 3: Deploy on Render

### Option A: Docker Deployment (*Recommended*)

Docker provides a reproducible, lightweight Alpine Linux container running Temurin OpenJDK 17, independent of platform defaults.

1. Log in to [Render Dashboard](https://dashboard.render.com).
2. Click **New +** → **Web Service**.
3. Connect your GitHub repository: `CRM-Web-Project-`.
4. Configure the Web Service:
   - **Name:** `edutake-lms` (or your choice)
   - **Region:** Closest to your database (e.g., Oregon or Frankfurt)
   - **Branch:** `main`
   - **Runtime:** `Docker`
   - **Dockerfile Path:** `./Dockerfile`
   - **Instance Type:** `Free`
5. Configure Health Check:
   - Expand **Advanced**.
   - **Health Check Path:** `/health`
6. Add Environment Variables (see table below).
7. Click **Create Web Service**.

---

### Option B: Native Maven/Java Build

If you prefer deploying without Docker:

1. In Render, select **Runtime:** `Java`.
2. Configure build and start commands:
   - **Build Command:**
     ```bash
     mvn clean package -DskipTests
     ```
   - **Start Command:**
     ```bash
     java -Dserver.port=$PORT -Dspring.profiles.active=prod -jar target/EducationCrmProject-1.0.jar
     ```
3. Expand **Advanced** and set **Health Check Path:** `/health`.
4. Set Environment Variables and click **Create Web Service**.

---

## 5. Required Environment Variables

Configure these in the Render Dashboard under **Environment**:

| Variable Name | Description | Example / Recommended Value | Required |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Activates production configuration | `prod` | **Yes** |
| `DATABASE_URL` | JDBC connection string to cloud MySQL | `jdbc:mysql://<host>:<port>/<dbname>?sslMode=REQUIRED` | **Yes** |
| `DATABASE_USERNAME` | Database username | `avnadmin` or `root` | **Yes** |
| `DATABASE_PASSWORD` | Database password | `<your_db_password>` | **Yes** |
| `SEED_ADMIN_PASSWORD` | Password for root admin `admin@edutake.com` | `<choose_a_strong_password>` | **Recommended** |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Schema creation mode | `update` (initial deploy) | **Optional** (defaults to `update`) |
| `RAZORPAY_KEY_ID` | Razorpay Merchant Key ID | `rzp_test_xxxx` | Optional (falls back to demo placeholder) |
| `RAZORPAY_KEY_SECRET` | Razorpay Key Secret | `<secret_key>` | Optional (falls back to demo placeholder) |
| `DB_MAX_POOL_SIZE` | Max database connections for free tier | `5` | Optional (defaults to `5`) |

---

## 6. Accessing the Deployed Application

Once the build logs show:

```text
Tomcat started on port 10000 (http)
Started EducationApplication in ... seconds
Successfully initialized settings and RBAC roles...
```

1. Open the public Render URL: `https://edutake-lms.onrender.com`.
2. Access the `/health` endpoint to verify status:
   `https://edutake-lms.onrender.com/health` → `{"service":"EduTake LMS/CRM","status":"UP",...}`
3. Log in with the administrator account:
   - **Email:** `admin@edutake.com`
   - **Password:** The value specified in `SEED_ADMIN_PASSWORD`
4. Access role portals:
   - **Admin:** `https://edutake-lms.onrender.com/admin/dashboard`
   - **Student / Browse Courses:** `https://edutake-lms.onrender.com/courses`
   - **Register Student:** `https://edutake-lms.onrender.com/register`

---

## 7. Free Tier Hosting Considerations

> [!NOTE]
> **Spin-Down on Inactivity (Cold Starts):**  
> On Render's free tier, the web service spins down after 15 minutes of inactivity. The first incoming request after idle may take 30–50 seconds to wake up the container. This is normal behavior on free plans.

> [!NOTE]
> **Ephemeral Filesystem:**  
> Free tier containers on Render have ephemeral disk storage. Files uploaded directly via the browser (e.g. course thumbnails) persist during the session, but will reset upon container redeploy. For full production deployments, connect an S3 or Cloudinary external storage bucket.

---

## 8. Troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| **Port scan timeout / Deploy failed** | Application did not bind to `$PORT` | Verify `server.port=${PORT:8080}` is present in `application.properties` and `application-prod.properties`. |
| **Table 'xxx' doesn't exist** | Fresh database with `ddl-auto=validate` | Ensure `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is set in environment variables. |
| **Cannot log in as admin** | Initial admin not created | Ensure `SEED_ADMIN_PASSWORD` was supplied in Render environment variables during first boot. |
| **Too many connections to database** | Free DB connection limit exceeded | Ensure `DB_MAX_POOL_SIZE=5` is set in environment variables. |
| **Razorpay placeholder error** | Missing Razorpay keys | Default placeholders are pre-configured; verify properties are not overridden with empty strings. |
