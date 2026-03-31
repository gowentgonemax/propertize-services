# Backend–Frontend Gap Analysis

> Generated: June 2025

## Summary

| Metric                        | Value | Coverage |
| ----------------------------- | ----- | -------- |
| Backend Controllers           | 72    | 100%     |
| Fully Mapped (page + service) | 37    | 51%      |
| Partially Mapped              | 18    | 25%      |
| Unmapped / Diagnostic         | 17    | 24%      |
| Frontend Pages                | ~105  | 100%     |
| Pages with Backend Support    | ~89   | 85%      |
| Frontend Service Files        | ~64   | —        |
| Services Calling Real APIs    | ~52   | 81%      |

---

## Propertize Core (45 Controllers)

### Fully Covered (32)

Property, Tenant, Lease, Invoice, Expense, Maintenance, Task, Notification,
Rental Application, Message, User, Vendor, Asset, Approval, Audit, Document,
Milestone, Report, Organization, Analytics, Metrics, Admin Stats,
Admin Org Management, Next Auth Session, Public Contact, Contact, Search,
Notification Preference, Support Ticket, Schedule Event (page only), etc.

### Partial / Embedded (8)

| Controller                                      | Gap                                    |
| ----------------------------------------------- | -------------------------------------- |
| InspectionController `/inspections`             | Service exists, no standalone page     |
| ScheduleEventController `/schedule-events`      | Page exists, no dedicated service file |
| TenantProfileController `/tenants/{id}/profile` | Embedded inside tenant detail page     |
| ContactController `/contacts`                   | Service exists, no CRUD page           |
| LogController `/logs`                           | Diagnostic, service exists             |
| NotificationPreferenceController                | Service exists, settings-embedded      |
| SMSNotificationController `/notifications/sms`  | Not wired in frontend                  |
| TenantScreeningController `/screening`          | Backend-only process                   |

### Unused / Diagnostic (5)

- DebugController `/debug`
- MailDiagnosticsController `/mail`
- ValidationDiagnosticController `/diagnostics`
- MemoryManagementController `/admin/memory`
- TestCredentialsController `/test/credentials`

These are admin/dev tools — no frontend needed.

---

## Employee Service (2 Controllers)

| Controller                             | Status                                                           |
| -------------------------------------- | ---------------------------------------------------------------- |
| EmployeeController `/api/v1/employees` | **FULL** — employee.service.ts + `/dashboard/employees/**` pages |
| HealthCheckController                  | Infra — no frontend needed                                       |

---

## Payment Service (11 Controllers)

### Covered (4)

| Controller                        | Frontend                                   |
| --------------------------------- | ------------------------------------------ |
| PaymentController `/payments`     | payment.service + `/dashboard/payments/**` |
| StripePaymentController `/stripe` | stripe.service + payment modals            |
| TransactionHistoryController      | transaction.service + payment detail page  |
| StripeWebhookController           | Infra (webhook) — no frontend              |

### Backend Only (2)

| Controller                                     | Notes                                     |
| ---------------------------------------------- | ----------------------------------------- |
| PaymentContextController `/payments`           | Vendor/platform/owner payouts — automated |
| PaymentReminderController `/payment-reminders` | Automated reminders                       |

### Unmapped / Missing @RequestMapping (5)

| Controller                           | Issue                                                         |
| ------------------------------------ | ------------------------------------------------------------- |
| ApplicationFeeController             | No `@RequestMapping` path defined                             |
| OrganizationApplicationFeeController | No `@RequestMapping` path defined                             |
| PaymentMethodController              | No `@RequestMapping` — frontend can't manage payment methods  |
| StripeCustomerController             | No `@RequestMapping` — frontend can't manage Stripe customers |
| PromoCodeController `/promo-codes`   | Page exists but no service file                               |

**Action needed:** Add `@RequestMapping` annotations to the 4 unmapped controllers and create frontend service files.

---

## Payroll Service (9 Controllers)

### Covered (3)

| Controller                                      | Frontend                                  |
| ----------------------------------------------- | ----------------------------------------- |
| PayrollController `/clients/{clientId}/payroll` | payroll.service + `/dashboard/payroll/**` |
| TimesheetController `/api/v1/timesheets`        | payroll.service + timesheets page         |
| LeaveController `/api/v1/leave`                 | payroll.service + leaves page             |

### Partial (3)

| Controller                                 | Gap                                                                    |
| ------------------------------------------ | ---------------------------------------------------------------------- |
| EmployeeController (payroll)               | Page exists at `/dashboard/payroll/employees`, no dedicated service    |
| CompensationController                     | Page exists at `/dashboard/payroll/compensation`, no dedicated service |
| TimeEntryController `/api/v1/time-entries` | Backend only — no frontend page                                        |

### Unmapped (3)

| Controller               | Issue                        |
| ------------------------ | ---------------------------- |
| DepartmentController     | No `@RequestMapping` visible |
| ClientController         | No `@RequestMapping` visible |
| EmployeeEntityController | Internal data model          |

### API Path Mismatch

- Frontend may call `/api/v1/leaves` (plural) but LeaveController is at `/api/v1/leave` (singular) — verify at runtime.

---

## Auth Service (5 Controllers)

| Controller                   | Status                                                            |
| ---------------------------- | ----------------------------------------------------------------- |
| AuthController `/auth`       | **FULL** — login, logout, token refresh, password reset           |
| RbacController `/auth`       | Partial — permissions guide page, but no admin RBAC management UI |
| RbacPublicController `/rbac` | Used internally in role dropdowns                                 |
| UserManagementController     | Covered via user.service                                          |
| HealthCheckController        | Infra                                                             |

---

## High Priority Recommendations

1. **Payment service:** Add `@RequestMapping` paths to ApplicationFeeController, OrganizationApplicationFeeController, PaymentMethodController, StripeCustomerController
2. **Promo codes:** Create `promo-code.service.ts` frontend service file
3. **Payroll leave path:** Verify singular vs plural path mismatch
4. **Schedule events:** Wire `ScheduleEventController` to a frontend service

## Medium Priority

1. Create payment method management page (user-facing)
2. Create promo code admin page with proper service file
3. Wire InspectionsController to dedicated page
4. Implement time entry UI for payroll

## Low Priority (Cleanup)

1. Remove or gate diagnostic controllers behind admin profile
2. Merge UtilityController + UtilController
3. Move TestCredentialsController to test profile only
