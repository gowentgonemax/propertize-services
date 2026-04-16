# Propertize RBAC — Role Permissions Reference

> **Source of truth**: `auth-service/src/main/resources/rbac.yml`  
> Changes must be applied to all three copies: `auth-service`, `api-gateway`, `propertize`.  
> The auth-service re-seeds roles on startup; restart the service after any rbac.yml edit.

---

## Role Hierarchy Overview

| Role                   | Level | Category     | Scope        | Org Types |
| ---------------------- | ----: | ------------ | ------------ | --------- |
| PLATFORM_OVERSIGHT     |  1000 | platform     | platform     | All       |
| EMERGENCY_ACCESS       |   999 | special      | platform     | All       |
| PLATFORM_OPERATIONS    |   970 | platform     | platform     | All       |
| PLATFORM_ENGINEERING   |   950 | platform     | platform     | All       |
| PLATFORM_ANALYTICS     |   930 | platform     | platform     | All       |
| PORTFOLIO_OWNER        |   920 | portfolio    | portfolio    | All       |
| ORGANIZATION_OWNER     |   900 | organization | organization | All       |
| SOLO_OWNER             |   870 | organization | organization | IPO       |
| HOA_DIRECTOR           |   890 | organization | organization | HA        |
| CFO                    |   890 | organization | organization | CORP      |
| ORGANIZATION_ADMIN     |   850 | organization | organization | All       |
| HR_MANAGER             |   820 | organization | organization | CORP, PMC |
| PROPERTY_MANAGER       |   800 | organization | organization | All       |
| OPERATIONS_MANAGER     |   780 | organization | organization | CORP, PMC |
| LEASE_SPECIALIST       |   750 | organization | team         | All       |
| PORTFOLIO_ANALYST      |   750 | organization | organization | REI, CORP |
| INVESTOR_RELATIONS     |   730 | organization | organization | REI, CORP |
| OWNER_RELATIONS        |   700 | organization | organization | PMC       |
| LEASING_AGENT          |   700 | organization | team         | All       |
| ACCOUNTANT             |   650 | organization | team         | All       |
| COMMUNITY_MANAGER      |   650 | organization | organization | HA        |
| MAINTENANCE_SUPERVISOR |   600 | organization | team         | All       |
| BOARD_MEMBER           |   600 | organization | organization | HA        |
| TEAM_LEAD              |   500 | organization | team         | All       |
| CASE_WORKER            |   550 | organization | self         | HA        |
| INSPECTOR              |   450 | operational  | team         | All       |
| MAINTENANCE_TECHNICIAN |   400 | operational  | team         | All       |
| TEAM_MEMBER            |   300 | operational  | team         | All       |
| VENDOR                 |   200 | external     | self         | All       |
| TENANT                 |   150 | external     | self         | All       |
| APPLICANT              |   100 | external     | self         | All       |
| READ_ONLY              |    50 | external     | self         | All       |

> **Org Type Keys**: IPO = Individual Property Owner, HA = Housing Association, REI = Real Estate Investor, CORP = Corporate, PMC = Property Management Company

---

## Platform Roles

### PLATFORM_OVERSIGHT (Level 1000)

_Platform-wide read and monitor access across all organizations._

**CAN do:**

- Full read/list across all resources (users, organizations, properties, leases, tenants, payments, reports, etc.)
- Read employee and payroll data
- Read financial data (payments, invoices, payroll)
- Read milestones, tasks, documents, notifications
- View all dashboards and audit logs
- Access analytics and reporting
- View maintenance, vendor, and contact data

**CANNOT do:**

- Create, update, or delete users, organizations, properties (read-only)
- Manage payroll or approve payments
- System admin operations (configure, backup, restore)

**Explicit Denials**: ORGANIZATION_CREATE/DELETE/MANAGE, USER_CREATE/UPDATE/DELETE, PROPERTY_CREATE/UPDATE/DELETE/MANAGE, TENANT_CREATE/UPDATE/DELETE/MANAGE, MAINTENANCE_CREATE/UPDATE/DELETE, PAYMENT_CREATE/UPDATE/DELETE/MANAGE, PAYROLL_MANAGE/PROCESS/APPROVE, SYSTEM_ADMIN

---

### PLATFORM_OPERATIONS (Level 970)

_Manage platform operations and help organizations with day-to-day issues._

**CAN do:**

- Read employee data (EMPLOYEE_READ, EMPLOYEE_LIST)
- Read organizations and users
- Standard operational read access

**CANNOT do:**

- Full financial management, payroll management, system admin

---

### PLATFORM_ENGINEERING (Level 950)

_System infrastructure and monitoring._

**CAN do:**

- System monitoring, configure, backup, restore, update, troubleshoot

**CANNOT do:**

- Business operations (property/tenant/lease management)

---

### PLATFORM_ANALYTICS (Level 930)

_Business intelligence and reporting._

**CAN do:**

- Full report access (generate, read, export, customize)
- Read employee and payroll for reporting
- Dashboard and audit log reads

---

## Portfolio / Multi-Org Roles

### PORTFOLIO_OWNER (Level 920)

_Multi-organization investor — full business operations across all owned organizations._

**CAN do:**

- Full organization management (create, configure, delete owned orgs)
- Full property management
- Full lease and tenant management
- Full payroll management (create, process, approve)
- Full employee management
- Full financial management (payments, invoices)
- Full reporting and dashboards
- Full milestone and task management
- Full document, vendor, contact management

**CANNOT do:**

- System admin (SYSTEM_ADMIN, SYSTEM_CONFIGRUE etc.)
- Platform-level access

**Explicit Denials**: SYSTEM_ADMIN, ORGANIZATION_DELETE (top-level), SYSTEM_CONFIGURE

---

## Organization Roles

### ORGANIZATION_OWNER (Level 900)

_Single-organization landlord — property & tenant management._

**CAN do:**

- Read/update own organization
- Full user management within org (invite, manage)
- Full property management
- Full lease management
- Full tenant management
- Payment monitoring (READ, LIST only)
- Invoice monitoring (READ, LIST only)
- Full maintenance management
- Generate and read reports
- Full milestone management ✓ _(added)_
- Full task management
- Full vendor, contact, document management
- Manage rental applications (list, read, approve, reject, review)
- View employees (READ, LIST) ✓ _(added)_
- Audit log read, notifications

**CANNOT do:**

- Create/delete organizations
- Create, update, delete, or manage employees (view only)
- Create, update, delete, or process/approve/manage payroll
- System admin

**Explicit Denials**: EMPLOYEE_CREATE/UPDATE/DELETE/MANAGE, PAYROLL_CREATE/UPDATE/DELETE/PROCESS/APPROVE/MANAGE, ORGANIZATION_CREATE/DELETE

---

### ORGANIZATION_ADMIN (Level 850)

_Day-to-day org administrator._

**CAN do:**

- Organization read
- Create/read/update/list/invite users
- Full property, lease, tenant, maintenance management
- Payment and invoice read/management
- Generate/read reports
- Full milestone management ✓ _(added)_
- View employees (READ, LIST) ✓ _(added)_
- Full task, vendor, contact, document management
- Notification read

**CANNOT do:**

- Full employee management (view only)
- Payroll management
- Delete organization

**Explicit Denials**: EMPLOYEE_MANAGE, PAYROLL_MANAGE, ORGANIZATION_DELETE

---

### PROPERTY_MANAGER (Level 800)

_Property portfolio manager — properties, leases, tenants, maintenance._

**CAN do:**

- Full property, lease, tenant, maintenance management
- Payment monitoring (READ, LIST)
- Invoice create, read, send
- Employee read and list
- Generate/read reports
- Task management
- Milestone read/list/create/update ✓ _(added)_
- Vendor, document, notification management

**CANNOT do:**

- User management, payroll, employee creation, organization management

---

### SOLO_OWNER (Level 870)

_Individual landlord with 1–3 properties, no staff. Simplified IPO role._

**CAN do:**

- Full property management (small portfolio)
- Lease and tenant management
- Payment and invoice monitoring
- Maintenance management
- Milestone management ✓ _(added)_
- Document, vendor, task management
- Own profile management

**CANNOT do:**

- Employee management, payroll, multi-org operations
- User creation or management within org

**Org Types**: INDIVIDUAL_PROPERTY_OWNER only  
**Explicit Denials**: EMPLOYEE_CREATE/UPDATE/DELETE/MANAGE, PAYROLL_CREATE/UPDATE/DELETE/PROCESS/APPROVE/MANAGE, USER_MANAGE, ORGANIZATION_CREATE/DELETE/UPDATE

---

## Functional Specialist Roles

### LEASE_SPECIALIST (Level 750)

**CAN do:** Full lease management, property/tenant read, documents, tasks  
**CANNOT do:** Property create, user management, financial management

### LEASING_AGENT (Level 700)

**CAN do:** Lease/tenant/property read, maintenance read, rental application management, notifications, tasks  
**CANNOT do:** Create properties, financial management, user management

### ACCOUNTANT (Level 650)

**CAN do:** Full financial operations — payment CRUD, invoice CRUD, payroll READ, report generation, employee read, expense management  
**CANNOT do:** Property/lease/tenant management, user management

### MAINTENANCE_SUPERVISOR (Level 600)

**CAN do:** Full maintenance management (all lifecycle), property read, task management, vendor management  
**CANNOT do:** Lease/tenant/payment management, user management

### TENANT_COORDINATOR (Level 550)

**CAN do:** Full tenant management, lease read, maintenance read, rental application management, contact management, tasks, notifications  
**CANNOT do:** Property create/update, financial management, user management

### TEAM_LEAD (Level 500)

**CAN do:** Tenant/property/lease read, maintenance management, task management, report read, notifications, employee read  
**CANNOT do:** Financial management, user management, org configuration

### INSPECTOR (Level 450)

**CAN do:** Property/lease/tenant/maintenance read, report read, document management, task read, notifications  
**CANNOT do:** Write operations on most resources, financial management

### MAINTENANCE_TECHNICIAN (Level 400)

**CAN do:** Maintenance read/update (own tasks), property/document read  
**CANNOT do:** Most management operations

### TEAM_MEMBER (Level 300)

**CAN do:** Basic read access — property/tenant/lease/maintenance read, task read, notifications  
**CANNOT do:** Write operations on any business resource

---

## Org-Type Specific Roles

### HOA_DIRECTOR (Level 890) — Housing Association only

_Board chair/director — governance, finance, community management._  
**CAN do:** Full org management, financial management, property/tenant/lease/maintenance management, milestone/task management, reports

### BOARD_MEMBER (Level 600) — Housing Association only

_Governance read-access and voting._  
**CAN do:** Organization/property/lease/tenant/maintenance read, document management, task management, milestone read/list  
**CANNOT do:** Financial writes, user management, operational changes

### COMMUNITY_MANAGER (Level 650) — Housing Association only

_Resident welfare and social programs._  
**CAN do:** Full tenant/maintenance management, lease/property read, contact/notification management, task/milestone management  
**CANNOT do:** Financial management, payroll

### CASE_WORKER (Level 550) — Housing Association only

_Individual resident support cases._  
**CAN do:** Tenant/lease/property read, maintenance/document/task management, milestone read/list, report read  
**CANNOT do:** Financial management, user management

### CFO (Level 890) — Corporate only

_Full financial control; payroll approval._  
**CAN do:** Full payroll management, full financial management, employee management, reporting, milestone/task management  
**Org Types**: CORPORATE only

### HR_MANAGER (Level 820) — CORP / PMC

_Employee lifecycle and payroll management._  
**CAN do:** Full employee management, payroll CRUD, user management, task/milestone management, reporting  
**Explicit Denials**: PROPERTY_MANAGE, LEASE_MANAGE  
**Org Types**: CORPORATE, PROPERTY_MANAGEMENT_COMPANY

### OPERATIONS_MANAGER (Level 780) — CORP / PMC

_Property and maintenance operations._  
**CAN do:** Full property/maintenance/vendor management, full employee management, task/milestone management, lease/tenant/payment read  
**Org Types**: CORPORATE, PROPERTY_MANAGEMENT_COMPANY

### INVESTOR_RELATIONS (Level 730) — REI / CORP

_Owner/investor reporting and communication._  
**CAN do:** Report generate/read/export, financial/property read, dashboard access, notifications  
**CANNOT do:** Write operations on properties/leases/tenants  
**Org Types**: REAL_ESTATE_INVESTOR, CORPORATE

### PORTFOLIO_ANALYST (Level 750) — REI / CORP

_Investment analytics and ROI._  
**CAN do:** Full report access, financial/property/tenant/lease read, dashboard, analytics, notifications  
**CANNOT do:** Write operations on business data  
**Org Types**: REAL_ESTATE_INVESTOR, CORPORATE

### OWNER_RELATIONS (Level 700) — PMC only

_Owner portal and property owner reporting._  
**CAN do:** Property/lease/tenant/payment/invoice/maintenance read, document/task/notification/contact management, milestone read/list  
**CANNOT do:** Employee management, payroll, write operations on core business data  
**Org Types**: PROPERTY_MANAGEMENT_COMPANY

---

## External / Self-Service Roles

### VENDOR (Level 200)

_External vendor portal access._  
**CAN do:** Maintenance read, document read/upload, task read, own profile  
**CANNOT do:** Access any organization data, user management

### TENANT (Level 150)

_Tenant self-service portal._  
**CAN do:** Own lease/payment/maintenance read, submit maintenance requests, manage own contact info, read notifications, document read/upload, view rental applications (own)  
**CANNOT do:** Access other tenants' data, organization management

### APPLICANT (Level 100)

_Rental application submission._  
**CAN do:** Submit/read own rental application, document upload, notifications  
**CANNOT do:** Any organization data

### READ_ONLY (Level 50)

_Observer/auditor — read-only._  
**CAN do:** Most read operations across the platform  
**CANNOT do:** Create, update, or delete anything

---

## Permission Groups (Composite Permissions)

| Group                | Includes                                                            |
| -------------------- | ------------------------------------------------------------------- |
| USER_MANAGE          | USER_CREATE/READ/UPDATE/DELETE/LIST/INVITE/SUSPEND/ACTIVATE         |
| ORGANIZATION_MANAGE  | ORGANIZATION_CREATE/READ/UPDATE/DELETE/LIST/CONFIGURE               |
| PROPERTY_MANAGE      | PROPERTY_CREATE/READ/UPDATE/DELETE/LIST/ASSIGN/ARCHIVE              |
| LEASE_MANAGE         | LEASE_CREATE/READ/UPDATE/DELETE/LIST/SIGN/TERMINATE/RENEW           |
| TENANT_MANAGE        | TENANT_CREATE/READ/UPDATE/DELETE/LIST/COMMUNICATE/SCREEN            |
| PAYMENT_MANAGE       | PAYMENT_CREATE/READ/UPDATE/DELETE/LIST/PROCESS                      |
| MAINTENANCE_MANAGE   | MAINTENANCE_CREATE/READ/UPDATE/DELETE/LIST/ASSIGN/COMPLETE/SCHEDULE |
| INVOICE_MANAGE       | INVOICE_CREATE/READ/UPDATE/DELETE/LIST/SEND/MARK_PAID               |
| EMPLOYEE_MANAGE      | EMPLOYEE_CREATE/READ/UPDATE/DELETE/LIST                             |
| PAYROLL_MANAGE       | PAYROLL_CREATE/READ/UPDATE/DELETE/LIST/PROCESS/APPROVE              |
| TASK_MANAGE          | TASK_CREATE/READ/UPDATE/DELETE/LIST                                 |
| **MILESTONE_MANAGE** | **MILESTONE_CREATE/READ/UPDATE/DELETE/LIST**                        |
| VENDOR_MANAGE        | VENDOR_CREATE/READ/UPDATE/DELETE/LIST                               |
| DOCUMENT_MANAGE      | DOCUMENT_CREATE/READ/UPDATE/DELETE/LIST/UPLOAD/DOWNLOAD             |
| REPORT_FULL          | REPORT_CREATE/READ/UPDATE/DELETE/LIST/GENERATE/EXPORT/CUSTOMIZE     |
| SYSTEM_ADMIN         | SYSTEM_CONFIGURE/MONITOR/BACKUP/RESTORE/UPDATE/TROUBLESHOOT         |

---

## Dashboard Page Access Summary

| Dashboard Page       | Minimum Role Required                                    |
| -------------------- | -------------------------------------------------------- |
| Dashboard Home       | All authenticated roles                                  |
| Properties           | LEASING_AGENT+                                           |
| Tenants              | LEASING_AGENT+                                           |
| Leases               | LEASING_AGENT+                                           |
| Rental Applications  | LEASING_AGENT+                                           |
| Maintenance          | MAINTENANCE_TECHNICIAN+                                  |
| Payments             | ACCOUNTANT+                                              |
| Invoices             | ACCOUNTANT+                                              |
| Promo Codes (view)   | ORGANIZATION_OWNER+ (PAYMENT_READ or INVOICE_READ)       |
| Promo Codes (manage) | ORGANIZATION_ADMIN+ (PAYMENT_MANAGE or INVOICE_MANAGE)   |
| Milestones           | PROPERTY_MANAGER+ _(fixed — was causing 403)_            |
| Payroll              | ACCOUNTANT+                                              |
| Payroll / Employees  | ORGANIZATION_ADMIN+                                      |
| Employees (HR)       | ORGANIZATION_OWNER+ _(fixed — EMPLOYEE_READ/LIST added)_ |
| Reports              | ACCOUNTANT+                                              |
| Users                | ORGANIZATION_OWNER+                                      |
| Documents            | TEAM_MEMBER+                                             |
| Audit Log            | ORGANIZATION_OWNER+                                      |
| Settings             | ORGANIZATION_OWNER+                                      |
| Platform Admin       | PLATFORM_OPERATIONS+                                     |
| Organizations        | PLATFORM_OPERATIONS+                                     |

---

## Add User Panel — Role Selection

Roles available when adding a user depend on the **current user's role and level**:

- **Platform admins** (isPlatformRole = true): see only platform-category roles
- **Org admins and below**: see organization/team/operations-category roles only
  - External roles (TENANT, VENDOR, APPLICANT) are **excluded** from the "Add User" modal ✓ _(fixed)_
- A user can only assign roles with a **lower level** than their own

---

## Recent Changes

| Change                                                                            | Scope                 | Reason                                                        |
| --------------------------------------------------------------------------------- | --------------------- | ------------------------------------------------------------- |
| Added `MILESTONE_MANAGE` permission group                                         | All 3 rbac.yml        | Milestones page was returning 403 — permissions never existed |
| Added MILESTONE_MANAGE to PORTFOLIO_OWNER, ORGANIZATION_OWNER, ORGANIZATION_ADMIN | All 3 rbac.yml        | Core org roles need milestone access                          |
| Added MILESTONE_READ/LIST/CREATE/UPDATE to PROPERTY_MANAGER                       | All 3 rbac.yml        | Property managers track project milestones                    |
| Added MILESTONE_READ/LIST to BOARD_MEMBER, CASE_WORKER, etc.                      | auth-service rbac.yml | Org-type specific roles need read access                      |
| Added EMPLOYEE_READ/LIST to ORGANIZATION_OWNER and ORGANIZATION_ADMIN             | All 3 rbac.yml        | Org owners can now view their employee list                   |
| Removed external roles from Add User modal                                        | rbacStore.ts          | TENANT/VENDOR/APPLICANT are not org members                   |
| Promo Codes: allow view with PAYMENT_READ or INVOICE_READ                         | promo-codes/page.tsx  | ORG_OWNER can now see promo codes                             |
| Promo Codes: write actions gated by PAYMENT_MANAGE or INVOICE_MANAGE              | promo-codes/page.tsx  | Correct permission separation                                 |
| Session timeout: replaced NODE_ENV check with `NEXT_PUBLIC_SESSION_TIMEOUT_MS`    | DashboardShell.tsx    | No hardcoded environment branching                            |
| Analytics flush: replaced raw `fetch` with `httpClient`                           | dashboardAnalytics.ts | Eliminates 401 errors — auth headers now sent                 |
