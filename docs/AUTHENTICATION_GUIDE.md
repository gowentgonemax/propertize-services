# Bruno Collection - Authentication Guide

**Last Updated:** January 24, 2026  
**Status:** ✅ Ready to Use

---

## 🔐 Quick Start - How to Authenticate

### Step 1: Start the Application

Make sure the application is running:

```bash
# Check if application is running
curl http://localhost:8080/actuator/health

# If not running, start it:
./mvnw spring-boot:run
```

### Step 2: Run the Login Request

1. Open Bruno
2. Select the **Local** environment (top-right corner)
3. Navigate to: **01-Authentication → Login.bru**
4. Click **Send**

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "OWN-IqcRoVi",
  "roles": ["OWNER", "ADMIN"]
}
```

### Step 3: Verify Token is Saved

The `accessToken` is automatically saved and will be used for all subsequent requests.

To verify:
- Check Bruno console: You should see "✅ Tokens saved successfully"
- The token is stored in environment variables
- All requests with `auth: bearer` will automatically use it

---

## 🚨 Troubleshooting Authentication Errors

### Error: "Authentication Required" (401)

**Problem:** You haven't logged in, or your token expired.

**Solution:**
1. Run the **Login** request from `01-Authentication/Login.bru`
2. Verify you see "✅ Tokens saved successfully" in console
3. Try your request again

### Error: "Invalid credentials" (401)

**Problem:** Wrong username or password.

**Solution:**
1. Check the credentials in `Login.bru`:
   ```json
   {
     "username": "OWN-IqcRoVi",
     "password": "V7s?U#xQ9m!A"
   }
   ```
2. These are the default credentials created by the application
3. If changed, update the request body

### Error: "Token expired" (401)

**Problem:** Your access token has expired (typically after 24 hours).

**Solution:**
1. **Option A:** Run **Login** again to get a new token
2. **Option B:** Use **Refresh-Token.bru** to refresh your token

### Error: Connection refused

**Problem:** Application is not running.

**Solution:**
```bash
# Start PostgreSQL and Redis first
docker-compose up -d db redis
# OR
brew services start postgresql@15 redis

# Then start the application
./mvnw spring-boot:run

# Wait for: "Started PropertizeApplication in X.XXX seconds"
```

---

## 📋 Authentication Flow

### First Time Setup

```
1. Start Application
   ↓
2. Run Login Request (01-Authentication/Login.bru)
   ↓
3. Token automatically saved to environment
   ↓
4. All other requests now work automatically
```

### Daily Usage

```
1. Open Bruno
2. Run Login request (if token expired)
3. Use any other endpoint
```

### Token Refresh (Optional)

```
1. When token expires (after 24 hours)
2. Run Refresh-Token.bru
   OR
3. Simply login again
```

---

## 🔑 Available Authentication Requests

### 1. Login.bru ⭐ **START HERE**
- **Purpose:** Get access token
- **When to use:** First time, or after token expires
- **Auth required:** No
- **Saves:** `accessToken`, `refreshToken`

### 2. Refresh-Token.bru
- **Purpose:** Refresh expired access token
- **When to use:** When access token expires
- **Auth required:** Yes (uses refresh token)
- **Saves:** New `accessToken`

### 3. Get-Current-User.bru
- **Purpose:** Get authenticated user details
- **When to use:** To verify login status
- **Auth required:** Yes
- **Returns:** User profile

### 4. Logout.bru
- **Purpose:** Invalidate current session
- **When to use:** When ending session
- **Auth required:** Yes
- **Clears:** Server-side session

### 5. Change-Password.bru
- **Purpose:** Change user password
- **Auth required:** Yes

### 6. Forgot-Password.bru
- **Purpose:** Request password reset
- **Auth required:** No

### 7. Get-Permissions.bru
- **Purpose:** Get user permissions
- **Auth required:** Yes

---

## 🎯 Using Tokens in Other Requests

### Automatic (Recommended)

All requests in the collection are configured with:
```
auth: bearer
token: {{accessToken}}
```

This means they **automatically** use the saved token. You don't need to do anything!

### Manual (If needed)

If creating a new request, add this:

```javascript
auth:bearer {
  token: {{accessToken}}
}
```

---

## 📝 Common Workflows

### Workflow 1: Testing API Endpoints

```
1. Run Login.bru                    → Get token
2. Run Get-Current-User.bru        → Verify login
3. Run any other endpoint          → Should work!
```

### Workflow 2: Testing Different Roles

```
1. Login with OWNER credentials    → Full access
2. Test admin operations
3. Logout
4. Login with different role       → Test role restrictions
```

### Workflow 3: Token Refresh

```
1. Work with API (token valid 24h)
2. After 24h, get 401 error
3. Run Refresh-Token.bru          → Get new token
4. Continue working
```

---

## 🔐 Default Credentials

The application creates default users on startup:

### Owner/Admin Account
```json
{
  "username": "OWN-IqcRoVi",
  "password": "V7s?U#xQ9m!A"
}
```
**Roles:** OWNER, ADMIN  
**Access:** Full system access

### Additional Accounts
Check application logs on startup for other test accounts.

---

## 🐛 Debug Checklist

If authentication isn't working:

- [ ] Application is running (check: `curl http://localhost:8080/actuator/health`)
- [ ] Using correct environment (select **Local** in Bruno)
- [ ] Login request returns 200 OK
- [ ] Console shows "✅ Tokens saved successfully"
- [ ] Access token is visible in environment variables
- [ ] Other requests have `auth: bearer` configured
- [ ] Token hasn't expired (valid for 24 hours)

---

## 📊 Authentication Status Codes

| Status | Meaning | Action |
|--------|---------|--------|
| 200 | Success | ✅ Authenticated |
| 401 | Unauthorized | 🔄 Run Login.bru |
| 403 | Forbidden | ⚠️ Check user permissions |
| 404 | Not Found | ❌ Check endpoint URL |
| 500 | Server Error | 🐛 Check application logs |

---

## 🎓 Understanding the Auth System

### How It Works

1. **Login:** Send username/password → Get JWT tokens
2. **Token Storage:** Bruno saves tokens automatically
3. **Protected Requests:** Include token in `Authorization: Bearer <token>` header
4. **Validation:** Server validates token on each request
5. **Expiration:** Token expires after 24 hours → Login again

### Token Types

- **Access Token:** Used for API requests (expires in 24h)
- **Refresh Token:** Used to get new access token (expires in 7 days)

### Security Notes

- ✅ Tokens are stored securely in Bruno environment
- ✅ Never commit tokens to version control
- ✅ Tokens are user-specific
- ✅ Different roles have different permissions

---

## 🚀 Quick Reference

### Most Common Actions

**Get authenticated:**
```
01-Authentication/Login.bru → Send
```

**Check if authenticated:**
```
01-Authentication/Get-Current-User.bru → Send
```

**Refresh expired token:**
```
01-Authentication/Refresh-Token.bru → Send
```

**Test any endpoint:**
```
Just run it! Token is applied automatically.
```

---

## 📞 Still Having Issues?

### Application Not Starting
→ See: `APPLICATION_STARTUP_GUIDE.md`

### Bruno Collection Issues
→ See: `bruno-collection/MIGRATION_GUIDE.md`

### API Endpoint Issues
→ See: `bruno-collection/README.md`

### General Help
→ See: `PROJECT_INDEX.md`

---

**Status:** ✅ Authentication system working  
**Default Login:** Available in Login.bru  
**Token Validity:** 24 hours  
**Need Help:** Follow Quick Start section above
