# Bruno Collection Updates - February 1, 2026

## Summary
Updated all Lease and Tenant API requests to align with the refactored DTO structure (v2.0).

---

## What Changed

### DTO Structure Overhaul
- **OLD:** Flat response structure with all fields at root level
- **NEW:** Nested DTO structure with domain-specific sections

Example:
```json
// OLD Response
{
  "id": "lease-123",
  "monthlyRent": 2500.00,
  "tenantFirstName": "John"
}

// NEW Response
{
  "basicInfo": {
    "leaseId": "lease-123"
  },
  "financial": {
    "monthlyRent": 2500.00
  },
  "tenant": {
    "firstName": "John"
  }
}
```

---

## Updated Files

### 📁 06-Leases/

#### ✏️ Modified Files

1. **Create-Lease.bru**
   - Updated endpoint from `/leases/create` → `/leases`
   - Removed deprecated fields: `leaseType`, `noticePeriodDays`, `petsAllowed`, etc.
   - Added new fields: `petDeposit`, `utilitiesIncluded`, `status`
   - Updated response parsing to use nested structure
   - Added comprehensive documentation

2. **Get-Lease-By-ID.bru** (if exists)
   - Response now returns nested DTOs
   - Parse `basicInfo.leaseId` instead of `id`

3. **Get-All-Leases.bru** (if exists)
   - Response returns `LeaseListResponse` with simplified list items
   - Updated filtering parameters

#### 🆕 New Files

1. **Renew-Lease.bru**
   - NEW: `POST /leases/{leaseId}/renew`
   - Creates a new lease from existing lease
   - Request body: `LeaseRenewalRequest`
   - Response: New `LeaseResponse` with PENDING status

2. **Update-Lease.bru**
   - NEW: `PUT /leases/{leaseId}`
   - Updates existing lease fields
   - Request body: `LeaseUpdateRequest` (all fields optional)
   - Cannot update dates, property, or tenant

---

### 📁 05-Tenants/

#### ✏️ Modified Files

1. **Create-Tenant.bru**
   - Completely restructured request body
   - **Core fields remain flat** (firstName, lastName, email, phoneNumber)
   - **Domain-specific data moved to nested objects:**
     - `personalInfo` - DOB, SSN, marital status
     - `identification` - Driver's license, passport
     - `contactInfo` - Emergency contact, mailing address
     - `employment` - Employer, income, job title
     - `petVehicleInfo` - Pet and vehicle details
     - `preferences` - Payment and notification preferences
   - Response: `TenantCreateResponse` with simplified structure
   - Added comprehensive field documentation

2. **Get-Tenant-By-ID.bru** (if exists)
   - Response returns nested `TenantResponse` structure
   - Parse `basicInfo.tenantId` instead of `id`
   - Access fields via nested sections (e.g., `contact.email`)

3. **Get-All-Tenants.bru** (if exists)
   - Response returns `TenantListResponse` with summary items
   - Updated query parameters

#### 🆕 New Files

1. **Search-Tenants.bru**
   - NEW: `POST /tenants/search`
   - Advanced search with multiple filters
   - Request body: `TenantSearchRequest`
   - Supports filtering by status, credit score, balance, etc.
   - Returns paginated results

---

## Breaking Changes

### 1. Response Structure
**Impact:** HIGH  
**Frontend Action Required:** YES

All responses now use nested DTOs instead of flat structure.

#### Migration Example:

```typescript
// OLD CODE ❌
const leaseId = response.id;
const rent = response.monthlyRent;
const tenantName = response.tenantFirstName;

// NEW CODE ✅
const leaseId = response.basicInfo.leaseId;
const rent = response.financial.monthlyRent;
const tenantName = response.tenant.firstName;
```

### 2. Request Field Names
**Impact:** MEDIUM  
**Frontend Action Required:** YES

Some request fields have been renamed or restructured.

#### Lease Request Changes:

| Old Field | New Field | Notes |
|-----------|-----------|-------|
| `leaseType` | ❌ Removed | No longer needed |
| `noticePeriodDays` | ❌ Removed | Moved to terms |
| `petsAllowed` | ❌ Removed | Use petDeposit instead |
| `parkingIncluded` | ❌ Removed | Use utilitiesIncluded |
| `termsAndConditions` | `terms` | Renamed |
| N/A | `petDeposit` | NEW field |
| N/A | `utilitiesIncluded` | NEW array field |

#### Tenant Request Changes:

| Old Field | New Location | Notes |
|-----------|--------------|-------|
| `phone` | `phoneNumber` | Renamed |
| `dateOfBirth` | `personalInfo.dateOfBirth` | Nested |
| `address` | `contactInfo.mailingAddress` | Nested |
| `emergencyContact` | `contactInfo.emergencyContact*` | Flattened |
| `status` | `tenantStatus` | Renamed |

### 3. Endpoint Changes
**Impact:** LOW  
**Frontend Action Required:** YES (minimal)

| Old Endpoint | New Endpoint | Change |
|--------------|--------------|--------|
| `POST /leases/create` | `POST /leases` | Removed `/create` |
| All other endpoints | ✅ No change | - |

### 4. Enum Values
**Impact:** MEDIUM  
**Frontend Action Required:** Verify enum usage

New enums introduced:
- `LeaseComplianceStatusEnum`
- `TenantSegmentEnum`
- `LeaseTypeEnum` (STANDARD, MONTH_TO_MONTH, SHORT_TERM)

Status enums updated:
- `LeaseStatusEnum`: Added `SUSPENDED`
- `TenantStatusEnum`: Added `SCREENING`, `MOVED_OUT`

---

## Frontend Migration Checklist

### Phase 1: Update API Client Types ✅

1. **Update Lease Types**
   ```typescript
   // Update LeaseResponse interface
   interface LeaseResponse {
     organizationId: string;
     organizationName: string;
     basicInfo: LeaseBasicInfoDTO;
     financial: LeaseFinancialDTO;
     tenant: LeaseTenantDTO;
     property: LeasePropertyDTO;
     dates: LeaseDatesDTO;
     terms: LeaseTermsDTO;
     utilities: LeaseUtilitiesDTO;
     audit: LeaseAuditDTO;
     performance: LeasePerformanceDTO;
     // ... quick access fields
   }
   ```

2. **Update Tenant Types**
   ```typescript
   interface TenantResponse {
     organizationId: string;
     organizationName: string;
     basicInfo: TenantBasicInfoDTO;
     contact: TenantContactDTO;
     employment: TenantEmploymentDTO;
     screening: TenantScreeningDTO;
     rating: TenantRatingDTO;
     leaseInfo: TenantLeaseInfoDTO;
     performance: TenantPerformanceDTO;
     auditMetadata: AuditMetadataDTO;
     // ... quick access fields
   }
   ```

3. **Update Request Types**
   ```typescript
   interface LeaseCreateRequest {
     propertyId: string;
     tenantId: string;
     startDate: string; // ISO date
     endDate: string;
     monthlyRent: number;
     securityDeposit?: number;
     petDeposit?: number;
     lateFeeAmount?: number;
     lateFeeGracePeriod?: number;
     rentDueDay: number;
     terms?: string;
     utilitiesIncluded?: string[];
     status?: LeaseStatusEnum;
   }

   interface TenantCreateRequest {
     // Core fields (flat)
     firstName: string;
     lastName: string;
     email: string;
     phoneNumber: string;
     // Nested objects (all optional)
     personalInfo?: TenantPersonalInfoDTO;
     identification?: TenantIdentificationDTO;
     contactInfo?: TenantContactDTO;
     employment?: TenantEmploymentDTO;
     petVehicleInfo?: TenantPetVehicleDTO;
     preferences?: TenantPreferencesDTO;
     tenantStatus?: TenantStatusEnum;
   }
   ```

### Phase 2: Update API Service Methods ✅

1. **Update Response Parsing**
   ```typescript
   // Lease Service
   async getLease(leaseId: string): Promise<LeaseResponse> {
     const response = await api.get(`/leases/${leaseId}`);
     return response; // Already matches new structure
   }

   // Tenant Service
   async createTenant(request: TenantCreateRequest): Promise<TenantCreateResponse> {
     const response = await api.post('/tenants', request);
     return response;
   }
   ```

2. **Update Helper Functions**
   ```typescript
   // OLD ❌
   function getLeaseRent(lease: any): number {
     return lease.monthlyRent;
   }

   // NEW ✅
   function getLeaseRent(lease: LeaseResponse): number {
     return lease.financial.monthlyRent;
   }
   ```

### Phase 3: Update UI Components ✅

1. **Update Data Access Patterns**
   ```typescript
   // Lease Details Component
   function LeaseDetails({ lease }: { lease: LeaseResponse }) {
     return (
       <div>
         <h2>Lease {lease.basicInfo.leaseNumber}</h2>
         <p>Status: {lease.basicInfo.status}</p>
         <p>Rent: ${lease.financial.monthlyRent}</p>
         <p>Tenant: {lease.tenant.firstName} {lease.tenant.lastName}</p>
         <p>Property: {lease.property.propertyName}</p>
         <p>Start: {lease.dates.startDate}</p>
         <p>End: {lease.dates.endDate}</p>
       </div>
     );
   }

   // Tenant Profile Component
   function TenantProfile({ tenant }: { tenant: TenantResponse }) {
     return (
       <div>
         <h2>{tenant.basicInfo.firstName} {tenant.basicInfo.lastName}</h2>
         <p>Email: {tenant.contact.email}</p>
         <p>Phone: {tenant.contact.phone}</p>
         <p>Status: {tenant.basicInfo.status}</p>
         <p>Current Lease: {tenant.leaseInfo?.currentPropertyName}</p>
         <p>Payment Rate: {tenant.performance.onTimePaymentRate}%</p>
       </div>
     );
   }
   ```

2. **Update Form Handlers**
   ```typescript
   // Lease Creation Form
   function handleLeaseSubmit(formData: FormData) {
     const request: LeaseCreateRequest = {
       propertyId: formData.get('propertyId'),
       tenantId: formData.get('tenantId'),
       startDate: formData.get('startDate'),
       endDate: formData.get('endDate'),
       monthlyRent: parseFloat(formData.get('monthlyRent')),
       securityDeposit: parseFloat(formData.get('securityDeposit')),
       petDeposit: parseFloat(formData.get('petDeposit') || '0'),
       rentDueDay: parseInt(formData.get('rentDueDay')),
       terms: formData.get('terms'),
       utilitiesIncluded: formData.getAll('utilities'),
     };
     
     leaseService.createLease(request);
   }

   // Tenant Creation Form
   function handleTenantSubmit(formData: FormData) {
     const request: TenantCreateRequest = {
       firstName: formData.get('firstName'),
       lastName: formData.get('lastName'),
       email: formData.get('email'),
       phoneNumber: formData.get('phone'),
       personalInfo: {
         dateOfBirth: formData.get('dateOfBirth'),
         maritalStatus: formData.get('maritalStatus'),
       },
       employment: {
         employerName: formData.get('employer'),
         jobTitle: formData.get('jobTitle'),
         annualIncome: parseFloat(formData.get('income')),
       },
       tenantStatus: 'APPLICANT',
     };
     
     tenantService.createTenant(request);
   }
   ```

### Phase 4: Update Tests ✅

1. **Update Mock Data**
   ```typescript
   const mockLeaseResponse: LeaseResponse = {
     organizationId: 'org-123',
     organizationName: 'Test Org',
     basicInfo: {
       leaseId: 'lease-123',
       leaseNumber: 'LEASE-2026-001',
       status: 'ACTIVE',
       type: 'STANDARD',
     },
     financial: {
       monthlyRent: 2500.00,
       securityDeposit: 2500.00,
       totalDeposit: 2500.00,
     },
     // ... other nested sections
   };
   ```

2. **Update Test Assertions**
   ```typescript
   test('should display lease rent', () => {
     render(<LeaseDetails lease={mockLeaseResponse} />);
     
     // OLD ❌
     // expect(screen.getByText('$2500.00')).toBeInTheDocument();
     
     // NEW ✅
     expect(screen.getByText(`$${mockLeaseResponse.financial.monthlyRent}`)).toBeInTheDocument();
   });
   ```

---

## Bruno Collection Testing Guide

### Required Environment Variables

Ensure these variables are set in your Bruno environment:

```
baseUrl=http://localhost:8080
apiVersion=api/v1
organizationId=<your-org-id>
accessToken=<your-jwt-token>
propertyId=<test-property-id>
tenantId=<test-tenant-id>
leaseId=<test-lease-id>
```

### Testing Sequence

1. **Authentication**
   - Run `01-Authentication/Login-Organization-Owner.bru`
   - Verify `accessToken` is saved

2. **Create Tenant**
   - Run `05-Tenants/Create-Tenant.bru`
   - Verify `tenantId` is saved
   - Check response structure matches `TenantCreateResponse`

3. **Create Lease**
   - Run `06-Leases/Create-Lease.bru`
   - Verify `leaseId` is saved
   - Check response structure has nested DTOs

4. **Get Lease**
   - Run `06-Leases/Get-Lease-By-ID.bru`
   - Verify nested structure: `basicInfo`, `financial`, `tenant`, etc.

5. **Renew Lease**
   - Run `06-Leases/Renew-Lease.bru`
   - Verify new lease is created with PENDING status
   - Verify `renewedLeaseId` is saved

6. **Search Tenants**
   - Run `05-Tenants/Search-Tenants.bru`
   - Test different filters
   - Verify pagination works

---

## Common Issues & Solutions

### Issue 1: 404 Not Found on `/leases/create`
**Cause:** Endpoint changed from `/leases/create` → `/leases`  
**Solution:** Update endpoint in Bruno and frontend code

### Issue 2: Cannot read property 'monthlyRent' of undefined
**Cause:** Trying to access flat field that's now nested  
**Solution:** Use `response.financial.monthlyRent` instead

### Issue 3: Validation error: "leaseType is not allowed"
**Cause:** Removed fields still being sent  
**Solution:** Remove deprecated fields from request

### Issue 4: Empty response sections
**Cause:** Some nested sections may be null if data doesn't exist  
**Solution:** Use optional chaining: `lease.performance?.totalRentCollected`

---

## Performance Improvements

### New Features in v2.0

1. **Performance Tracking**
   - Lease: `totalRentCollected`, `outstandingBalance`, `onTimePaymentCount`
   - Tenant: `paymentOnTimeRate`, `riskScore`, `tenantSegment`

2. **Quick Access Fields**
   - Important fields duplicated at root level for quick access
   - No need to traverse nested objects for common fields

3. **Comprehensive Audit Trail**
   - All entities now have `audit` section
   - Tracks created/updated by user and timestamps

4. **Soft Delete Support**
   - `deleted` flag on all entities
   - Enables data retention and recovery

---

## Documentation References

- **Full API Reference:** `/docs/LEASE_TENANT_API_REFERENCE_FEB_2026.md`
- **DTO Package Structure:** `/src/main/java/com/propertize/dto/`
- **Enum Definitions:** `/src/main/java/com/propertize/enums/`

---

## Support

For questions or issues:
1. Check API reference document first
2. Test with Bruno collection to verify endpoint behavior
3. Review error messages for field validation details
4. Check console logs for detailed debugging information

---

**Last Updated:** February 1, 2026  
**Version:** 2.0  
**Author:** Propertize Platform Team
