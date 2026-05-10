# Privacy Policy Addendum — Stripe Payment Processing & GDPR Compliance

**Last Updated:** May 8, 2026  
**Version:** 1.0 (Comprehensive)

---

## 1. Payment Data Processing — Stripe Integration

### 1.1 What Payment Data We Collect

When you make a payment on Propertize, we collect:

- **Your name and email address** (for receipt and contact)
- **Billing address** (for payment verification and fraud prevention)
- **Payment amount and transaction date**
- **Payment method type** (e.g., "Visa ending in 4242")
- **Last four digits** of your card (display purposes only)
- **Card brand and expiration date** (month/year only)
- **Payment status** (succeeded, failed, etc.)

**What we do NOT collect or store:**

- ❌ Full credit card numbers
- ❌ CVV/security codes
- ❌ Card PIN or authentication data
- ❌ Unencrypted payment credentials

### 1.2 How We Handle Card Data Securely (Stripe Elements)

**Your card data NEVER touches our servers.** Here's how it works:

1. You enter your card details in a secure form on your browser
2. **Stripe Elements** (a PCI-DSS certified component) tokenizes the card in real-time
3. Only a secure token (like `pm_1A2b3C4d5E6f7G`) is sent to our servers
4. We store only the token and basic metadata (brand, last 4 digits)
5. All charges are processed through Stripe's API using the token

This means:

- ✅ We achieve **PCI-DSS Level 1** compliance (highest security)
- ✅ Your card data is encrypted end-to-end by Stripe
- ✅ We never see or store sensitive payment information

### 1.3 Stripe's Role as Payment Processor

Stripe is our payment service provider. Under GDPR, Stripe is a **Data Processor** on our behalf. Stripe:

- Processes payments securely using industry standards (TLS 1.2+)
- Stores card tokens and transaction history for compliance
- May collect additional data for fraud detection and regulatory reporting
- Does NOT use your data for marketing without consent
- Complies with GDPR, PCI-DSS, and international regulations

**Stripe's privacy policy:** https://stripe.com/en-us/privacy

### 1.4 Additional Data Stripe May Collect

In addition to payment data, Stripe may collect:

- **Device data** (IP address, browser type, device fingerprint) — for fraud prevention
- **Identity verification data** (government ID, business registration) — if requested for account verification
- **Biometric data** (facial recognition via Stripe Identity) — if you request premium verification
- **Transaction patterns and history** — for fraud detection via Stripe Radar
- **Geographic and behavioral signals** — to prevent unauthorized transactions

These are used for:

- Fraud detection and prevention
- Regulatory compliance and AML (Anti-Money Laundering)
- Identity verification
- Risk assessment
- Dispute resolution

---

## 2. Your Data Rights Under GDPR & UK-GDPR

As a resident of the EU/UK or any GDPR-protected jurisdiction, you have the following rights:

### 2.1 Right of Access (Article 15)

**You can request a copy of all your payment data.**

- Includes: payments, stored payment methods, transaction history, fraud flags
- Response time: Within 30 days
- **How to request:** [support email] or dashboard → Settings → Data Export

### 2.2 Right to Erasure ("Right to Be Forgotten") (Article 17)

**You can request permanent deletion of your payment data.**

- Stage 1: Soft-delete (your data is hidden, not immediately destroyed)
- Stage 2: Hard-delete (after 30 days, data is permanently removed from our database)
- Stripe payment methods are detached from your customer profile
- **Limitations:** Some data may be retained for legal/compliance reasons (see retention policy below)
- **How to request:** Dashboard → Settings → Delete My Payment Data

### 2.3 Right to Data Portability (Article 20)

**You can receive your data in a portable format.**

- We provide JSON and CSV exports of your payment history
- Compatible with other platforms (import to your accounting system, etc.)
- **How to request:** Dashboard → Settings → Export My Data

### 2.4 Right to Rectification (Article 16)

**You can correct inaccurate payment information.**

- Update billing address
- Correct name or email
- **How to request:** Dashboard → Settings → Update Billing Info

### 2.5 Right to Object (Article 21)

**You can object to certain processing activities.**

- Fraud prevention profiling (Stripe Radar)
- Marketing communications based on payment history
- **How to request:** [support email]

### 2.6 Right to Restrict Processing (Article 18)

**You can request temporary suspension of data processing.**

- While you dispute a transaction
- While you're evaluating a data rights request
- **How to request:** [support email]

---

## 3. Data Retention & Pseudonymization

### 3.1 How Long We Keep Your Payment Data

| Data Type                          | Retention Period                | Reason                                     |
| ---------------------------------- | ------------------------------- | ------------------------------------------ |
| **Completed payment records**      | 3 years                         | PCI-DSS requirement + tax/legal compliance |
| **Failed payment records**         | 2 years                         | Dispute resolution & fraud tracking        |
| **Payment methods (stored cards)** | Until deleted by you or 3 years | PCI-DSS; can be deleted anytime            |
| **Soft-deleted data**              | 30 days                         | Recovery window if deletion was accidental |
| **Card fingerprints**              | 7 years                         | Card deduplication per PCI-DSS             |
| **Audit logs**                     | 7 years                         | Regulatory and compliance requirements     |

**After retention expires:** Data is permanently deleted or pseudonymized (cannot be traced back to you).

### 3.2 Pseudonymization Process

When we delete your data, we may retain it in a pseudonymous form (without your name/email):

- ✅ Reduces fraud ring detection (helps protect other customers)
- ✅ Ensures compliance with payments law (GDPR Article 6(1)(c))
- ❌ Cannot be linked back to you without additional information
- ❌ Your privacy is protected even in pseudonymous form

---

## 4. Data Sharing & Third Parties

We share your payment data with:

| Organization                  | Purpose                               | Legal Basis                               |
| ----------------------------- | ------------------------------------- | ----------------------------------------- |
| **Stripe**                    | Payment processing                    | Contractual necessity (GDPR Art. 6(1)(b)) |
| **Stripe financial partners** | Fraud prevention, compliance          | Legitimate interest (GDPR Art. 6(1)(f))   |
| **Banks & payment networks**  | Settlement, chargeback handling       | Contractual necessity                     |
| **Tax authorities**           | Tax compliance, reporting             | Legal obligation (GDPR Art. 6(1)(c))      |
| **Law enforcement**           | Court orders, criminal investigations | Legal obligation (GDPR Art. 6(1)(c))      |
| **Accountants/auditors**      | Financial audits                      | Contractual necessity                     |
| **Your organization admins**  | Payment management                    | Contractual necessity                     |

**We do NOT:**

- Sell your data to marketers
- Share data with unrelated third parties
- Use data for profiling without consent
- Transfer data outside the EEA (except via Standard Contractual Clauses)

---

## 5. Cross-Border Data Transfers

Some of your payment data is processed by Stripe and third parties outside the EEA (European Economic Area). We ensure adequate protection through:

### 5.1 Standard Contractual Clauses (SCCs)

- Stripe includes SCCs in its Data Processing Agreement
- Ensures GDPR-equivalent protection even outside the EEA
- Covers transfers to the US, Canada, Australia, etc.

### 5.2 Data Privacy Framework (EU-US DPF)

- Stripe is certified under the EU-U.S. Data Privacy Framework
- Provides additional safeguards for EU → US data transfers
- Regularly audited by EU authorities

**Your data is protected even when transferred internationally.**

---

## 6. Security Measures

We protect your payment data through:

| Security Control          | Implementation                                           |
| ------------------------- | -------------------------------------------------------- |
| **HTTPS/TLS 1.2+**        | All data in transit encrypted                            |
| **End-to-end encryption** | Stripe Elements encrypts card data before transmission   |
| **Database encryption**   | Payment records encrypted at rest (AES-256)              |
| **Access control**        | Only authorized staff can access payment data            |
| **Firewalls**             | Network isolation; no direct internet access to database |
| **Non-root users**        | Services run with minimum required privileges            |
| **Rate limiting**         | 10 req/min on payment endpoints (prevents brute force)   |
| **Webhook validation**    | HMAC-SHA256 signature verification for all Stripe events |
| **Audit logging**         | All payment operations logged for compliance             |
| **No sensitive logging**  | Card numbers, CVVs never logged                          |
| **Regular backups**       | Encrypted backups; tested recovery procedures            |

---

## 7. Stripe Identity & Fraud Prevention

If you use **Stripe Identity** for verification:

- We may collect your **government-issued ID** (passport, driver's license)
- We may use **facial recognition** for identity matching
- Stripe processes this data per its [Stripe Identity Privacy Policy](https://stripe.com/en-gb/legal/stripe-identity)
- Data is encrypted and kept separate from payment data
- You can delete your ID data anytime (subject to legal holds)

If you use **Stripe Radar** for fraud prevention:

- Stripe analyzes your transaction patterns for suspicious activity
- May result in declined payments if fraud is suspected
- You can request manual review of declined transactions

---

## 8. Your Lawful Basis for Processing

We process your payment data based on:

| Processing Activity       | Lawful Basis                                     | GDPR Article |
| ------------------------- | ------------------------------------------------ | ------------ |
| **Process payment**       | Contractual necessity                            | Art. 6(1)(b) |
| **Fraud prevention**      | Legitimate interest (protect ourselves & others) | Art. 6(1)(f) |
| **Regulatory compliance** | Legal obligation                                 | Art. 6(1)(c) |
| **Data retention (tax)**  | Legal obligation                                 | Art. 6(1)(c) |
| **Dispute resolution**    | Contractual necessity                            | Art. 6(1)(b) |

---

## 9. Complaints & Data Protection Authority

If you believe your data rights have been violated:

1. **Contact us first:** [support email]
2. **File a complaint with your local Data Protection Authority:**
   - **EU/EEA:** https://edpb.ec.europa.eu/about-edpb/board/members_en
   - **UK:** https://ico.org.uk/make-a-complaint/
   - **US:** No formal DPA; contact your state's Attorney General
   - **Other countries:** Contact your local privacy regulator

---

## 10. Contact Information

- **Data Protection Officer:** [DPO email]
- **Privacy Team:** [support email]
- **Stripe's DPA:** Included in [Stripe DPA](https://stripe.com/en-us/legal/dpa)
- **Stripe Contact:** support@stripe.com

---

## 11. Updates to This Policy

We may update this policy as:

- Laws change
- New Stripe features are added
- Security improvements are made

**We'll notify you of material changes via email.**

---

**By using Propertize's payment features, you agree to this privacy policy and Stripe's terms.**
