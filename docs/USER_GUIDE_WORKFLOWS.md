# Propertize — End-User Workflow Guide

> How Rental Applications, Tenants, Leases, and Payments work from start to finish.

---

## Overview

Propertize connects **landlords / property managers** with **tenants** through a structured digital
workflow. Every action — from the moment someone applies for a property to the day they move out
and make their final payment — follows a defined process that keeps all parties informed and
protected.

The four core areas are:

| Area                   | Who acts                     | What happens                                           |
| ---------------------- | ---------------------------- | ------------------------------------------------------ |
| **Rental Application** | Prospective tenant + Manager | Apply, screen, approve or reject                       |
| **Tenant**             | Manager                      | Track lifecycle from applicant to former tenant        |
| **Lease**              | Manager + Tenant             | Draft, sign, execute, manage, and archive the contract |
| **Payment**            | Tenant + Manager             | Collect rent, deposits, fees, and process refunds      |

---

## 1. Rental Application

### What is a Rental Application?

A rental application is the entry point for any new tenant. The prospective renter submits
personal, employment, and reference information. The property manager then reviews it, runs
checks, and decides to approve or reject.

### Application Lifecycle

```
DRAFT → SUBMITTED → PENDING → UNDER_REVIEW → SCREENING_IN_PROGRESS → APPROVED
                                                                     ↘ REJECTED
                                                                     ↘ ON_HOLD
                                                                     ↘ INFO_REQUIRED
              ↘ WITHDRAWN (applicant withdraws at any time before decision)
              ↘ CANCELLED  (manager cancels)
              ↘ EXPIRED    (no action taken within the allowed window)
```

### Step-by-Step Process

#### Step 1 — Applicant Submits

The prospective tenant fills out the application form and submits it.  
Status moves: `DRAFT → SUBMITTED`

Key information collected:

- Full name, date of birth, government ID
- Current and previous address history
- Employment details and monthly income
- Emergency contacts
- Pet and vehicle information
- References (employer, previous landlord)

#### Step 2 — Initial Review

The property manager receives the submission and opens it for review.  
Status moves: `SUBMITTED → PENDING → UNDER_REVIEW`

The manager can:

- **Assign** the application to a specific reviewer (once assigned, only that reviewer can approve or reject it)
- Request additional information from the applicant (`INFO_REQUIRED`)
- Place the application on hold (`ON_HOLD`)

#### Step 3 — Screening

Background and credit checks are initiated.  
Status moves: `UNDER_REVIEW → SCREENING_IN_PROGRESS` (or `BACKGROUND_CHECK_IN_PROGRESS`)

The system tracks:

- Background check provider response
- Credit score thresholds
- Criminal record results
- Eviction history

The manager can view check results at any time via `GET /api/v1/applications/{id}/background-check`.

#### Step 4 — Decision

**Approve:**  
Status moves: `SCREENING_IN_PROGRESS → APPROVED`

When approved, the system automatically:

1. Creates a **Tenant record** (with status `APPLICANT`) from the application snapshot
2. Creates a **Lease in DRAFT** for the linked property
3. Notifies the applicant by email/SMS

**Reject:**  
Status moves: `SCREENING_IN_PROGRESS → REJECTED`  
The manager must provide a rejection reason. The applicant is notified.

**Withdraw (applicant-initiated):**  
The applicant can withdraw at any point before a decision.  
Status moves: → `WITHDRAWN`

---

## 2. Tenant

### What is a Tenant Record?

A Tenant record is created as soon as an application is approved. It tracks the person's full
lifecycle with your organisation — from initial applicant all the way through to a former or
former-bad-standing record.

### Tenant Lifecycle

```
APPLICANT → SCREENING → APPROVED → ACTIVE → NOTICE_GIVEN → MOVING_OUT → FORMER
                                                                        ↘ EVICTED
             ↘ REJECTED
             ↘ BLACKLISTED  (admin action, permanent)
             ↘ SUSPENDED    (temporary hold)
             ↘ INACTIVE     (account deactivated)
```

#### Status Meanings

| Status         | Meaning                                      | Can Access Property? |
| -------------- | -------------------------------------------- | -------------------- |
| `APPLICANT`    | Application submitted, awaiting review       | No                   |
| `SCREENING`    | Background/credit checks in progress         | No                   |
| `APPROVED`     | Application approved, waiting to move in     | Yes                  |
| `ACTIVE`       | Currently residing in the property           | Yes                  |
| `NOTICE_GIVEN` | Tenant has formally given move-out notice    | Yes                  |
| `MOVING_OUT`   | In the process of vacating                   | Yes                  |
| `FORMER`       | Has moved out, no active tenancy             | No                   |
| `REJECTED`     | Application was rejected                     | No                   |
| `BLACKLISTED`  | Permanently banned from renting              | No                   |
| `SUSPENDED`    | Temporarily suspended (e.g. payment dispute) | No                   |
| `INACTIVE`     | Account deactivated                          | No                   |
| `EVICTED`      | Legally removed from property                | No                   |

### Key Tenant Actions

| Action           | Endpoint                                   | Description                             |
| ---------------- | ------------------------------------------ | --------------------------------------- |
| View all tenants | `GET /api/v1/tenants`                      | Filtered list with pagination           |
| View one tenant  | `GET /api/v1/tenants/{id}`                 | Full profile including payment history  |
| Update profile   | `PUT /api/v1/tenants/{id}`                 | Update name, contact, financial details |
| Patch profile    | `PATCH /api/v1/tenants/{id}`               | Update only specific fields             |
| Change status    | `PATCH /api/v1/tenants/{id}/status`        | Move through lifecycle states           |
| Payment history  | `GET /api/v1/tenants/{id}/payment-history` | All payments associated with tenant     |
| Statistics       | `GET /api/v1/tenants/statistics`           | Aggregate counts by status              |

### Tenant Profile Fields

A tenant profile stores:

- **Identity**: first, middle, last name, preferred name, date of birth, government ID
- **Contact**: email, phone, alternate phone
- **Current tenancy**: property address, unit, lease start/end dates, rent amount
- **Financial**: security deposit, move-in costs, last month rent paid
- **References**: employer name, previous landlord details
- **Emergency contact**: name, relationship, phone

---

## 3. Lease

### What is a Lease?

A lease is the legally binding contract between the landlord and the tenant. Propertize manages
the full lifecycle of a lease digitally — from drafting, through the e-signature process, to
archival after the tenancy ends.

### Lease Lifecycle

```
DRAFT → PENDING_SIGNATURES → EXECUTED → ARCHIVED
  ↕          ↕                 ↓
  ↓       RECALLED            EXPIRED
PENDING → ACTIVE → TERMINATED
           ↓
        RENEWED → (new lease)
           ↓
        INACTIVE
        CANCELLED
```

#### Status Meanings

| Status               | Meaning                                              | Editable? |
| -------------------- | ---------------------------------------------------- | --------- |
| `DRAFT`              | Being written/edited by the manager                  | ✅ Yes    |
| `PENDING`            | Awaiting approval or additional review               | No        |
| `PENDING_SIGNATURES` | Sent for e-signature; awaiting all parties           | No        |
| `ACTIVE`             | Fully signed and in force                            | No        |
| `INACTIVE`           | Active lease placed on hold                          | No        |
| `EXPIRED`            | End date has passed                                  | No        |
| `TERMINATED`         | Ended early by either party                          | No        |
| `RENEWED`            | Extended with new terms (new lease created)          | No        |
| `CANCELLED`          | Cancelled before activation                          | No        |
| `EXECUTED`           | All signatures collected; tamper-proof and immutable | No        |
| `ARCHIVED`           | Filed for record-keeping; final terminal state       | No        |

### Step-by-Step Lease Process

#### Step 1 — Create the Draft

When a rental application is approved, a lease is automatically created in `DRAFT` status.  
The manager can also create one manually: `POST /api/v1/leases/create`

In DRAFT, the manager can:

- Edit all lease terms (dates, rent amount, deposit, pet policy, utility inclusion, etc.)
- Add or remove clauses (each clause can be marked `required` or optional)
- Set payment schedule and grace period
- Assign a tenant: `PATCH /api/v1/leases/{id}/assign-tenant`

> **Only DRAFT leases are fully editable.** Once sent for signatures, content is locked.

#### Step 2 — Send for E-Signature

When the lease is ready:  
`POST /api/v1/leases/{id}/send-for-signature`

What happens automatically:

1. All draft clauses are **snapshotted** (a permanent immutable copy is saved)
2. The lease content is **locked** — no further edits are possible
3. Status moves: `DRAFT → PENDING_SIGNATURES`
4. All signing parties receive a signature request

#### Step 3 — Signing Period

Each party (tenant, landlord, co-signers if applicable) reviews and signs the document.

If the manager needs to make changes after sending:  
`POST /api/v1/leases/{id}/recall?reason=...`

This **recalls** the lease:

- Outstanding signature envelopes are voided
- Draft clauses are restored
- Status moves back: `PENDING_SIGNATURES → DRAFT`
- The manager can edit and re-send

#### Step 4 — Execute

Once all signatures are collected:  
`POST /api/v1/leases/{id}/execute`

- Status moves: `PENDING_SIGNATURES → EXECUTED`
- A signed certificate key and document hash are stored for legal audit trail
- The lease is now **immutable**

#### Step 5 — Active Tenancy

Status is moved to `ACTIVE` when the start date arrives (or manually by the manager).

During active tenancy the manager can:

- Download the lease PDF: `GET /api/v1/leases/{id}/document`
- Send expiration reminders: `POST /api/v1/leases/{id}/notify-expiration`
- View lease statistics: `GET /api/v1/leases/statistics`

#### Step 6 — End of Tenancy

**Renewal:**  
`POST /api/v1/leases/{id}/renew`  
Provide a new end date and optionally a new monthly rent.  
Status moves: `ACTIVE → RENEWED`. A new lease is created automatically.

**Termination:**  
`POST /api/v1/leases/{id}/terminate`  
Provide termination date and reason.  
Status moves: `ACTIVE → TERMINATED`

**Natural Expiry:**  
When the end date passes the status moves automatically: `ACTIVE → EXPIRED`

**Archival:**  
Final terminal state: `EXECUTED → ARCHIVED`  
The signed lease is stored permanently for record-keeping.

#### Lease Types

| Type             | Description                                     |
| ---------------- | ----------------------------------------------- |
| `FIXED_TERM`     | Set start and end date (e.g. 12-month tenancy)  |
| `MONTH_TO_MONTH` | Rolls over month-by-month until notice is given |
| `WEEK_TO_WEEK`   | Short-term, rolls weekly                        |
| `COMMERCIAL`     | Commercial property lease                       |
| `SUBLEASE`       | Tenant sublets to a sub-tenant                  |

---

## 4. Payments

### What is a Payment?

A payment records any financial transaction between a tenant and the organisation. This includes
rent, security deposits, late fees, maintenance charges, and more.

### Payment Lifecycle

```
PENDING → COMPLETED
        ↘ FAILED → (retry) → COMPLETED
                            ↘ FAILED (final)
COMPLETED → REFUNDED
          ↘ PARTIALLY_REFUNDED
```

#### Status Meanings

| Status               | Meaning                                                     |
| -------------------- | ----------------------------------------------------------- |
| `PENDING`            | Payment initiated, awaiting processing                      |
| `COMPLETED`          | Successfully collected                                      |
| `FAILED`             | Processing failed (insufficient funds, gateway error, etc.) |
| `REFUNDED`           | Full amount returned to payer                               |
| `PARTIALLY_REFUNDED` | A portion of the amount returned                            |

### Payment Categories

| Category                | Description                                          |
| ----------------------- | ---------------------------------------------------- |
| `RENT`                  | Monthly rent payment                                 |
| `SECURITY_DEPOSIT`      | One-time deposit held against damage                 |
| `LATE_FEE`              | Fee applied when rent is paid after the grace period |
| `UTILITY_PAYMENT`       | Utility bill (water, electricity, etc.)              |
| `MAINTENANCE_PAYMENT`   | Charge for maintenance work                          |
| `PLATFORM_SUBSCRIPTION` | Manager's subscription fee to Propertize             |
| `OWNER_PAYOUT`          | Disbursement to property owner                       |

### Key Payment Actions

| Action          | Endpoint                                 | Description                  |
| --------------- | ---------------------------------------- | ---------------------------- |
| Create payment  | `POST /api/v1/payments`                  | Record a new payment         |
| Process payment | `POST /api/v1/payments/{id}/process`     | Submit to payment gateway    |
| Refund payment  | `POST /api/v1/payments/{id}/refund`      | Issue full or partial refund |
| View by tenant  | `GET /api/v1/payments/tenant/{tenantId}` | All payments for a tenant    |
| View by lease   | `GET /api/v1/payments/lease/{leaseId}`   | All payments on a lease      |
| Update payment  | `PATCH /api/v1/payments/{id}`            | Correct metadata or notes    |

### Rent Payment Flow (typical monthly cycle)

```
1. Manager creates a PENDING payment record for the month's rent
2. Tenant pays via the connected payment gateway
3. System receives gateway confirmation → status: COMPLETED
4. If payment is after the due date + grace period → late fee is automatically calculated
5. If payment fails → retry logic kicks in (tracked via retryCount + lastRetryAt)
6. Manager can view full history via GET /api/v1/tenants/{id}/payment-history
```

### Late Payments

The system automatically detects if a payment is late by comparing `paymentDate` against
`dueDate + gracePeriodDays` (configured on the lease). Late payments:

- Set the `isLate` flag on the payment record
- Calculate and store `daysLate`
- Can trigger a `LATE_FEE` payment to be created

---

## 5. Complete End-to-End Example

Here is the full journey for a typical new tenant:

```
1. APPLY
   Prospective tenant submits a rental application online.
   Status: Application = SUBMITTED

2. REVIEW
   Property manager reviews the application and assigns it to a reviewer.
   Status: Application = UNDER_REVIEW

3. SCREEN
   Background and credit checks are initiated automatically.
   Status: Application = SCREENING_IN_PROGRESS

4. APPROVE
   Manager approves the application.
   → Tenant record is created (status: APPLICANT → APPROVED)
   → Lease is auto-created in DRAFT
   Status: Application = APPROVED

5. DRAFT LEASE
   Manager fills out all lease terms and clauses in the draft.
   Assigns the approved tenant to the lease.
   Status: Lease = DRAFT

6. SEND FOR SIGNATURE
   Manager sends the completed draft for e-signature.
   → All clauses are snapshotted and locked
   Status: Lease = PENDING_SIGNATURES
   Status: Tenant = APPROVED (awaiting move-in)

7. SIGNATURES COLLECTED
   Tenant and landlord both sign the lease digitally.
   Manager executes the lease.
   Status: Lease = EXECUTED

8. MOVE IN
   Start date arrives. Manager activates the lease.
   → Tenant's portal credentials are provisioned
   Status: Lease = ACTIVE, Tenant = ACTIVE

9. MONTHLY RENT
   Each month a RENT payment is created and processed.
   Status: Payment = PENDING → COMPLETED

10. RENEWAL OR NOTICE
    Option A: Tenant agrees to renew → new lease created
              Status: Old Lease = RENEWED, New Lease = DRAFT
    Option B: Tenant gives notice to vacate
              Status: Tenant = NOTICE_GIVEN → MOVING_OUT

11. MOVE OUT
    Tenant vacates. Security deposit is reviewed.
    Status: Tenant = FORMER, Lease = TERMINATED or EXPIRED

12. ARCHIVAL
    Executed lease is archived for legal record-keeping.
    Status: Lease = ARCHIVED
```

---

## 6. Notifications

Propertize sends automated notifications at key points:

| Event                               | Who is notified     |
| ----------------------------------- | ------------------- |
| Application received                | Manager             |
| Application approved / rejected     | Applicant           |
| Lease sent for signature            | All signing parties |
| Lease executed                      | Tenant + Manager    |
| Rent payment received               | Tenant (receipt)    |
| Payment failed                      | Tenant + Manager    |
| Lease expiring in 90 / 60 / 30 days | Tenant + Manager    |
| Lease terminated                    | Tenant + Manager    |

---

## 7. Roles and Permissions

Access to each action is controlled by role:

| Role                 | Can Do                                                |
| -------------------- | ----------------------------------------------------- |
| **Admin**            | Full access to all operations                         |
| **Property Manager** | Create/manage applications, leases, tenants, payments |
| **Landlord / Owner** | View leases and payments for their properties         |
| **Tenant**           | View own lease, submit payments, track application    |
| **Reviewer**         | Review and approve/reject assigned applications       |

All requests pass through the API Gateway which validates the JWT token and enforces permissions
before forwarding the request to the relevant service.

---

_Document reflects the Propertize platform as of April 2026._
