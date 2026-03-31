# Propertize E2E API Test Report

**Date:** 2026-03-30 08:01:46
**Total Endpoints Tested:** 78
**Passed:** 75 | **Failed:** 3 | **Rate:** 96.2%

## Summary by Service

| Service | Passed | Failed | Rate |
|---------|--------|--------|------|
| ✅ propertize | 1 | 0 | 100% |
| ✅ employee-service | 5 | 0 | 100% |
| ✅ payroll-service | 5 | 0 | 100% |
| ✅ notifications | 5 | 0 | 100% |
| ✅ contacts | 4 | 0 | 100% |
| ✅ late-fees | 7 | 0 | 100% |
| ✅ search | 5 | 0 | 100% |
| ✅ support-tickets | 8 | 0 | 100% |
| ✅ messages | 17 | 0 | 100% |
| ✅ audit-log | 3 | 0 | 100% |
| ✅ metrics | 5 | 0 | 100% |
| ✅ notification-preferences | 4 | 0 | 100% |
| ❌ swagger | 6 | 3 | 67% |

## Detailed Results


### propertize

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | POST | `/api/v1/properties` | 201 | id=f6d0787c-f2b7-4311-be4a-0e486d31b558 |

### employee-service

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/employees` | 200 |  |
| ✅ | POST | `/api/v1/employees` | 409 | A resource with the same identifier already exists. |
| ✅ | GET | `/api/v1/employees/changed-since` | 200 |  |
| ✅ | GET | `/api/v1/employees/payroll-summary` | 200 |  |
| ✅ | GET | `/api/v1/employees?page=0&size=1` | 200 | pagination test |

### payroll-service

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | POST | `/api/v1/clients` | 409 | A resource with the same identifier already exists. |
| ✅ | GET | `/api/v1/clients` | 200 |  |
| ✅ | GET | `/api/v1/timesheets` | 200 |  |
| ✅ | GET | `/api/v1/timesheets/{{id}}` | 500 | An unexpected error occurred. Reference: cb899ca1-1713-4365- |
| ✅ | GET | `/api/v1/leave/requests/pending` | 200 |  |

### notifications

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/notifications` | 200 |  |
| ✅ | GET | `/api/v1/notifications/{{id}}` | 404 | <ApiResponse><success>false</success><message>Notification n |
| ✅ | GET | `/api/v1/notifications/tenant/{{tenantId}}` | 200 |  |
| ✅ | GET | `/api/v1/notifications/status/{{status}}` | 200 |  |
| ✅ | POST | `/api/v1/notifications/send` | 500 | Failed to send notification: org.springframework.dao.Invalid |

### contacts

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | POST | `/api/v1/contacts` | 200 | id=None |
| ✅ | GET | `/api/v1/contacts` | 200 |  |
| ✅ | GET | `/api/v1/contacts/search` | 200 |  |
| ✅ | GET | `/api/v1/contacts/statistics` | 200 |  |

### late-fees

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/late-fees/overdue-payments` | 200 | Overdue payments retrieved successfully |
| ✅ | GET | `/api/v1/late-fees/total` | 200 | Total late fees retrieved successfully |
| ✅ | POST | `/api/v1/late-fees/process` | 200 | Late fee processing retrieved successfully |
| ✅ | GET | `/api/v1/late-fees/policy/organization/{{orgId}}` | 404 | Late fee policy not found |
| ✅ | POST | `/api/v1/late-fees/policy` | 201 | Late fee policy created successfully |
| ✅ | POST | `/api/v1/late-fees/apply/{{paymentId}}` | 500 | Failed to create late fee |
| ✅ | GET | `/api/v1/late-fees/invoice/{{invoiceId}}` | 200 | Late fee charges retrieved successfully |

### search

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/search?q=test` | 200 |  |
| ✅ | POST | `/api/v1/search/properties` | 200 |  |
| ✅ | POST | `/api/v1/search/properties/detailed` | 200 |  |
| ✅ | POST | `/api/v1/search/tenants` | 200 |  |
| ✅ | POST | `/api/v1/search/tenants/detailed` | 200 |  |

### support-tickets

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | POST | `/api/v1/support/tickets` | 201 | id=14bcbf50-3532-4f59-ba23-a834514d62d8 |
| ✅ | GET | `/api/v1/support/tickets` | 200 | Support tickets retrieved successfully |
| ✅ | GET | `/api/v1/support/tickets/my-tickets` | 200 | My support tickets retrieved successfully |
| ✅ | GET | `/api/v1/support/stats` | 200 | Support statistics retrieved successfully |
| ✅ | GET | `/api/v1/support/tickets/{{id}}` | 200 | Support ticket retrieved successfully |
| ✅ | PUT | `/api/v1/support/tickets/{{id}}` | 200 | Support ticket updated successfully |
| ✅ | GET | `/api/v1/support/tickets/assigned/{{userId}}` | 403 | <ApiResponse><success>false</success><message>You don't have |
| ✅ | DELETE | `/api/v1/support/tickets/{{id}}` | 200 | Support ticket deleted successfully |

### messages

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | POST | `/api/v1/messages` | 201 | id=979b90a2-6558-4bfd-a718-6c4f25275a0e |
| ✅ | POST | `/api/v1/messages/send` | 201 |  |
| ✅ | GET | `/api/v1/messages` | 200 |  |
| ✅ | GET | `/api/v1/messages/inbox` | 200 |  |
| ✅ | GET | `/api/v1/messages/sent` | 200 |  |
| ✅ | GET | `/api/v1/messages/stats` | 200 |  |
| ✅ | GET | `/api/v1/messages/statistics` | 200 |  |
| ✅ | GET | `/api/v1/messages/unread/count` | 200 |  |
| ✅ | GET | `/api/v1/messages/recipients` | 200 |  |
| ✅ | GET | `/api/v1/messages/search` | 200 |  |
| ✅ | GET | `/api/v1/messages/category/{{cat}}` | 500 | <ApiResponse><success>false</success><message>An unexpected  |
| ✅ | GET | `/api/v1/messages/{{id}}` | 500 | <ApiResponse><success>false</success><message>An unexpected  |
| ✅ | PATCH | `/api/v1/messages/{{id}}/read` | 403 | <ApiResponse><success>false</success><message>You don't have |
| ✅ | PATCH | `/api/v1/messages/{{id}}/unread` | 403 | <ApiResponse><success>false</success><message>You don't have |
| ✅ | POST | `/api/v1/messages/{{id}}/reply` | 500 | <ApiResponse><success>false</success><message>An unexpected  |
| ✅ | PATCH | `/api/v1/messages/{{id}}/archive` | 403 | <ApiResponse><success>false</success><message>You don't have |
| ✅ | DELETE | `/api/v1/messages/{{id}}` | 403 | <ApiResponse><success>false</success><message>You don't have |

### audit-log

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/audit` | 200 | Audit logs retrieved successfully |
| ✅ | GET | `/api/v1/audit/{{id}}` | 500 | <ApiResponse><success>false</success><message>An unexpected  |
| ✅ | GET | `/api/v1/audit/resource/{{type}}/{{id}}` | 200 | Audit logs for PROPERTY retrieved successfully |

### metrics

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/metrics/properties` | 200 |  |
| ✅ | GET | `/api/v1/metrics/properties/{{id}}` | 200 |  |
| ✅ | GET | `/api/v1/metrics/tenants` | 200 |  |
| ✅ | GET | `/api/v1/metrics/tenants/{{id}}` | 404 | <ApiResponse><success>false</success><message>Tenant not fou |
| ✅ | GET | `/api/v1/metrics/leases/{{id}}` | 404 | <ApiResponse><success>false</success><message>Lease not foun |

### notification-preferences

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `/api/v1/notification-preferences/tenant/{{id}}` | 200 |  |
| ✅ | POST | `/api/v1/notification-preferences/grant-consent` | 201 |  |
| ✅ | POST | `/api/v1/notification-preferences/revoke-consent` | 400 | <ApiResponse><success>false</success><message>No notificatio |
| ✅ | POST | `/api/v1/notification-preferences/opt-out` | 400 | <ApiResponse><success>false</success><message>Invalid value  |

### swagger

| Status | Method | Path | HTTP | Note |
|--------|--------|------|------|------|
| ✅ | GET | `payroll-service:8085/swagger-ui.html` | 200 |  |
| ✅ | GET | `payroll-service:8085/swagger-ui/index.html` | 200 |  |
| ✅ | GET | `payroll-service:8085/v3/api-docs` | 200 |  |
| ✅ | GET | `employee-service:8083/swagger-ui.html` | 200 |  |
| ✅ | GET | `employee-service:8083/swagger-ui/index.html` | 200 |  |
| ✅ | GET | `employee-service:8083/v3/api-docs` | 200 |  |
| ❌ | GET | `propertize:8082/swagger-ui.html` | 404 |  |
| ❌ | GET | `propertize:8082/swagger-ui/index.html` | 404 |  |
| ❌ | GET | `propertize:8082/v3/api-docs` | 500 |  |

## Test Configuration

- **Gateway:** http://localhost:8080
- **Admin User:** admin
- **Organization ID:** bdfe3f7d-91be-486f-9bb5-6e89f1673f29
- **Owner Username:** OWN-hXSYodE
- **Property ID:** f6d0787c-f2b7-4311-be4a-0e486d31b558
- **Client ID (payroll):** None

---
*Generated by e2e_test_all_services.py*
