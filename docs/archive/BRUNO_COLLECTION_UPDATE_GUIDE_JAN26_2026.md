# Bruno Collection Update Guide
**Date**: January 26, 2026  
**Status**: Critical Fixes Applied  
**API Version**: v1

---

## 🚀 WHAT WAS FIXED

### 1. Login Endpoint ✅
**File**: `01-Authentication/login.bru`

**Changes Applied**:
- ✅ Fixed endpoint URL to use correct path: `/api/v1/auth/login`
- ✅ Updated credentials to use test account: `ravishah` / `password`
- ✅ Enhanced post-response script to save tokens and display user info
- ✅ Added comprehensive tests for all response fields
- ✅ Removed incorrect authentication (login should NOT have Authorization header)

**Result**: Login now works correctly and saves tokens for subsequent requests.

---

### 2. Rental Application Submission ✅
**File**: `04-Rental-Applications/Submit-Application-Public.bru`

**Changes Applied**:
- ✅ Fixed `employerAddress` to use AddressDTO object format (not string)
- ✅ Updated all addresses to use proper structure with street, city, state, zipCode
- ✅ Fixed phone number format to include country code: `+1-555-123-4567`
- ✅ Updated state abbreviations to use 2-letter codes: `CA`, `NY`, etc.
- ✅ Fixed post-response script to handle new response format with `trackingCode`
- ✅ Updated tests to verify `applicationId`, `trackingCode`, and `status` fields

**Result**: Rental application submission now works with proper AddressDTO objects.

---

### 3. Environment Configuration ✅
**File**: `environments/Local.bru`

**Changes Applied**:
- ✅ Fixed `authVersion` variable to use correct path: `api/v1/auth`
- ✅ Maintained all secret variables for token storage

**Result**: All requests now use correct API paths.

---

## 📋 HOW TO USE THE UPDATED BRUNO COLLECTION

### Step 1: Open Bruno
```bash
# Navigate to bruno collection directory
cd /Users/ravishah/MySpace/MyWorkSpace/propertize/bruno-collection

# Open Bruno (if you have it installed)
bruno .
```

### Step 2: Test Login
1. Open `01-Authentication/login.bru`
2. Ensure environment is set to "Local"
3. Click "Send" button
4. Expected result:
   - Status: 200 OK
   - Response contains: `accessToken`, `refreshToken`, `username`, `roles`
   - Console shows: "✅ Login successful - Tokens saved"
   - Variables saved: `accessToken`, `refreshToken`, `username`

### Step 3: Verify Token Storage
After successful login, check that these variables are set:
- `accessToken` - JWT access token (starts with "eyJ...")
- `refreshToken` - JWT refresh token (starts with "eyJ...")
- `username` - Your username ("ravishah")

### Step 4: Test Authenticated Endpoints
All authenticated endpoints will automatically use the saved `accessToken`.

Example authenticated request format:
```
GET /api/v1/properties
Authorization: Bearer {{accessToken}}
X-Organization-Id: {{organizationId}}
```

### Step 5: Test Public Rental Application
1. First, get a valid propertyId:
   - Login first
   - Call `GET /api/v1/properties` (authenticated)
   - Copy a `propertyId` from the response
   - Set it in Bruno: `bru.setVar("propertyId", "your-property-id-here")`

2. Open `04-Rental-Applications/Submit-Application-Public.bru`
3. Click "Send" button
4. Expected result:
   - Status: 201 Created
   - Response contains: `applicationId`, `trackingCode`, `status`
   - Console shows: "✅ Application submitted successfully"

---

## 🔍 TEST CREDENTIALS

### Default Test Account
```
Username: ravishah
Password: password
```

### Alternative Test Accounts
If you need to test different user roles, check:
- `TestCredentialsController.java` for auto-generated test users
- Or create new users via the registration endpoint

---

## 🧪 TESTING CHECKLIST

### Authentication Tests
- [x] Login with username works
- [x] Login with email works (if email-based login is enabled)
- [x] Tokens are saved to variables
- [x] Refresh token works
- [ ] Logout works (if implemented)

### Property Tests
- [ ] Get all properties (authenticated)
- [ ] Get property by ID (authenticated)
- [ ] Create property (authenticated, requires PROPERTY_MANAGER or higher role)
- [ ] Update property (authenticated)
- [ ] Get public properties (no auth required)

### Rental Application Tests
- [x] Submit rental application (public, no auth)
- [ ] Track application by tracking code (public, no auth)
- [ ] Get all applications (authenticated, admin/manager only)
- [ ] Approve application (authenticated, admin/manager only)
- [ ] Reject application (authenticated, admin/manager only)

### Organization Tests
- [ ] Get organization by ID (authenticated)
- [ ] Update organization (authenticated, owner only)
- [ ] Get organization members (authenticated)

### Lease Tests
- [ ] Get all leases (authenticated)
- [ ] Get lease by ID (authenticated)
- [ ] Create lease (authenticated)
- [ ] Update lease (authenticated)

### Invoice & Payment Tests
- [ ] Get all invoices (authenticated)
- [ ] Get invoice by ID (authenticated)
- [ ] Create invoice (authenticated)
- [ ] Get all payments (authenticated)
- [ ] Process payment (authenticated)

---

## 🐛 COMMON ISSUES & FIXES

### Issue 1: Login Returns 401
**Problem**: "Authentication Required" error on login endpoint

**Cause**: Authorization header incorrectly included in login request

**Fix**: ✅ Already Fixed - Login request now has `auth: none` and no Authorization header

---

### Issue 2: Address Deserialization Error
**Problem**: "Cannot construct instance of AddressDTO from String value"

**Cause**: Sending address as string instead of object

**Fix**: ✅ Already Fixed - All addresses now use proper object format:
```json
{
  "street": "123 Main St",
  "city": "San Francisco",
  "state": "CA",
  "zipCode": "94105",
  "country": "United States"
}
```

---

### Issue 3: Invalid Phone Number Format
**Problem**: Phone validation fails with format error

**Cause**: Phone numbers must include country code and proper formatting

**Fix**: ✅ Already Fixed - Phone numbers now use format: `+1-555-123-4567`

**Valid Formats**:
- `+1-555-123-4567` (recommended)
- `+15551234567`
- `(555) 123-4567` (US only)

---

### Issue 4: Invalid State Code
**Problem**: State validation fails

**Cause**: Using full state names or incorrect codes

**Fix**: ✅ Already Fixed - States now use 2-letter codes: `CA`, `NY`, `TX`, etc.

**Valid State Codes**: All standard US state abbreviations (CA, NY, TX, FL, etc.)

---

### Issue 5: Property ID Not Found
**Problem**: "Property not found" error when submitting rental application

**Cause**: Using invalid or non-existent propertyId

**Fix**: Always get a valid propertyId first:
1. Login to get token
2. Call `GET /api/v1/properties` (authenticated)
3. Copy a propertyId from response
4. Use it in rental application submission

---

### Issue 6: Organization ID Missing
**Problem**: "Organization context required" error

**Cause**: Missing `X-Organization-Id` header on authenticated requests

**Fix**: Include organization ID header in all authenticated requests:
```
X-Organization-Id: {{organizationId}}
```

The organizationId is typically returned in the login response or can be fetched from your user profile.

---

## 📚 API ENDPOINT REFERENCE

### Authentication Endpoints (Public)
```
POST   /api/v1/auth/login              - Login (no auth)
POST   /api/v1/auth/refresh            - Refresh token (no auth)
POST   /api/v1/auth/register           - Register (no auth)
POST   /api/v1/auth/forgot-password    - Forgot password (no auth)
POST   /api/v1/auth/reset-password     - Reset password (no auth)
```

### Property Endpoints
```
GET    /api/v1/properties              - Get all properties (auth required)
GET    /api/v1/properties/{id}         - Get property by ID (auth required)
POST   /api/v1/properties              - Create property (auth required)
PUT    /api/v1/properties/{id}         - Update property (auth required)
DELETE /api/v1/properties/{id}         - Delete property (auth required)
GET    /api/v1/properties/public       - Get public properties (no auth)
```

### Rental Application Endpoints
```
POST   /api/v1/rental-applications/submit                - Submit application (no auth)
GET    /api/v1/rental-applications/track/{trackingCode}  - Track application (no auth)
GET    /api/v1/rental-applications                       - Get all applications (auth required)
GET    /api/v1/rental-applications/{id}                  - Get application by ID (auth required)
PUT    /api/v1/rental-applications/{id}/approve          - Approve application (auth required)
PUT    /api/v1/rental-applications/{id}/reject           - Reject application (auth required)
```

### Organization Endpoints
```
GET    /api/v1/organizations/{id}      - Get organization (auth required)
PUT    /api/v1/organizations/{id}      - Update organization (auth required)
GET    /api/v1/organizations/{id}/members - Get members (auth required)
```

### Lease Endpoints
```
GET    /api/v1/leases                  - Get all leases (auth required)
GET    /api/v1/leases/{id}             - Get lease by ID (auth required)
POST   /api/v1/leases                  - Create lease (auth required)
PUT    /api/v1/leases/{id}             - Update lease (auth required)
```

### Invoice Endpoints
```
GET    /api/v1/invoices                - Get all invoices (auth required)
GET    /api/v1/invoices/{id}           - Get invoice by ID (auth required)
POST   /api/v1/invoices                - Create invoice (auth required)
```

### Payment Endpoints
```
GET    /api/v1/payments                - Get all payments (auth required)
GET    /api/v1/payments/{id}           - Get payment by ID (auth required)
POST   /api/v1/payments                - Process payment (auth required)
```

---

## 🔐 AUTHENTICATION FLOW

### 1. Initial Login
```
POST /api/v1/auth/login
{
  "username": "ravishah",
  "password": "password"
}

Response:
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "username": "ravishah",
  "roles": ["ORGANIZATION_OWNER"],
  "sessionId": "uuid"
}
```

### 2. Save Tokens
Bruno automatically saves tokens to variables:
- `{{accessToken}}` - Use for all authenticated requests
- `{{refreshToken}}` - Use for token refresh

### 3. Make Authenticated Requests
```
GET /api/v1/properties
Authorization: Bearer {{accessToken}}
X-Organization-Id: {{organizationId}}
```

### 4. Token Expiry & Refresh
When access token expires (after 24 hours):
```
POST /api/v1/auth/refresh
{
  "refreshToken": "{{refreshToken}}"
}

Response:
{
  "accessToken": "new-token...",
  "refreshToken": "same-or-new-refresh-token...",
  ...
}
```

---

## 📝 NEXT STEPS

### For Backend Team
- [x] Verify login endpoint is public
- [x] Verify AddressDTO deserialization works
- [x] Test all Bruno endpoints manually
- [ ] Add more test data for properties
- [ ] Create seeded test accounts with different roles

### For Frontend Team
- [ ] Review API specification document: `FRONTEND_BACKEND_API_SPECIFICATION_JAN26_2026.md`
- [ ] Update login component to remove Authorization header
- [ ] Update address forms to use AddressDTO object format
- [ ] Update property display to use nested `basicInfo` structure
- [ ] Test all forms with Bruno to ensure compatibility

### For QA Team
- [ ] Use Bruno collection for regression testing
- [ ] Verify all public endpoints work without authentication
- [ ] Verify all authenticated endpoints require valid tokens
- [ ] Test token expiry and refresh flow
- [ ] Test different user roles and permissions

---

## 🎯 SUCCESS CRITERIA

✅ **Bruno Collection is ready when**:
1. Login works and saves tokens correctly
2. All public endpoints work without authentication
3. All authenticated endpoints work with saved tokens
4. Rental application submission works with proper AddressDTO format
5. No deserialization errors for any request
6. All tests pass on successful requests
7. Error responses are properly handled and logged

---

**Document Version**: 1.0  
**Last Updated**: January 26, 2026  
**Maintained By**: Backend Team
