# Comprehensive API Fix - "Body is disturbed or locked" Errors

## 🔴 Problem Summary

All dashboard API calls are failing with:

- **Frontend Error**: "Body is disturbed or locked"
- **Backend Error**: 401 Unauthorized
- **Root Cause**: Client-side code calling backend APIs without proper authentication

## 📊 Failing Endpoints

```
❌ /api/v1/organizations/my → 500 ("Body is disturbed or locked")
❌ /api/v1/admin/organizations/applications → 500 (multiple attempts)
❌ /api/v1/admin/stats → 500
❌ /api/v1/admin/organizations → 500
```

## 🔍 Root Cause Analysis

### Issue 1: "Body is disturbed or locked"

**Where**: `httpClient.ts` error handling  
**Problem**: Response body is being read ONCE for data, but error handling tries to use that data again  
**Why it happens**: JavaScript's `Response` object can only be read once. After calling `.json()`, the body stream is consumed.

### Issue 2: Backend Returns 401, Not 500

**Backend logs show**: NO 500 errors - only 401 Unauthorized  
**Frontend sees**: 500 errors because httpClient throws errors incorrectly  
**Problem**: Client-side API calls don't include Authorization header

### Issue 3: Authentication Token Not Sent

**Expected Flow**:

1. User logs in → NextAuth stores session in httpOnly cookie
2. Dashboard loads → Client JS needs to call backend APIs
3. httpClient should get token from session
4. httpClient adds `Authorization: Bearer {token}` header

**What's Actually Happening**:

1. User logs in ✅
2. Dashboard loads ✅
3. httpClient calls `TokenManager.getAccessToken()` ✅
4. TokenManager calls `getSession()` ❓ (timing issue?)
5. Session returns `null` or token not yet available ❌
6. API call goes out WITHOUT Authorization header ❌
7. Backend returns 401 ❌
8. httpClient tries to read error body → "Body is disturbed" ❌

## 🛠️ Solution Strategy

### Option A: Use Next.js API Routes (RECOMMENDED)

Create proxy routes that run server-side, can read httpOnly cookies, and forward to backend:

```typescript
// /api/proxy/organizations/my/route.ts
import { NextRequest } from "next/server";
import { getToken } from "next-auth/jwt";

export async function GET(request: NextRequest) {
  const token = await getToken({ req: request });

  if (!token?.accessToken) {
    return Response.json({ error: "Unauthorized" }, { status: 401 });
  }

  const res = await fetch(`${process.env.API_URL}/api/v1/organizations/my`, {
    headers: {
      Authorization: `Bearer ${token.accessToken}`,
    },
  });

  return Response.json(await res.json(), { status: res.status });
}
```

### Option B: Fix Token Sync Timing (QUICK FIX)

Add a layout component that syncs session tokens to localStorage BEFORE rendering dashboard:

```typescript
// In CommonDashboardLayout or similar
useEffect(() => {
  async function syncTokens() {
    const session = await getSession();
    if (session?.user?.accessToken) {
      localStorage.setItem("access_token", session.user.accessToken);
      localStorage.setItem("refresh_token", session.user.refreshToken || "");
      localStorage.setItem("session_id", session.user.sessionId || "");
    }
  }
  syncTokens();
}, []);
```

### Option C: Wait for Session Before API Calls

Add a hook that ensures session is loaded:

```typescript
export function useAuthenticatedAPI() {
  const { data: session, status } = useSession();
  const isReady = status === "authenticated" && !!session?.user?.accessToken;

  return { isReady, session };
}
```

## 🎯 Recommended Immediate Fix

**Step 1**: Fix "Body is disturbed" error in httpClient  
**Step 2**: Add token sync in dashboard layout  
**Step 3**: Add loading state while session initializes

## ⚡ Quick Diagnostic Command

```bash
# Check if backend is actually returning 500 or 401
docker logs propertize-main-service 2>&1 | tail -50 | grep "ERROR\|Exception\|401\|500"

# Check if session API works
curl http://localhost:3000/api/auth/session

# Check if backend endpoint works with auth
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/v1/organizations/my
```

## 📋 Next Steps

1. **IMMEDIATE**: Check browser console for session data
2. **FIX 1**: Add proper error handling to prevent "Body is disturbed"
3. **FIX 2**: Ensure tokens are synced before API calls
4. **FIX 3**: Add loading states to prevent premature API calls
5. **LONG-TERM**: Migrate to Next.js API routes for all backend calls

## 🔧 Files to Modify

1. `propertize-front-end/src/services/httpClient.ts` - Fix error handling
2. `propertize-front-end/src/components/layout/CommonDashboardLayout.tsx` - Add token sync
3. `propertize-front-end/src/services/organization.service.ts` - Add loading guards
4. `propertize-front-end/src/hooks/useOrganization.ts` - Wait for session

## ✅ Success Criteria

- [ ] No "Body is disturbed or locked" errors
- [ ] API calls include Authorization headers
- [ ] Backend returns data, not 401 errors
- [ ] Dashboard loads without errors
- [ ] All organization data displays correctly
