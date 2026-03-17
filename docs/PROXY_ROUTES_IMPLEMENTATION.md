# Next.js API Proxy Routes Implementation

## Summary

Implemented Next.js API proxy routes to solve authentication issues where client-side code couldn't access httpOnly session cookies. Server-side proxy routes can read session cookies and forward authenticated requests to the backend.

## Problem Solved

**Before**: Client-side JavaScript → Direct backend API calls → Missing Authorization header → 401 errors → "Body is disturbed or locked" errors

**After**: Client-side JavaScript → Next.js API route (server-side) → Read session cookie → Extract JWT → Forward to backend with Authorization header → Success

## Files Created

### 1. `/api/proxy/organizations/my/route.ts`

- **Purpose**: Get current user's organizations
- **Backend endpoint**: `GET /api/v1/organizations/my`
- **Authentication**: Reads NextAuth session cookie, extracts accessToken
- **Usage**: `organizationService.getMyOrganizations()`

### 2. `/api/proxy/admin/organizations/route.ts`

- **Purpose**: Get all organizations (platform admin)
- **Backend endpoint**: `GET /api/v1/admin/organizations`
- **Features**: Supports pagination, filtering, sorting via query params
- **Usage**: `organizationService.getAllOrganizations()`

### 3. `/api/proxy/admin/organizations/applications/route.ts`

- **Purpose**: Get organization onboarding applications
- **Backend endpoint**: `GET /api/v1/admin/organizations/applications`
- **Features**: Full query parameter support for filtering/pagination
- **Usage**: `onboardingService.getApplications()`

### 4. `/api/proxy/admin/stats/route.ts`

- **Purpose**: Get platform-wide statistics
- **Backend endpoint**: `GET /api/v1/admin/stats`
- **Returns**: Organization counts, user counts, system metrics
- **Usage**: `organizationService.getPlatformStats()`

## Files Modified

### 1. `organization.service.ts`

**Changes**:

- Added `tokensReady` flag and `waitForTokens()` method
- Updated `getMyOrganizations()` to use `/api/proxy/organizations/my`
- Updated `getAllOrganizations()` to use `/api/proxy/admin/organizations`
- Updated `getPlatformStats()` to use `/api/proxy/admin/stats`
- Added token readiness checks before API calls

**Impact**: All organization-related API calls now go through proxy routes

### 2. `onboarding.service.ts`

**Changes**:

- Updated `getApplications()` to use `/api/proxy/admin/organizations/applications`

**Impact**: Organization application listing now uses proxy route

### 3. `httpClient.ts`

**Changes**:

- Added `bodyReadError` flag to prevent double-read of response body
- Wrapped response body reading in try-catch to handle "Body is disturbed" errors gracefully
- Enhanced 401 error messages with context about missing tokens
- Added `hadToken` flag to error details for debugging

**Impact**: Better error handling prevents cascading failures

## Architecture Benefits

### ✅ Security

- httpOnly cookies can't be accessed by client JavaScript
- Tokens never exposed to client-side code
- Server-side validation of all authentication

### ✅ Reliability

- No race conditions waiting for localStorage sync
- Session cookies automatically sent by browser
- No timing issues with token availability

### ✅ Maintainability

- Centralized authentication logic in proxy routes
- Consistent error handling
- Easy to add new proxy endpoints following same pattern

### ✅ Performance

- No need to sync tokens to localStorage
- Browser handles cookie management automatically
- Reduced client-side complexity

## Proxy Route Pattern

```typescript
import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";

const API_BASE = process.env.API_URL || "http://api-gateway:8080";

export async function GET(request: NextRequest) {
  // 1. Get JWT from NextAuth session
  const token = await getToken({
    req: request,
    secret: process.env.AUTH_SECRET || process.env.NEXTAUTH_SECRET,
  });

  // 2. Check authentication
  if (!token?.accessToken) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // 3. Forward to backend with auth
  const response = await fetch(`${API_BASE}/api/v1/endpoint`, {
    headers: {
      Authorization: `Bearer ${token.accessToken}`,
      "Content-Type": "application/json",
    },
  });

  // 4. Return response
  const data = await response.json();
  return NextResponse.json(data, { status: response.status });
}
```

## Testing

### Before Deploying

```bash
# Build frontend with new changes
cd propertize-front-end
npm run build

# Rebuild Docker image
cd ..
docker-compose up -d --build propertize-frontend
```

### After Deploying

1. **Clear browser cache/cookies** or use incognito mode
2. Login at `http://localhost:3000/login`
3. Navigate to dashboard
4. **Expected**: No "Body is disturbed" errors in console
5. **Expected**: API calls succeed and dashboard loads with data
6. **Verify**: Check Network tab - calls go to `/api/proxy/*` not `/api/v1/*`

### Debug Commands

```bash
# Check frontend logs
docker logs propertize-frontend --tail 50

# Check if proxy routes are working
curl -H "Cookie: authjs.session-token=YOUR_TOKEN" \
  http://localhost:3000/api/proxy/organizations/my

# Check backend is reachable from frontend container
docker exec propertize-frontend wget -O- http://api-gateway:8080/actuator/health
```

## Future Enhancements

### Additional Proxy Routes Needed

- `/api/proxy/admin/organizations/[id]` - Individual organization details
- `/api/proxy/admin/organizations/[id]/activate` - Organization lifecycle
- `/api/proxy/admin/organizations/[id]/suspend`
- `/api/proxy/admin/users` - User management
- `/api/proxy/organizations/[id]` - Org-scoped operations

### POST/PUT/DELETE Support

Currently only GET methods implemented. Add other HTTP methods as needed:

```typescript
export async function POST(request: NextRequest) {
  const token = await getToken({ req: request });
  const body = await request.json();

  const response = await fetch(`${API_BASE}/api/v1/endpoint`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token.accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  return NextResponse.json(await response.json());
}
```

### Caching Strategy

Consider adding Redis caching for frequently accessed endpoints:

```typescript
import { redisCache } from '@/lib/redis-cache'

const cacheKey = `orgs:${userId}:list`
const data = await redisCache.swrGet(cacheKey, async () => {
  const response = await fetch(...)
  return await response.json()
})
```

## Rollback Plan

If issues arise:

1. **Revert service changes**:

   ```typescript
   // In organization.service.ts, change back to:
   const response = await this.httpClient.get<...>(
     API_ENDPOINTS.ORGANIZATIONS.MY  // Instead of /api/proxy/...
   )
   ```

2. **Rebuild and redeploy**:

   ```bash
   docker-compose up -d --build propertize-frontend
   ```

3. **Old authentication flow will resume** (localStorage tokens)

## Success Metrics

- ✅ Zero "Body is disturbed or locked" errors
- ✅ Zero 401 Unauthorized errors on page load
- ✅ Dashboard loads with organization data
- ✅ Platform stats display correctly
- ✅ No race conditions or timing issues
- ✅ Consistent authentication across all API calls
