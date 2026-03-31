# Bruno API Testing - Quick Fix for 401 Errors

**Error:** `401 Unauthorized - Authentication Required`  
**Solution:** 2-minute fix

---

## 🚀 Quick Fix (2 Steps)

### Step 1: Start Application (if not running)

```bash
# Check if running
curl http://localhost:8080/actuator/health

# If you see "connection refused", start it:
cd /Users/ravishah/MySpace/MyWorkSpace/propertize

# Start dependencies (choose one method)
# Method A: Docker
docker-compose up -d db redis

# Method B: Local services
brew services start postgresql@15 redis

# Start application
./mvnw spring-boot:run

# Wait for: "Started PropertizeApplication"
```

### Step 2: Login in Bruno

1. Open **Bruno**
2. Select **"Local"** environment (top-right dropdown)
3. Open **01-Authentication → Login.bru**
4. Click **Send**
5. Wait for success message: ✅ "Tokens saved successfully"
6. Now try your other requests - they will work!

---

## ✅ That's It!

All your other requests will now work automatically. The token is used for all protected endpoints.

---

## 🔍 Verify It's Working

Test these in order:

1. ✅ **Login** → Should return tokens
2. ✅ **Get Current User** → Should return your user info
3. ✅ **Get All Properties** → Should return properties list

If all three work, you're good to go! 🎉

---

## 🐛 Still Getting 401?

### Check These:

1. **Environment Selected?**
   - Look at top-right in Bruno
   - Should say "Local"
   - If not, select it from dropdown

2. **Token Saved?**
   - After login, check Bruno console
   - Should see: ✅ "Tokens saved successfully"
   - If not, check the Login request script

3. **Application Running?**
   ```bash
   curl http://localhost:8080/actuator/health
   # Should return: {"status":"UP"}
   ```

4. **Correct Credentials?**
   - Login.bru uses: `OWN-IqcRoVi` / `V7s?U#xQ9m!A`
   - These are default credentials

---

## 📚 More Help

- **Full Auth Guide:** `bruno-collection/AUTHENTICATION_GUIDE.md`
- **Application Startup:** `APPLICATION_STARTUP_GUIDE.md`
- **Collection Updates:** `BRUNO_COLLECTION_UPDATE_REPORT.md`

---

**Most Common Issue:** Forgot to run Login request first!  
**Quick Fix:** Run 01-Authentication/Login.bru → Everything works!
