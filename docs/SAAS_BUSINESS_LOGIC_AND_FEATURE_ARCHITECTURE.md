# Propertize SaaS — Business Logic, Feature Architecture & Plan Design

> **Version**: 1.0  
> **Date**: April 2026  
> **Scope**: End‑to‑end analysis of customer types, feature matrix, subscription plans, RBAC, feature‑gating, and monetization for the Propertize property‑management platform.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Customer Segmentation — Deep Analysis](#2-customer-segmentation)
3. [Feature Inventory & Classification](#3-feature-inventory)
4. [Feature Matrix — Customer Type × Features](#4-feature-matrix-customer)
5. [Subscription Plan Design](#5-subscription-plans)
6. [Feature Matrix — Plan × Features](#6-feature-matrix-plan)
7. [RBAC Architecture — Roles × Permissions](#7-rbac-architecture)
8. [Feature‑Gating System Design](#8-feature-gating)
9. [Database & Schema Design](#9-database-design)
10. [Frontend UX Strategy by Tier](#10-frontend-ux)
11. [Monetization & Upgrade Paths](#11-monetization)
12. [Scalability Considerations](#12-scalability)
13. [Implementation Roadmap](#13-implementation-roadmap)
14. [Appendix — Current vs. Proposed Gap Analysis](#14-gap-analysis)

---

## 1. Executive Summary

Propertize is a multi‑tenant property‑management SaaS serving **five distinct organization types** that collapse into **three customer archetypes**:

| Archetype                     | Org Types                                                                    | Scale          | Key Differentiator                                 |
| ----------------------------- | ---------------------------------------------------------------------------- | -------------- | -------------------------------------------------- |
| **Individual Owner**          | IPO (Individual Property Owner)                                              | 1–10 units     | Self‑service, no staff, minimal overhead           |
| **Professional Organization** | PMC (Property Mgmt Co), REI (Real Estate Investor), HA (Housing Association) | 10–1,000 units | Teams, specialized workflows, vertical features    |
| **Enterprise / Multi‑Org**    | CORP (Corporate), Multi‑org PMC                                              | 1,000+ units   | API integrations, white‑label, cross‑org analytics |

The platform already has strong foundations: **32 RBAC roles**, **130+ feature flags** (`OrganizationFeatureEnum`), **5 subscription tiers**, **`FeatureService`** with org‑type gating, and **`ResourceLimits`/`ResourceUsage`** for quota enforcement. This document strengthens and extends these patterns.

---

## 2. Customer Segmentation — Deep Analysis {#2-customer-segmentation}

### 2.1 Individual Property Owners (IPO)

**Profile**: A landlord who owns 1–10 rental units. They self‑manage or use minimal help. They are cost-sensitive and want simplicity.

| Dimension               | Detail                                                                     |
| ----------------------- | -------------------------------------------------------------------------- |
| **Properties**          | 1–10 single‑family homes, condos, duplexes                                 |
| **Users**               | 1 (owner), possibly 1 co-owner                                             |
| **Staff**               | None — no employees on payroll                                             |
| **Revenue**             | Rent collection + basic financials                                         |
| **Pain points**         | Rent tracking, maintenance requests, tenant communication, tax preparation |
| **Decision driver**     | Price (< $50/mo), ease‑of‑use, mobile experience                           |
| **Tech sophistication** | Low to medium                                                              |

**What they NEED:**

- Property CRUD (add, edit, list, archive)
- Tenant management (add, track lease, contact info)
- Lease management (create, renew, terminate — simple workflow)
- Rent collection (online payment portal for tenants, auto‑reminders)
- Basic maintenance requests (tenant submits, owner tracks)
- Basic financial reporting (rent roll, income/expense statement)
- Document storage (leases, receipts, photos)
- Notification center (payment due, maintenance requests)

**What they DO NOT need:**

- Employee management / payroll
- Multi‑org switching
- Approval workflows (they're the only decision‑maker)
- Vendor management (they hire on an ad‑hoc basis)
- Portfolio analytics / ROI tracking (they think in cash flow, not IRR)
- Owner portal (they ARE the owner)
- White‑label / API access
- Board management / HOA features
- Team management / task assignment

**What's OPTIONAL (upsell):**

- Tenant screening (background/credit checks) — high‑value add‑on
- Inspection tracking — useful for move‑in/move‑out
- Late‑fee automation
- Expense categorization for tax purposes

---

### 2.2 Multi‑Property / Multi‑Owner Organizations

This archetype covers **three distinct org types** with overlapping needs but different vertical features:

#### 2.2.1 Property Management Companies (PMC)

**Profile**: Professional firms that manage properties on behalf of property owners. They have staff, handle day‑to‑day operations, and need to provide transparency to owners.

| Dimension           | Detail                                                                     |
| ------------------- | -------------------------------------------------------------------------- |
| **Properties**      | 10–5,000 units across multiple owners                                      |
| **Users**           | 5–50 (managers, leasing agents, maintenance staff, accounting)             |
| **Staff**           | Yes — full or part‑time employees, contractors                             |
| **Revenue**         | Management fees (% of rent collected), leasing fees, maintenance markups   |
| **Pain points**     | Owner reporting, staff coordination, vendor management, scaling operations |
| **Decision driver** | Workflow efficiency, owner satisfaction, margin optimization               |
| **Unique need**     | **Owner Portal** — property owners see their asset performance             |

**Essential features:**

- Everything in Individual Owner PLUS:
- Staff management (employees, roles, departments)
- Owner portal (transparent reporting to property owners)
- Advanced maintenance workflows (assign → dispatch → complete → invoice)
- Vendor management (preferred vendors, bid comparison, performance tracking)
- Lease management with approval workflows
- Financial reporting (by property, by owner, by portfolio)
- Inspection tracking with photo documentation
- Communication hub (owner, tenant, vendor, staff channels)
- Task management and assignment
- Document management with sharing permissions

**Optional / upsell:**

- Payroll processing for staff
- Tenant screening integration
- Payment plan management (payment arrangements for tenants)
- Promo codes for move‑in specials
- Advanced analytics
- Bulk operations (bulk lease renewals, bulk notices)

#### 2.2.2 Real Estate Investors (REI)

**Profile**: Investors focused on portfolio returns. They may self-manage or hire a PMC. They care about numbers, not operations.

| Dimension           | Detail                                                                  |
| ------------------- | ----------------------------------------------------------------------- |
| **Properties**      | 5–500 units as investment assets                                        |
| **Users**           | 1–10 (investor, analyst, accountant)                                    |
| **Staff**           | Usually none — operations outsourced to PMC                             |
| **Revenue**         | Rental income, appreciation, tax benefits                               |
| **Pain points**     | Portfolio visibility, ROI tracking, market comparison, tax optimization |
| **Decision driver** | Data quality, analytics depth, tax reporting                            |
| **Unique need**     | **Investment analytics** — IRR, cap rate, cash-on-cash, ROI             |

**Essential features:**

- Property management (ownership records, not operations)
- Portfolio analytics dashboard
- ROI tracking per property
- Investment metrics (cap rate, cash‑on‑cash, IRR, NOI)
- Market analysis and property comparison
- Tax reporting and expense categorization
- Financial reporting (P&L per property, portfolio‑wide)
- Document management (purchase agreements, tax filings)

**What they DO NOT need:**

- Maintenance operations (they delegate to PMC)
- Tenant communication (handled by PMC)
- Staff management / payroll
- Vendor management
- Lease management workflows

**Optional / upsell:**

- Owner portal (if they use a PMC, they're the owner)
- Tenant screening
- Multi-currency support (international portfolios)

#### 2.2.3 Housing Associations (HA)

**Profile**: Community‑governed residential organizations. Think HOAs, co‑ops, resident associations. Unique governance structure.

| Dimension           | Detail                                                                    |
| ------------------- | ------------------------------------------------------------------------- |
| **Properties**      | 50–1,000 units in a community                                             |
| **Users**           | 10–50 (board, staff, committee members)                                   |
| **Staff**           | Yes — property manager, maintenance, administrative                       |
| **Revenue**         | Dues/assessments, amenity fees, special assessments                       |
| **Pain points**     | Board governance, community communication, compliance, amenity scheduling |
| **Decision driver** | Community engagement, compliance, transparency                            |
| **Unique need**     | **Governance tools** — voting, board management, compliance               |

**Essential features:**

- Everything in Individual Owner PLUS:
- Community portal (resident-facing website/portal)
- Amenity booking (pool, gym, clubhouse)
- Member voting (resolutions, board elections)
- Board management (meetings, minutes, terms)
- HOA compliance tracking (violations, fines, hearings)
- Staff management
- Assessment/dues collection (instead of "rent")
- Communication hub (mass notices, community updates)
- Document management (CC&Rs, meeting minutes, financials)
- Financial reporting (assessment income, reserve fund balance)
- Maintenance with work orders

**What they DO NOT need:**

- Lease management (residents are owners/shareholders)
- Tenant screening
- Owner portal (they are owner‑operated)
- Investment analytics / ROI tracking
- Rental applications

---

### 2.3 Multi‑Organization Handlers (Enterprise / CORP)

**Profile**: Large property management firms or corporate real estate teams that manage multiple organizations, regions, or business entities from a single platform.

| Dimension           | Detail                                                                           |
| ------------------- | -------------------------------------------------------------------------------- |
| **Properties**      | 1,000–100,000+ units across multiple orgs                                        |
| **Users**           | 50–500+                                                                          |
| **Staff**           | Large — multiple departments, regional managers                                  |
| **Revenue**         | Diversified — management fees, in‑house services, consulting                     |
| **Pain points**     | Cross‑org reporting, consistency, integration with ERP/accounting, brand control |
| **Decision driver** | Scalability, integrations, control, compliance                                   |
| **Unique need**     | **Multi‑org management** with centralised oversight                              |

**Essential features (everything from PMC PLUS):**

- Multi‑organization management (switch between orgs)
- Cross‑org analytics and reporting
- API access for integrations (ERP, accounting, CRM)
- Custom integrations (webhooks, data pipelines)
- White‑label / custom branding per org
- Multi‑currency support
- Advanced security (IP restrictions, SSO/SAML, MFA enforcement)
- Custom roles and permissions
- Approval workflows with delegation
- Full payroll and HR management
- Bulk operations across orgs
- SLA tracking and compliance
- Audit trail (complete, exportable, tamper‑evident)
- Dedicated support / SLAs

**Optional add‑ons:**

- Data warehouse export
- Custom report builder
- AI‑powered insights (predictive maintenance, rent optimization)

---

## 3. Feature Inventory & Classification {#3-feature-inventory}

Every platform feature classified by necessity level across customer types.

### 3.1 Feature Tiers

| Tier           | Meaning                                    | Gate Mechanism                                          |
| -------------- | ------------------------------------------ | ------------------------------------------------------- |
| **Core**       | Included in every plan, cannot be disabled | Always enabled                                          |
| **Standard**   | Included from Professional plan upward     | Subscription tier check                                 |
| **Advanced**   | Available in Business+ plans or as add‑on  | Tier + feature flag                                     |
| **Enterprise** | Enterprise plan only                       | Tier + feature flag                                     |
| **Vertical**   | Only applicable to specific org types      | `FeatureService.isFeatureSuitableForOrganizationType()` |
| **Add‑on**     | Purchasable separately on any plan         | Feature flag (per‑org toggle)                           |

### 3.2 Complete Feature Classification

#### Property & Lease Management

| Feature                     | Tier       | IPO | PMC | REI | HA  | CORP | Notes                |
| --------------------------- | ---------- | --- | --- | --- | --- | ---- | -------------------- |
| Property CRUD               | Core       | ✅  | ✅  | ✅  | ✅  | ✅   |                      |
| Property photos & details   | Core       | ✅  | ✅  | ✅  | ✅  | ✅   |                      |
| Property listing (public)   | Core       | ✅  | ✅  | ❌  | ❌  | ✅   | REI/HA don't list    |
| Lease creation & management | Core       | ✅  | ✅  | ❌  | ❌  | ✅   | REI/HA: no leases    |
| Lease amendments            | Standard   | ✅  | ✅  | ❌  | ❌  | ✅   |                      |
| Lease renewal workflows     | Standard   | ❌  | ✅  | ❌  | ❌  | ✅   | IPO: manual renewal  |
| Lease approval workflows    | Advanced   | ❌  | ✅  | ❌  | ❌  | ✅   | Multi-step approvals |
| Bulk lease operations       | Enterprise | ❌  | ✅  | ❌  | ❌  | ✅   |                      |
| Property portfolio view     | Standard   | ❌  | ✅  | ✅  | ✅  | ✅   |                      |

#### Tenant Management

| Feature                       | Tier     | IPO | PMC | REI | HA   | CORP | Notes             |
| ----------------------------- | -------- | --- | --- | --- | ---- | ---- | ----------------- |
| Tenant CRUD                   | Core     | ✅  | ✅  | ❌  | ✅\* | ✅   | \*HA: "Residents" |
| Tenant communication          | Core     | ✅  | ✅  | ❌  | ✅   | ✅   |                   |
| Tenant portal (self‑service)  | Core     | ✅  | ✅  | ❌  | ✅   | ✅   |                   |
| Rental applications           | Standard | ✅  | ✅  | ❌  | ❌   | ✅   |                   |
| Tenant screening              | Add‑on   | ✅  | ✅  | ❌  | ❌   | ✅   | $5–15/screen      |
| Payment plans (arrangements)  | Standard | ❌  | ✅  | ❌  | ✅   | ✅   |                   |
| Move‑in / move‑out checklists | Standard | ❌  | ✅  | ❌  | ❌   | ✅   |                   |

#### Financial Management

| Feature                     | Tier       | IPO | PMC | REI | HA   | CORP | Notes            |
| --------------------------- | ---------- | --- | --- | --- | ---- | ---- | ---------------- |
| Rent collection (online)    | Core       | ✅  | ✅  | ❌  | ✅\* | ❌   | \*HA: "Dues"     |
| Payment tracking & receipts | Core       | ✅  | ✅  | ✅  | ✅   | ✅   |                  |
| Late fee automation         | Standard   | ✅  | ✅  | ❌  | ✅   | ✅   |                  |
| Invoice generation          | Standard   | ❌  | ✅  | ❌  | ✅   | ✅   |                  |
| Expense tracking            | Core       | ✅  | ✅  | ✅  | ✅   | ✅   |                  |
| Basic financial reports     | Core       | ✅  | ✅  | ✅  | ✅   | ✅   |                  |
| Advanced financial reports  | Standard   | ❌  | ✅  | ✅  | ✅   | ✅   |                  |
| Tax reporting               | Standard   | ❌  | ❌  | ✅  | ❌   | ✅   | REI core need    |
| Promo codes                 | Standard   | ❌  | ✅  | ❌  | ❌   | ✅   | Move‑in specials |
| Multi‑currency              | Enterprise | ❌  | ❌  | ❌  | ❌   | ✅   |                  |
| Stripe integration          | Core       | ✅  | ✅  | ❌  | ✅   | ✅   |                  |
| Bank reconciliation         | Advanced   | ❌  | ✅  | ✅  | ✅   | ✅   |                  |

#### Maintenance & Operations

| Feature                        | Tier       | IPO | PMC | REI | HA  | CORP | Notes                        |
| ------------------------------ | ---------- | --- | --- | --- | --- | ---- | ---------------------------- |
| Basic maintenance requests     | Core       | ✅  | ✅  | ❌  | ✅  | ✅   |                              |
| Advanced maintenance workflows | Standard   | ❌  | ✅  | ❌  | ✅  | ✅   | Assign → dispatch → complete |
| Work orders                    | Standard   | ❌  | ✅  | ❌  | ✅  | ✅   |                              |
| Vendor management              | Standard   | ❌  | ✅  | ❌  | ✅  | ✅   |                              |
| Vendor matching (AI)           | Advanced   | ❌  | ✅  | ❌  | ✅  | ✅   | Python AI service            |
| Inspection tracking            | Standard   | ❌  | ✅  | ❌  | ✅  | ✅   |                              |
| Asset tracking                 | Standard   | ❌  | ✅  | ❌  | ✅  | ✅   | HVAC, appliances             |
| Predictive maintenance (AI)    | Enterprise | ❌  | ❌  | ❌  | ❌  | ✅   |                              |

#### Staff & HR

| Feature                 | Tier       | IPO  | PMC | REI | HA  | CORP | Notes               |
| ----------------------- | ---------- | ---- | --- | --- | --- | ---- | ------------------- |
| Employee management     | Standard   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Department management   | Standard   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Position management     | Standard   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Timesheet tracking      | Advanced   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Leave management        | Advanced   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Payroll processing      | Advanced   | ❌   | ✅  | ❌  | ✅  | ✅   | Add‑on for Standard |
| Compensation management | Advanced   | ❌   | ✅  | ❌  | ❌  | ✅   |                     |
| User management         | Core       | ✅\* | ✅  | ✅  | ✅  | ✅   | \*IPO: self only    |
| Role assignment         | Standard   | ❌   | ✅  | ❌  | ✅  | ✅   |                     |
| Custom roles            | Enterprise | ❌   | ❌  | ❌  | ❌  | ✅   |                     |

#### Analytics & Reporting

| Feature                   | Tier       | IPO | PMC | REI | HA  | CORP | Notes         |
| ------------------------- | ---------- | --- | --- | --- | --- | ---- | ------------- |
| Basic reporting           | Core       | ✅  | ✅  | ✅  | ✅  | ✅   |               |
| Occupancy reports         | Standard   | ❌  | ✅  | ✅  | ✅  | ✅   |               |
| Delinquency reports       | Standard   | ✅  | ✅  | ❌  | ✅  | ✅   |               |
| Rent roll                 | Standard   | ✅  | ✅  | ❌  | ✅  | ✅   |               |
| Portfolio analytics       | Vertical   | ❌  | ❌  | ✅  | ❌  | ✅   | REI/CORP only |
| ROI tracking              | Vertical   | ❌  | ❌  | ✅  | ❌  | ✅   | REI/CORP only |
| Investment metrics        | Vertical   | ❌  | ❌  | ✅  | ❌  | ✅   | Cap rate, IRR |
| Market analysis           | Vertical   | ❌  | ❌  | ✅  | ❌  | ✅   |               |
| Property comparison       | Vertical   | ❌  | ❌  | ✅  | ❌  | ✅   |               |
| Cross‑org reporting       | Enterprise | ❌  | ❌  | ❌  | ❌  | ✅   |               |
| Custom dashboards         | Enterprise | ❌  | ❌  | ❌  | ❌  | ✅   |               |
| AI‑powered insights       | Enterprise | ❌  | ❌  | ❌  | ❌  | ✅   |               |
| Report export (PDF/Excel) | Standard   | ❌  | ✅  | ✅  | ✅  | ✅   |               |

#### Communication & Collaboration

| Feature                      | Tier     | IPO | PMC | REI | HA  | CORP | Notes  |
| ---------------------------- | -------- | --- | --- | --- | --- | ---- | ------ |
| Notification center          | Core     | ✅  | ✅  | ✅  | ✅  | ✅   |        |
| Email notifications          | Core     | ✅  | ✅  | ✅  | ✅  | ✅   |        |
| SMS notifications            | Add‑on   | ✅  | ✅  | ❌  | ✅  | ✅   | Twilio |
| In‑app messaging             | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |        |
| Communication hub            | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |        |
| WebSocket real‑time updates  | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |        |
| Mass notices / announcements | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |        |
| Support tickets              | Core     | ✅  | ✅  | ✅  | ✅  | ✅   |        |

#### Documents & Storage

| Feature                   | Tier       | IPO | PMC | REI | HA  | CORP | Notes            |
| ------------------------- | ---------- | --- | --- | --- | --- | ---- | ---------------- |
| Document upload (basic)   | Core       | ✅  | ✅  | ✅  | ✅  | ✅   | 5 GB on Basic    |
| Document management       | Standard   | ❌  | ✅  | ✅  | ✅  | ✅   | Folders, sharing |
| Lease document generation | Standard   | ❌  | ✅  | ❌  | ❌  | ✅   |                  |
| E‑signatures              | Add‑on     | ✅  | ✅  | ❌  | ✅  | ✅   |                  |
| Bulk document ops         | Enterprise | ❌  | ❌  | ❌  | ❌  | ✅   |                  |

#### Workflow & Automation

| Feature               | Tier     | IPO | PMC | REI | HA  | CORP | Notes |
| --------------------- | -------- | --- | --- | --- | --- | ---- | ----- |
| Task management       | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |       |
| Calendar / scheduling | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |       |
| Approval workflows    | Advanced | ❌  | ✅  | ❌  | ✅  | ✅   |       |
| Milestones            | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |       |
| Automated reminders   | Core     | ✅  | ✅  | ❌  | ✅  | ✅   |       |
| Dunning / collections | Standard | ❌  | ✅  | ❌  | ✅  | ✅   |       |

#### Vertical — PMC Exclusive

| Feature                    | Tier         | Notes                                 |
| -------------------------- | ------------ | ------------------------------------- |
| Owner Portal               | PMC‑Vertical | Owners see their property performance |
| Owner reporting            | PMC‑Vertical | Monthly owner statements              |
| Owner relations management | PMC‑Vertical | Dedicated role                        |

#### Vertical — REI/Investor Exclusive

| Feature                       | Tier         | Notes                        |
| ----------------------------- | ------------ | ---------------------------- |
| Portfolio analytics dashboard | REI‑Vertical | IRR, cap rate, NOI           |
| Investment metrics            | REI‑Vertical | Cash‑on‑cash, appreciation   |
| Market analysis               | REI‑Vertical | Comps, market trends         |
| Property comparison           | REI‑Vertical | Side‑by‑side analysis        |
| Tax optimization              | REI‑Vertical | Depreciation, 1031 exchanges |

#### Vertical — HA Exclusive

| Feature          | Tier        | Notes                    |
| ---------------- | ----------- | ------------------------ |
| Community portal | HA‑Vertical | Resident hub             |
| Amenity booking  | HA‑Vertical | Pool, gym, clubhouse     |
| Member voting    | HA‑Vertical | Resolutions, elections   |
| Board management | HA‑Vertical | Meetings, minutes, terms |
| HOA compliance   | HA‑Vertical | Violations, fines        |

#### Enterprise / Platform

| Feature                  | Tier       | Notes                                      |
| ------------------------ | ---------- | ------------------------------------------ |
| Multi‑org management     | Enterprise | Org switching, cross‑org views             |
| API access               | Enterprise | REST API for integrations                  |
| Custom integrations      | Enterprise | Webhooks, data pipelines                   |
| White‑label branding     | Enterprise | Custom domain, logos, colors               |
| Multi‑currency           | Enterprise | International portfolios                   |
| Advanced security        | Enterprise | SSO/SAML, IP whitelisting, MFA enforcement |
| Custom roles             | Enterprise | Create bespoke permission sets             |
| Permission delegation    | Enterprise | Temporary access grants                    |
| Audit trail (exportable) | Enterprise | Compliance‑grade logging                   |
| SLA management           | Enterprise | Contractual performance tracking           |
| Dedicated support        | Enterprise | Priority support channel                   |

---

## 4. Feature Matrix — Customer Type × Features {#4-feature-matrix-customer}

### Condensed Matrix

| Category                   | IPO                    | PMC                      | REI                      | HA                     | CORP                |
| -------------------------- | ---------------------- | ------------------------ | ------------------------ | ---------------------- | ------------------- |
| **Property Management**    | Basic CRUD             | Full suite               | Ownership records        | Unit management        | Full suite          |
| **Tenant / Resident Mgmt** | Basic                  | Full + screening         | ❌ Delegated             | Residents + community  | Full                |
| **Lease Management**       | Simple                 | Workflows + approvals    | ❌                       | ❌ (assessments)       | Workflows           |
| **Rent / Payment**         | Collection + reminders | Full + invoicing         | ❌                       | Dues + assessments     | Full                |
| **Maintenance**            | Basic requests         | Full workflows + vendors | ❌                       | Full + community       | Full                |
| **Staff / HR**             | ❌                     | Employees + payroll      | ❌                       | Staff + payroll        | Full HR suite       |
| **Analytics**              | Basic reports          | Financial + occupancy    | **Investment analytics** | Financial + community  | Cross‑org analytics |
| **Communication**          | Notifications          | Full hub                 | Notifications            | Mass notices + portal  | Full hub            |
| **Documents**              | Basic upload (5 GB)    | Full management          | Storage                  | Full + compliance docs | Full + bulk         |
| **Workflows**              | Auto‑reminders         | Full task/approval       | ❌                       | Board workflows        | Full + delegation   |
| **Vertical**               | —                      | Owner portal             | Portfolio/ROI            | Community/governance   | API/integrations    |
| **Multi‑org**              | ❌                     | ❌                       | ❌                       | ❌                     | ✅                  |

### Feature Count by Customer Type

| Customer | Core | Standard    | Advanced | Vertical | Enterprise | Total |
| -------- | ---- | ----------- | -------- | -------- | ---------- | ----- |
| **IPO**  | 15   | 3 (add‑ons) | 0        | 0        | 0          | ~18   |
| **PMC**  | 15   | 20          | 8        | 3        | 0          | ~46   |
| **REI**  | 10   | 6           | 2        | 6        | 0          | ~24   |
| **HA**   | 13   | 16          | 6        | 5        | 0          | ~40   |
| **CORP** | 15   | 20          | 10       | 0        | 12         | ~57   |

---

## 5. Subscription Plan Design {#5-subscription-plans}

### 5.1 Proposed Four‑Tier Plan Structure

The current system has 5 tier enums (`BASIC`, `PROFESSIONAL`, `INVESTOR`, `ENTERPRISE`, `ASSOCIATION`). This works but creates a disjointed pricing page. Below is a **user‑facing 4‑plan structure** that maps cleanly to the 5 backend tiers:

| Plan Name        | Backend Tier   | Target Customer         | Price Model              | Starting Price                       |
| ---------------- | -------------- | ----------------------- | ------------------------ | ------------------------------------ |
| **Starter**      | `BASIC`        | Individual Owners (IPO) | Per property/mo          | **$9.99/property**                   |
| **Professional** | `PROFESSIONAL` | PMC, HA                 | Base + overage           | **$199/mo** (includes 50 properties) |
| **Investor**     | `INVESTOR`     | REI                     | Per property + analytics | **$15/property + $99 analytics**     |
| **Enterprise**   | `ENTERPRISE`   | CORP, large PMC         | Custom negotiated        | **Contact sales**                    |

> **Note**: Housing Associations use the **Professional** plan with HA-vertical features auto-enabled. The `ASSOCIATION` backend tier is a Professional variant with per‑unit pricing and community features. On the pricing page, HA sees "Professional — Community Edition" with per-unit pricing.

### 5.2 Detailed Plan Specifications

#### Starter Plan ($9.99/property/month)

| Resource   | Limit                  |
| ---------- | ---------------------- |
| Properties | Up to 10               |
| Users      | 1 (owner)              |
| Storage    | 5 GB                   |
| Support    | Email, community forum |

**Included features:**

- Property management (CRUD, photos, details)
- Tenant management (CRUD, contact, lease)
- Simple lease management (create, renew, terminate)
- Online rent collection (Stripe)
- Payment tracking and receipts
- Automated rent reminders
- Basic maintenance requests
- Expense tracking
- Basic financial reports (income statement, rent roll)
- Document upload (5 GB)
- Notification center (email)
- Support tickets
- Tenant self-service portal

**Not included:**

- Employee / staff management
- Payroll
- Advanced maintenance workflows
- Vendor management
- Invoice generation
- Task management
- In-app messaging
- Advanced reporting
- Approval workflows
- Lease document generation
- Multi‑org support
- API access

**Available add‑ons:**
| Add‑on | Price |
|--------|-------|
| Tenant screening | $15/screen |
| SMS notifications | $0.05/SMS |
| E‑signatures | $5/mo |
| Late fee automation | $4.99/mo |
| Additional storage (10 GB) | $2.99/mo |

---

#### Professional Plan ($199/month)

| Resource   | Limit                        |
| ---------- | ---------------------------- |
| Properties | 50 included, $5/additional   |
| Users      | 20                           |
| Storage    | 100 GB                       |
| Support    | Email, chat (business hours) |

**Includes everything in Starter PLUS:**

- Employee management (employees, departments, positions)
- Advanced maintenance workflows (assign → dispatch → complete)
- Work orders
- Vendor management and matching
- Inspection tracking
- Invoice generation and management
- Late fee policies and automation
- Payment plans (tenant arrangements)
- Promo codes
- Advanced financial reporting (by property, owner, date range)
- Occupancy and delinquency reports
- Rent roll reports
- Report export (PDF, Excel)
- Document management (folders, permissions, sharing)
- Communication hub
- In‑app messaging
- Task management and assignment
- Calendar / scheduling
- Milestones
- Dunning / collections
- WebSocket real‑time updates
- Mass notices
- Role assignment (system roles)
- Lease renewal and amendment workflows

**Vertical features auto‑enabled by org type:**

- **PMC**: Owner portal, owner reporting, owner relations
- **HA**: Community portal, amenity booking, member voting, board management, HOA compliance

**Not included:**

- Payroll processing
- Approval workflows with delegation
- Cross‑org analytics
- Custom roles
- API access
- White‑label
- Multi‑currency

**Available add‑ons:**
| Add‑on | Price |
|--------|-------|
| Payroll processing | $49/mo + $6/employee |
| Tenant screening | $12/screen |
| SMS notifications | $0.04/SMS |
| E‑signatures | $9.99/mo |
| Approval workflows | $29/mo |
| Additional storage (50 GB) | $9.99/mo |
| Additional users (5 pack) | $19.99/mo |

---

#### Investor Plan ($15/property + $99 analytics/month)

| Resource   | Limit                        |
| ---------- | ---------------------------- |
| Properties | Up to 100                    |
| Users      | 10                           |
| Storage    | 50 GB                        |
| Support    | Email, chat (business hours) |

**Includes everything in Starter PLUS:**

- Portfolio analytics dashboard
- ROI tracking per property
- Investment metrics (cap rate, cash‑on‑cash, IRR, NOI)
- Market analysis
- Property comparison (side‑by‑side)
- Tax reporting and optimization
- Advanced financial reporting (portfolio‑wide P&L)
- Document management
- Occupancy and performance reports
- Report export

**Not included:**

- Maintenance workflows (investors delegate to PMC)
- Tenant management (delegated)
- Staff management / payroll
- Vendor management
- Invoicing
- Communication hub
- Task management
- Approval workflows

**Available add‑ons:**
| Add‑on | Price |
|--------|-------|
| Multi‑currency | $29/mo |
| Additional storage (25 GB) | $4.99/mo |
| Additional users (5 pack) | $14.99/mo |
| Data export API | $49/mo |

---

#### Enterprise Plan (Custom pricing)

| Resource   | Limit                          |
| ---------- | ------------------------------ |
| Properties | Unlimited                      |
| Users      | 100 (expandable)               |
| Storage    | 1 TB (expandable)              |
| Support    | Dedicated account manager, SLA |

**Includes everything in Professional + Investor PLUS:**

- Multi‑organization management
- Cross‑org analytics and reporting
- API access (REST, webhooks)
- Custom integrations
- White‑label branding
- Multi‑currency support
- Advanced security (SSO/SAML, IP whitelisting, MFA enforcement)
- Custom roles and permissions
- Permission delegation (temporal permissions)
- Approval workflows with delegation
- Full payroll and HR management (included, not add‑on)
- Bulk operations (leases, notices, documents)
- Exportable audit trail
- SLA management
- Compliance suite
- AI‑powered insights (predictive maintenance, rent optimization)
- Custom dashboards
- Dedicated onboarding and training

---

### 5.3 Plan Mapping Summary

```
                    Starter    Professional   Investor   Enterprise
                    ────────   ────────────   ────────   ──────────
Org Types:          IPO        PMC, HA        REI        CORP, Large PMC
Properties:         1-10       50-5,000       1-100      Unlimited
Users:              1          20             10         100+
Storage:            5 GB       100 GB         50 GB      1 TB+
Staff Mgmt:         ❌         ✅             ❌         ✅
Payroll:            ❌         Add-on         ❌         ✅ (included)
Vertical:           —          PMC/HA auto    REI auto   All verticals
Multi-org:          ❌         ❌             ❌         ✅
API:                ❌         ❌             ❌         ✅
Custom roles:       ❌         ❌             ❌         ✅
```

---

## 6. Feature Matrix — Plan × Features {#6-feature-matrix-plan}

### Compact Plan × Feature Matrix

Legend: ✅ = Included | 🔌 = Add‑on | ❌ = Not available | 🏢 = Vertical (auto by org type)

| Feature              | Starter | Professional | Investor | Enterprise |
| -------------------- | ------- | ------------ | -------- | ---------- |
| **CORE**             |         |              |          |            |
| Property CRUD        | ✅      | ✅           | ✅       | ✅         |
| Tenant management    | ✅      | ✅           | ❌       | ✅         |
| Simple lease mgmt    | ✅      | ✅           | ❌       | ✅         |
| Rent collection      | ✅      | ✅           | ❌       | ✅         |
| Basic maintenance    | ✅      | ✅           | ❌       | ✅         |
| Basic reports        | ✅      | ✅           | ✅       | ✅         |
| Expense tracking     | ✅      | ✅           | ✅       | ✅         |
| Doc upload (basic)   | ✅      | ✅           | ✅       | ✅         |
| Notifications        | ✅      | ✅           | ✅       | ✅         |
| Tenant portal        | ✅      | ✅           | ❌       | ✅         |
| Auto reminders       | ✅      | ✅           | ❌       | ✅         |
| Support tickets      | ✅      | ✅           | ✅       | ✅         |
| **STANDARD**         |         |              |          |            |
| Advanced maintenance | ❌      | ✅           | ❌       | ✅         |
| Work orders          | ❌      | ✅           | ❌       | ✅         |
| Vendor management    | ❌      | ✅           | ❌       | ✅         |
| Inspection tracking  | ❌      | ✅           | ❌       | ✅         |
| Employee management  | ❌      | ✅           | ❌       | ✅         |
| Invoice generation   | ❌      | ✅           | ❌       | ✅         |
| Late fee policies    | 🔌      | ✅           | ❌       | ✅         |
| Payment plans        | ❌      | ✅           | ❌       | ✅         |
| Advanced reports     | ❌      | ✅           | ✅       | ✅         |
| Report export        | ❌      | ✅           | ✅       | ✅         |
| Doc management       | ❌      | ✅           | ✅       | ✅         |
| Communication hub    | ❌      | ✅           | ❌       | ✅         |
| In‑app messaging     | ❌      | ✅           | ❌       | ✅         |
| Task management      | ❌      | ✅           | ❌       | ✅         |
| Calendar/scheduling  | ❌      | ✅           | ❌       | ✅         |
| Role assignment      | ❌      | ✅           | ❌       | ✅         |
| Lease workflows      | ❌      | ✅           | ❌       | ✅         |
| Promo codes          | ❌      | ✅           | ❌       | ✅         |
| Dunning/collections  | ❌      | ✅           | ❌       | ✅         |
| Real‑time updates    | ❌      | ✅           | ❌       | ✅         |
| **ADVANCED**         |         |              |          |            |
| Payroll processing   | ❌      | 🔌           | ❌       | ✅         |
| Timesheets           | ❌      | 🔌           | ❌       | ✅         |
| Leave management     | ❌      | 🔌           | ❌       | ✅         |
| Compensation mgmt    | ❌      | 🔌           | ❌       | ✅         |
| AI vendor matching   | ❌      | 🔌           | ❌       | ✅         |
| Approval workflows   | ❌      | 🔌           | ❌       | ✅         |
| **VERTICAL**         |         |              |          |            |
| Owner portal         | ❌      | 🏢 PMC       | ❌       | 🏢         |
| Owner reporting      | ❌      | 🏢 PMC       | ❌       | 🏢         |
| Community portal     | ❌      | 🏢 HA        | ❌       | 🏢         |
| Amenity booking      | ❌      | 🏢 HA        | ❌       | 🏢         |
| Member voting        | ❌      | 🏢 HA        | ❌       | 🏢         |
| Board management     | ❌      | 🏢 HA        | ❌       | 🏢         |
| HOA compliance       | ❌      | 🏢 HA        | ❌       | 🏢         |
| Portfolio analytics  | ❌      | ❌           | ✅       | ✅         |
| ROI tracking         | ❌      | ❌           | ✅       | ✅         |
| Investment metrics   | ❌      | ❌           | ✅       | ✅         |
| Market analysis      | ❌      | ❌           | ✅       | ✅         |
| Property comparison  | ❌      | ❌           | ✅       | ✅         |
| Tax reporting        | ❌      | ❌           | ✅       | ✅         |
| **ENTERPRISE**       |         |              |          |            |
| Multi‑org mgmt       | ❌      | ❌           | ❌       | ✅         |
| API access           | ❌      | ❌           | ❌       | ✅         |
| Custom integrations  | ❌      | ❌           | ❌       | ✅         |
| White‑label          | ❌      | ❌           | ❌       | ✅         |
| Multi‑currency       | ❌      | ❌           | 🔌       | ✅         |
| Advanced security    | ❌      | ❌           | ❌       | ✅         |
| Custom roles         | ❌      | ❌           | ❌       | ✅         |
| Perm delegation      | ❌      | ❌           | ❌       | ✅         |
| Audit export         | ❌      | ❌           | ❌       | ✅         |
| Bulk operations      | ❌      | ❌           | ❌       | ✅         |
| AI insights          | ❌      | ❌           | ❌       | ✅         |
| Dedicated support    | ❌      | ❌           | ❌       | ✅         |

---

## 7. RBAC Architecture — Roles × Permissions {#7-rbac-architecture}

### 7.1 Role Applicability by Plan

The system already has 32 roles. Here's how they map to plans:

| Role                   | Level | Starter | Professional | Investor | Enterprise |
| ---------------------- | ----- | ------- | ------------ | -------- | ---------- |
| **Platform Tier**      |       |         |              |          |            |
| PLATFORM_OVERSIGHT     | 1000  | —       | —            | —        | —          |
| PLATFORM_OPERATIONS    | 970   | —       | —            | —        | —          |
| PLATFORM_ENGINEERING   | 950   | —       | —            | —        | —          |
| PLATFORM_ANALYTICS     | 930   | —       | —            | —        | —          |
| EMERGENCY_ACCESS       | 999   | —       | —            | —        | —          |
| **Portfolio Tier**     |       |         |              |          |            |
| PORTFOLIO_OWNER        | 920   | ❌      | ❌           | ❌       | ✅         |
| **Org Owner Tier**     |       |         |              |          |            |
| ORGANIZATION_OWNER     | 900   | ❌      | ✅           | ✅       | ✅         |
| SOLO_OWNER             | 870   | ✅      | ❌           | ❌       | ❌         |
| **Org Admin Tier**     |       |         |              |          |            |
| ORGANIZATION_ADMIN     | 850   | ❌      | ✅           | ✅       | ✅         |
| CFO                    | 890   | ❌      | ❌           | ❌       | ✅         |
| HR_MANAGER             | 820   | ❌      | ✅           | ❌       | ✅         |
| HOA_DIRECTOR           | 890   | ❌      | ✅ HA        | ❌       | ✅         |
| **Functional Tier**    |       |         |              |          |            |
| PROPERTY_MANAGER       | 800   | ❌      | ✅           | ❌       | ✅         |
| OPERATIONS_MANAGER     | 780   | ❌      | ✅           | ❌       | ✅         |
| LEASE_SPECIALIST       | 750   | ❌      | ✅           | ❌       | ✅         |
| PORTFOLIO_ANALYST      | 750   | ❌      | ❌           | ✅       | ✅         |
| INVESTOR_RELATIONS     | 730   | ❌      | ❌           | ✅       | ✅         |
| LEASING_AGENT          | 700   | ❌      | ✅           | ❌       | ✅         |
| ACCOUNTANT             | 650   | ❌      | ✅           | ✅       | ✅         |
| OWNER_RELATIONS        | 670   | ❌      | ✅ PMC       | ❌       | ✅         |
| COMMUNITY_MANAGER      | 650   | ❌      | ✅ HA        | ❌       | ✅         |
| MAINTENANCE_SUPERVISOR | 600   | ❌      | ✅           | ❌       | ✅         |
| BOARD_MEMBER           | 600   | ❌      | ✅ HA        | ❌       | ✅         |
| TENANT_COORDINATOR     | 550   | ❌      | ✅           | ❌       | ✅         |
| CASE_WORKER            | 550   | ❌      | ✅ HA        | ❌       | ✅         |
| **Operational Tier**   |       |         |              |          |            |
| TEAM_LEAD              | 500   | ❌      | ✅           | ❌       | ✅         |
| INSPECTOR              | 450   | ❌      | ✅           | ❌       | ✅         |
| MAINTENANCE_TECHNICIAN | 400   | ❌      | ✅           | ❌       | ✅         |
| TEAM_MEMBER            | 300   | ❌      | ✅           | ❌       | ✅         |
| **External Tier**      |       |         |              |          |            |
| VENDOR                 | 200   | ❌      | ✅           | ❌       | ✅         |
| TENANT                 | 150   | ✅      | ✅           | ❌       | ✅         |
| APPLICANT              | 100   | ✅      | ✅           | ❌       | ✅         |
| READ_ONLY              | 50    | ❌      | ✅           | ✅       | ✅         |

### 7.2 Permission Categories

| Category        | Key Permissions                                                                               | Gate                |
| --------------- | --------------------------------------------------------------------------------------------- | ------------------- |
| **Property**    | `PROPERTY_CREATE`, `PROPERTY_READ`, `PROPERTY_UPDATE`, `PROPERTY_DELETE`, `PROPERTY_MANAGE`   | Core                |
| **Tenant**      | `TENANT_CREATE`, `TENANT_READ`, `TENANT_UPDATE`, `TENANT_DELETE`, `TENANT_MANAGE`             | Core                |
| **Lease**       | `LEASE_CREATE`, `LEASE_READ`, `LEASE_UPDATE`, `LEASE_DELETE`, `LEASE_APPROVE`, `LEASE_MANAGE` | Core/Standard       |
| **Payment**     | `PAYMENT_CREATE`, `PAYMENT_READ`, `PAYMENT_PROCESS`, `PAYMENT_MANAGE`                         | Core                |
| **Maintenance** | `MAINTENANCE_CREATE`, `MAINTENANCE_READ`, `MAINTENANCE_ASSIGN`, `MAINTENANCE_MANAGE`          | Core/Standard       |
| **Employee**    | `EMPLOYEE_CREATE`, `EMPLOYEE_READ`, `EMPLOYEE_UPDATE`, `EMPLOYEE_DELETE`, `EMPLOYEE_MANAGE`   | Standard            |
| **Payroll**     | `PAYROLL_CREATE`, `PAYROLL_READ`, `PAYROLL_PROCESS`, `PAYROLL_APPROVE`, `PAYROLL_MANAGE`      | Advanced            |
| **Invoice**     | `INVOICE_CREATE`, `INVOICE_READ`, `INVOICE_SEND`, `INVOICE_MANAGE`                            | Standard            |
| **Report**      | `REPORT_READ`, `REPORT_EXPORT`, `REPORT_FULL`                                                 | Standard            |
| **User**        | `USER_CREATE`, `USER_READ`, `USER_UPDATE`, `USER_DELETE`, `USER_MANAGE`                       | Standard            |
| **Org**         | `ORGANIZATION_READ`, `ORGANIZATION_UPDATE`, `ORGANIZATION_MANAGE`                             | Core                |
| **Vendor**      | `VENDOR_CREATE`, `VENDOR_READ`, `VENDOR_UPDATE`, `VENDOR_MANAGE`                              | Standard            |
| **Document**    | `DOCUMENT_CREATE`, `DOCUMENT_READ`, `DOCUMENT_DELETE`, `DOCUMENT_MANAGE`                      | Core/Standard       |
| **Audit**       | `AUDIT_READ`, `AUDIT_EXPORT`                                                                  | Standard/Enterprise |
| **System**      | `SYSTEM_ADMIN`, `ADMIN_ACCESS`                                                                | Enterprise          |

### 7.3 RBAC Decision Flow

```
Request arrives at API Gateway
         │
         ▼
┌─────────────────────────────┐
│  1. JWT Token Validation    │  ← Auth-service validates token
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐
│  2. Extract User Context    │  ← X-User-ID, X-User-Email,
│     from Headers            │    X-User-Roles, X-Org-ID
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐
│  3. RBAC Permission Check   │  ← RbacAuthorizationFilter checks
│     (Gateway Filter)        │    user.roles has required permission
└────────────┬────────────────┘
             │ ✅ Permitted
             ▼
┌─────────────────────────────┐
│  4. Feature Gate Check      │  ← FeatureService checks:
│     (Service Layer)         │    org.tier.hasFeature(feature)
│                             │    && isFeatureSuitableForOrgType()
│                             │    && !org.isSubscriptionExpired()
└────────────┬────────────────┘
             │ ✅ Feature available
             ▼
┌─────────────────────────────┐
│  5. Resource Limit Check    │  ← ResourceLimits vs ResourceUsage
│     (Service Layer)         │    e.g., property count < maxProperties
└────────────┬────────────────┘
             │ ✅ Within limits
             ▼
┌─────────────────────────────┐
│  6. Business Logic          │  ← Execute the operation
└─────────────────────────────┘
```

### 7.4 Triple Gate Model

Every user action passes through **three independent gates**:

| Gate                | Checks                                            | Where Enforced                           | Failure Response          |
| ------------------- | ------------------------------------------------- | ---------------------------------------- | ------------------------- |
| **Gate 1: RBAC**    | Does the user's role have this permission?        | `RbacAuthorizationFilter` (Gateway)      | HTTP 403                  |
| **Gate 2: Feature** | Does the org's subscription include this feature? | `FeatureService` (Backend service)       | HTTP 403 + upgrade prompt |
| **Gate 3: Quota**   | Is the org within resource limits?                | `ResourceLimits` check (Backend service) | HTTP 402 + limit info     |

**Frontend mirrors all three gates:**

- Gate 1: `useRbacStore().hasPermission()` → hide/show UI elements
- Gate 2: `organizationService.checkFeature()` → show upgrade banners
- Gate 3: `org.resourceUsage` vs `org.resourceLimits` → show usage meters

---

## 8. Feature‑Gating System Design {#8-feature-gating}

### 8.1 Current Architecture (What Exists)

The platform already has a solid foundation:

```
OrganizationEntity
  ├── organizationTypeEnum (IPO, PMC, REI, HA, CORP)
  ├── organizationSubscriptionTierEnum (BASIC, PROFESSIONAL, INVESTOR, ENTERPRISE, ASSOCIATION)
  ├── enabledOrganizationFeatureEnums: Set<OrganizationFeatureEnum>  ← per-org feature flags
  ├── resourceLimits: ResourceLimits (maxProperties, maxUsers, maxStorageGb)
  └── resourceUsage: ResourceUsage (propertyCount, userCount, storageUsedGb)

FeatureService
  ├── isFeatureAvailable(org, feature)  ← tier check + org-type check + expiry check
  ├── getAvailableFeatures(org)
  ├── getFeaturesForUI(org)
  ├── canUpgradeToAccessFeature(org, feature)
  └── getMinimumTierForFeature(feature)

Frontend
  └── organizationService.checkFeature(orgId, feature) → { available, reason, upgradeable }
```

### 8.2 Proposed Enhancements

#### 8.2.1 Add‑on Feature Support

Currently, features are either "in tier" or not. We need a middle ground for add‑ons:

**Concept**: `enabledOrganizationFeatureEnums` in `Organization` entity already stores per-org feature overrides. Add-ons should write directly to this set.

```
Decision tree for isFeatureAvailable(org, feature):

  1. Is feature in org.enabledFeatures (add-on overrides)?  → YES → check org-type suitability
  2. Is feature in org.subscriptionTier.defaultFeatures?     → YES → check org-type suitability
  3. Neither?                                                 → NO
```

**New table needed**: `organization_addons`

```sql
CREATE TABLE organization_addons (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL REFERENCES organizations(id),
    feature         VARCHAR(50) NOT NULL,       -- OrganizationFeatureEnum name
    activated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,                   -- NULL = permanent while subscription active
    price_monthly   DECIMAL(10,2),               -- Monthly charge for this add-on
    status          VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, CANCELLED, EXPIRED
    created_by      BIGINT,
    UNIQUE(organization_id, feature)
);
```

#### 8.2.2 Feature Check Caching (Redis)

Currently `FeatureService` checks are computed per-request. For high-traffic orgs:

```
Cache key: "features:{orgId}" → Set<OrganizationFeatureEnum>
TTL: 5 minutes
Invalidate on: subscription change, add-on purchase, feature toggle
```

This is partially implemented (V15 Flyway migration mentions "Redis permission cache") — needs completion.

#### 8.2.3 Frontend Feature Gate Component

```tsx
// Proposed: <FeatureGate> component
<FeatureGate
  feature="VENDOR_MANAGEMENT"
  fallback={<UpgradeBanner feature="Vendor Management" />}
>
  <VendorManagementPage />
</FeatureGate>;

// Proposed: useFeature() hook
const { available, loading, upgradeable, minimumTier } =
  useFeature("VENDOR_MANAGEMENT");
```

#### 8.2.4 Sidebar/Navigation Gating

The sidebar should dynamically show/hide navigation items based on:

1. User role (RBAC) — already implemented via `hasPermission()`
2. Org feature flags — **needs implementation**
3. Org type — certain sections auto-hidden (e.g., "Leases" hidden for HA)

**Proposed nav item definition:**

```tsx
interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
  permission?: string; // RBAC gate
  feature?: string; // Feature flag gate
  orgTypes?: OrganizationType[]; // Org type gate (show only for these)
  excludeOrgTypes?: OrganizationType[]; // Hide for these
  plan?: SubscriptionTier; // Minimum plan
}
```

---

## 9. Database & Schema Design {#9-database-design}

### 9.1 Core Tables for SaaS Feature Gating

The following tables drive the feature-gating logic. Most already exist:

#### Existing Tables (No Changes Needed)

```sql
-- organizations table (auth-service Flyway V2)
-- Already has: subscription_tier, org_type, operational_status, organization_status

-- organization_features (join table)
-- Already stores: Set<OrganizationFeatureEnum> per org (enabledFeatures)

-- rbac_roles table (auth-service Flyway V10)
-- Already has: role definitions with applicable_org_types

-- users table
-- Already has: roles, organization_id, organization_ids (multi-org)
```

#### New Tables Needed

```sql
-- 1. Add-on purchases
CREATE TABLE organization_addons (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    feature_code    VARCHAR(50) NOT NULL,
    addon_name      VARCHAR(100) NOT NULL,
    price_monthly   DECIMAL(10,2) NOT NULL,
    activated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    billing_cycle   VARCHAR(20) DEFAULT 'MONTHLY', -- MONTHLY, ANNUAL
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    cancelled_at    TIMESTAMP,
    created_by      BIGINT,
    updated_at      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_org_addon UNIQUE (organization_id, feature_code),
    CONSTRAINT fk_org_addon_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- 2. Subscription history (audit trail for plan changes)
CREATE TABLE subscription_history (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    previous_tier   VARCHAR(30),
    new_tier        VARCHAR(30) NOT NULL,
    change_type     VARCHAR(20) NOT NULL, -- UPGRADE, DOWNGRADE, RENEWAL, CANCELLATION
    effective_date  TIMESTAMP NOT NULL,
    reason          TEXT,
    changed_by      BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_sub_hist_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- 3. Usage events (for metered billing and analytics)
CREATE TABLE usage_events (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,  -- PROPERTY_CREATED, USER_ADDED, STORAGE_USED, etc.
    quantity        INTEGER DEFAULT 1,
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    billing_period  VARCHAR(7),  -- '2026-04'
    CONSTRAINT fk_usage_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
CREATE INDEX idx_usage_org_period ON usage_events(organization_id, billing_period);

-- 4. Feature access log (analytics: what features orgs actually use)
CREATE TABLE feature_access_log (
    id              BIGSERIAL PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    feature_code    VARCHAR(50) NOT NULL,
    user_id         BIGINT,
    accessed_at     TIMESTAMP DEFAULT NOW(),
    was_available   BOOLEAN NOT NULL,
    denial_reason   VARCHAR(100)
);
CREATE INDEX idx_feature_access_org ON feature_access_log(organization_id, feature_code);
-- Partition by month for performance
```

### 9.2 Organization Entity Evolution

Current `Organization` entity already has `subscriptionTier`, `enabledFeatures`, `resourceLimits`, `resourceUsage`. Additions needed:

```java
// Add to Organization entity:

@Column(name = "billing_email")
private String billingEmail;

@Column(name = "stripe_customer_id")
private String stripeCustomerId;

@Column(name = "stripe_subscription_id")
private String stripeSubscriptionId;

@Column(name = "trial_ends_at")
private LocalDateTime trialEndsAt;

@Column(name = "is_trial")
private Boolean isTrial = false;

// Add computed method:
public boolean isInTrialPeriod() {
    return Boolean.TRUE.equals(isTrial) &&
           trialEndsAt != null &&
           trialEndsAt.isAfter(LocalDateTime.now());
}

public boolean canAccessFeature(OrganizationFeatureEnum feature) {
    // Check add-ons first, then tier
    return enabledOrganizationFeatureEnums.contains(feature) ||
           (organizationSubscriptionTierEnum != null &&
            organizationSubscriptionTierEnum.hasFeature(feature));
}
```

### 9.3 ER Diagram (Feature Gating Focus)

```
┌──────────────────────┐       ┌────────────────────────┐
│   organizations      │       │  organization_features  │
│──────────────────────│  1:N  │────────────────────────│
│ id (UUID)            │◄──────│ organization_id (FK)   │
│ organization_code    │       │ feature (ENUM)          │
│ organization_type    │       └────────────────────────┘
│ subscription_tier    │
│ operational_status   │       ┌────────────────────────┐
│ resource_limits_*    │  1:N  │  organization_addons   │
│ resource_usage_*     │◄──────│────────────────────────│
│ subscription_start   │       │ organization_id (FK)   │
│ subscription_expires │       │ feature_code           │
│ stripe_customer_id   │       │ price_monthly          │
│ stripe_subscription  │       │ status                 │
│ trial_ends_at        │       └────────────────────────┘
└──────────┬───────────┘
           │ 1:N              ┌────────────────────────┐
           │                  │  subscription_history   │
           └─────────────────►│────────────────────────│
                              │ organization_id (FK)   │
                              │ previous_tier          │
                              │ new_tier               │
                              │ change_type            │
                              └────────────────────────┘
```

---

## 10. Frontend UX Strategy by Tier {#10-frontend-ux}

### 10.1 Design Principles

1. **Every tier feels premium** — No degraded UI. Starter users get the same design quality.
2. **Progressive disclosure** — Don't show what you can't use. Hide features behind gates, not grayed-out buttons.
3. **Contextual upsell** — When a user encounters a gate, show value proposition + one-click upgrade.
4. **Org-type adaptive** — Terminology and navigation adapt (e.g., HA sees "Residents" not "Tenants", "Dues" not "Rent").

### 10.2 Sidebar Configuration by Plan

#### Starter (IPO / SOLO_OWNER)

```
📊 Dashboard
🏠 Properties
👤 Tenants
📄 Leases
💰 Payments
🔧 Maintenance
📁 Documents
📊 Reports (basic)
⚙️ Settings
```

#### Professional (PMC)

```
📊 Dashboard
🏠 Properties
  └── Portfolio View
👤 Tenants
📄 Leases
💰 Payments
  ├── Invoices
  └── Late Fees
🔧 Maintenance
  ├── Work Orders
  └── Inspections
👥 Employees
💼 Vendors
🏢 Owner Portal          ← PMC vertical
📁 Documents
📋 Tasks
📅 Calendar
📊 Reports
  ├── Financial
  ├── Occupancy
  └── Delinquency
🔔 Notifications
👤 Users
📝 Audit Log
⚙️ Settings
```

#### Professional (HA)

```
📊 Dashboard
🏘️ Properties (Units)
👥 Residents             ← "Tenants" renamed
💰 Dues & Assessments    ← "Payments" renamed
🔧 Maintenance
  └── Work Orders
👥 Staff
🏛️ Board                 ← HA vertical
  ├── Meetings
  └── Voting
🏊 Amenities             ← HA vertical
📋 Compliance            ← HA vertical
📁 Documents
📊 Reports
🔔 Community Portal      ← HA vertical
⚙️ Settings
```

#### Investor (REI)

```
📊 Dashboard
📈 Portfolio Analytics    ← REI primary view
🏠 Properties
💰 Financial Overview
📊 Reports
  ├── ROI Tracking
  ├── Investment Metrics
  ├── Market Analysis
  └── Tax Reporting
📁 Documents
⚙️ Settings
```

#### Enterprise (CORP)

```
🏢 Organization Switcher ← Multi-org
📊 Dashboard
🏠 Properties
👤 Tenants
📄 Leases
💰 Payments & Finance
🔧 Maintenance & Ops
👥 HR & Payroll
  ├── Employees
  ├── Payroll
  ├── Timesheets
  └── Leave
📈 Analytics (cross-org)
🔗 Integrations
📁 Documents
📋 Tasks & Workflows
📊 Advanced Reports
🛡️ Security & Audit
⚙️ Settings
  ├── Custom Roles
  ├── API Keys
  └── White Label
```

### 10.3 Upgrade Experience

When a feature is gated, the UI should show:

```
┌─────────────────────────────────────────────────────────┐
│  🔒  Vendor Management                                  │
│                                                          │
│  Manage your contractors, compare bids, and track        │
│  vendor performance — all in one place.                  │
│                                                          │
│  Available on Professional plan and above.               │
│                                                          │
│  [Upgrade to Professional →]    [Learn More]             │
└─────────────────────────────────────────────────────────┘
```

For resource limits:

```
┌─────────────────────────────────────────────────────────┐
│  Properties: 3/3 used  ████████████████████ 100%         │
│                                                          │
│  You've reached your property limit.                     │
│  [Add More Properties — Upgrade Plan →]                  │
└─────────────────────────────────────────────────────────┘
```

### 10.4 Org-Type Terminology Mapping

| Concept   | IPO       | PMC       | REI       | HA        | CORP           |
| --------- | --------- | --------- | --------- | --------- | -------------- |
| Unit      | Property  | Property  | Asset     | Unit      | Property       |
| Occupant  | Tenant    | Tenant    | Tenant    | Resident  | Tenant         |
| Payment   | Rent      | Rent      | Income    | Dues      | Revenue        |
| Contract  | Lease     | Lease     | —         | —         | Lease          |
| Dashboard | Dashboard | Dashboard | Portfolio | Community | Command Center |

---

## 11. Monetization & Upgrade Paths {#11-monetization}

### 11.1 Revenue Streams

| Stream                  | Description                                                 | Margin |
| ----------------------- | ----------------------------------------------------------- | ------ |
| **Subscription fees**   | Monthly/annual plan charges                                 | 80–90% |
| **Add-on features**     | Per-feature monthly charges                                 | 85–95% |
| **Tenant screening**    | Per-screen charge (pass-through + markup)                   | 40–60% |
| **Payment processing**  | Stripe fee passthrough (2.9% + $0.30) + platform fee (0.5%) | 15–20% |
| **SMS notifications**   | Per-SMS charge (Twilio passthrough + markup)                | 30–50% |
| **E-signatures**        | Per-document or monthly charge                              | 70–80% |
| **Overage charges**     | Per-property over tier limit                                | 90%    |
| **Storage overage**     | Per-GB over tier limit                                      | 80%    |
| **White-label setup**   | One-time setup fee for Enterprise                           | 90%    |
| **Custom integrations** | Professional services for Enterprise                        | 60–70% |
| **Data export**         | API access / data warehouse export fee                      | 90%    |

### 11.2 Upgrade Paths

```
                                  ┌──────────────┐
                    ┌────────────►│  Enterprise   │
                    │             │  (Custom)     │
                    │             └──────────────┘
                    │                    ▲
┌──────────┐   ┌───┴──────────┐         │
│  Starter │──►│ Professional │─────────┘
│  ($9.99) │   │   ($199)     │
└──────────┘   └──────────────┘
      │                ▲
      │                │
      │         ┌──────┴───────┐
      └────────►│   Investor   │──────────────────►  Enterprise
                │   ($15+$99)  │
                └──────────────┘
```

**Upgrade triggers (product-led growth):**

| Trigger                        | From → To                 | Mechanism                    |
| ------------------------------ | ------------------------- | ---------------------------- |
| Hit property limit (3)         | Starter → Professional    | In-app banner + email        |
| Try to add employee            | Starter → Professional    | Feature gate → upgrade modal |
| Need vendor management         | Starter → Professional    | Feature gate → upgrade modal |
| Want ROI tracking              | Starter/Pro → Investor    | Sidebar upsell               |
| Need multi-org                 | Any → Enterprise          | Contact sales CTA            |
| Need API access                | Any → Enterprise          | Feature gate → sales         |
| Payroll add-on usage > $200/mo | Professional → Enterprise | Account review               |
| 50+ properties on Pro          | Professional → Enterprise | Proactive outreach           |

### 11.3 Trial Strategy

- **14-day free trial** of Professional plan for all new sign-ups
- Full feature access during trial
- Credit card required after trial
- Downgrade to Starter if no payment
- Trial extension (7 days) available via support — builds relationship

### 11.4 Annual Pricing Incentive

| Plan         | Monthly        | Annual (monthly equiv.)  | Savings    |
| ------------ | -------------- | ------------------------ | ---------- |
| Starter      | $9.99/prop     | $7.99/prop               | 20%        |
| Professional | $199           | $159                     | 20%        |
| Investor     | $15/prop + $99 | $12/prop + $79           | 20%        |
| Enterprise   | Custom         | Custom (10-15% discount) | Negotiated |

---

## 12. Scalability Considerations {#12-scalability}

### 12.1 Feature Gate Performance

| Concern                         | Solution                                | Status                |
| ------------------------------- | --------------------------------------- | --------------------- |
| Feature checks on every request | Redis cache (5-min TTL) per org         | Partially implemented |
| RBAC checks on every request    | JWT claims include role + permissions   | ✅ Implemented        |
| Resource limit checks           | In-memory cache with event invalidation | Needs implementation  |
| Sidebar rendering               | Client-side memoization of nav config   | Needs implementation  |

### 12.2 Multi-Tenant Data Isolation

| Layer           | Current                                                | Recommended                                   |
| --------------- | ------------------------------------------------------ | --------------------------------------------- |
| Database        | Shared DB, `organization_id` column                    | ✅ Sufficient for current scale               |
| Queries         | `@Where(clause = "organization_id = :orgId")` patterns | ✅ Implemented via `OrganizationScopedEntity` |
| API Gateway     | Organization context via headers                       | ✅ Implemented                                |
| Storage (MinIO) | Buckets per org                                        | Verify implementation                         |
| Cache (Redis)   | Key prefix per org                                     | ✅ Implemented                                |

### 12.3 Growth Scaling Tiers

| Scale                         | Users    | Properties | Architecture                                                                               |
| ----------------------------- | -------- | ---------- | ------------------------------------------------------------------------------------------ |
| **Seed** (0–100 orgs)         | 500      | 2,000      | Current — single Postgres, all services                                                    |
| **Growth** (100–1,000 orgs)   | 5,000    | 20,000     | Read replicas, Redis cluster, CDN                                                          |
| **Scale** (1,000–10,000 orgs) | 50,000   | 200,000    | Horizontal service scaling, connection pooling, partitioned tables                         |
| **Enterprise** (10,000+ orgs) | 500,000+ | 2M+        | Schema-per-org option, dedicated Postgres instances for Enterprise clients, event sourcing |

### 12.4 Feature Flag Hot-Reload

For operational agility, feature flags should be changeable without deployment:

```
Current:   OrganizationFeatureEnum → compile-time enum → requires deploy
Proposed:  Keep enum for type safety, add `feature_flags` table for runtime overrides

feature_flags table:
  - code (VARCHAR) — matches enum name
  - organization_id (nullable — NULL = global)
  - enabled (BOOLEAN)
  - percentage (INT, 0-100) — for gradual rollouts
  - updated_at (TIMESTAMP)
```

This allows:

- **Kill switches** — disable a feature globally if it's broken
- **Gradual rollouts** — enable for 10% of orgs, then 50%, then 100%
- **Beta programs** — enable features for specific orgs before general release

---

## 13. Implementation Roadmap {#13-implementation-roadmap}

### Phase 1: Foundation (Immediate)

| Task                                                                  | Effort | Priority |
| --------------------------------------------------------------------- | ------ | -------- |
| Create `organization_addons` table + Flyway migration                 | Small  | P0       |
| Create `subscription_history` table + Flyway migration                | Small  | P0       |
| Add `stripeCustomerId`, `stripeSubscriptionId` to Organization entity | Small  | P0       |
| Implement `<FeatureGate>` React component                             | Medium | P0       |
| Implement `useFeature()` React hook                                   | Medium | P0       |
| Add feature gates to sidebar navigation                               | Medium | P0       |
| Build upgrade modal / banner components                               | Medium | P1       |

### Phase 2: Billing Integration

| Task                                                | Effort | Priority |
| --------------------------------------------------- | ------ | -------- |
| Stripe Billing integration (subscription lifecycle) | Large  | P0       |
| Plan selection during onboarding                    | Medium | P0       |
| Usage-based billing (overage calculation)           | Medium | P1       |
| Add-on purchase flow (frontend + backend)           | Large  | P1       |
| Billing dashboard (current plan, usage, invoices)   | Large  | P1       |
| Plan upgrade/downgrade flow                         | Medium | P1       |

### Phase 3: Plan Enforcement

| Task                                              | Effort | Priority |
| ------------------------------------------------- | ------ | -------- |
| Enforce `ResourceLimits` on property creation     | Small  | P0       |
| Enforce `ResourceLimits` on user creation         | Small  | P0       |
| Storage quota enforcement (MinIO)                 | Medium | P1       |
| Feature-gate all Standard+ endpoints (backend)    | Medium | P0       |
| Feature-gate all Standard+ UI elements (frontend) | Large  | P0       |
| Trial period logic (14-day, automatic downgrade)  | Medium | P1       |
| Grace period for failed payments                  | Medium | P2       |

### Phase 4: Org-Type Adaptations

| Task                                                         | Effort | Priority |
| ------------------------------------------------------------ | ------ | -------- |
| Org-type terminology engine (Tenant → Resident, Rent → Dues) | Medium | P1       |
| Adapted sidebar per org type                                 | Medium | P1       |
| HA-specific pages (Board, Amenities, Voting)                 | Large  | P2       |
| REI-specific pages (Portfolio Analytics, ROI)                | Large  | P2       |
| PMC-specific pages (Owner Portal, Owner Reports)             | Large  | P2       |

### Phase 5: Advanced / Enterprise

| Task                                                | Effort | Priority |
| --------------------------------------------------- | ------ | -------- |
| API key management + rate limiting per org          | Medium | P2       |
| Webhook configuration UI                            | Medium | P2       |
| Custom role builder (Enterprise)                    | Large  | P2       |
| White-label branding settings                       | Large  | P3       |
| Multi-currency support                              | Large  | P3       |
| SSO/SAML integration                                | Large  | P3       |
| Runtime feature flag system (`feature_flags` table) | Medium | P2       |
| Cross-org analytics dashboard                       | Large  | P3       |

---

## 14. Appendix — Current vs. Proposed Gap Analysis {#14-gap-analysis}

### What Already Exists ✅

| Component                                    | Status      | Notes                                                  |
| -------------------------------------------- | ----------- | ------------------------------------------------------ |
| `OrganizationTypeEnum` (5 types)             | ✅ Complete | IPO, PMC, REI, CORP, HA                                |
| `OrganizationSubscriptionTierEnum` (5 tiers) | ✅ Complete | BASIC, PROFESSIONAL, INVESTOR, ENTERPRISE, ASSOCIATION |
| `OrganizationFeatureEnum` (25+ features)     | ✅ Complete | 7 categories                                           |
| `FeatureService`                             | ✅ Complete | Tier + org-type checks                                 |
| `ResourceLimits` / `ResourceUsage`           | ✅ Complete | maxProperties, maxUsers, maxStorageGb                  |
| `SubscriptionInfo` embedded                  | ✅ Complete | Tier, start, expiry, renewal, upgrade/downgrade        |
| `enabledFeatures` set per org                | ✅ Complete | Stored in `organization_features` join table           |
| RBAC with 32 roles                           | ✅ Complete | With `applicableOrgTypes`                              |
| `applicableOrgTypes` in rbac.yml             | ✅ Complete | Org-type-specific role restrictions                    |
| Frontend `checkFeature()` API                | ✅ Complete | Returns `{ available, reason }`                        |
| `OrganizationPricingModelEnum`               | ✅ Complete | 5 pricing models                                       |
| Stripe integration (payment-service)         | ✅ Partial  | Payment processing, not billing subscriptions          |
| `OperationalStatusEnum`                      | ✅ Complete | TRIAL, ACTIVE, SUSPENDED, etc.                         |

### What's Implemented ✅ and What's Missing 🔧

| Component                                         | Status         | Priority | Notes                                                   |
| ------------------------------------------------- | -------------- | -------- | ------------------------------------------------------- |
| **Frontend `<FeatureGate>` component**            | ✅ Implemented | P0       | `FeatureGuard.tsx` + `MultiFeatureGuard.tsx`            |
| **Frontend `useFeature()` hook**                  | ✅ Implemented | P0       | In `OrganizationContext`                                |
| **Sidebar feature gating**                        | ✅ Implemented | P0       | 5 feature gates in `DashboardSidebar.tsx`               |
| **Upgrade modal / upsell banners**                | ✅ Implemented | P0       | `FeatureLockGuard` with upgrade CTA + compact variant   |
| **`organization_addons` table**                   | ✅ Implemented | P0       | V16 Flyway migration                                    |
| **`subscription_history` table**                  | ✅ Implemented | P1       | V16 Flyway migration                                    |
| **Stripe customer/subscription IDs on Org**       | ✅ Implemented | P0       | V16 migration + Organization entity fields              |
| **Resource limit enforcement on property create** | ✅ Implemented | P0       | `PropertyService.createProperty()` → `canAddProperty()` |
| **Resource limit enforcement on user create**     | ✅ Implemented | P0       | `UserController.createUser()` → `canAddUser()`          |
| **Plan selection in onboarding**                  | ✅ Implemented | P0       | Premium step 4 in onboarding form                       |
| **Stripe Billing integration**                    | ❌ Missing     | P0       | No automated subscription billing webhook handling      |
| **Billing dashboard page**                        | ❌ Missing     | P1       | Users can't see/manage their plan                       |
| **Org-type terminology engine**                   | ❌ Missing     | P1       | HA users see "Tenants" instead of "Residents"           |
| **Trial period automation**                       | ❌ Missing     | P1       | No automatic trial → Starter downgrade                  |
| **Feature usage analytics**                       | ❌ Missing     | P2       | Can't track feature adoption                            |
| **Runtime feature flags**                         | ❌ Missing     | P2       | Can't toggle features without deploy                    |
| **Redis feature cache**                           | ⚠️ Partial     | P1       | Migration exists, implementation incomplete             |
| **HA vertical pages**                             | ❌ Missing     | P2       | Board, amenity, voting pages not built                  |
| **REI vertical pages**                            | ❌ Missing     | P2       | Portfolio analytics, ROI pages not built                |
| **PMC owner portal**                              | ❌ Missing     | P2       | Owner-facing portal not built                           |
| **API key management**                            | ❌ Missing     | P3       | Enterprise API access not configurable                  |
| **White-label settings**                          | ❌ Missing     | P3       | Enterprise branding not configurable                    |

### Priority Summary

**P0 — DONE ✅:**

- Frontend feature gating (`<FeatureGuard>`, `useFeature()`)
- Sidebar gating by feature + org type
- Resource limit enforcement (property + user creation)
- Upgrade/upsell UI (`FeatureLockGuard` with "Upgrade Plan" CTA)
- Plan selection in onboarding
- DB tables: `organization_addons`, `subscription_history`, Stripe IDs on org

**P0 — REMAINING ❌:**

- Stripe Billing webhook handler (subscription lifecycle events)

**P1 (Needed within 1–2 months post-launch):**

- Billing dashboard
- Org-type terminology and sidebar adaptation
- Trial period automation
- Subscription history tracking
- Redis feature cache completion

**P2 (Growth phase):**

- Vertical pages (HA, REI, PMC)
- Feature usage analytics
- Runtime feature flags
- API key management

**P3 (Scale phase):**

- White-label
- Multi-currency
- SSO/SAML
- Cross-org analytics
- Custom report builder

---

_This document serves as the canonical reference for Propertize's SaaS business logic, feature architecture, and plan design. All implementation should reference this document for feature-gating decisions, plan boundaries, and role-based access control._
