#!/usr/bin/env python3
import io
import json
import time
from datetime import date, timedelta

import requests

BASE_URL = "http://localhost:8080"
MAILPIT_URL = "http://localhost:8025/api/v1/messages"
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "Admin@123"


def parse_json(resp):
    try:
        return resp.json()
    except Exception:
        return {"raw": resp.text[:300]}


def extract_data(body):
    if isinstance(body, dict) and "data" in body:
        return body["data"]
    return body


def log(step, ok, detail=""):
    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {step}" + (f" — {detail}" if detail else ""))


def main():
    session = requests.Session()
    session.headers.update({"Accept": "application/json"})

    # 1) Login
    login_resp = session.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"usernameOrEmail": ADMIN_USERNAME, "password": ADMIN_PASSWORD},
        timeout=25,
    )
    login_body = parse_json(login_resp)
    token = login_body.get("accessToken") or login_body.get("token")
    if not token and isinstance(login_body.get("data"), dict):
        token = login_body["data"].get("accessToken") or login_body["data"].get("token")

    if not token:
        log("Admin login", False, f"HTTP {login_resp.status_code} {str(login_body)[:120]}")
        return 1
    log("Admin login", True)

    auth_headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}

    # 2) Mailpit before
    m_before = session.get(MAILPIT_URL, timeout=15)
    m_before_body = parse_json(m_before)
    total_before = int(m_before_body.get("total", 0))
    log("Mailpit reachable", m_before.status_code == 200, f"total={total_before}")

    # 3) Create property
    uid = str(int(time.time()))[-6:]
    property_payload = {
        "propertyName": f"E2E Upload Property {uid}",
        "type": "APARTMENT",
        "address": {
            "street": "123 Upload Lane",
            "city": "Jersey City",
            "state": "NJ",
            "postalCode": "07302",
            "country": "USA",
        },
        "bedrooms": 1,
        "bathrooms": 1,
        "squareFeet": 700,
        "monthlyRent": 2100.00,
        "securityDeposit": 2100.00,
        "applicationFee": 50.00,
        "furnishedStatus": False,
        "utilitiesIncluded": True,
        "hasParking": True,
        "hasLaundry": True,
    }
    create_prop = session.post(
        f"{BASE_URL}/api/v1/properties",
        headers={**auth_headers, "Content-Type": "application/json"},
        json=property_payload,
        timeout=30,
    )
    create_prop_body = parse_json(create_prop)
    prop_data = extract_data(create_prop_body) if create_prop.status_code in (200, 201) else {}
    property_id = None
    if isinstance(prop_data, dict):
        property_id = prop_data.get("id") or prop_data.get("propertyId") or (prop_data.get("basicInfo") or {}).get("id")
    log("Create property", create_prop.status_code in (200, 201), f"id={property_id}")
    if not property_id:
        print(json.dumps(create_prop_body)[:400])
        return 1

    # 4) Document upload
    files = {"file": ("e2e-doc.txt", io.BytesIO(b"e2e doc upload test"), "text/plain")}
    data = {"entityType": "PROPERTY", "entityId": property_id, "description": "e2e upload"}
    doc_resp = session.post(
        f"{BASE_URL}/api/v1/documents/upload",
        headers={"Authorization": f"Bearer {token}"},
        files=files,
        data=data,
        timeout=35,
    )
    doc_body = parse_json(doc_resp)
    photo_url = doc_body.get("previewUrl") or doc_body.get("downloadUrl") or doc_body.get("storageUrl")
    log("Upload document", doc_resp.status_code in (200, 201), f"url_found={bool(photo_url)}")
    if doc_resp.status_code not in (200, 201):
        print(json.dumps(doc_body)[:400])
        return 1

    # 5) Property picture add (persist via property photos field)
    if photo_url:
        patch_payload = {
            "photos": [
                {
                    "url": photo_url,
                    "caption": "e2e uploaded photo",
                    "order": 1,
                    "isPrimary": True,
                }
            ]
        }
        patch_resp = session.patch(
            f"{BASE_URL}/api/v1/properties/{property_id}",
            headers={**auth_headers, "Content-Type": "application/json"},
            json=patch_payload,
            timeout=25,
        )
        patch_body = parse_json(patch_resp)
        log("Patch property with photo", patch_resp.status_code in (200, 201), f"http={patch_resp.status_code}")

        get_prop = session.get(f"{BASE_URL}/api/v1/properties/{property_id}", headers=auth_headers, timeout=20)
        get_prop_body = parse_json(get_prop)
        get_prop_data = extract_data(get_prop_body)
        photos = None
        if isinstance(get_prop_data, dict):
            photos = get_prop_data.get("photos") or (get_prop_data.get("mediaInfo") or {}).get("photos")
        log("Verify property photo persisted", bool(photos), f"count={len(photos) if isinstance(photos, list) else 0}")

    # 6) Submit + approve rental application to test mail
    app_payload = {
        "propertyId": property_id,
        "firstName": "Mail",
        "lastName": "Probe",
        "email": f"mail.probe.{uid}@example.com",
        "phone": "+12015550111",
        "dateOfBirth": "1990-05-01",
        "desiredMoveInDate": (date.today() + timedelta(days=21)).isoformat(),
        "numberOfOccupants": 1,
        "creditScore": 710,
        "currentAddress": {
            "street": "1 Main",
            "city": "Newark",
            "state": "NJ",
            "postalCode": "07101",
            "country": "USA",
            "addressType": "CURRENT",
        },
        "employmentInfo": {
            "employmentStatus": "FULL_TIME",
            "employerName": "Acme",
            "jobTitle": "Engineer",
            "monthlyIncome": 6500,
            "employmentStartDate": "2020-01-01",
        },
        "emergencyContact": {
            "name": "Emergency Contact",
            "phone": "+12015550112",
            "email": "emergency@example.com",
            "relationship": "SIBLING",
        },
        "bankruptcyHistory": False,
        "evictionHistory": False,
    }

    app_resp = session.post(
        f"{BASE_URL}/api/v1/rental-applications/submit",
        headers={**auth_headers, "Content-Type": "application/json"},
        json=app_payload,
        timeout=35,
    )
    app_body = parse_json(app_resp)
    app_data = extract_data(app_body)
    application_id = None
    if isinstance(app_data, dict):
        application_id = app_data.get("id") or (app_data.get("workflow") or {}).get("id")
    log("Submit rental application", app_resp.status_code in (200, 201), f"id={application_id}")

    if application_id:
        bg_resp = session.post(
            f"{BASE_URL}/api/v1/rental-applications/{application_id}/background-check/not-required",
            headers={**auth_headers, "Content-Type": "application/json"},
            json={"reason": "E2E", "comments": "mail check"},
            timeout=20,
        )
        log("Waive background check", bg_resp.status_code in (200, 201, 204), f"http={bg_resp.status_code}")

        approve_resp = session.post(
            f"{BASE_URL}/api/v1/rental-applications/{application_id}/approve",
            headers={**auth_headers, "Content-Type": "application/json"},
            json={"notes": "E2E approval", "createTenant": True, "leaseDurationMonths": 12},
            timeout=35,
        )
        log("Approve rental application", approve_resp.status_code in (200, 201), f"http={approve_resp.status_code}")

    m_after = session.get(MAILPIT_URL, timeout=15)
    m_after_body = parse_json(m_after)
    total_after = int(m_after_body.get("total", 0))
    delta = total_after - total_before
    log("Mailpit post-approval delta", delta > 0, f"before={total_before}, after={total_after}, delta={delta}")

    if isinstance(m_after_body.get("messages"), list):
        for msg in m_after_body["messages"][:3]:
            print("MAIL:", msg.get("Subject"), msg.get("To"))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
