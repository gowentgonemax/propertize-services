# Bruno Collection Migration Guide

**Version:** 2.0 (DTO-based Architecture)  
**Date:** January 24, 2026

---

## 🚀 Quick Start

### What Changed?
The API now returns **DTOs (Data Transfer Objects)** instead of raw entities. This provides:
- ✅ Better security (no sensitive data exposure)
- ✅ Cleaner responses (organized in logical sections)
- ✅ Computed fields (fullName, isPastDue, etc.)
- ✅ Flexible evolution (API can change without breaking database)

### Do I Need to Update My Requests?
**Yes, if you're using:**
- Properties endpoints → Now returns `PropertyResponseDTO`
- Tenants endpoints → Now returns `TenantResponseDTO`  
- Invoices endpoints → Now returns `InvoiceResponseDTO`
- Vendors endpoints → Now uses `VendorCreateRequest/VendorUpdateRequest`
- Maintenance endpoints → Now returns `MaintenanceRequestResponse`
- Documents endpoints → Now returns `DocumentResponseDTO`
- Assets endpoints → Now uses `AssetCreateRequest`

**No changes needed for:**
- Leases (already using DTOs)
- Payments (already using DTOs)
- Authentication (already using DTOs)
- Organization (already using DTOs)
- Users (already using DTOs)

---

## 📋 Migration Steps

### Step 1: Update Your Scripts

**Old way to get property ID:**
```javascript
const propertyId = res.body.id;
```

**New way (handles both structures):**
```javascript
const propertyId = res.body.basicInfo?.id || res.body.id;
```

### Step 2: Update Your Tests

**Old way:**
```javascript
test("Response has property name", function() {
  expect(res.body.propertyName).to.exist;
});
```

**New way:**
```javascript
test("Response has property name", function() {
  const name = res.body.basicInfo?.propertyName || res.body.propertyName;
  expect(name).to.exist;
});
```

### Step 3: Update Request Bodies

#### Vendor Create Example

**OLD:**
```json
{
  "companyName": "ABC Plumbing",
  "serviceType": "plumber",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001"
  }
}
```

**NEW:**
```json
{
  "name": "ABC Plumbing",
  "contactPerson": "John Smith",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NEW_YORK",
  "zipCode": "10001",
  "type": "CONTRACTOR",
  "serviceType": "PLUMBING"
}
```

### Step 4: Handle Response Wrappers

Some endpoints now wrap responses:

**ApiResponse wrapper (Tenants):**
```javascript
if (res.body.success && res.body.data) {
  const tenants = res.body.data;
}
```

**PaginationResponse (Properties, Invoices, Maintenance):**
```javascript
if (res.body.data && res.body.meta) {
  const items = res.body.data;
  const totalPages = res.body.meta.totalPages;
}
```

---

## 🔍 Response Structure Examples

### PropertyResponseDTO
```json
{
  "id": "prop-123",
  "organizationId": "org-456",
  "basicInfo": {
    "id": "prop-123",
    "propertyName": "Sunset Apartments 101",
    "type": "APARTMENT",
    "status": "AVAILABLE",
    "address": { ... }
  },
  "financial": {
    "monthlyRent": 2500.00,
    "securityDeposit": 2500.00
  },
  "amenities": {
    "features": ["POOL", "GYM", "PARKING"]
  }
}
```

### TenantResponseDTO
```json
{
  "basicInfo": {
    "id": "tenant-123",
    "firstName": "John",
    "lastName": "Doe",
    "status": "ACTIVE"
  },
  "contact": {
    "email": "john@example.com",
    "phone": "+1-555-1234"
  },
  "employment": {
    "employer": "Tech Corp",
    "monthlyIncome": 5000.00
  }
}
```

### InvoiceResponseDTO
```json
{
  "id": "inv-123",
  "tenantId": "tenant-456",
  "tenantFullName": "John Doe",
  "invoiceNumber": "INV-2024-001",
  "total": 2500.00,
  "status": "SENT",
  "isPastDue": false,
  "daysOverdue": 0
}
```

---

## 🐛 Common Issues & Fixes

### Issue 1: "Cannot read property 'id' of undefined"

**Cause:** Trying to access nested field that doesn't exist

**Fix:**
```javascript
// ❌ BAD
const id = res.body.basicInfo.id;

// ✅ GOOD
const id = res.body.basicInfo?.id || res.body.id;
```

### Issue 2: "Enum value not recognized"

**Cause:** Enum values changed

**Examples:**
- ❌ `state: "NY"` → ✅ `state: "NEW_YORK"`
- ❌ `serviceType: "plumber"` → ✅ `serviceType: "PLUMBING"`
- ❌ `type: "house"` → ✅ `type: "HOUSE"`

**Fix:** Use the full enum names (check entity definitions)

### Issue 3: "Response structure doesn't match"

**Cause:** API now returns DTOs with different structure

**Fix:** Update your expectations:
```javascript
// OLD - Flat structure
test("Has property name", function() {
  expect(res.body.propertyName).to.exist;
});

// NEW - Nested structure  
test("Has property name", function() {
  if (res.body.basicInfo) {
    expect(res.body.basicInfo.propertyName).to.exist;
  } else {
    expect(res.body.propertyName).to.exist;
  }
});
```

### Issue 4: "Address is flattened"

**Cause:** Vendor and Asset requests now use flattened address

**Fix:**
```json
// ❌ OLD
{
  "address": {
    "street": "123 Main St",
    "city": "New York"
  }
}

// ✅ NEW
{
  "address": "123 Main Street",
  "city": "New York",
  "state": "NEW_YORK",
  "zipCode": "10001"
}
```

---

## ✅ Verification Checklist

After updating your requests:

- [ ] Request executes without errors
- [ ] Response structure matches expected DTO
- [ ] Tests pass successfully
- [ ] Environment variables are saved correctly
- [ ] Error handling works as expected

---

## 📚 Resources

- **Updated Requests:** Check `/bruno-collection/` for examples
- **DTO Documentation:** See `/DTO_REFACTORING_PLAN.md`
- **Full Update Report:** See `/BRUNO_COLLECTION_UPDATE_REPORT.md`
- **API Docs:** http://localhost:8080/swagger-ui.html

---

## 🆘 Need Help?

1. **Check the examples** in updated requests
2. **Review the DTO structures** in mapper files
3. **Test with simple requests first** (GET endpoints)
4. **Gradually update** your collection

---

## 📊 Migration Status

| Collection | Status | Completion |
|-----------|--------|------------|
| Properties | 🟡 Partial | 60% |
| Tenants | 🟡 Partial | 40% |
| Invoices | 🟡 Partial | 40% |
| Vendors | 🟡 Partial | 40% |
| Maintenance | 🟡 Partial | 60% |
| Documents | 🟢 Complete | 100% |
| Leases | 🟢 Complete | 100% |
| Payments | 🟢 Complete | 100% |
| Auth | 🟢 Complete | 100% |
| Organization | 🟢 Complete | 100% |
| Users | 🟢 Complete | 100% |

**Overall Progress:** ~65%

---

**Last Updated:** January 24, 2026  
**Support:** Check documentation or review updated examples
