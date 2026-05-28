# SSPD Service Management System

A full-stack business management system for service shops — covering sales, purchases, inventory, service jobs, accounting, and more.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5.8 (Java 17) |
| Database | MySQL 8 (`ser_db`) |
| Authentication | JWT (RS256) |
| Frontend | Web UI served via Nginx |
| Mobile | React Native (Expo) — Android |

---

## Features

- **Sales & Invoicing** — Create sales, manage invoices, apply discounts and credits
- **Purchase Management** — Purchase orders, purchase returns, supplier tracking
- **Service Jobs** — Job cards, repair tracking, warranty management
- **Inventory & Stock** — Stock levels, shelf locations, barcode scanning
- **Customer & Supplier Management** — Full CRM with credit limits
- **Staff Management** — Staff profiles, attendance, performance reports
- **RBAC** — Role-based access control with granular permissions
- **Accounting & Journals** — Double-entry journal, profit & loss, financial statements
- **Reports & Dashboard** — Sales ranking, staff reports, summary reports
- **Booking System** — Appointment and service booking
- **Real-time Chat** — WebSocket-based chat (STOMP)
- **Backup** — Scheduled automatic database backups
- **Export & Print** — PDF/Excel export, receipt and label printing
- **Mobile App** — Android app with barcode scanner support

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Node.js 18+ (for mobile app)
- Nginx (for production deployment)

---

## Getting Started

### 1. Database Setup

```sql
CREATE DATABASE ser_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ser_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Run Backend

```bash
./mvnw spring-boot:run
```

Backend starts at `https://localhost:8080`

### 4. Production Deployment (Nginx)

```bash
# Copy nginx config
cp deploy/nginx.conf /etc/nginx/conf.d/sspd.conf

# Edit server_name in nginx.conf to match your IP/hostname
sudo nginx -t && sudo systemctl reload nginx
```

---

## Mobile App (Android)

```bash
cd mobile-app
npm install
npm start          # development
npm run android    # run on Android device/emulator
npm run build:apk  # build APK via EAS
```

---

## Project Structure

```
servicemgmt-master/
├── src/
│   └── main/
│       ├── java/org/sspd/servicemgmt/
│       │   ├── saleoptions/          # Sales & invoicing
│       │   ├── purchaseoptions/      # Purchases & returns
│       │   ├── servicejoboptions/    # Service job tracking
│       │   ├── stockoptions/         # Inventory management
│       │   ├── customeroptions/      # Customer CRM
│       │   ├── staffoptions/         # Staff management
│       │   ├── rbacoptions/          # Roles & permissions
│       │   ├── accountingoptions/    # Accounting & journals
│       │   ├── reportoptions/        # Reports & dashboard
│       │   ├── bookingoptions/       # Booking system
│       │   ├── chatoptions/          # Real-time chat
│       │   ├── backupoptions/        # Database backup
│       │   └── authoption/           # JWT authentication
│       └── resources/
│           ├── application.properties
│           └── ui/                   # Frontend web app
├── mobile-app/                       # React Native (Expo) app
├── deploy/
│   └── nginx.conf                    # Nginx reverse proxy config
└── pom.xml
```

---

## API

All API endpoints are prefixed with `/api/`. The backend runs with HTTPS and JWT authentication required on protected routes.

WebSocket endpoint: `/ws-clinic/`

---

## License

Private — SSPD Internal Use Only.
