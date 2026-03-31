# Propertize Platform — API Endpoint Inventory

**Generated**: 2026-03-30  
**Total Endpoints**: 300+  
**Format**: `SERVICE | HTTP_METHOD | PATH | CONTROLLER | AUTH`

---

## 1. AUTH-SERVICE (port 8081)

### AuthController — `/api/v1/auth`

| #   | Method | Path                                | Auth |
| --- | ------ | ----------------------------------- | ---- |
| 1   | POST   | `/api/v1/auth/login`                | no   |
| 2   | POST   | `/api/v1/auth/refresh`              | no   |
| 3   | POST   | `/api/v1/auth/logout`               | no   |
| 4   | POST   | `/api/v1/auth/forgot-password`      | no   |
| 5   | POST   | `/api/v1/auth/reset-password`       | no   |
| 6   | GET    | `/api/v1/auth/validate-reset-token` | no   |
| 7   | POST   | `/api/v1/auth/change-password`      | no   |
| 8   | GET    | `/api/v1/auth/me`                   | no   |
| 9   | PUT    | `/api/v1/auth/me`                   | no   |
| 10  | POST   | `/api/v1/auth/switch-organization`  | no   |

### RbacController — `/api/v1/auth`

| #   | Method | Path                                                       | Auth |
| --- | ------ | ---------------------------------------------------------- | ---- |
| 11  | POST   | `/api/v1/auth/authorize`                                   | no   |
| 12  | GET    | `/api/v1/auth/permissions/{role}`                          | no   |
| 13  | POST   | `/api/v1/auth/permissions/resolve`                         | no   |
| 14  | POST   | `/api/v1/auth/permissions/check`                           | no   |
| 15  | GET    | `/api/v1/auth/roles`                                       | no   |
| 16  | GET    | `/api/v1/auth/roles/{role}`                                | no   |
| 17  | GET    | `/api/v1/auth/roles/scope/{scope}`                         | no   |
| 18  | GET    | `/api/v1/auth/rbac/config`                                 | no   |
| 19  | GET    | `/api/v1/auth/rbac/endpoints`                              | no   |
| 20  | POST   | `/api/v1/auth/cache/invalidate`                            | no   |
| 21  | POST   | `/api/v1/auth/temporal-permissions`                        | no   |
| 22  | GET    | `/api/v1/auth/temporal-permissions/user/{userId}`          | no   |
| 23  | DELETE | `/api/v1/auth/temporal-permissions/{id}`                   | no   |
| 24  | GET    | `/api/v1/auth/temporal-permissions/granted-by/{grantorId}` | no   |
| 25  | POST   | `/api/v1/auth/composite-roles`                             | no   |
| 26  | GET    | `/api/v1/auth/composite-roles`                             | no   |
| 27  | GET    | `/api/v1/auth/composite-roles/{id}`                        | no   |
| 28  | DELETE | `/api/v1/auth/composite-roles/{id}`                        | no   |
| 29  | POST   | `/api/v1/auth/resolve-permissions`                         | no   |
| 30  | POST   | `/api/v1/auth/delegations`                                 | no   |
| 31  | GET    | `/api/v1/auth/delegations/by-me`                           | no   |
| 32  | GET    | `/api/v1/auth/delegations/to-me`                           | no   |
| 33  | DELETE | `/api/v1/auth/delegations/{id}`                            | no   |
| 34  | GET    | `/api/v1/auth/delegation-rules`                            | no   |
| 35  | POST   | `/api/v1/auth/custom-roles`                                | no   |
| 36  | GET    | `/api/v1/auth/custom-roles`                                | no   |
| 37  | GET    | `/api/v1/auth/custom-roles/{id}`                           | no   |
| 38  | PUT    | `/api/v1/auth/custom-roles/{id}`                           | no   |
| 39  | DELETE | `/api/v1/auth/custom-roles/{id}`                           | no   |
| 40  | GET    | `/api/v1/auth/rbac/audit-logs`                             | no   |
| 41  | GET    | `/api/v1/auth/rbac/audit-logs/denials`                     | no   |
| 42  | GET    | `/api/v1/auth/rbac/audit-logs/summary`                     | no   |
| 43  | POST   | `/api/v1/auth/rbac/ip-rules`                               | no   |
| 44  | GET    | `/api/v1/auth/rbac/ip-rules`                               | no   |
| 45  | DELETE | `/api/v1/auth/rbac/ip-rules/{id}`                          | no   |
| 46  | POST   | `/api/v1/auth/rbac/ip-rules/check`                         | no   |
| 47  | GET    | `/api/v1/auth/fields/{resource}/{role}`                    | no   |
| 48  | POST   | `/api/v1/auth/fields/resolve`                              | no   |
| 49  | GET    | `/api/v1/auth/fields/resources`                            | no   |
| 50  | POST   | `/api/v1/auth/scope/resolve`                               | no   |
| 51  | GET    | `/api/v1/auth/conditions/{role}/{permission}`              | no   |
| 52  | POST   | `/api/v1/auth/time/check`                                  | no   |

### RbacPublicController — `/api/v1/rbac`

| #   | Method | Path                       | Auth |
| --- | ------ | -------------------------- | ---- |
| 53  | GET    | `/api/v1/rbac/roles`       | no   |
| 54  | GET    | `/api/v1/rbac/permissions` | no   |

### CustomRoleController — `/api/v1/rbac`

| #   | Method | Path                                             | Auth |
| --- | ------ | ------------------------------------------------ | ---- |
| 55  | POST   | `/api/v1/rbac/custom-roles`                      | no   |
| 56  | GET    | `/api/v1/rbac/custom-roles`                      | no   |
| 57  | GET    | `/api/v1/rbac/custom-roles/{id}`                 | no   |
| 58  | PUT    | `/api/v1/rbac/custom-roles/{id}`                 | no   |
| 59  | DELETE | `/api/v1/rbac/custom-roles/{id}`                 | no   |
| 60  | POST   | `/api/v1/rbac/custom-roles/{id}/assign`          | no   |
| 61  | DELETE | `/api/v1/rbac/custom-roles/{id}/assign/{userId}` | no   |
| 62  | GET    | `/api/v1/rbac/users/{userId}/custom-roles`       | no   |

### UserManagementController — `/api/v1/users`

| #   | Method | Path                                | Auth |
| --- | ------ | ----------------------------------- | ---- |
| 63  | GET    | `/api/v1/users`                     | no   |
| 64  | POST   | `/api/v1/users`                     | no   |
| 65  | GET    | `/api/v1/users/{id}`                | no   |
| 66  | GET    | `/api/v1/users/username/{username}` | no   |
| 67  | GET    | `/api/v1/users/email/{email}`       | no   |
| 68  | PUT    | `/api/v1/users/{id}`                | no   |
| 69  | PUT    | `/api/v1/users/{id}/password`       | no   |

### HealthCheckController — `/api/health`

| #   | Method | Path          | Auth |
| --- | ------ | ------------- | ---- |
| 70  | GET    | `/api/health` | no   |

---

## 2. PROPERTIZE — Core Service (port 8082)

### PropertyController — `/api/v1/properties`

| #   | Method | Path                                                         | Auth |
| --- | ------ | ------------------------------------------------------------ | ---- |
| 1   | GET    | `/api/v1/properties`                                         | no   |
| 2   | GET    | `/api/v1/properties/{id}`                                    | no   |
| 3   | GET    | `/api/v1/properties/statistics`                              | no   |
| 4   | POST   | `/api/v1/properties`                                         | no   |
| 5   | POST   | `/api/v1/properties/bulk`                                    | no   |
| 6   | PUT    | `/api/v1/properties/{id}`                                    | no   |
| 7   | DELETE | `/api/v1/properties/{id}`                                    | no   |
| 8   | GET    | `/api/v1/properties/public`                                  | no   |
| 9   | GET    | `/api/v1/properties/public/available`                        | no   |
| 10  | GET    | `/api/v1/properties/public/listing/{listingCode}`            | no   |
| 11  | GET    | `/api/v1/properties/organization/{organizationId}`           | no   |
| 12  | GET    | `/api/v1/properties/by-organization-code/{organizationCode}` | no   |
| 13  | PATCH  | `/api/v1/properties/{id}`                                    | no   |

### TenantController — `/api/v1/tenants`

| #   | Method | Path                                   | Auth                             |
| --- | ------ | -------------------------------------- | -------------------------------- |
| 14  | GET    | `/api/v1/tenants`                      | yes — @Authorize(TENANT, LIST)   |
| 15  | GET    | `/api/v1/tenants/me`                   | yes — @Authorize(TENANT, READ)   |
| 16  | GET    | `/api/v1/tenants/{id}`                 | yes — @Authorize(TENANT, READ)   |
| 17  | POST   | `/api/v1/tenants`                      | yes — @Authorize(TENANT, CREATE) |
| 18  | PUT    | `/api/v1/tenants/{id}`                 | no                               |
| 19  | PATCH  | `/api/v1/tenants/{id}`                 | no                               |
| 20  | DELETE | `/api/v1/tenants/{id}`                 | no                               |
| 21  | GET    | `/api/v1/tenants/{id}/payment-history` | no                               |
| 22  | GET    | `/api/v1/tenants/statistics`           | no                               |
| 23  | PATCH  | `/api/v1/tenants/{id}/status`          | no                               |

### TenantProfileController — `/api/v1/tenants/{tenantId}/profile`

| #   | Method | Path                                                   | Auth |
| --- | ------ | ------------------------------------------------------ | ---- |
| 24  | GET    | `/api/v1/tenants/{tenantId}/profile/complete`          | no   |
| 25  | POST   | `/api/v1/tenants/{tenantId}/profile/emergency-contact` | no   |
| 26  | GET    | `/api/v1/tenants/{tenantId}/profile/emergency-contact` | no   |
| 27  | POST   | `/api/v1/tenants/{tenantId}/profile/employment`        | no   |
| 28  | GET    | `/api/v1/tenants/{tenantId}/profile/employment`        | no   |
| 29  | POST   | `/api/v1/tenants/{tenantId}/profile/preferences`       | no   |
| 30  | GET    | `/api/v1/tenants/{tenantId}/profile/preferences`       | no   |

### LeaseController — `/api/v1/leases`

| #   | Method | Path                                    | Auth |
| --- | ------ | --------------------------------------- | ---- |
| 31  | GET    | `/api/v1/leases`                        | no   |
| 32  | GET    | `/api/v1/leases/{id}`                   | no   |
| 33  | POST   | `/api/v1/leases/create`                 | no   |
| 34  | POST   | `/api/v1/leases/{id}/terminate`         | no   |
| 35  | GET    | `/api/v1/leases/{id}/document`          | no   |
| 36  | POST   | `/api/v1/leases/{id}/renew`             | no   |
| 37  | POST   | `/api/v1/leases/{id}/notify-expiration` | no   |
| 38  | PATCH  | `/api/v1/leases/{id}/status`            | no   |
| 39  | GET    | `/api/v1/leases/statistics`             | no   |

### OrganizationController — `/api/v1/organizations`

| #   | Method | Path                                                        | Auth |
| --- | ------ | ----------------------------------------------------------- | ---- |
| 40  | POST   | `/api/v1/organizations/apply`                               | no   |
| 41  | POST   | `/api/v1/organizations`                                     | no   |
| 42  | GET    | `/api/v1/organizations/my`                                  | no   |
| 43  | PUT    | `/api/v1/organizations/{organizationId}`                    | no   |
| 44  | DELETE | `/api/v1/organizations/{organizationId}`                    | no   |
| 45  | GET    | `/api/v1/organizations/{organizationId}`                    | no   |
| 46  | GET    | `/api/v1/organizations/by-code/{organizationCode}`          | no   |
| 47  | GET    | `/api/v1/organizations/{organizationId}/features`           | no   |
| 48  | GET    | `/api/v1/organizations/{organizationId}/features/{feature}` | no   |
| 49  | GET    | `/api/v1/organizations/onboarding/apply-check`              | no   |
| 50  | POST   | `/api/v1/organizations/onboarding/apply`                    | no   |
| 51  | GET    | `/api/v1/organizations/onboarding/track/{trackingId}`       | no   |
| 52  | PATCH  | `/api/v1/organizations/onboarding/{trackingId}`             | no   |
| 53  | POST   | `/api/v1/organizations/onboarding/{trackingId}/submit`      | no   |
| 54  | POST   | `/api/v1/organizations/onboarding/{trackingId}/resubmit`    | no   |
| 55  | DELETE | `/api/v1/organizations/onboarding/{trackingId}`             | no   |

### AdminOrganizationManagementController — `/api/v1/admin/organizations`

| #   | Method | Path                                                      | Auth |
| --- | ------ | --------------------------------------------------------- | ---- |
| 56  | GET    | `/api/v1/admin/organizations`                             | no   |
| 57  | GET    | `/api/v1/admin/organizations/{id}`                        | no   |
| 58  | GET    | `/api/v1/admin/organizations/statistics`                  | no   |
| 59  | POST   | `/api/v1/admin/organizations/{id}/activate`               | no   |
| 60  | PATCH  | `/api/v1/admin/organizations/{id}/status`                 | no   |
| 61  | POST   | `/api/v1/admin/organizations/{id}/deactivate`             | no   |
| 62  | POST   | `/api/v1/admin/organizations/{id}/suspend`                | no   |
| 63  | POST   | `/api/v1/admin/organizations/{id}/cancel`                 | no   |
| 64  | POST   | `/api/v1/admin/organizations/{id}/reactivate`             | no   |
| 65  | GET    | `/api/v1/admin/organizations/applications`                | no   |
| 66  | GET    | `/api/v1/admin/organizations/applications/pending`        | no   |
| 67  | GET    | `/api/v1/admin/organizations/applications/pending-count`  | no   |
| 68  | GET    | `/api/v1/admin/organizations/applications/{id}`           | no   |
| 69  | POST   | `/api/v1/admin/organizations/applications/{id}/assign`    | no   |
| 70  | POST   | `/api/v1/admin/organizations/applications/{id}/review`    | no   |
| 71  | GET    | `/api/v1/admin/organizations/applications/my-assignments` | no   |
| 72  | GET    | `/api/v1/admin/organizations/applications/stats`          | no   |
| 73  | GET    | `/api/v1/admin/organizations/applications/search`         | no   |

### MaintenanceRequestController — `/api/v1/maintenance`

| #   | Method | Path                                                              | Auth |
| --- | ------ | ----------------------------------------------------------------- | ---- |
| 74  | GET    | `/api/v1/maintenance`                                             | no   |
| 75  | GET    | `/api/v1/maintenance/{id}`                                        | no   |
| 76  | GET    | `/api/v1/maintenance/open`                                        | no   |
| 77  | GET    | `/api/v1/maintenance/assigned/{assignedTo}`                       | no   |
| 78  | GET    | `/api/v1/maintenance/count/status/{status}`                       | no   |
| 79  | GET    | `/api/v1/maintenance/count/property/{propertyId}/status/{status}` | no   |
| 80  | POST   | `/api/v1/maintenance`                                             | no   |
| 81  | PUT    | `/api/v1/maintenance/{id}`                                        | no   |
| 82  | DELETE | `/api/v1/maintenance/{id}`                                        | no   |
| 83  | POST   | `/api/v1/maintenance/{id}/notes`                                  | no   |
| 84  | GET    | `/api/v1/maintenance/{id}/notes`                                  | no   |
| 85  | GET    | `/api/v1/maintenance/notes/recent`                                | no   |
| 86  | POST   | `/api/v1/maintenance/tenant/submit`                               | no   |
| 87  | GET    | `/api/v1/maintenance/tenant/my-requests`                          | no   |
| 88  | GET    | `/api/v1/maintenance/tenant/request/{id}`                         | no   |
| 89  | PUT    | `/api/v1/maintenance/tenant/request/{id}/cancel`                  | no   |
| 90  | GET    | `/api/v1/maintenance/tenant/count/open`                           | no   |
| 91  | GET    | `/api/v1/maintenance/tenant/summary`                              | no   |
| 92  | GET    | `/api/v1/maintenance/statistics`                                  | no   |

### VendorController — `/api/v1/vendors`

| #   | Method | Path                             | Auth |
| --- | ------ | -------------------------------- | ---- |
| 93  | GET    | `/api/v1/vendors`                | no   |
| 94  | GET    | `/api/v1/vendors/{id}`           | no   |
| 95  | POST   | `/api/v1/vendors`                | no   |
| 96  | PUT    | `/api/v1/vendors/{id}`           | no   |
| 97  | DELETE | `/api/v1/vendors/{id}`           | no   |
| 98  | GET    | `/api/v1/vendors/search`         | no   |
| 99  | GET    | `/api/v1/vendors/search/zipcode` | no   |
| 100 | GET    | `/api/v1/vendors/search/city`    | no   |

### InvoiceController — `/api/v1/invoices`

| #   | Method | Path                                       | Auth |
| --- | ------ | ------------------------------------------ | ---- |
| 101 | GET    | `/api/v1/invoices`                         | no   |
| 102 | GET    | `/api/v1/invoices/{id}`                    | no   |
| 103 | GET    | `/api/v1/invoices/tenant/{tenantId}`       | no   |
| 104 | GET    | `/api/v1/invoices/lease/{leaseId}`         | no   |
| 105 | GET    | `/api/v1/invoices/status/{status}`         | no   |
| 106 | GET    | `/api/v1/invoices/overdue`                 | no   |
| 107 | GET    | `/api/v1/invoices/tenant/{tenantId}/count` | no   |
| 108 | GET    | `/api/v1/invoices/lease/{leaseId}/count`   | no   |
| 109 | POST   | `/api/v1/invoices`                         | no   |
| 110 | PUT    | `/api/v1/invoices/{id}`                    | no   |
| 111 | PATCH  | `/api/v1/invoices/{id}/mark-paid`          | no   |
| 112 | DELETE | `/api/v1/invoices/{id}`                    | no   |
| 113 | GET    | `/api/v1/invoices/statistics`              | no   |

### ExpenseController — `/api/v1/expenses`

| #   | Method | Path                       | Auth |
| --- | ------ | -------------------------- | ---- |
| 114 | GET    | `/api/v1/expenses`         | no   |
| 115 | GET    | `/api/v1/expenses/summary` | no   |
| 116 | GET    | `/api/v1/expenses/{id}`    | no   |
| 117 | POST   | `/api/v1/expenses`         | no   |
| 118 | PUT    | `/api/v1/expenses/{id}`    | no   |
| 119 | DELETE | `/api/v1/expenses/{id}`    | no   |

### RentalApplicationController — `/api/v1/rental-applications`

| #   | Method | Path                                                                        | Auth |
| --- | ------ | --------------------------------------------------------------------------- | ---- |
| 120 | POST   | `/api/v1/rental-applications/submit`                                        | no   |
| 121 | GET    | `/api/v1/rental-applications/track/{trackingId}`                            | no   |
| 122 | GET    | `/api/v1/rental-applications`                                               | no   |
| 123 | GET    | `/api/v1/rental-applications/{applicationId}`                               | no   |
| 124 | GET    | `/api/v1/rental-applications/property/{propertyId}`                         | no   |
| 125 | GET    | `/api/v1/rental-applications/status/{status}`                               | no   |
| 126 | GET    | `/api/v1/rental-applications/search`                                        | no   |
| 127 | PUT    | `/api/v1/rental-applications/{applicationId}`                               | no   |
| 128 | POST   | `/api/v1/rental-applications/{applicationId}/approve`                       | no   |
| 129 | POST   | `/api/v1/rental-applications/{applicationId}/reject`                        | no   |
| 130 | POST   | `/api/v1/rental-applications/{applicationId}/assign`                        | no   |
| 131 | PATCH  | `/api/v1/rental-applications/{applicationId}/status`                        | no   |
| 132 | DELETE | `/api/v1/rental-applications/{applicationId}`                               | no   |
| 133 | GET    | `/api/v1/rental-applications/stats`                                         | no   |
| 134 | GET    | `/api/v1/rental-applications/{applicationId}/background-check`              | no   |
| 135 | POST   | `/api/v1/rental-applications/{applicationId}/background-check/waive`        | no   |
| 136 | POST   | `/api/v1/rental-applications/{applicationId}/background-check/not-required` | no   |
| 137 | POST   | `/api/v1/rental-applications/{applicationId}/background-check/initiate`     | no   |
| 138 | GET    | `/api/v1/rental-applications/{applicationId}/background-check/can-approve`  | no   |

### TenantScreeningController — `/api/v1/screening`

| #   | Method | Path                                                           | Auth |
| --- | ------ | -------------------------------------------------------------- | ---- |
| 139 | POST   | `/api/v1/screening/applications`                               | no   |
| 140 | GET    | `/api/v1/screening/applications/{applicationId}/status`        | no   |
| 141 | GET    | `/api/v1/screening/applications/{applicationId}/progress`      | no   |
| 142 | GET    | `/api/v1/screening/tenants/{tenantId}/credit-check`            | no   |
| 143 | GET    | `/api/v1/screening/tenants/{tenantId}/criminal-check`          | no   |
| 144 | POST   | `/api/v1/screening/tenants/{tenantId}/employment-verification` | no   |
| 145 | GET    | `/api/v1/screening/tenants/{tenantId}/comprehensive`           | no   |
| 146 | POST   | `/api/v1/screening/tenants/{tenantId}/references`              | no   |
| 147 | PUT    | `/api/v1/screening/references/{referenceId}/verify`            | no   |
| 148 | GET    | `/api/v1/screening/tenants/{tenantId}/screening-requirement`   | no   |
| 149 | PUT    | `/api/v1/screening/tenants/{tenantId}/screening-requirement`   | no   |
| 150 | POST   | `/api/v1/screening/tenants/{tenantId}/waive-screening`         | no   |
| 151 | POST   | `/api/v1/screening/tenants/{tenantId}/complete-screening`      | no   |
| 152 | GET    | `/api/v1/screening/tenants/{tenantId}/can-proceed`             | no   |

### MessageController — `/api/v1/messages`

| #   | Method | Path                                          | Auth |
| --- | ------ | --------------------------------------------- | ---- |
| 153 | GET    | `/api/v1/messages`                            | no   |
| 154 | GET    | `/api/v1/messages/inbox`                      | no   |
| 155 | GET    | `/api/v1/messages/sent`                       | no   |
| 156 | GET    | `/api/v1/messages/statistics` (also `/stats`) | no   |
| 157 | GET    | `/api/v1/messages/unread/count`               | no   |
| 158 | GET    | `/api/v1/messages/recipients`                 | no   |
| 159 | GET    | `/api/v1/messages/{messageId}`                | no   |
| 160 | GET    | `/api/v1/messages/thread/{threadId}`          | no   |
| 161 | GET    | `/api/v1/messages/search`                     | no   |
| 162 | GET    | `/api/v1/messages/category/{category}`        | no   |
| 163 | POST   | `/api/v1/messages` (also `/send`)             | no   |
| 164 | POST   | `/api/v1/messages/{messageId}/reply`          | no   |
| 165 | PATCH  | `/api/v1/messages/{messageId}/read`           | no   |
| 166 | PATCH  | `/api/v1/messages/{messageId}/unread`         | no   |
| 167 | PATCH  | `/api/v1/messages/{messageId}/archive`        | no   |
| 168 | DELETE | `/api/v1/messages/{messageId}`                | no   |

### DocumentController — `/api/v1/documents`

| #   | Method | Path                                               | Auth |
| --- | ------ | -------------------------------------------------- | ---- |
| 169 | GET    | `/api/v1/documents`                                | no   |
| 170 | GET    | `/api/v1/documents/entity/{entityType}/{entityId}` | no   |
| 171 | GET    | `/api/v1/documents/{documentId}`                   | no   |
| 172 | POST   | `/api/v1/documents/upload`                         | no   |
| 173 | DELETE | `/api/v1/documents/{documentId}`                   | no   |
| 174 | GET    | `/api/v1/documents/type/{entityType}`              | no   |
| 175 | GET    | `/api/v1/documents/search`                         | no   |

### NotificationController — `/api/v1/notifications`

| #   | Method | Path                                      | Auth |
| --- | ------ | ----------------------------------------- | ---- |
| 176 | GET    | `/api/v1/notifications`                   | no   |
| 177 | GET    | `/api/v1/notifications/{id}`              | no   |
| 178 | GET    | `/api/v1/notifications/tenant/{tenantId}` | no   |
| 179 | GET    | `/api/v1/notifications/status/{status}`   | no   |
| 180 | POST   | `/api/v1/notifications/send`              | no   |

### NotificationPreferenceController — `/api/v1/notification-preferences`

| #   | Method | Path                                                     | Auth |
| --- | ------ | -------------------------------------------------------- | ---- |
| 181 | GET    | `/api/v1/notification-preferences/tenant/{tenantId}`     | no   |
| 182 | POST   | `/api/v1/notification-preferences/grant-consent`         | no   |
| 183 | POST   | `/api/v1/notification-preferences/revoke-consent`        | no   |
| 184 | POST   | `/api/v1/notification-preferences/opt-out`               | no   |
| 185 | PUT    | `/api/v1/notification-preferences/update`                | no   |
| 186 | POST   | `/api/v1/notification-preferences/initialize/{tenantId}` | no   |
| 187 | GET    | `/api/v1/notification-preferences/check-consent`         | no   |

### SMSNotificationController — `/api/v1/notifications/sms`

| #   | Method | Path                                           | Auth |
| --- | ------ | ---------------------------------------------- | ---- |
| 188 | POST   | `/api/v1/notifications/sms/send`               | no   |
| 189 | POST   | `/api/v1/notifications/sms/payment-reminder`   | no   |
| 190 | POST   | `/api/v1/notifications/sms/application-status` | no   |
| 191 | POST   | `/api/v1/notifications/sms/maintenance-update` | no   |

### ReportController — `/api/v1/reports`

| #   | Method | Path                                          | Auth |
| --- | ------ | --------------------------------------------- | ---- |
| 192 | GET    | `/api/v1/reports`                             | no   |
| 193 | GET    | `/api/v1/reports/dashboard`                   | no   |
| 194 | GET    | `/api/v1/reports/available`                   | no   |
| 195 | GET    | `/api/v1/reports/financial`                   | no   |
| 196 | GET    | `/api/v1/reports/delinquency`                 | no   |
| 197 | GET    | `/api/v1/reports/occupancy`                   | no   |
| 198 | GET    | `/api/v1/reports/rent-roll`                   | no   |
| 199 | GET    | `/api/v1/reports/maintenance`                 | no   |
| 200 | GET    | `/api/v1/reports/vendor-performance`          | no   |
| 201 | GET    | `/api/v1/reports/tenant/payment-history`      | no   |
| 202 | GET    | `/api/v1/reports/tenant/maintenance-requests` | no   |
| 203 | GET    | `/api/v1/reports/vendor/work-orders`          | no   |
| 204 | GET    | `/api/v1/reports/vendor/invoices`             | no   |
| 205 | GET    | `/api/v1/reports/financial/export/pdf`        | no   |
| 206 | GET    | `/api/v1/reports/financial/export/excel`      | no   |
| 207 | GET    | `/api/v1/reports/occupancy/export/pdf`        | no   |
| 208 | GET    | `/api/v1/reports/maintenance/export/excel`    | no   |

### SearchController — `/api/v1/search`

| #   | Method | Path                                 | Auth |
| --- | ------ | ------------------------------------ | ---- |
| 209 | GET    | `/api/v1/search`                     | no   |
| 210 | POST   | `/api/v1/search/properties`          | no   |
| 211 | POST   | `/api/v1/search/properties/detailed` | no   |
| 212 | POST   | `/api/v1/search/tenants`             | no   |
| 213 | POST   | `/api/v1/search/tenants/detailed`    | no   |

### AssetController — `/api/v1/assets`

| #   | Method | Path                  | Auth |
| --- | ------ | --------------------- | ---- |
| 214 | GET    | `/api/v1/assets`      | no   |
| 215 | GET    | `/api/v1/assets/{id}` | no   |
| 216 | POST   | `/api/v1/assets`      | no   |
| 217 | PUT    | `/api/v1/assets/{id}` | no   |
| 218 | DELETE | `/api/v1/assets/{id}` | no   |

### UtilityController — `/api/v1/utilities`

| #   | Method | Path                                            | Auth                              |
| --- | ------ | ----------------------------------------------- | --------------------------------- |
| 219 | GET    | `/api/v1/utilities`                             | yes — @Authorize(UTILITY, LIST)   |
| 220 | GET    | `/api/v1/utilities/{id}`                        | yes — @Authorize(UTILITY, READ)   |
| 221 | GET    | `/api/v1/utilities/property/{propertyId}`       | yes — @Authorize(UTILITY, READ)   |
| 222 | GET    | `/api/v1/utilities/tenant/{tenantId}`           | yes — @Authorize(UTILITY, READ)   |
| 223 | GET    | `/api/v1/utilities/type/{type}`                 | yes — @Authorize(UTILITY, READ)   |
| 224 | GET    | `/api/v1/utilities/property/{propertyId}/count` | yes — @Authorize(UTILITY, READ)   |
| 225 | POST   | `/api/v1/utilities`                             | yes — @Authorize(UTILITY, CREATE) |
| 226 | PUT    | `/api/v1/utilities/{id}`                        | yes — @Authorize(UTILITY, UPDATE) |
| 227 | DELETE | `/api/v1/utilities/{id}`                        | yes — @Authorize(UTILITY, DELETE) |

### LateFeeController — `/api/v1/late-fees`

| #   | Method | Path                                                     | Auth |
| --- | ------ | -------------------------------------------------------- | ---- |
| 228 | POST   | `/api/v1/late-fees/apply/{paymentId}`                    | no   |
| 229 | POST   | `/api/v1/late-fees/waive/{chargeId}`                     | no   |
| 230 | GET    | `/api/v1/late-fees/invoice/{invoiceId}`                  | no   |
| 231 | GET    | `/api/v1/late-fees/policy/organization/{organizationId}` | no   |
| 232 | POST   | `/api/v1/late-fees/policy`                               | no   |
| 233 | GET    | `/api/v1/late-fees/total`                                | no   |
| 234 | GET    | `/api/v1/late-fees/overdue-payments`                     | no   |
| 235 | POST   | `/api/v1/late-fees/process`                              | no   |

### ApprovalWorkflowController — `/api/v1/approvals`

| #   | Method | Path                                              | Auth |
| --- | ------ | ------------------------------------------------- | ---- |
| 236 | POST   | `/api/v1/approvals`                               | no   |
| 237 | GET    | `/api/v1/approvals/pending`                       | no   |
| 238 | GET    | `/api/v1/approvals/pending/type/{workflowType}`   | no   |
| 239 | GET    | `/api/v1/approvals/{approvalId}`                  | no   |
| 240 | GET    | `/api/v1/approvals/my-submissions`                | no   |
| 241 | GET    | `/api/v1/approvals/organization/{organizationId}` | no   |
| 242 | POST   | `/api/v1/approvals/{approvalId}/decision`         | no   |
| 243 | POST   | `/api/v1/approvals/{approvalId}/execute`          | no   |
| 244 | DELETE | `/api/v1/approvals/{approvalId}`                  | no   |
| 245 | GET    | `/api/v1/approvals/count/pending`                 | no   |
| 246 | GET    | `/api/v1/approvals/expiring-soon`                 | no   |

### TaskController — `/api/v1/tasks`

| #   | Method | Path                            | Auth |
| --- | ------ | ------------------------------- | ---- |
| 247 | POST   | `/api/v1/tasks`                 | no   |
| 248 | GET    | `/api/v1/tasks/{taskId}`        | no   |
| 249 | GET    | `/api/v1/tasks`                 | no   |
| 250 | GET    | `/api/v1/tasks/my-tasks`        | no   |
| 251 | GET    | `/api/v1/tasks/overdue`         | no   |
| 252 | GET    | `/api/v1/tasks/statistics`      | no   |
| 253 | PUT    | `/api/v1/tasks/{taskId}`        | no   |
| 254 | PATCH  | `/api/v1/tasks/{taskId}/status` | no   |
| 255 | DELETE | `/api/v1/tasks/{taskId}`        | no   |

### InspectionController — `/api/v1/inspections`

| #   | Method | Path                                        | Auth |
| --- | ------ | ------------------------------------------- | ---- |
| 256 | GET    | `/api/v1/inspections`                       | no   |
| 257 | GET    | `/api/v1/inspections/{id}`                  | no   |
| 258 | GET    | `/api/v1/inspections/upcoming`              | no   |
| 259 | GET    | `/api/v1/inspections/property/{propertyId}` | no   |
| 260 | GET    | `/api/v1/inspections/tenant/{tenantId}`     | no   |
| 261 | GET    | `/api/v1/inspections/type/{type}`           | no   |
| 262 | POST   | `/api/v1/inspections`                       | no   |
| 263 | PUT    | `/api/v1/inspections/{id}`                  | no   |
| 264 | DELETE | `/api/v1/inspections/{id}`                  | no   |

### MilestoneController — `/api/v1/milestones`

| #   | Method | Path                                        | Auth |
| --- | ------ | ------------------------------------------- | ---- |
| 265 | POST   | `/api/v1/milestones`                        | no   |
| 266 | GET    | `/api/v1/milestones/{milestoneId}`          | no   |
| 267 | GET    | `/api/v1/milestones`                        | no   |
| 268 | GET    | `/api/v1/milestones/overdue`                | no   |
| 269 | GET    | `/api/v1/milestones/completed`              | no   |
| 270 | GET    | `/api/v1/milestones/statistics`             | no   |
| 271 | PUT    | `/api/v1/milestones/{milestoneId}`          | no   |
| 272 | PATCH  | `/api/v1/milestones/{milestoneId}/progress` | no   |
| 273 | DELETE | `/api/v1/milestones/{milestoneId}`          | no   |

### ScheduleEventController — `/api/v1/schedule-events`

| #   | Method | Path                                   | Auth |
| --- | ------ | -------------------------------------- | ---- |
| 274 | GET    | `/api/v1/schedule-events`              | no   |
| 275 | GET    | `/api/v1/schedule-events/{id}`         | no   |
| 276 | POST   | `/api/v1/schedule-events`              | no   |
| 277 | PUT    | `/api/v1/schedule-events/{id}`         | no   |
| 278 | DELETE | `/api/v1/schedule-events/{id}`         | no   |
| 279 | GET    | `/api/v1/schedule-events/upcoming`     | no   |
| 280 | GET    | `/api/v1/schedule-events/availability` | no   |

### AnalyticsController — `/api/v1/analytics`

| #   | Method | Path                         | Auth |
| --- | ------ | ---------------------------- | ---- |
| 281 | POST   | `/api/v1/analytics/events`   | no   |
| 282 | POST   | `/api/v1/analytics/track`    | no   |
| 283 | POST   | `/api/v1/analytics/pageview` | no   |
| 284 | GET    | `/api/v1/analytics/summary`  | no   |
| 285 | GET    | `/api/v1/analytics/events`   | no   |

### MetricsController — `/api/v1/metrics`

| #   | Method | Path                                      | Auth |
| --- | ------ | ----------------------------------------- | ---- |
| 286 | GET    | `/api/v1/metrics/properties/{propertyId}` | no   |
| 287 | GET    | `/api/v1/metrics/properties`              | no   |
| 288 | GET    | `/api/v1/metrics/tenants/{tenantId}`      | no   |
| 289 | GET    | `/api/v1/metrics/tenants`                 | no   |
| 290 | GET    | `/api/v1/metrics/leases/{leaseId}`        | no   |
| 291 | GET    | `/api/v1/metrics/leases`                  | no   |
| 292 | GET    | `/api/v1/metrics/dashboard`               | no   |

### UserController — `/api/v1/users`

| #   | Method | Path                 | Auth                           |
| --- | ------ | -------------------- | ------------------------------ |
| 293 | POST   | `/api/v1/users`      | yes — @Authorize(USER, CREATE) |
| 294 | GET    | `/api/v1/users`      | yes — @Authorize(USER, LIST)   |
| 295 | GET    | `/api/v1/users/{id}` | yes — @Authorize(USER, READ)   |

### ContactController — `/api/v1/contacts`

| #   | Method | Path                          | Auth |
| --- | ------ | ----------------------------- | ---- |
| 296 | POST   | `/api/v1/contacts`            | no   |
| 297 | GET    | `/api/v1/contacts`            | no   |
| 298 | GET    | `/api/v1/contacts/{id}`       | no   |
| 299 | PUT    | `/api/v1/contacts/{id}`       | no   |
| 300 | DELETE | `/api/v1/contacts/{id}`       | no   |
| 301 | GET    | `/api/v1/contacts/search`     | no   |
| 302 | GET    | `/api/v1/contacts/statistics` | no   |

### AdminContactController — `/api/v1/admin/contact` (also `/admin/contacts`)

| #   | Method | Path                               | Auth                              |
| --- | ------ | ---------------------------------- | --------------------------------- |
| 303 | GET    | `/api/v1/admin/contact`            | yes — @Authorize(CONTACT, MANAGE) |
| 304 | GET    | `/api/v1/admin/contact/{id}`       | no                                |
| 305 | PUT    | `/api/v1/admin/contact/{id}`       | no                                |
| 306 | DELETE | `/api/v1/admin/contact/{id}`       | no                                |
| 307 | GET    | `/api/v1/admin/contact/search`     | no                                |
| 308 | GET    | `/api/v1/admin/contact/statistics` | no                                |

### SupportTicketController — `/api/v1/support`

| #   | Method | Path                                        | Auth |
| --- | ------ | ------------------------------------------- | ---- |
| 309 | POST   | `/api/v1/support/tickets`                   | no   |
| 310 | PUT    | `/api/v1/support/tickets/{ticketId}`        | no   |
| 311 | GET    | `/api/v1/support/tickets/{ticketId}`        | no   |
| 312 | GET    | `/api/v1/support/tickets`                   | no   |
| 313 | GET    | `/api/v1/support/tickets/my-tickets`        | no   |
| 314 | GET    | `/api/v1/support/tickets/assigned/{userId}` | no   |
| 315 | GET    | `/api/v1/support/stats`                     | no   |
| 316 | DELETE | `/api/v1/support/tickets/{ticketId}`        | no   |

### AuditLogController — `/api/v1/audit`

| #   | Method | Path                                             | Auth |
| --- | ------ | ------------------------------------------------ | ---- |
| 317 | GET    | `/api/v1/audit`                                  | no   |
| 318 | GET    | `/api/v1/audit/{id}`                             | no   |
| 319 | GET    | `/api/v1/audit/resource/{entityType}/{entityId}` | no   |
| 320 | GET    | `/api/v1/audit/user/{userId}`                    | no   |
| 321 | GET    | `/api/v1/audit/stats`                            | no   |
| 322 | GET    | `/api/v1/audit/correlation/{correlationId}`      | no   |
| 323 | GET    | `/api/v1/audit/export`                           | no   |

### AdminStatsController — `/api/v1/admin`

| #   | Method | Path                  | Auth |
| --- | ------ | --------------------- | ---- |
| 324 | GET    | `/api/v1/admin/stats` | no   |

### PublicContactController — `/api/v1/public/contact`

| #   | Method | Path                     | Auth        |
| --- | ------ | ------------------------ | ----------- |
| 325 | POST   | `/api/v1/public/contact` | no (public) |

### SessionController — `/api/v1/sessions`

| #   | Method | Path                                  | Auth                                     |
| --- | ------ | ------------------------------------- | ---------------------------------------- |
| 326 | GET    | `/api/v1/sessions/current`            | no                                       |
| 327 | GET    | `/api/v1/sessions/my-sessions`        | no                                       |
| 328 | DELETE | `/api/v1/sessions/{sessionId}`        | no                                       |
| 329 | DELETE | `/api/v1/sessions/all-except-current` | no                                       |
| 330 | GET    | `/api/v1/sessions/count`              | no                                       |
| 331 | GET    | `/api/v1/sessions/statistics`         | yes — @Authorize(SESSION, LIST_ALL)      |
| 332 | POST   | `/api/v1/sessions/cleanup`            | yes — @Authorize(SESSION, CONFIGURE)     |
| 333 | DELETE | `/api/v1/sessions/user/{username}`    | yes — @Authorize(SESSION, TERMINATE_ALL) |

### MemoryManagementController — `/api/v1/admin/memory`

| #   | Method | Path                                 | Auth |
| --- | ------ | ------------------------------------ | ---- |
| 334 | GET    | `/api/v1/admin/memory/stats`         | no   |
| 335 | GET    | `/api/v1/admin/memory/heap-usage`    | no   |
| 336 | POST   | `/api/v1/admin/memory/gc`            | no   |
| 337 | POST   | `/api/v1/admin/memory/clear-caches`  | no   |
| 338 | POST   | `/api/v1/admin/memory/force-cleanup` | no   |
| 339 | GET    | `/api/v1/admin/memory/threads`       | no   |

### LogController — `/api/v1/logs`

| #   | Method | Path           | Auth |
| --- | ------ | -------------- | ---- |
| 340 | GET    | `/api/v1/logs` | no   |
| 341 | POST   | `/api/v1/logs` | no   |

### UtilController — `/api/v1/util`

| #   | Method | Path                           | Auth |
| --- | ------ | ------------------------------ | ---- |
| 342 | POST   | `/api/v1/util/hash-password`   | no   |
| 343 | POST   | `/api/v1/util/verify-password` | no   |
| 344 | POST   | `/api/v1/util/unlock-account`  | no   |
| 345 | POST   | `/api/v1/util/reset-password`  | no   |

### TestCredentialsController — `/api/v1/test/credentials`

| #   | Method | Path                                           | Auth |
| --- | ------ | ---------------------------------------------- | ---- |
| 346 | GET    | `/api/v1/test/credentials`                     | no   |
| 347 | GET    | `/api/v1/test/credentials/role/{role}`         | no   |
| 348 | GET    | `/api/v1/test/credentials/username/{username}` | no   |
| 349 | GET    | `/api/v1/test/credentials/health`              | no   |

### Miscellaneous Controllers

| #   | Method | Path                                           | Controller                     | Auth |
| --- | ------ | ---------------------------------------------- | ------------------------------ | ---- |
| 350 | GET    | `/api/v1/debug/whoami`                         | DebugController                | no   |
| 351 | GET    | `/api/auth/session`                            | NextAuthSessionController      | no   |
| 352 | GET    | `/health`                                      | HealthController               | no   |
| 353 | GET    | `/api/v1/health`                               | HealthController               | no   |
| 354 | GET    | `/api/v1/mail/status`                          | MailDiagnosticsController      | no   |
| 355 | GET    | `/api/v1/diagnostics/phone-validation-pattern` | ValidationDiagnosticController | no   |
| 356 | GET    | `/api/v1/diagnostics/validation-info`          | ValidationDiagnosticController | no   |
| 357 | GET    | `/internal/diagnostics/jwt-key`                | DiagnosticsController          | no   |

### GraphQL Resolvers (via `/graphql` endpoint)

| #   | Type  | Query Name                | Resolver                   | Auth |
| --- | ----- | ------------------------- | -------------------------- | ---- |
| G1  | Query | `platformDashboard`       | AllDashboardsQueryResolver | no   |
| G2  | Query | `portfolioDashboard`      | AllDashboardsQueryResolver | no   |
| G3  | Query | `accountantDashboard`     | AllDashboardsQueryResolver | no   |
| G4  | Query | `maintenanceDashboard`    | AllDashboardsQueryResolver | no   |
| G5  | Query | `oversightDashboard`      | AllDashboardsQueryResolver | no   |
| G6  | Query | `leasingDashboard`        | AllDashboardsQueryResolver | no   |
| G7  | Query | `dashboardSummary`        | DashboardQueryResolver     | no   |
| G8  | Query | `propertyPerformance`     | DashboardQueryResolver     | no   |
| G9  | Query | `occupancyTrends`         | DashboardQueryResolver     | no   |
| G10 | Query | `financialMetrics`        | DashboardQueryResolver     | no   |
| G11 | Query | `maintenanceOverview`     | DashboardQueryResolver     | no   |
| G12 | Query | `recentActivities`        | DashboardQueryResolver     | no   |
| G13 | Query | `tenantSummary`           | DashboardQueryResolver     | no   |
| G14 | Query | `propertyManagerOverview` | ManagerQueryResolver       | no   |
| G15 | Query | (multiple queries)        | DashboardGraphQLController | no   |
| G16 | Query | (multiple queries)        | PropertyGraphQLController  | no   |

---

## 3. EMPLOYEE-SERVICE (port 8083)

### EmployeeController — `/api/v1/employees`

| #   | Method | Path                                 | Auth |
| --- | ------ | ------------------------------------ | ---- |
| 1   | GET    | `/api/v1/employees`                  | no   |
| 2   | GET    | `/api/v1/employees/{id}`             | no   |
| 3   | GET    | `/api/v1/employees/by-user/{userId}` | no   |
| 4   | POST   | `/api/v1/employees`                  | no   |
| 5   | POST   | `/api/v1/employees/{id}/activate`    | no   |
| 6   | POST   | `/api/v1/employees/{id}/terminate`   | no   |
| 7   | GET    | `/api/v1/employees/changed-since`    | no   |
| 8   | GET    | `/api/v1/employees/payroll-summary`  | no   |

### HealthCheckController (no base path)

| #   | Method | Path                         | Auth |
| --- | ------ | ---------------------------- | ---- |
| 9   | GET    | `/actuator/health`           | no   |
| 10  | GET    | `/health`                    | no   |
| 11  | GET    | `/api/v1/health`             | no   |
| 12  | GET    | `/actuator/health/readiness` | no   |
| 13  | GET    | `/actuator/health/liveness`  | no   |

---

## 4. PAYMENT-SERVICE (port 8084)

### PaymentController — `/api/v1/payments`

| #   | Method | Path                                 | Auth |
| --- | ------ | ------------------------------------ | ---- |
| 1   | GET    | `/api/v1/payments`                   | no   |
| 2   | GET    | `/api/v1/payments/{id}`              | no   |
| 3   | GET    | `/api/v1/payments/tenant/{tenantId}` | no   |
| 4   | GET    | `/api/v1/payments/lease/{leaseId}`   | no   |
| 5   | POST   | `/api/v1/payments`                   | no   |
| 6   | POST   | `/api/v1/payments/{id}/process`      | no   |
| 7   | POST   | `/api/v1/payments/{id}/refund`       | no   |
| 8   | PATCH  | `/api/v1/payments/{id}`              | no   |

### PaymentContextController — `/api/v1/payments`

| #   | Method | Path                                     | Auth |
| --- | ------ | ---------------------------------------- | ---- |
| 9   | POST   | `/api/v1/payments/vendor`                | no   |
| 10  | POST   | `/api/v1/payments/platform-subscription` | no   |
| 11  | POST   | `/api/v1/payments/owner-payout`          | no   |

### PaymentMethodController — `/api/v1/payment-methods`

| #   | Method | Path                                        | Auth |
| --- | ------ | ------------------------------------------- | ---- |
| 12  | GET    | `/api/v1/payment-methods/tenant/{tenantId}` | no   |
| 13  | GET    | `/api/v1/payment-methods/{id}`              | no   |
| 14  | POST   | `/api/v1/payment-methods/card`              | no   |
| 15  | POST   | `/api/v1/payment-methods/ach`               | no   |
| 16  | POST   | `/api/v1/payment-methods/{id}/default`      | no   |
| 17  | DELETE | `/api/v1/payment-methods/{id}`              | no   |

### StripePaymentController — `/api/v1/stripe`

| #   | Method | Path                                          | Auth |
| --- | ------ | --------------------------------------------- | ---- |
| 18  | POST   | `/api/v1/stripe/payment-intents`              | no   |
| 19  | POST   | `/api/v1/stripe/payment-intents/{id}/confirm` | no   |
| 20  | POST   | `/api/v1/stripe/payment-intents/{id}/capture` | no   |
| 21  | POST   | `/api/v1/stripe/payment-intents/{id}/cancel`  | no   |
| 22  | POST   | `/api/v1/stripe/refunds`                      | no   |
| 23  | GET    | `/api/v1/stripe/payment-intents/{id}`         | no   |

### StripeCustomerController — `/api/v1/stripe/customers`

| #   | Method | Path                                                                             | Auth |
| --- | ------ | -------------------------------------------------------------------------------- | ---- |
| 24  | POST   | `/api/v1/stripe/customers`                                                       | no   |
| 25  | POST   | `/api/v1/stripe/customers/{customerId}/payment-methods/{paymentMethodId}/attach` | no   |
| 26  | DELETE | `/api/v1/stripe/customers/payment-methods/{paymentMethodId}/detach`              | no   |

### StripeWebhookController — `/api/v1/webhooks/stripe`

| #   | Method | Path                      | Auth                  |
| --- | ------ | ------------------------- | --------------------- |
| 27  | POST   | `/api/v1/webhooks/stripe` | no (Stripe signature) |

### TransactionHistoryController — `/api/v1/transactions`

| #   | Method | Path                                               | Auth |
| --- | ------ | -------------------------------------------------- | ---- |
| 28  | GET    | `/api/v1/transactions`                             | no   |
| 29  | GET    | `/api/v1/transactions/{id}`                        | no   |
| 30  | GET    | `/api/v1/transactions/reference/{referenceNumber}` | no   |

### PromoCodeController — `/api/v1/promo-codes`

| #   | Method | Path                           | Auth |
| --- | ------ | ------------------------------ | ---- |
| 31  | GET    | `/api/v1/promo-codes`          | no   |
| 32  | GET    | `/api/v1/promo-codes/{id}`     | no   |
| 33  | POST   | `/api/v1/promo-codes`          | no   |
| 34  | PUT    | `/api/v1/promo-codes/{id}`     | no   |
| 35  | DELETE | `/api/v1/promo-codes/{id}`     | no   |
| 36  | POST   | `/api/v1/promo-codes/validate` | no   |

### ApplicationFeeController — `/api/v1/application-fees`

| #   | Method | Path                                                            | Auth |
| --- | ------ | --------------------------------------------------------------- | ---- |
| 37  | GET    | `/api/v1/application-fees/{id}`                                 | no   |
| 38  | GET    | `/api/v1/application-fees/by-application/{rentalApplicationId}` | no   |
| 39  | POST   | `/api/v1/application-fees`                                      | no   |
| 40  | POST   | `/api/v1/application-fees/{id}/process-payment`                 | no   |

### OrganizationApplicationFeeController — `/api/v1/organization-application-fees`

| #   | Method | Path                                                                 | Auth |
| --- | ------ | -------------------------------------------------------------------- | ---- |
| 41  | GET    | `/api/v1/organization-application-fees/tracking/{trackingId}`        | no   |
| 42  | GET    | `/api/v1/organization-application-fees/{id}`                         | no   |
| 43  | POST   | `/api/v1/organization-application-fees/initiate`                     | no   |
| 44  | POST   | `/api/v1/organization-application-fees/{trackingId}/complete`        | no   |
| 45  | GET    | `/api/v1/organization-application-fees/tracking/{trackingId}/status` | no   |

### PaymentReminderController — `/api/v1/payment-reminders`

| #   | Method | Path                                            | Auth |
| --- | ------ | ----------------------------------------------- | ---- |
| 46  | GET    | `/api/v1/payment-reminders/payment/{paymentId}` | no   |

---

## 5. PAYROLL-SERVICE (port 8085)

### PayrollController — `/api/v1/clients/{clientId}/payroll`

| #   | Method | Path                                                     | Auth |
| --- | ------ | -------------------------------------------------------- | ---- |
| 1   | GET    | `/api/v1/clients/{clientId}/payroll`                     | no   |
| 2   | GET    | `/api/v1/clients/{clientId}/payroll/{payrollId}`         | no   |
| 3   | POST   | `/api/v1/clients/{clientId}/payroll`                     | no   |
| 4   | POST   | `/api/v1/clients/{clientId}/payroll/{payrollId}/process` | no   |
| 5   | POST   | `/api/v1/clients/{clientId}/payroll/{payrollId}/approve` | no   |

### EmployeeController (payroll) — `/api/v1`

| #   | Method | Path                                        | Auth |
| --- | ------ | ------------------------------------------- | ---- |
| 6   | GET    | `/api/v1/clients/{clientId}/employees`      | no   |
| 7   | GET    | `/api/v1/employees/{id}`                    | no   |
| 8   | POST   | `/api/v1/clients/{clientId}/employees`      | no   |
| 9   | PUT    | `/api/v1/employees/{id}`                    | no   |
| 10  | POST   | `/api/v1/employees/{id}/terminate`          | no   |
| 11  | POST   | `/api/v1/employees/{id}/sync`               | no   |
| 12  | POST   | `/api/v1/clients/{clientId}/employees/sync` | no   |

### EmployeeEntityController — `/api/v1/employees`

| #   | Method | Path                                           | Auth |
| --- | ------ | ---------------------------------------------- | ---- |
| 13  | POST   | `/api/v1/employees`                            | no   |
| 14  | GET    | `/api/v1/employees/{id}`                       | no   |
| 15  | GET    | `/api/v1/employees/by-number/{employeeNumber}` | no   |
| 16  | GET    | `/api/v1/employees/client/{clientId}`          | no   |
| 17  | GET    | `/api/v1/employees/client/{clientId}/active`   | no   |
| 18  | GET    | `/api/v1/employees/client/{clientId}/search`   | no   |
| 19  | PUT    | `/api/v1/employees/{id}`                       | no   |
| 20  | POST   | `/api/v1/employees/{id}/terminate`             | no   |

### TimesheetController — `/api/v1/timesheets`

| #   | Method | Path                                       | Auth |
| --- | ------ | ------------------------------------------ | ---- |
| 21  | GET    | `/api/v1/timesheets`                       | no   |
| 22  | GET    | `/api/v1/timesheets/{id}`                  | no   |
| 23  | GET    | `/api/v1/timesheets/employee/{employeeId}` | no   |
| 24  | POST   | `/api/v1/timesheets/{id}/submit`           | no   |
| 25  | POST   | `/api/v1/timesheets/{id}/approve`          | no   |
| 26  | POST   | `/api/v1/timesheets/{id}/reject`           | no   |
| 27  | GET    | `/api/v1/timesheets/pending/{clientId}`    | no   |

### TimeEntryController — `/api/v1/time-entries`

| #   | Method | Path                                               | Auth |
| --- | ------ | -------------------------------------------------- | ---- |
| 28  | POST   | `/api/v1/time-entries`                             | no   |
| 29  | GET    | `/api/v1/time-entries/{id}`                        | no   |
| 30  | GET    | `/api/v1/time-entries/employee/{employeeId}`       | no   |
| 31  | GET    | `/api/v1/time-entries/employee/{employeeId}/range` | no   |
| 32  | GET    | `/api/v1/time-entries/client/{clientId}/range`     | no   |
| 33  | GET    | `/api/v1/time-entries/client/{clientId}/pending`   | no   |
| 34  | POST   | `/api/v1/time-entries/{id}/approve`                | no   |
| 35  | POST   | `/api/v1/time-entries/{id}/reject`                 | no   |
| 36  | DELETE | `/api/v1/time-entries/{id}`                        | no   |
| 37  | GET    | `/api/v1/time-entries/employee/{employeeId}/hours` | no   |

### CompensationController — `/api/v1/compensation`

| #   | Method | Path                                                 | Auth                                                       |
| --- | ------ | ---------------------------------------------------- | ---------------------------------------------------------- |
| 38  | POST   | `/api/v1/compensation`                               | yes — @PreAuthorize("hasAuthority('compensation:create')") |
| 39  | PUT    | `/api/v1/compensation/{id}`                          | yes — @PreAuthorize("hasAuthority('compensation:update')") |
| 40  | GET    | `/api/v1/compensation/{id}`                          | yes — @PreAuthorize("hasAuthority('compensation:read')")   |
| 41  | GET    | `/api/v1/compensation/employee/{employeeId}/current` | yes — @PreAuthorize("hasAuthority('compensation:read')")   |
| 42  | GET    | `/api/v1/compensation/employee/{employeeId}/history` | yes — @PreAuthorize("hasAuthority('compensation:read')")   |
| 43  | DELETE | `/api/v1/compensation/{id}/deactivate`               | yes — @PreAuthorize("hasAuthority('compensation:delete')") |
| 44  | DELETE | `/api/v1/compensation/{id}`                          | yes — @PreAuthorize("hasAuthority('compensation:delete')") |

### LeaveController — `/api/v1/leave`

| #   | Method | Path                                           | Auth |
| --- | ------ | ---------------------------------------------- | ---- |
| 45  | GET    | `/api/v1/leave/requests/{id}`                  | no   |
| 46  | GET    | `/api/v1/leave/requests/employee/{employeeId}` | no   |
| 47  | POST   | `/api/v1/leave/requests/{id}/approve`          | no   |
| 48  | POST   | `/api/v1/leave/requests/{id}/reject`           | no   |
| 49  | GET    | `/api/v1/leave/balances/{employeeId}`          | no   |
| 50  | GET    | `/api/v1/leave/requests/pending`               | no   |

### DepartmentController — `/api/v1/departments`

| #   | Method | Path                                           | Auth |
| --- | ------ | ---------------------------------------------- | ---- |
| 51  | POST   | `/api/v1/departments`                          | no   |
| 52  | GET    | `/api/v1/departments/{id}`                     | no   |
| 53  | GET    | `/api/v1/departments/client/{clientId}`        | no   |
| 54  | GET    | `/api/v1/departments/client/{clientId}/active` | no   |
| 55  | PUT    | `/api/v1/departments/{id}`                     | no   |
| 56  | PATCH  | `/api/v1/departments/{id}/deactivate`          | no   |
| 57  | DELETE | `/api/v1/departments/{id}`                     | no   |

### ClientController — `/api/v1/clients`

| #   | Method | Path                   | Auth |
| --- | ------ | ---------------------- | ---- |
| 58  | GET    | `/api/v1/clients`      | no   |
| 59  | GET    | `/api/v1/clients/{id}` | no   |
| 60  | POST   | `/api/v1/clients`      | no   |
| 61  | PUT    | `/api/v1/clients/{id}` | no   |
| 62  | DELETE | `/api/v1/clients/{id}` | no   |

---

## 6. API-GATEWAY (port 8080)

### TokenController — `/api/v1/gateway`

| #   | Method | Path                                     | Auth |
| --- | ------ | ---------------------------------------- | ---- |
| 1   | POST   | `/api/v1/gateway/token/validate`         | no   |
| 2   | POST   | `/api/v1/gateway/token/refresh/validate` | no   |
| 3   | GET    | `/api/v1/gateway/health`                 | no   |
| 4   | GET    | `/api/v1/gateway/info`                   | no   |

### CircuitBreakerMonitoringController — `/api/v1/gateway/monitoring`

| #   | Method | Path                                                       | Auth |
| --- | ------ | ---------------------------------------------------------- | ---- |
| 5   | GET    | `/api/v1/gateway/monitoring/circuit-breakers`              | no   |
| 6   | GET    | `/api/v1/gateway/monitoring/circuit-breakers/{name}`       | no   |
| 7   | POST   | `/api/v1/gateway/monitoring/circuit-breakers/{name}/state` | no   |
| 8   | GET    | `/api/v1/gateway/monitoring/cache/stats`                   | no   |
| 9   | POST   | `/api/v1/gateway/monitoring/cache/clear`                   | no   |
| 10  | GET    | `/api/v1/gateway/monitoring/health`                        | no   |

### FallbackController — `/fallback`

| #   | Method | Path                     | Auth |
| --- | ------ | ------------------------ | ---- |
| 11  | GET    | `/fallback/propertize`   | no   |
| 12  | GET    | `/fallback/auth-service` | no   |
| 13  | GET    | `/fallback/employecraft` | no   |
| 14  | GET    | `/fallback/wagecraft`    | no   |
| 15  | GET    | `/fallback/default`      | no   |

---

## 7. PYTHON SERVICES

### report-service (port 8090)

| #   | Method | Path                         | Controller | Auth |
| --- | ------ | ---------------------------- | ---------- | ---- |
| 1   | GET    | `/reports/financial/pdf`     | main.py    | no   |
| 2   | GET    | `/reports/delinquency/excel` | main.py    | no   |
| 3   | GET    | `/reports/rent-roll/excel`   | main.py    | no   |
| 4   | GET    | `/health`                    | main.py    | no   |

### vendor-matching (port 8091)

| #   | Method | Path             | Controller | Auth |
| --- | ------ | ---------------- | ---------- | ---- |
| 5   | POST   | `/match-vendors` | matcher.py | no   |
| 6   | GET    | `/health`        | matcher.py | no   |

### document-service (port 8092)

| #   | Method | Path                       | Controller | Auth |
| --- | ------ | -------------------------- | ---------- | ---- |
| 7   | POST   | `/documents/upload`        | main.py    | no   |
| 8   | GET    | `/documents/url`           | main.py    | no   |
| 9   | DELETE | `/documents/{object_name}` | main.py    | no   |
| 10  | GET    | `/health`                  | main.py    | no   |

### search-reranker (port 8093)

| #   | Method | Path      | Controller  | Auth |
| --- | ------ | --------- | ----------- | ---- |
| 11  | POST   | `/rerank` | reranker.py | no   |
| 12  | GET    | `/health` | reranker.py | no   |

---

## Summary Statistics

| Service          | Endpoints                    | With Auth Annotations                       |
| ---------------- | ---------------------------- | ------------------------------------------- |
| auth-service     | 70                           | 0 (JWT validated at gateway)                |
| propertize       | ~357 (REST) + ~16 (GraphQL)  | ~25 (@Authorize)                            |
| employee-service | 13                           | 0                                           |
| payment-service  | 46                           | 0                                           |
| payroll-service  | 62                           | 7 (@PreAuthorize on CompensationController) |
| api-gateway      | 15                           | 0                                           |
| report-service   | 4                            | 0                                           |
| vendor-matching  | 2                            | 0                                           |
| document-service | 4                            | 0                                           |
| search-reranker  | 2                            | 0                                           |
| **TOTAL**        | **~375+ REST + ~16 GraphQL** | **~32**                                     |

### Auth Model Notes

- **Gateway-level auth**: JWT tokens are validated at the API Gateway. The gateway forwards `X-User-ID`, `X-User-Email`, `X-User-Roles` headers to downstream services.
- **@Authorize**: Custom annotation on propertize service using dynamic RBAC policy evaluation (resource + action based).
- **@PreAuthorize**: Standard Spring Security annotation used only on `CompensationController` in payroll-service.
- **Most endpoints**: Rely on gateway-level JWT validation rather than method-level annotations.
- **Python services**: No auth annotations; expected to be called internally or proxied through the gateway.
