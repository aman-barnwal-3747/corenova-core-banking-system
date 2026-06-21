# 🏛️ CoreNova Bank — Core Banking System

> Enterprise-grade Core Banking System inspired by **Finacle / ICICI iCore**, built with **Spring Boot 3, Java 17, React, PostgreSQL, Redis, and JWT**.

Full customer lifecycle · multi-account operations · UPI/NEFT/RTGS/IMPS payment engine · maker-checker approvals · KYC workflows · RBAC · audit logging · loan management.

---

## 📐 Architecture

```
React Frontend  →  Spring Boot REST API  →  PostgreSQL
   (Vite, 3000)        (8080/api)            (5432)
                            │
                            ├──→ Redis (6379)      — caching
                            └──→ logs/*.json       — ELK pipeline
```

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, JWT (jjwt) |
| Frontend | React 18, Vite, React Router, Recharts |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Docs | Swagger / OpenAPI 3 |
| Logging | Logback + Logstash encoder (ELK-ready) |
| DevOps | Docker, Docker Compose |

---

## 🚀 Quick Start (Docker — Recommended)

```bash
git clone <repo-url> corenova-bank
cd corenova-bank
cp .env.example .env          # edit secrets as needed

docker compose up -d --build
```

| Service | URL |
|---|---|
| Frontend (Dashboard) | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| pgAdmin (optional) | `docker compose --profile dev up -d` → http://localhost:5050 |

Stop everything: `docker compose down` (add `-v` to also wipe DB/Redis volumes).

---

## 🔑 Demo Credentials

Seeded automatically by `DataInitializer` on first backend startup.

| Username | Password | Role | Access |
|---|---|---|---|
| `admin` | `Admin@123` | ADMIN | Full system, Users & Roles, Audit Logs |
| `manager` | `Manager@123` | MANAGER | Approvals (checker), reports, account management |
| `teller` | `Teller@123` | TELLER | Transactions, account opening, KYC submission |
| `auditor` | `Audit@123` | AUDITOR | Read-only audit logs & reports |

Demo customers/accounts: `1001000001` (Savings, Aman Verma), `1001000002` (Current, Priya Sharma), `1001000003` (Salary, Ravi Kumar).

---

## 🛠️ Manual Setup (Without Docker)

### Prerequisites
- Java 17+, Maven 3.9+
- Node.js 20+, npm
- PostgreSQL 16 running locally
- Redis 7 running locally

### 1. Database
```sql
CREATE DATABASE corenova_bank_db;
CREATE USER corenova_user WITH PASSWORD 'Corenova@123';
GRANT ALL PRIVILEGES ON DATABASE corenova_bank_db TO corenova_user;
```

### 2. Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
# → http://localhost:8080/api  |  Swagger: /swagger-ui.html
```
Config: `backend/src/main/resources/application.yml`. Tables auto-created via `ddl-auto: update`; demo data seeded on first run.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
# → http://localhost:3000  (proxies /api to localhost:8080)
```

---

## 📦 Project Structure

```
corenova-bank/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/corenova/bank/
│       │   ├── controller/      → REST endpoints (Auth, Account, Transaction,
│       │   │                       Customer, Loan, Beneficiary, Approval, User,
│       │   │                       Dashboard, AuditLog)
│       │   ├── service/         → service interfaces
│       │   ├── serviceimpl/     → business logic (Tx engine, Loans, KYC, MakerChecker)
│       │   ├── repository/      → Spring Data JPA repositories
│       │   ├── entity/          → JPA entities (User, Customer, Account,
│       │   │                       Transaction, Loan, Beneficiary, ApprovalWorkflow,
│       │   │                       AuditLog)
│       │   ├── dto/{request,response}/
│       │   ├── security/{jwt,config}/  → JWT provider, filter, SecurityConfig
│       │   ├── config/          → CacheConfig, OpenApiConfig, AsyncConfig,
│       │   │                       ScheduledJobs (EOD batch), DataInitializer
│       │   ├── exception/       → GlobalExceptionHandler
│       │   ├── audit/           → AuditLogService (async)
│       │   └── enums/           → Role, AccountType/Status, TransactionType/Status,
│       │                           KycStatus, ApprovalStatus
│       └── resources/
│           ├── application.yml
│           └── logback-spring.xml
│
├── frontend/
│   ├── Dockerfile, nginx.conf
│   └── src/
│       ├── api/index.js         → Axios client + all service modules
│       ├── hooks/useAuth.jsx     → Auth context
│       ├── components/layout/   → Sidebar, AppShell (TopBar)
│       ├── pages/                → 17 pages (see below)
│       └── styles/global.css
│
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🧭 Feature Map (Sidebar → Module)

| Sidebar Item | Page | Backend Controller | PDF Section |
|---|---|---|---|
| Dashboard | `DashboardPage` | `DashboardController` | §3 Architecture |
| Accounts | `AccountsPage` | `AccountController` | §6 Account Management |
| Transactions | `TransactionsPage` | `TransactionController` | §7 Transaction Engine |
| Fund Transfer | `FundTransferPage` | `TransactionController` | §7 Transaction Engine |
| Beneficiaries | `BeneficiariesPage` | `BeneficiaryController` | §8 Payment Gateway |
| Payments | `PaymentsPage` | `TransactionController` (UPI/NEFT/RTGS/IMPS) | §8 Payment Gateway |
| Loans | `LoansPage` | `LoanController` | Advanced module |
| Cards | `CardsPage` | — (UI demo) | — |
| Investments | `InvestmentsPage` | — (UI demo) | — |
| Reports | `ReportsPage` | composed from Dashboard/Loan/Audit APIs | MIS |
| Approvals | `ApprovalsPage` | `ApprovalController` | §9 Maker-Checker |
| Customers | `CustomersPage` | `CustomerController` | §5 Customer Module |
| Users & Roles | `UsersRolesPage` | `UserController` | §4 Roles (RBAC) |
| Audit Logs | `AuditLogsPage` | `AuditLogController` | §10 Audit Logging |
| Settings | `SettingsPage` | `AuthController` (password) | §4 Auth Module |
| Support | `SupportPage` | — (UI) | — |

---

## 🔐 Security (PDF §11)

- **JWT** stateless auth — `Authorization: Bearer <token>` (24h access / 7d refresh)
- **BCrypt** (strength 12) password hashing
- **RBAC** via `@PreAuthorize` — roles: `ADMIN`, `MANAGER`, `TELLER`, `AUDITOR`, `CUSTOMER`
- **Maker-Checker** four-eyes principle for high-value ops (≥ ₹5L, freezes, KYC approval, loan sanction) — 24h SLA, auto-expiry
- **Account lockout** after 5 failed logins
- **Pessimistic row locking** on transfer source accounts (prevents double-spend)

---

## 💸 Payment Rails (PDF §8)

| Rail | Limit | Settlement |
|---|---|---|
| Internal Transfer | None | Instant |
| UPI | ₹1,00,000/txn | Instant |
| NEFT | None | ~30-min batches (auto-settled by scheduler) |
| RTGS | Min ₹2,00,000 | Real-time |
| IMPS | ₹5,00,000/txn | Instant, 24×7 |

24-hour beneficiary cooling period enforced per RBI fraud-prevention guidelines.

---

## ⏱️ Scheduled Batch Jobs (`ScheduledJobs.java`)

| Job | Schedule | Purpose |
|---|---|---|
| Daily limit reset | 00:00 | Resets `dailyAmountUsedToday` |
| Interest posting | 23:30 | Quarter-end interest credit on savings |
| Dormancy check | 01:00 | Flags accounts inactive 12+ months |
| NEFT settlement | Every 30 min | Moves PROCESSING → SUCCESS |
| NPA classification | 02:00 | RBI IRAC norms on overdue loans |
| Approval SLA expiry | Hourly | PENDING → EXPIRED after 24h |

---

## 📊 Observability (PDF §15)

- **Swagger UI**: `/api/swagger-ui.html` — JWT "Authorize" button built in
- **Actuator**: `/api/actuator/health`
- **Structured JSON logs**: `backend/logs/corenova-bank.json` + dedicated `corenova-bank-audit.json` (7-year retention) — ready for Filebeat → Logstash → Elasticsearch → Kibana

---

## 🧪 API Testing

1. `POST /api/auth/login` with demo credentials → copy `accessToken`
2. Open Swagger UI → **Authorize** → paste `Bearer <token>`
3. Try `GET /api/dashboard`, `POST /api/transactions/transfer`, etc.

---

## 📄 License

Proprietary — CoreNova Bank. Built as an educational/portfolio reference implementing Finacle/iCore-inspired CBS architecture (PDF Phases 1–6 complete).
