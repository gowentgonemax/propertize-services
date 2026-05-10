# Propertize — Codebase Audit Findings

> **Generated:** April 2026
> **Scope:** `propertize/` main service — DTOs, entities, constants

---

## 1. Frontend ↔ Backend DTO Mapping Gaps

These are mismatches between the Next.js frontend types (`propertize-front-end/src/types/`) and the Java backend DTOs (`propertize/src/main/java/com/propertize/dto/`). These should be resolved incrementally, feature-by-feature.

### 1.1 Lease Domain (Critical)

| Backend DTO                                                                                                                                                                           | Frontend Status                                              |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| `LeaseCreateRequest` (nested: `LeaseDates`, `LeaseTerms`, `LeaseFinancials`, `LateFeePolicy`, `SecurityDeposit`, `UtilityConfig`, `PetPolicy`, `LeaseRestrictions`, `LeaseClauseDTO`) | **No matching frontend types** — frontend sends flat objects |
| `LeaseResponseDTO` (12 nested sections: `basicInfo`, `dates`, `terms`, `financials`, etc.)                                                                                            | Frontend `Lease` interface is flat, missing ~40 fields       |
| `LeaseSignatureResponse`, `LeaseDocumentResponse`                                                                                                                                     | No frontend equivalents                                      |

**Impact:** Lease creation/editing from frontend will fail or silently drop fields.

### 1.2 Property Domain

| Issue                                                                                                                                                                                 | Detail                                                                                                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Backend returns 12 nested sections (`basicInfo`, `financial`, `location`, `amenities`, `parking`, `utilities`, `pet`, `specifications`, `media`, `units`, `availability`, `metadata`) | Frontend flattens into single `Property` interface                                                                                                                                                                 |
| Missing frontend fields                                                                                                                                                               | `yearBuilt`, `lotSize`, `parkingSpaces`, `laundryType`, `heatingType`, `coolingType`, `flooringType`, `roofType`, `exteriorType`, `foundationType`, `waterSource`, `sewerType`, `zoning`, `hoaFee`, `hoaFrequency` |

### 1.3 Rental Application Domain

| Issue                                                                                                      | Detail                                                |
| ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| 12+ nested backend DTOs (`RentalApplicantSnapshot`, `ApplicationStatusHistory`, screening results)         | Frontend has basic `RentalApplication` interface only |
| Screening DTOs (`CreditCheckResult`, `BackgroundCheckResult`, `EvictionCheckResult`, `IncomeVerification`) | No frontend equivalents                               |

### 1.4 Payment Domain

| Issue                                                                                     | Detail                                                                                                                                     |
| ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `PaymentResponse` has nested `PaymentMethodDetails`, `RecurringPaymentInfo`, `RefundInfo` | Frontend `Payment` interface is flat                                                                                                       |
| Missing fields                                                                            | `processingFee`, `netAmount`, `paymentGateway`, `gatewayTransactionId`, `refundAmount`, `refundReason`, `refundDate`, `recurringFrequency` |

### 1.5 Tenant Domain

| Issue                                                                                                                                                      | Detail                                                                                                                           |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Backend has 8 nested response sections (`personalInfo`, `contactInfo`, `emergencyContact`, `employmentInfo`, `leaseInfo`, `documents`, `vehicles`, `pets`) | Frontend `Tenant` is flat                                                                                                        |
| Missing fields                                                                                                                                             | `ssn`, `driversLicense`, `employer`, `annualIncome`, `emergencyContactName`, `emergencyContactPhone`, `emergencyContactRelation` |

### 1.6 ID Type Mismatch

| Layer            | ID Type                     |
| ---------------- | --------------------------- |
| Backend entities | `String` (UUID format)      |
| Frontend types   | `number` in many interfaces |

**Impact:** Type conversion errors at runtime.

### 1.7 Pagination Inconsistency

| Layer            | Page size param                   |
| ---------------- | --------------------------------- |
| Backend (Spring) | `size` (default 10)               |
| Frontend         | Some use `limit`, some use `size` |

---

## 2. Embeddable Entity Candidates

These entity fields could be extracted into `@Embeddable` value objects to reduce duplication. Listed by priority/impact.

### 2.1 HIGH Priority — PersonName

**Pattern:** `firstName` + `lastName` (+ optional `middleName`) repeated across 5+ entities.

| Entity                    | Fields                                                 |
| ------------------------- | ------------------------------------------------------ |
| `Tenant`                  | `firstName`, `lastName`                                |
| `User`                    | `firstName`, `lastName`                                |
| `RentalApplicantSnapshot` | `firstName`, `lastName`                                |
| `Invoice`                 | `tenantFirstName`, `tenantLastName`                    |
| `ApplicationFee`          | `applicantName` (single combined field — inconsistent) |

**Proposed `@Embeddable`:**

```java
@Embeddable
@Getter @Setter
public class PersonName {
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

**Risk:** Medium — requires updating all repositories, mappers, and queries that reference these fields.

### 2.2 HIGH Priority — ApprovalInfo

**Pattern:** `approvedBy` + `approvedDate` + `approvalNotes` across 3 entities.

| Entity               | Fields                                      |
| -------------------- | ------------------------------------------- |
| `ApprovalWorkflow`   | `approvedBy`, `approvedAt`, `approvalNotes` |
| `RentalApplication`  | `reviewedBy`, `reviewedAt`, `reviewNotes`   |
| `MaintenanceRequest` | `approvedBy`, `approvedDate`                |

### 2.3 MEDIUM Priority — SenderInfo / RecipientInfo (Message entity)

**Fields:** `senderName`, `senderEmail`, `senderType` / `recipientName`, `recipientEmail`, `recipientType`

### 2.4 MEDIUM Priority — AssetLifecycle

**Pattern:** `purchaseDate`, `purchasePrice`, `warrantyExpiration`, `depreciationMethod`
**Entity:** `Asset`

### 2.5 MEDIUM Priority — NotificationLifecycle

**Pattern:** `sentAt`, `readAt`, `expiresAt` across `Notification`, `Announcement`, `Message`

### 2.6 LOW Priority — VeterinaryContact

**Pattern:** `vetName`, `vetPhone` in `Pet` entity (single entity, low value)

### 2.7 LOW Priority — FeeWaiverInfo

**Pattern:** `waiverReason`, `waivedBy`, `waivedAt` in `ApplicationFee` and `LateFee`

**Recommendation:** Start with `PersonName` (highest reuse count). Test thoroughly — embeddable changes affect Hibernate column mapping and can cause `DuplicateMappingException` if not done carefully.

---

## 3. Magic Strings/Numbers — Completed Fixes

The following magic values were extracted to constants in this audit:

### 3.1 New File: `BusinessRuleConstants.java`

| Inner Class      | Constants Extracted                                                                                                                                                                                     |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Session`        | `MAX_SESSIONS_PER_USER=5`, `TIMEOUT_SECONDS=3600`, `CLEANUP_INTERVAL_MS`                                                                                                                                |
| `Lease`          | `EXPIRATION_WINDOW_DAYS=60`, `EXPIRATION_NOTICE_DAYS=30`, `INVOICE_GRACE_PERIOD_DAYS=5`                                                                                                                 |
| `LeaseSignature` | `TOKEN_EXPIRY_DAYS=30`, `REMINDER_INTERVAL_DAYS=3`, `DEFAULT_SIGNATURE_METHOD="EMAIL_LINK"`                                                                                                             |
| `Approval`       | `EXPIRING_SOON_HOURS=24`, `DEFAULT_EXPIRATION_DAYS=3`, `ONBOARDING_EXPIRATION_DAYS=7`, `HIGH_VALUE_PAYMENT_EXPIRATION_DAYS=5`, `DATA_EXPORT_EXPIRATION_DAYS=1`, `DELETION_SUSPENSION_EXPIRATION_DAYS=3` |
| `PasswordReset`  | `TOKEN_EXPIRY_HOURS=24`                                                                                                                                                                                 |
| `ApplicationFee` | `DEFAULT_DUE_DAYS=7`                                                                                                                                                                                    |
| `Support`        | `CONTACT_HISTORY_DAYS=7`, `TICKET_HISTORY_DAYS=30`                                                                                                                                                      |
| `Email`          | `PREVIEW_MAX_LENGTH=200`                                                                                                                                                                                |
| `LateFee`        | `DEFAULT_GRACE_PERIOD_DAYS=3`                                                                                                                                                                           |
| `Time`           | `MS_PER_MINUTE=60000`, `HOURS_PER_DAY=24.0`                                                                                                                                                             |

### 3.2 Extended: `ApplicationConstants.java`

| Inner Class | Constants Added                                                                                                                                                                                                        |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Headers`   | `X_FORWARDED_FOR`, `X_REAL_IP`, `ORGANIZATION_CODE`, `ORGANIZATION_ID`, `SESSION_ID`, `API_VERSION`, `CLIENT_CODE`, `REQUESTED_WITH`, `TOTAL_COUNT`, `PAGE_NUMBER`, `PAGE_SIZE`, `TOTAL_PAGES`, `CONTENT_TYPE_OPTIONS` |
| `SkipPaths` | `AUTH`, `PUBLIC`, `ACTUATOR`, `ACTUATOR_HEALTH`, `ACTUATOR_INFO`, `SWAGGER_UI`, `API_DOCS`, `FAVICON`                                                                                                                  |

### 3.3 Files Updated to Use Constants

| File                                | Changes                                                 |
| ----------------------------------- | ------------------------------------------------------- |
| `SessionManagementService.java`     | `MAX_SESSIONS_PER_USER`, `X-Forwarded-For`, `X-Real-IP` |
| `SessionTrackingFilter.java`        | All 5 skip path strings                                 |
| `RequestResponseLoggingFilter.java` | `X-Correlation-ID`, `X-Forwarded-For`, `X-Real-IP`      |
| `LeaseService.java`                 | `plusDays(60)`, `plusDays(5)`, `plusDays(30)`           |
| `LeaseSignatureService.java`        | `plusDays(30)`, `"EMAIL_LINK"`, `minusDays(3)`          |
| `ApprovalWorkflowService.java`      | `plusHours(24)`, all 5 switch case day values           |
| `PasswordResetService.java`         | `plusHours(24)`                                         |
| `ApplicationFeeService.java`        | `plusDays(7)`                                           |
| `ContactUsService.java`             | `minusDays(7)`                                          |
| `SupportTicketService.java`         | `minusDays(30)`                                         |
| `EmailService.java`                 | `200` preview length                                    |

---

## 4. Remaining Items (Not Fixed — Require Larger Refactoring)

### 4.1 CorsConfig / WebMvcConfig Header Strings

`CorsConfig.java` and `WebMvcConfig.java` have hardcoded header strings in allowed/exposed headers lists. These could use `ApplicationConstants.Headers.*` but the change touches CORS security config and should be done with integration testing.

### 4.2 Additional Magic Numbers in Other Services

Some services in `employee-service/`, `payment-service/`, `payroll-service/` likely have similar magic numbers. A per-service audit should be done when those services are next modified.

### 4.3 Embeddable Extractions

All embeddable candidates identified in Section 2 require careful migration with test coverage. They should be done one at a time with full compile + test verification.

### 4.4 Frontend Type Alignment

DTO mapping gaps in Section 1 require coordinated frontend + backend changes. Recommended approach:

1. Start with the Lease domain (most impacted)
2. Create proper TypeScript interfaces matching backend nested DTOs
3. Update API service files to transform responses
4. Add proper ID type handling (string UUIDs)
