#!/usr/bin/env python3
import io
import time
from datetime import date, timedelta

import requests

BASE_URL = "http://localhost:8080"
MAILPIT_URL = "http://localhost:8025/api/v1/messages"


def unwrap(body):
    if isinstance(body, dict) and "data" in body:
        return body["data"]
    return body


def extract_status_and_lease(app_payload):
    if not isinstance(app_payload, dict):
        return None, None

    workflow = app_payload.get("workflow") or {}
    lease_document = app_payload.get("leaseDocument") or {}

    status = app_payload.get("status") or workflow.get("status")
    lease_id = app_payload.get("leaseId") or lease_document.get("leaseId")
    return status, lease_id


def wait_for_mail_total(session, baseline_total, timeout_seconds=6):
    deadline = time.time() + timeout_seconds
    latest_total = baseline_total

    while time.time() < deadline:
        latest_total = int(session.get(MAILPIT_URL, timeout=15).json().get("total", 0))
        if latest_total > baseline_total:
            return latest_total
        time.sleep(1)

    return latest_total


def main() -> int:
    session = requests.Session()
    session.headers.update({"Accept": "application/json"})

    login = session.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"usernameOrEmail": "admin", "password": "Admin@123"},
        timeout=25,
    ).json()

    token = login.get("accessToken") or login.get("token")
    if not token and isinstance(login.get("data"), dict):
        token = login["data"].get("accessToken") or login["data"].get("token")

    if not token:
        print("LOGIN_FAIL", login)
        return 1

    auth_headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}

    before_total = int(session.get(MAILPIT_URL, timeout=15).json().get("total", 0))

    uid = str(int(time.time()))[-6:]
    property_payload = {
        "propertyName": f"E2E Photo Patch {uid}",
        "type": "APARTMENT",
        "address": {
            "street": "55 Patch Ave",
            "city": "Jersey City",
            "state": "NJ",
            "postalCode": "07302",
            "country": "USA",
        },
        "bedrooms": 1,
        "bathrooms": 1,
        "squareFeet": 700,
        "monthlyRent": 2100,
        "securityDeposit": 2100,
        "applicationFee": 50,
    }

    created = session.post(
        f"{BASE_URL}/api/v1/properties",
        headers={**auth_headers, "Content-Type": "application/json"},
        json=property_payload,
        timeout=30,
    )
    created_body = unwrap(created.json())
    property_id = created_body.get("id") or (created_body.get("basicInfo") or {}).get("id")
    print("CREATE_PROPERTY", created.status_code, property_id)

    files = {"file": ("photo.txt", io.BytesIO(b"fake-image-content"), "text/plain")}
    data = {"entityType": "PROPERTY", "entityId": property_id, "description": "patch photo"}
    uploaded = session.post(
        f"{BASE_URL}/api/v1/documents/upload",
        headers={"Authorization": f"Bearer {token}"},
        files=files,
        data=data,
        timeout=35,
    )
    uploaded_body = uploaded.json()
    photo_url = uploaded_body.get("previewUrl") or uploaded_body.get("downloadUrl") or uploaded_body.get("storageUrl")
    print("UPLOAD_DOC", uploaded.status_code, bool(photo_url))

    patch_payload = {
        "photos": [
            {
                "url": photo_url,
                "caption": "patched",
                "order": 1,
                "isPrimary": True,
            }
        ]
    }
    patched = session.patch(
        f"{BASE_URL}/api/v1/properties/{property_id}",
        headers={**auth_headers, "Content-Type": "application/json"},
        json=patch_payload,
        timeout=25,
    )
    print("PATCH_PHOTO", patched.status_code)

    fetched_property = unwrap(
        session.get(
            f"{BASE_URL}/api/v1/properties/{property_id}",
            headers=auth_headers,
            timeout=20,
        ).json()
    )
    photo_urls = ((fetched_property.get("marketing") or {}).get("photoUrls") or []) if isinstance(fetched_property, dict) else []
    print("PHOTO_URLS_COUNT", len(photo_urls))

    application_payload = {
        "propertyId": property_id,
        "firstName": "Lease",
        "lastName": "Flow",
        "email": f"lease.flow.{uid}@example.com",
        "phone": "+12015550166",
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

    submitted = session.post(
        f"{BASE_URL}/api/v1/rental-applications/submit",
        headers={**auth_headers, "Content-Type": "application/json"},
        json=application_payload,
        timeout=35,
    )
    submitted_body = unwrap(submitted.json())
    application_id = submitted_body.get("id") or (submitted_body.get("workflow") or {}).get("id")
    print("SUBMIT_APP", submitted.status_code, application_id)

    after_submit_total = wait_for_mail_total(session, before_total)
    print("MAIL_DELTA_AFTER_SUBMIT", after_submit_total - before_total, before_total, after_submit_total)

    approved = session.post(
        f"{BASE_URL}/api/v1/rental-applications/{application_id}/approve",
        headers={**auth_headers, "Content-Type": "application/json"},
        json={"notes": "approve for lease pending"},
        timeout=35,
    )
    print("APPROVE", approved.status_code)

    tracked_after_approve = unwrap(
        session.get(
            f"{BASE_URL}/api/v1/rental-applications/{application_id}",
            headers=auth_headers,
            timeout=20,
        ).json()
    )
    status_after_approve, lease_id_after_approve = extract_status_and_lease(tracked_after_approve)
    print(
        "STATUS_AFTER_APPROVE",
        status_after_approve,
        lease_id_after_approve,
    )

    executed = session.post(
        f"{BASE_URL}/api/v1/rental-applications/{application_id}/lease-executed",
        headers={**auth_headers, "Content-Type": "application/json"},
        json={"notes": "lease signed"},
        timeout=45,
    )
    print("LEASE_EXECUTED_CALL", executed.status_code)

    tracked_after_exec = unwrap(
        session.get(
            f"{BASE_URL}/api/v1/rental-applications/{application_id}",
            headers=auth_headers,
            timeout=20,
        ).json()
    )
    status_after_exec, lease_id = extract_status_and_lease(tracked_after_exec)
    print(
        "STATUS_AFTER_EXEC",
        status_after_exec,
        lease_id,
    )

    if lease_id:
        lease_get = session.get(f"{BASE_URL}/api/v1/leases/{lease_id}", headers=auth_headers, timeout=20)
        print("GET_LEASE", lease_get.status_code)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
