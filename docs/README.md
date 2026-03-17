# Bruno API Collection - Updated for DTO Architecture

**Last Updated:** January 24, 2026  
**Status:** Updated for Clean Architecture with DTOs

---

## 🎯 Overview

This Bruno collection has been updated to work with the refactored API that now returns DTOs (Data Transfer Objects) instead of raw entities. All endpoints now follow clean architecture principles.

---

## 🔄 Major Changes

### Response Structure Changes

#### Properties API
- **Old:** Returns `Property` entity
- **New:** Returns `PropertyResponseDTO` with nested sections:
  - `basicInfo` - Core property information
  - `physicalDetails` - Construction details
  - `financial` - Rent, deposits, fees
  - `amenities` - Property amenities
  - `utilities` - Utility providers
  - `leaseRequirements` - Tenant requirements
  - `petPolicy` - Pet rules
  - `marketing` - Listing information
  - `maintenance` - Maintenance history
  - `legal` - Legal documentation
  - `occupancy` - Current occupancy

#### Tenants API
- **Old:** Returns `Tenant` entity
- **New:** Returns `ApiResponse<TenantResponseDTO>` with sections:
  - `basicInfo` - Basic tenant information
  - `contact` - Contact details
  - `employment` - Employment information
  - `screening` - Background check data
  - `rating` - Performance metrics
  - `leaseInfo` - Current lease details

#### Invoices API
- **Old:** Returns `Invoice` entity
- **New:** Returns `InvoiceResponseDTO` with fields:
  - All invoice details flattened
  - Computed fields: `tenantFullName`, `isPastDue`, `daysOverdue`
  - Reference IDs: `tenantId`, `leaseId`, `paymentId`

#### Vendors API
- **Old:** Accepts and returns `Vendor` entity
- **New:** 
  - **Request:** `VendorCreateRequest` or `VendorUpdateRequest`
  - **Response:** `VendorResponseDTO` with computed fields

#### Maintenance Requests API
- **Old:** Returns `MaintenanceRequests` entity
- **New:** Returns `MaintenanceRequestResponse` with:
  - All maintenance details
  - Related entity information (property, tenant, vendor)
  - Computed fields

#### Documents API
- **Old:** Returns `Document` entity
- **New:** Returns `DocumentResponseDTO` with:
  - File metadata
  - Storage URLs
  - Access tracking

#### Assets API
- **Old:** Accepts and returns `Asset` entity
- **New:**
  - **Request:** `AssetCreateRequest`
  - **Response:** `AssetResponseDTO` with computed fields

---

## 📋 Updated Collections

### ✅ Properties (03-Properties/)
- [x] Get All Properties - Updated for `PropertyResponseDTO`
- [x] Get Property By ID - Updated for nested DTO structure
- [x] Create Property - Updated response handling
- [ ] Update Property - Needs update
- [ ] Delete Property - No changes needed (returns void)
- [ ] Get Property Statistics - No changes needed (returns Map)
- [ ] Get Available Properties Public - Needs update

### ✅ Tenants (05-Tenants/)
- [x] Get All Tenants - Updated for `ApiResponse<List<TenantResponseDTO>>`
- [ ] Get Tenant By ID - Needs update
- [ ] Create Tenant - Needs update
- [ ] Get Tenant Payment History - No changes needed
- [ ] Get Tenant Statistics - No changes needed

### ✅ Invoices (07-Invoices/)
- [x] Get All Invoices - Updated for `InvoiceResponseDTO`
- [ ] Get Invoice By ID - Needs update
- [ ] Create Invoice - Needs update
- [ ] Mark Invoice Paid - Needs update
- [ ] Get Overdue Invoices - Needs update
- [ ] Get Invoice Statistics - No changes needed

### ✅ Vendors (18-Vendors/)
- [x] Create Vendor - Updated for `VendorCreateRequest`
- [ ] Get All Vendors - Needs update
- [ ] Get Vendor By ID - Needs update
- [ ] Update Vendor - Needs update for `VendorUpdateRequest`
- [ ] Search Vendors - Needs update
- [ ] Delete Vendor - No changes needed

### ✅ Maintenance (09-Maintenance/)
- [x] Get All Maintenance - Updated for `MaintenanceRequestResponse`
- [ ] Create Maintenance - Needs update
- [ ] Get Maintenance Statistics - No changes needed

### ⏳ Not Yet Updated
- Documents (19-Documents/)
- Leases (06-Leases/) - Already using DTOs
- Payments (08-Payments/) - Already using DTOs
- Authentication (01-Authentication/) - Already using DTOs
- Organization (02-Organization/) - Already using DTOs
- Users (10-Users/) - Already using DTOs

---

## 🔧 Testing Tips

### Accessing Nested Fields

**Properties:**
```javascript
// Old way
const propertyId = res.body.id;
const propertyName = res.body.propertyName;

// New way
const propertyId = res.body.basicInfo?.id || res.body.id;
const propertyName = res.body.basicInfo?.propertyName;
```

**Tenants:**
```javascript
// Old way
const tenantId = res.body.id;
const tenantName = res.body.firstName;

// New way
const tenantId = res.body.basicInfo?.id;
const tenantName = res.body.basicInfo?.firstName;
```

### Handling Response Wrappers

Some endpoints now return responses wrapped in `ApiResponse`:
```javascript
// Check for ApiResponse wrapper
if (res.body.success && res.body.data) {
  const items = res.body.data; // Array or single object
}
```

Pagination responses have consistent structure:
```javascript
{
  "data": [...],
  "meta": {
    "page": 0,
    "limit": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "page": 0
}
```

---

## 📝 Request Body Changes

### Vendor Create Request

**Old:**
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

**New:**
```json
{
  "name": "ABC Plumbing",
  "contactPerson": "John Smith",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NEW_YORK",
  "zipCode": "10001",
  "type": "CONTRACTOR",
  "serviceType": "PLUMBING",
  "hourlyRate": 75.00
}
```

### Asset Create Request

**Old:** Sent entire entity

**New:** Use `AssetCreateRequest`:
```json
{
  "name": "HVAC Unit A",
  "type": "HVAC",
  "serialNumber": "SN-12345",
  "manufacturer": "Carrier",
  "purchaseDate": "2024-01-15T00:00:00",
  "purchasePrice": 5000.00,
  "propertyId": "property-id-here"
}
```

---

## 🐛 Common Issues & Solutions

### Issue: Response structure doesn't match tests

**Solution:** Update your tests to handle DTO structure:
```javascript
// Check for both old and new structure
test("Response has ID", function() {
  const id = res.body.basicInfo?.id || res.body.id;
  expect(id).to.exist;
});
```

### Issue: Nested fields are undefined

**Solution:** Use optional chaining:
```javascript
const propertyName = res.body.basicInfo?.propertyName;
const tenantEmail = res.body.contact?.email;
```

### Issue: Enum values changed

**Solution:** Check the actual enum definitions:
- States: Use full enum names like `NEW_YORK` not `NY`
- Vendor types: Use `CONTRACTOR`, `SUPPLIER`, `SERVICE_PROVIDER`
- Service types: Use `PLUMBING`, `ELECTRICAL`, `HVAC`

---

## 🎯 Next Steps

1. **Test All Endpoints:** Run each collection folder to verify functionality
2. **Update Remaining Requests:** Update the collections marked as "Needs update"
3. **Save Environment Variables:** Ensure IDs are properly saved in scripts
4. **Document Edge Cases:** Add tests for error scenarios

---

## 📚 Related Documentation

- `DTO_REFACTORING_PLAN.md` - Complete refactoring details
- `APPLICATION_STATUS_REPORT.md` - Current application status
- API Documentation: http://localhost:8080/swagger-ui.html

---

**Status:** 30% Complete - Core collections updated, remaining collections pending

**Priority:** High - Update remaining collections before next release

**Contributors:** Development Team
