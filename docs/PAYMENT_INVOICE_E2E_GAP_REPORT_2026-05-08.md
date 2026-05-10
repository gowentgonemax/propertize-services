# Payment and Invoice E2E Gap Report

Date: 2026-05-08
Scope: Frontend payment/invoice pages, backend payload contracts, build and runtime validation
Environment: Local Docker stack + local frontend dev server

## 1. Executive Summary

This report captures the real gaps found between payment/invoice backend payloads and frontend rendering, plus backend build blockers discovered during sync validation.

Current state:

- Frontend mapping for payment and invoice list/detail pages is hardened and compiles cleanly.
- API contract drift (flat vs nested tenant fields, mixed status casing) was confirmed and normalized in UI.
- Full browser E2E validation remains partially blocked by unrelated environment instability (500/503 errors from organization/session dependent endpoints).
- payment-service backend still has unresolved Docker compile issues in promo code code path.

## 2. What Was Validated

Validated with live API responses:

- GET /api/v1/payments
- GET /api/v1/invoices

Validated in frontend build:

- TypeScript compile: npx tsc --noEmit

Validated in backend:

- payment-service local compile attempt
- payment-service Docker image build attempt

## 3. Confirmed Gaps and Fix Status

### Gap A: Payment detail identity mapping showed wrong title/tenant/email

Observed symptom:

- Payment detail UI showed amount-like title, Unknown Tenant, no email for valid records.

Root cause:

- UI expected nested tenant object in many records.
- Actual payment payload frequently returns mostly scalar fields, without populated nested tenant block.

Fix applied:

- Added resilient identity derivation and fallback mapping via shared financial display helper.
- Updated payment list/detail rendering paths to use normalized display values.

Status: Fixed in frontend code.

### Gap B: Invoice tenant fields shape mismatch (flat vs nested)

Observed symptom:

- Invoice views failed to consistently show tenant identity fields.

Root cause:

- Frontend assumed nested tenant object.
- Live invoice payload includes flat fields such as tenantFirstName, tenantLastName, tenantFullName.

Fix applied:

- Added flat-field support in shared financial display helper.
- Updated invoice list/detail pages to consume normalized tenant values.

Status: Fixed in frontend code.

### Gap C: Invoice status casing mismatch broke filtering/stat cards

Observed symptom:

- Invoice filtering/stat counters were inconsistent.

Root cause:

- Backend returns status values with mixed case (example: Paid), while frontend logic assumed strict uppercase enum-like values.

Fix applied:

- Added status normalization in invoice page logic for filters and counters.

Status: Fixed in frontend code.

### Gap D: Service response envelope variance (wrapped/unwrapped)

Observed symptom:

- Invoice service consumed payloads inconsistently under data/content/raw response variants.

Root cause:

- API endpoints return heterogeneous envelopes in different paths.

Fix applied:

- Hardened invoice service response unwrapping.

Status: Fixed in frontend code.

### Gap E: Full browser E2E not fully stable

Observed symptom:

- UI flows intermittently blocked by 500/503 on unrelated endpoints (organization/session dependent routes).

Root cause:

- Environment instability, not payment/invoice mapping logic.

Status: Not fully resolved in this cycle.
Action required:

- Stabilize auth/organization APIs and rerun browser E2E suite.

## 4. Backend Build/Integration Sync Findings

### Finding 1: Local payment-service compile failed initially

Error:

- release version 25 not supported

Action taken:

- Aligned payment-service Java target to 21 in pom.
- Updated payment-service Dockerfile build/runtime base images from Java 25 to Java 21.

Result:

- Removes local toolchain mismatch blocker.

### Finding 2: Docker compile still fails in promo code path

Current blocker:

- Docker build still reports compile-time missing accessor methods in promo code classes during PromoCodeService compile.

Impact:

- payment-service image build is not yet green.
- Full backend integration tests for payment-service are blocked until this compile issue is resolved.

Status: Open.

## 5. FE-BE Sync Matrix

- Payment list/detail field sync: Green (frontend normalized)
- Invoice list/detail field sync: Green (frontend normalized)
- Invoice status semantics sync: Green (frontend normalized)
- Runtime E2E stability (auth/org dependencies): Amber (environment instability)
- payment-service Docker build: Red (promo compile blocker)

## 6. Recommended Next Actions

1. Resolve payment-service promo code compile errors in Docker context first.
2. Rebuild payment-service image and run containerized service health check.
3. Run targeted payment/invoice integration tests against gateway routes.
4. Re-run browser E2E for payment and invoice pages after environment stabilization.
5. Promote this report to a release readiness checklist entry for FE/BE sync sign-off.

## 7. Evidence Notes

Evidence sources in this validation cycle:

- Live payload inspection for payment and invoice endpoints.
- Frontend TypeScript compile validation.
- payment-service local compile and Docker build outputs.

This report reflects observed behavior from this session only and should be refreshed after backend compile blockers are fixed.
