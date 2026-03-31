#!/usr/bin/env python3
import subprocess, json

BASE = "http://localhost:8080"

def curl(method, path, token=None, body=None):
    args = ['curl', '-s', '-w', '\nHTTP:%{http_code}', '-X', method, f'{BASE}{path}',
            '-H', 'Content-Type: application/json']
    if token:
        args += ['-H', f'Authorization: Bearer {token}']
    if body:
        args += ['-d', json.dumps(body)]
    r = subprocess.run(args, capture_output=True, text=True)
    lines = r.stdout.strip().split('\n')
    status = lines[-1].replace('HTTP:', '')
    try:
        body_data = json.loads(lines[0])
    except:
        body_data = lines[0][:200]
    return int(status), body_data

# Login
status, data = curl('POST', '/api/v1/auth/login', body={"email": "admin@propertize.com", "password": "password"})
admin_token = data.get('accessToken', '') if isinstance(data, dict) else ''
print(f"Admin login: {status}, token: {'OK' if admin_token else 'FAILED'}")

status, data = curl('POST', '/api/v1/auth/login', body={"usernameOrEmail": "OWN-SJ6IA6X", "password": "7Khp$P9u$%vx"})
own_token = data.get('accessToken', '') if isinstance(data, dict) else ''
print(f"Org Owner login: {status}, token: {'OK' if own_token else 'FAILED'}")

tok = own_token or admin_token

# Test all employee endpoints
print("\n--- Employee Endpoints ---")
endpoints = [
    ('GET', '/api/v1/employees'),
    ('GET', '/api/v1/employees/payroll-summary'),
    ('GET', '/api/v1/employees/by-user/1'),
    ('GET', '/api/v1/employees/changed-since?since=2024-01-01T00:00:00'),
]

for method, path in endpoints:
    s, d = curl(method, path, token=tok)
    if isinstance(d, dict):
        content = d.get('totalElements', d.get('content', d.get('error', str(d)[:100])))
    else:
        content = str(d)[:100]
    print(f"  {method} {path}: HTTP {s} | {content}")

# Try creating an employee
print("\n--- Create Employee ---")
emp_body = {
    "firstName": "Test",
    "lastName": "Employee",
    "email": "test.employee@propertize.com",
    "employmentType": "FULL_TIME",
    "hireDate": "2024-01-01"
}
s, d = curl('POST', '/api/v1/employees', token=tok, body=emp_body)
if isinstance(d, dict) and 'id' in d:
    emp_id = d['id']
    print(f"  POST /api/v1/employees: HTTP {s}, created id={emp_id}")
    
    s2, d2 = curl('GET', f'/api/v1/employees/{emp_id}', token=tok)
    print(f"  GET /api/v1/employees/{emp_id}: HTTP {s2} | {d2.get('employeeNumber','') if isinstance(d2,dict) else d2}")
    
    s3, d3 = curl('POST', f'/api/v1/employees/{emp_id}/activate', token=tok)
    print(f"  POST activate: HTTP {s3}")
else:
    print(f"  POST /api/v1/employees: HTTP {s} | {d}")
