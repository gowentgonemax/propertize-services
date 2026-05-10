# Stripe Payment Incident Response Plan (IRP)

**Last Updated:** May 8, 2026  
**Approval:** CTO, Security Lead, Legal  
**Review Cycle:** Annually + after each incident  
**Incident Classification:** Critical (customer data involved)

---

## Overview

This plan defines the process for responding to Stripe/payment-related security incidents:

- **Breaches** (card data exposed)
- **Fraud** (unauthorized charges)
- **Service outages** (payment processing down)
- **Compliance violations** (GDPR, PCI-DSS breaches)

**Key Principle:** Speed + transparency + customer protection

---

## Incident Severity Levels

| Severity        | Examples                                                              | Response Time | Approval               |
| --------------- | --------------------------------------------------------------------- | ------------- | ---------------------- |
| **🔴 Critical** | Card data exposed, large fraud, API key compromised                   | **15 min**    | CEO + Legal + Security |
| **🟠 High**     | Payment failures affecting 100+ customers, failed webhook retry storm | **1 hour**    | VP Eng + Security      |
| **🟡 Medium**   | Single customer fraud, minor payment delay, webhook failures < 5%     | **4 hours**   | Eng Lead + Support     |
| **🟢 Low**      | Typo in receipt, slow response (< 2s), informational only             | **24 hours**  | Team Lead              |

---

## Incident Response Team

### Roles & On-Call

| Role                        | Responsibilities                                 | On-Call         |
| --------------------------- | ------------------------------------------------ | --------------- |
| **Incident Commander** (IC) | Leads response, coordinates teams, decisions     | Primary on-call |
| **Security Lead**           | Investigates breach, forensics, compliance       | Secondary       |
| **Payment Lead**            | Monitors Stripe dashboard, payment flow          | Tertiary        |
| **Legal**                   | Compliance, customer notification, documentation | On-demand       |
| **Communications**          | Customer emails, status page, press (if needed)  | Primary         |

**On-call schedule:** [Link to PagerDuty / On-call Calendar]

---

## Phase 1 — Detection & Alerting (First 15 Minutes)

### 1.1 Automated Detection

**Monitoring triggers (should page immediately):**

```
🚨 Critical Alerts:
- Webhook failure rate > 20% for 5+ min
- Payment success rate < 80% for 10+ min
- 10+ invalid webhook signatures in 1 minute (tampering?)
- API key errors (401 Unauthorized) from payment-service
- DataRetentionTask fails (data cleanup broken)
- Stripe API timeout (connection issues)
```

**How to set up monitoring:**

```yaml
# Prometheus/AlertManager config
alert: StripeWebhookFailureRate
expr: rate(webhook_processing_error_total[5m]) > 0.2
for: 5m
severity: critical

alert: StripePaymentSuccessRateLow
expr: stripe_payment_success_rate < 0.8
for: 10m
severity: critical

alert: StripeInvalidSignatures
expr: increase(webhook_signature_invalid_total[1m]) > 10
for: 1m
severity: critical
```

### 1.2 Manual Detection

**Customer reports to support:**

- "My payment failed but I was charged twice"
- "I see fraudulent charges on my card"
- "Your app says payment failed but money is gone"

**Support SOP:**

1. Create ticket with `[PAYMENT_INCIDENT]` tag
2. Page Incident Commander immediately
3. Gather details: customer ID, transaction ID, timestamps, amounts
4. Do NOT make refund decisions yet

### 1.3 Incident Commander Decision

**Within 5 minutes:**

```
Q1: Is card data exposed or at risk?
   YES → Go to Phase 2 (BREACH)
   NO  → Continue

Q2: Are customers affected (failed charges)?
   YES → Go to Phase 3 (SERVICE DISRUPTION)
   NO  → Continue

Q3: Is the system still functioning?
   YES → Go to Phase 2 (INVESTIGATION)
   NO  → STOP everything. Go to Phase 3 (CRITICAL OUTAGE)
```

**Decision log entry:**

```
🔴 INCIDENT OPENED
Date: 2026-05-08 14:30 UTC
Type: [BREACH | SERVICE_DISRUPTION | OUTAGE]
Severity: [CRITICAL | HIGH | MEDIUM | LOW]
IC: John Doe
Status: INVESTIGATING
```

---

## Phase 2 — Breach Response (Card Data Exposed)

### 2.1 Containment (First 30 Minutes)

**Immediate actions (within 30 min):**

```bash
# 1. STOP all payment processing (go to fail-safe mode)
kubectl set env deployment/payment-service \
  PAYMENT_PROCESSING_ENABLED=false
# Result: All payment endpoints return 503 (Service Unavailable)

# 2. Revoke ALL active API keys immediately
# Navigate to Stripe Dashboard → Settings → API Keys
# Click "Roll" on ALL keys (this invalidates them)
# This prevents attacker from using leaked keys
# YES, this breaks payment service, but it stops damage

# 3. Enable emergency webhook endpoint lockdown
# Update webhook IP whitelist to only your office IPs
# (prevent attacker from triggering fake webhooks)

# 4. Collect evidence (DO NOT DELETE)
# Backup all logs from affected time period
aws s3 cp /var/log/payment-service/ \
  s3://propertize-incident-archive/2026-05-08/ --recursive

# 5. Document containment actions
cat > incident_log.txt << EOF
CONTAINMENT ACTIONS TAKEN:
- 14:30 UTC: Payment processing disabled
- 14:31 UTC: API keys revoked (3 keys)
- 14:32 UTC: Webhook IP whitelist enabled
- 14:33 UTC: Logs backed up to S3
- 14:34 UTC: Legal & Security notified
EOF
```

### 2.2 Forensics (30 Min - 2 Hours)

**Investigate root cause:**

```bash
# 1. Determine what data was exposed
# Check:
# - Payment records: customer names, last 4 digits, amounts
# - Payment methods: stored card tokens (NOT full cards, Stripe tokenization)
# - Webhook logs: any suspicious patterns?

# 2. When was it exposed?
# Check commit history for credential leaks:
git log --all --source --remotes -S "STRIPE_API_KEY=" | head -20

# Check AWS CloudTrail for unauthorized API calls:
aws cloudtrail lookup-events --lookup-attributes AttributeKey=EventName,AttributeValue=PutSecret --max-results 100

# 3. Who had access?
# List users with AWS/Stripe access during breach window:
aws iam get-user
aws iam list-users

# 4. Determine scope: How many customers affected?
SELECT COUNT(DISTINCT user_id) FROM payment WHERE created_at > '2026-05-08 14:00' AND created_at < '2026-05-08 14:30';
# Expected: number of customers affected
```

### 2.3 Notification — Internal (Immediately)

**Notify leadership (within 30 min):**

```
📧 TO: CEO, CTO, Legal Lead, Compliance Officer

SUBJECT: 🚨 INCIDENT: Potential Payment Data Breach - Immediate Action

CONTENT:
Severity: CRITICAL
Date: 2026-05-08 14:30 UTC
Type: Payment data exposure

SITUATION:
[Factual description - what happened, how we know]

ACTIONS TAKEN:
- All payment processing halted
- API keys revoked
- Evidence preserved for forensics
- Law enforcement notified (if necessary)

NEXT STEPS:
- Forensic investigation (2-4 hours)
- Customer notification (if data confirmed compromised)
- Regulatory notification (if required by law)

CONTACT: John Doe (IC) / legal@propertize.co
```

### 2.4 Law Enforcement & Regulatory Notification

**If legal obligation to notify authorities:**

```
🔴 NOTIFY WITHIN 24-72 HOURS (depending on jurisdiction):

✅ EU/UK:
- Data Protection Authority (DPA) → PII exposed
- Example: UK ICO (https://ico.org.uk/for-organisations/data-breaches/)
- Template: "We are notifying you of a personal data breach (GDPR Article 33)"

✅ US:
- FTC (if payment card data) → https://reportfraud.ftc.gov/
- State Attorney General → if > 250 residents affected
- FBI (if criminal: contact local FBI field office)

✅ Other:
- Stripe (notify your Stripe account team)
- Card networks (Visa, Mastercard, Amex) → through Stripe

TIMING:
- Internal notification (leadership): Immediately (< 1 hour)
- External notification (customers): ASAP, no more than 72 hours
- Regulatory notification: Within 24-72 hours (varies by law)
```

### 2.5 Customer Notification Letter

**Template (customize based on actual scope):**

```
Subject: Important Security Notice — Action Required

Dear [Customer Name],

We are writing to inform you of a potential security incident that may have
affected your payment information on Propertize.

WHAT HAPPENED:
On [DATE] at [TIME UTC], we discovered [SPECIFIC INCIDENT DESCRIPTION].

WHAT WE KNOW:
- Your [NAME, EMAIL, BILLING ADDRESS] may have been exposed
- NO full credit card numbers were at risk (we use Stripe Elements tokenization)
- We have revoked all API keys and halted payment processing immediately

WHAT WE'RE DOING:
- We are conducting a full forensic investigation
- We have notified law enforcement and regulators
- We are implementing additional security controls

WHAT YOU SHOULD DO:
1. Monitor your credit card statement for unauthorized charges
2. Contact your bank if you see suspicious activity
3. Consider credit freezes if highly concerned:
   - Equifax: https://www.equifaxsecurity2017.com/
   - Experian: https://www.experian.com/
   - TransUnion: https://www.transunion.com/

NEXT STEPS:
- We will provide updates via email every 48 hours
- Check https://propertize.co/security-incident for details
- Contact our security team: security@propertize.co

Sincerely,
[CEO/Security Lead Name]
Propertize
```

### 2.6 Post-Breach Investigation (Days 1-3)

**Timeline reconstruction:**

```sql
-- 1. Find when data was first accessed abnormally
SELECT * FROM payment
WHERE created_at < (SELECT exposed_date FROM incidents)
ORDER BY accessed_at DESC LIMIT 100;

-- 2. Identify which user/system accessed it
SELECT user_id, action, timestamp
FROM audit_log
WHERE resource = 'payment' AND action IN ('READ', 'EXPORT')
ORDER BY timestamp DESC;

-- 3. Determine if exfiltration occurred
-- Check for large data downloads, unusual API calls
SELECT * FROM api_call_log
WHERE service = 'payment-service'
AND timestamp BETWEEN '2026-05-08 14:00' AND '2026-05-08 15:00'
ORDER BY response_size DESC;
```

---

## Phase 3 — Service Disruption Response (Payments Failing)

### 3.1 Assessment (First 5 Minutes)

**Diagnose root cause:**

```bash
# 1. Check Stripe API status
curl -s https://status.stripe.com/api/v2/status.json | jq '.status.description'

# 2. Check payment-service logs
kubectl logs -f deployment/payment-service --tail=100 | grep -i error

# 3. Check webhook failures
kubectl logs deployment/payment-service | grep "WebhookValidation"

# 4. Check database connectivity
kubectl exec -it pod/payment-service -- \
  java -jar app.jar --shell "SELECT 1;" # quick DB test
```

### 3.2 Mitigation

**Temporary workarounds:**

| Issue                  | Mitigation                                         | Duration               |
| ---------------------- | -------------------------------------------------- | ---------------------- |
| **Stripe API is down** | Queue payments locally; retry when Stripe recovers | Auto (Stripe recovers) |
| **Webhook failures**   | Enable manual webhook retry in Stripe Dashboard    | Until fixed            |
| **API key expired**    | Rotate to new key (see Key Rotation Runbook)       | 30 min                 |
| **Database down**      | Failover to read replica; switch to backup DB      | 15 min                 |

**Implement circuit breaker:**

```java
// In StripePaymentService.java
@CircuitBreaker(name = "stripe-api", fallbackMethod = "paymentFallback")
public PaymentResponse createPayment(PaymentRequest req) {
    // ...call Stripe API...
}

public PaymentResponse paymentFallback(PaymentRequest req) {
    log.warn("Stripe API down; queuing payment for retry");
    paymentRetryQueue.add(req);
    return PaymentResponse.builder()
        .status(PaymentStatus.QUEUED)
        .message("Payment queued; will process when service recovers")
        .build();
}
```

### 3.3 Communication — Status Page

**Update status page (within 5 min):**

```
🔴 INCIDENT: Payment Processing Delayed

Status: Investigating
Severity: HIGH

We are experiencing delays in payment processing. Our team is investigating.
Customers may see "Payment processing..." messages lasting longer than usual.

Updates will be posted every 15 minutes.

Last updated: 2026-05-08 14:30 UTC
```

### 3.4 Customer Communication

**Email to affected customers (within 30 min):**

```
Subject: Payment Processing Temporarily Delayed — We're Working on It

Hi [Customer],

We are currently experiencing elevated latency with our payment processor
(Stripe). Your payment is queued and will be processed once the issue resolves.

ESTIMATED RESOLUTION: [TIME ESTIMATE]

No action is needed on your part. We will keep you updated.

Status: https://propertize.co/status
```

---

## Phase 4 — Recovery & Restoration

### 4.1 Service Restoration

**Steps to restore payment processing:**

```bash
# 1. Verify root cause is fixed
kubectl logs deployment/payment-service | grep "Stripe" | grep "SUCCESS" | tail -5

# 2. Re-enable payment processing
kubectl set env deployment/payment-service \
  PAYMENT_PROCESSING_ENABLED=true

# 3. Flush queued payments
# (DataRetentionTask or manual script)
./scripts/flush_payment_retry_queue.sh

# 4. Monitor recovery
watch -n 5 'kubectl top deployment/payment-service'

# 5. Verify no data loss
SELECT COUNT(*) FROM payment WHERE status = 'queued' AND created_at < '2026-05-08 14:00'
# Expected: 0 (all queued payments processed)
```

### 4.2 Verification

**Run validation tests:**

```bash
# 1. Create test payment
curl -X POST "https://api.propertize.co/api/v1/payments/test" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{"amount": 1.00, "currency": "usd"}' \
  -v

# Expected response: 200 OK, payment_intent created

# 2. Verify webhooks
curl -X GET "https://api.propertize.co/api/v1/webhooks/test" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected: Webhook delivered successfully

# 3. Check monitoring metrics
kubectl get --raw /metrics | grep stripe | head -20
# Should see normal rates (not 0)
```

### 4.3 Status Page Update

**Final update:**

```
✅ RESOLVED: Payment Processing Restored

Resolution: [Brief explanation]
Duration: [Time from start to resolution]

All payments have been processed successfully. No customer data was compromised.

Root cause: [Technical summary]

Preventive measures: [What we changed to prevent future]

Thank you for your patience!
```

---

## Phase 5 — Post-Incident Review

### 5.1 Timeline Documentation

**Create incident timeline (within 24 hours):**

```markdown
# Incident Postmortem: Payment Processing Outage (2026-05-08)

## Timeline

- 14:30 UTC: Customer reports payment failed
- 14:31 UTC: IC notified; payment service checked
- 14:32 UTC: Root cause identified (Stripe API timeout)
- 14:35 UTC: Circuit breaker activated; payments queued
- 14:45 UTC: Stripe recovered; manual flush of queue
- 15:00 UTC: All payments processed; service restored

## Duration: 30 minutes

## Impact: 47 payments delayed; 0 failed; $0 in chargebacks

## Root Cause: Stripe had an internal issue (their status page updated 1 hour later)

## Why We Didn't Detect Earlier:

- Our monitoring checked Stripe API response time but not error rate
- Action: Add error_rate monitoring (not just latency)

## What Worked Well:

- ✅ Circuit breaker prevented cascading failures
- ✅ Teams communicated quickly
- ✅ Recovery was automated

## What To Improve:

- ⚠️ Customer notification was manual (should be automated)
- ⚠️ Status page update was delayed (should have alert)
- ⚠️ No scheduled retry of failed webhooks

## Action Items:

- [ ] Implement automated status page updates
- [ ] Add error rate to monitoring alerting (not just latency)
- [ ] Implement automatic webhook retries
- [ ] Improve Stripe status page integration
```

### 5.2 Root Cause Analysis (RCA)

**5 Whys analysis:**

```
Q1: Why did payments fail?
A: Stripe API timed out (connection timeout > 30 sec)

Q2: Why did Stripe API time out?
A: Stripe had a load spike on their infrastructure

Q3: Why did we not detect this?
A: We monitored latency but not error_rate or timeout percentage

Q4: Why did we not have a fallback?
A: Circuit breaker only engaged AFTER errors started

Q5: Why did we not know sooner?
A: Stripe didn't update their status page immediately

→ ROOT CAUSE: Insufficient monitoring + insufficient fallback mechanism
```

### 5.3 Preventive Actions

**Actions to prevent recurrence:**

| Action                                          | Owner    | Timeline | Priority |
| ----------------------------------------------- | -------- | -------- | -------- |
| Add Stripe API error_rate alerting              | Platform | 1 week   | HIGH     |
| Implement local payment queuing with retry      | Platform | 2 weeks  | HIGH     |
| Integrate Stripe status page API into dashboard | DevOps   | 1 week   | MEDIUM   |
| Automated status page updates                   | Platform | 2 weeks  | MEDIUM   |
| Double timeout thresholds in payment flow       | Platform | 1 week   | LOW      |

### 5.4 Team Meeting & Lessons Learned

**Schedule postmortem meeting:**

```
Attendees: IC, Payment Lead, Engineering Lead, Communications
Time: 1 hour after incident resolved
Format: Blameless - focus on systems, not people

Agenda:
1. Timeline review (10 min)
2. Root cause analysis (15 min)
3. What went well (10 min)
4. What to improve (15 min)
5. Action items & owners (10 min)

Outcome: Written postmortem document shared with all stakeholders
```

---

## Checklist for Future Incidents

### 🔴 CRITICAL Incident (Card Data Breach)

- [ ] Incident Commander assigned immediately
- [ ] Payment processing halted
- [ ] All API keys revoked
- [ ] Evidence preserved to S3
- [ ] Legal team notified
- [ ] Forensic investigation started
- [ ] Customer notification drafted
- [ ] Regulatory notification queued
- [ ] Postmortem scheduled

### 🟠 HIGH Incident (Payment Failures)

- [ ] Root cause diagnosed
- [ ] Circuit breaker activated (if applicable)
- [ ] Status page updated
- [ ] Customer notification sent
- [ ] Monitoring alerts checked
- [ ] Logs archived
- [ ] Postmortem scheduled

### 🟡 MEDIUM Incident (Single Customer Fraud)

- [ ] Fraud flagged in Stripe Dashboard
- [ ] Customer contacted
- [ ] Refund issued (if applicable)
- [ ] Documentation updated

---

## Contacts & Escalation

### On-Call Escalation Path

```
Tier 1 (Payment Team Lead)
  ↓ (if no response in 5 min)
Tier 2 (VP Engineering)
  ↓ (if no response in 5 min)
Tier 3 (CTO)
  ↓ (if still critical)
Tier 4 (CEO + Legal)
```

### Key Contacts

| Role                         | Name        | Phone          | Email                                  |
| ---------------------------- | ----------- | -------------- | -------------------------------------- |
| Incident Commander (Primary) | John Doe    | +1-555-0100    | john@propertize.co                     |
| Security Lead                | Jane Smith  | +1-555-0101    | jane@propertize.co                     |
| Legal                        | Bob Johnson | +1-555-0102    | bob@propertize.co                      |
| Stripe Account Team          | N/A         | N/A            | [Stripe assigned account manager]      |
| AWS Support                  | N/A         | 1-844-AWS-SUPP | https://console.aws.amazon.com/support |

---

## Related Documentation

- **STRIPE_PRIVACY_POLICY_ADDENDUM.md** — GDPR/privacy compliance
- **PCI_DSS_COMPLIANCE_CHECKLIST.md** — Security requirements
- **STRIPE_KEY_ROTATION_RUNBOOK.md** — Key management SOP
- **Payment Service Architecture** — Code structure & design
- **Stripe API Documentation** — https://stripe.com/docs

---

**Last Updated:** May 8, 2026  
**Next Review:** May 8, 2027  
**Owner:** Security & Platform Teams

---

**In an emergency, contact: 🚨 [Incident Commander Phone]**
