# Stripe API Key Rotation Runbook

**Last Updated:** May 8, 2026  
**Frequency:** Every 6 months (recommended) or immediately if key is compromised  
**Owner:** Security & Platform Teams  
**Approval:** CTO + Security Lead

---

## Overview

Stripe API keys are sensitive credentials that grant access to your entire Stripe account. This runbook documents the process for securely rotating (replacing) old keys with new ones without disrupting service.

**Key Rotation Policy:**

- ✅ Rotate API keys every 6 months (proactive security)
- ✅ Rotate immediately if key is compromised
- ✅ Maintain two valid keys during transition (old + new)
- ✅ Revoke old keys after successful new key deployment
- ✅ Document all rotations for audit trail

---

## Prerequisites

- **Access Required:**
  - Stripe Dashboard admin access (https://dashboard.stripe.com)
  - SSH access to production environment
  - AWS Secrets Manager access (or `.env` file management)
  - Deployment permissions (CircleCI, GitHub Actions, etc.)

- **Tools Needed:**
  - `curl` or `jq` for testing API calls
  - Deployment tool (Helm, Docker Compose, Kubernetes, etc.)
  - Communication tools (Slack, email) for notifications

- **Timing:**
  - Schedule during low-traffic period (off-peak hours)
  - Allocate 30-45 minutes for the full rotation
  - Notify payment team to monitor dashboards

---

## Step 1 — Generate New API Keys in Stripe

### 1.1 Access Stripe Dashboard

1. Log in to [Stripe Dashboard](https://dashboard.stripe.com)
2. Navigate to **Settings** → **API Keys**
3. Verify you are on the correct account (production vs. test)

### 1.2 Create New Restricted Keys (Recommended)

**Best Practice:** Use Stripe **Restricted Keys** instead of full API keys.

1. Click **Create New Key**
2. Choose **Restricted Key**
3. Set permissions:

   ```
   ✅ Grants:
   - Charges (read + create)
   - Payment Intents (read + create + update)
   - Refunds (read + create)
   - Payment Methods (read + create)
   - Customers (read + create + update)
   - Webhooks (read)

   ❌ Denies:
   - Subscriptions (if not used)
   - Disputes (read-only unless needed)
   - Account (any modifications)
   - API Keys (prevent key creation)
   ```

4. Assign a descriptive name:

   ```
   Format: propertize_stripe_[environment]_[date]
   Example: propertize_stripe_prod_2026_05
   ```

5. Click **Create Restricted Key**
6. Copy the new key (format: `rk_live_...` for live or `rk_test_...` for test)

### 1.3 Create New Webhook Signing Secret (if rotating)

Webhook signing secrets rotate independently from API keys:

1. Navigate to **Webhooks** in Stripe Dashboard
2. Click on your webhook endpoint (e.g., `https://api.propertize.co/webhooks/stripe`)
3. Click **Reveal signing secret**
4. Copy or regenerate if needed

---

## Step 2 — Store New Keys Securely

### 2.1 Store in AWS Secrets Manager (Recommended)

```bash
# Store in AWS Secrets Manager
aws secretsmanager create-secret \
  --name propertize/stripe/api-key-prod-2026-05 \
  --description "Stripe API key (production, May 2026)" \
  --secret-string "rk_live_abc123xyz789..."

# Verify the secret was created
aws secretsmanager get-secret-value \
  --secret-id propertize/stripe/api-key-prod-2026-05
```

### 2.2 Store in `.env` File (Temporary)

If using `.env` file management:

```bash
# Create new .env with new keys
cat > .env.new << EOF
STRIPE_API_KEY=rk_live_abc123xyz789...
STRIPE_WEBHOOK_SECRET=whsec_abc123xyz789...
EOF

# Verify format
cat .env.new
```

### 2.3 Document the Rotation

```bash
# Create audit entry
cat > rotation_audit_2026_05.log << EOF
Date: 2026-05-08 14:00 UTC
Action: Stripe API Key Rotation
Old Key: rk_live_xxxxxx...xxxx (last 6 chars)
New Key: rk_live_yyyyyy...yyyy (last 6 chars)
Webhook Secret Rotated: YES
Deployed By: [Your Name]
Verification: ✅ PASSED
EOF
```

---

## Step 3 — Deploy New Keys (Zero-Downtime)

### 3.1 Update Environment Variables

#### Option A: AWS Secrets Manager (Preferred)

```bash
# Update application to pull from new secret
aws secretsmanager update-secret \
  --secret-id propertize/stripe/api-key \
  --secret-string "rk_live_abc123xyz789..."

# Restart payment service (rolling restart, no downtime)
kubectl rollout restart deployment/payment-service

# Monitor rollout
kubectl rollout status deployment/payment-service
```

#### Option B: Kubernetes Secrets

```bash
# Update secret in cluster
kubectl delete secret stripe-api-key
kubectl create secret generic stripe-api-key \
  --from-literal=stripe-api-key=rk_live_abc123xyz789...

# Update deployment to reference new secret
kubectl patch deployment payment-service \
  -p '{"spec":{"template":{"metadata":{"annotations":{"force-rotate":"2026-05-08"}}}}}'

# Rolling restart
kubectl rollout restart deployment/payment-service
```

#### Option C: Direct `.env` Update (Docker Compose)

```bash
# Update .env file
sed -i 's/STRIPE_API_KEY=rk_live_.*/STRIPE_API_KEY=rk_live_abc123xyz789.../' .env

# Restart payment service (keep other services running)
docker-compose up -d payment-service

# Verify
docker logs payment-service | grep "Stripe"
```

### 3.2 Verify Service Starts Successfully

```bash
# Check logs for errors
docker logs payment-service | grep -i "stripe\|error" | head -20

# Or for Kubernetes
kubectl logs deployment/payment-service -f --all-containers=true

# Should see:
# ✅ "✓ Stripe API Key loaded successfully"
# ✅ "✓ Webhook secret configured"
# ❌ No "Invalid API key" errors
```

---

## Step 4 — Test Payment Operations

### 4.1 Create Test Payment

```bash
# Test payment intent creation (test mode)
curl -X POST "https://api.propertize.co/api/v1/stripe/payment-intents" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user-123" \
  -d '{
    "amount": 10.00,
    "currency": "usd",
    "paymentMethodId": "pm_test_chargeSuccess",
    "description": "Key rotation test"
  }'

# Expected response:
# {
#   "status": "SUCCESS",
#   "data": {
#     "stripePaymentIntentId": "pi_test_...",
#     "status": "succeeded"
#   }
# }
```

### 4.2 Monitor Webhook Events

```bash
# Check webhook logs in Stripe Dashboard
# Navigate to: Settings → Webhooks → [Your Endpoint] → Events

# Should see successful deliveries:
# - payment_intent.created
# - charge.succeeded
# - charge.updated

# Or use Stripe CLI (if available)
stripe listen --events payment_intent.succeeded
```

### 4.3 Run Integration Tests

```bash
# Run payment service tests
cd payment-service
./mvnw test -Dtest=StripePaymentServiceTest

# Expected: All tests pass with new key
# ✅ createPaymentIntent
# ✅ confirmPaymentIntent
# ✅ createRefund
```

---

## Step 5 — Monitor for Errors (24-48 hours)

### 5.1 Dashboard Monitoring

**Metrics to watch:**

| Metric                      | Threshold | Alert                       |
| --------------------------- | --------- | --------------------------- |
| Payment Intent Success Rate | < 95%     | 🔴 Alert if < 95%           |
| Payment Confirmation Errors | > 5/min   | 🟡 Warn if > 5, alert > 10  |
| Webhook Failures            | > 5/min   | 🔴 Alert immediately        |
| API Error Rate              | > 1%      | 🟡 Warn if > 1%, alert > 5% |

**Check via:**

- Stripe Dashboard → Payments → Success rate
- Stripe Dashboard → Webhooks → Recent events
- Application logs → payment-service → ERROR level

### 5.2 Log Monitoring

```bash
# Check for API key errors in logs
docker logs payment-service 2>&1 | grep -i "api.*key\|unauthorized\|invalid"

# Should see ZERO matches
# If matches found: key not configured correctly, rollback immediately

# Check webhook signature errors
docker logs payment-service 2>&1 | grep "SignatureVerificationException"

# Should see ZERO new errors
```

### 5.3 Stakeholder Notifications

Send updates to:

- 📧 Payment team (monitoring dashboards)
- 📧 Engineering lead (ready to rollback)
- 📊 Send daily report for 48 hours:

```
📋 Stripe Key Rotation Status Report — Day 1

✅ Metrics:
- Payment success rate: 99.7%
- Webhook success rate: 100%
- Errors: 0 API key related

✅ No customer impact observed

Next: Remove old key after 48h if all metrics stable
```

---

## Step 6 — Deactivate Old Key (After 48 Hours)

### 6.1 Verify New Key Is Stable

```bash
# Check that no services are still using old key
# Search logs for old key pattern (last 6 chars)
docker logs payment-service | grep "rk_live_xxxxxx"

# Should see: (0 matches)
```

### 6.2 Disable Old Key in Stripe Dashboard

1. Navigate to **Settings** → **API Keys**
2. Find the **old key** (e.g., `rk_live_xxxxxx...xxxx`)
3. Click **Roll key** or **Revoke** (do NOT delete, keep for audit)
4. Confirm revocation

**Screenshot:** [Settings → API Keys → [Old Key] → Roll]

### 6.3 Archive Old Key Reference

```bash
# Document the old key (masked) for audit trail
cat >> rotation_audit_2026_05.log << EOF

Deactivation Completed:
- Old Key Revoked: rk_live_xxxxxx...xxxx (2026-05-10 14:00 UTC)
- Archive Location: AWS Secrets Manager (propertize/stripe/old-keys/)
- Reason: Routine 6-month rotation
- Revoked By: [Your Name]
EOF

# Archive for records
aws s3 cp rotation_audit_2026_05.log s3://propertize-compliance-archive/stripe/
```

---

## Step 7 — Document Rotation Completion

### 7.1 Update Rotation Log

```bash
cat > STRIPE_KEY_ROTATION_LOG.md << EOF
# Stripe API Key Rotation Log

## Rotation 2026-05 (May 8, 2026)

| Field | Value |
|-------|-------|
| Date | 2026-05-08 14:00 UTC |
| Rotated By | John Doe (Security Team) |
| Environment | Production |
| Old Key | rk_live_xxxxxx...xxxx (Revoked 2026-05-10) |
| New Key | rk_live_yyyyyy...yyyy (Active) |
| Webhook Secret | Updated |
| Downtime | 0 minutes (rolling restart) |
| Tests Passed | ✅ All (payment, webhooks, refunds) |
| Customer Impact | None |
| Status | ✅ COMPLETE |

### Changes Made:
- [x] New restricted key created in Stripe
- [x] Key stored in AWS Secrets Manager
- [x] payment-service restarted with new key
- [x] Integration tests passed (24/24)
- [x] 48-hour monitoring completed (no errors)
- [x] Old key revoked
- [x] Audit log archived

### Next Rotation:
- **Scheduled:** November 8, 2026 (6 months)
- **Owner:** Security Team
EOF

# Commit to git
git add STRIPE_KEY_ROTATION_LOG.md
git commit -m "docs: record Stripe key rotation 2026-05"
git push
```

### 7.2 Update Key Rotation Calendar

```bash
# Add reminder for next rotation (6 months)
# Calendar: May 2026 → November 2026

echo "2026-11-08: Stripe API Key Rotation (6-month cycle)" >> COMPLIANCE_CALENDAR.md
```

---

## Rollback Procedure (If Issues Occur)

### Emergency: Revert to Old Key

If new key causes payment failures:

```bash
# 1. Immediately revert to old key
kubectl set env deployment/payment-service \
  STRIPE_API_KEY=rk_live_xxxxxx_OLD...

# 2. Force pod restart
kubectl rollout restart deployment/payment-service

# 3. Verify recovery
kubectl logs deployment/payment-service | grep "Stripe"

# 4. Document incident
cat > INCIDENT_2026_05.md << EOF
## Incident: Stripe Key Rotation Failed

Date: 2026-05-08 14:30 UTC
Severity: High
Duration: 5 minutes

Root Cause: [TBD]
Resolution: Reverted to old key; new key investigation pending

Action Items:
- [ ] Investigate why new key failed
- [ ] Retry rotation with different approach
- [ ] Post-mortem analysis
EOF
```

---

## Key Management Best Practices

| Practice                 | Implementation                                           |
| ------------------------ | -------------------------------------------------------- |
| **Restricted Keys**      | Use role-based API keys (not global secret key)          |
| **No Hardcoding**        | Keys always in environment variables / secrets manager   |
| **No Git Commits**       | Never commit API keys to version control                 |
| **Rotation Schedule**    | Rotate every 6 months (automatic reminders)              |
| **Access Control**       | Only platform/security team can rotate keys              |
| **Audit Trail**          | Document all rotations in git + compliance system        |
| **Immediate Revocation** | If key is exposed, revoke immediately (do not reuse)     |
| **Webhook Secrets**      | Rotate independently every 6 months                      |
| **Key Naming**           | Use descriptive names with dates (`stripe_prod_2026_05`) |

---

## Support & Questions

- **Stripe Support:** https://support.stripe.com
- **Stripe API Docs:** https://stripe.com/docs/keys
- **Internal Slack:** #payments-security
- **Contact:** [security-team@propertize.co](mailto:security-team@propertize.co)

---

**Last tested:** May 8, 2026  
**Next scheduled rotation:** November 8, 2026  
**Owner:** Security & Platform Teams
