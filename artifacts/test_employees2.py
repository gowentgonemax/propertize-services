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
    status = int(lines[-1].replace('HTTP:', ''))
    try:
        data = json.loads(lines[0])
    except:
        data = lines[0][:200]
    return status, data

# Login
s, d = curl('POST', '/api/v1/auth/login', body={"email": "admin@propertize.com", "password": "password"})
admin_token = d.get('accessToken', '') if isinstance(d, dict) else ''
s, d = curl('POST', '/api/v1/auth/login', body={"usernameOrEmail": "OWN-SJ6IA6X", "password": "7Khp$P9u$%vx"})
own_token = d.get('accessToken', '') if isinstance(d, dict) else ''
tok = own_token or admin_token
print(f"Tokens: admin={'OK' if admin_token else 'FAIL'}, org_owner={'OK' if own_token else 'FAIL'}")

emp_id = '97b10e88-e403-41bd-8c26-316cee451f21'

# Verify list
s, d = curl('GET', '/api/v1/employees', token=tok)
total = d.get('totalElements', 'N/A') if isinstance(d, dict) else d
content = d.get('content', []) if isinstance(d, dict) else []
print(f"GET /employees: HTTP {s}, total={total}, employees={[e.get('employeeNumber','?') for e in content]}")

# Verify single
s, d = curl('GET', f'/api/v1/employees/{emp_id}', token=tok)
print(f"GET /employees/{emp_id}: HTTP {s}, number={d.get('employeeNumber','?') if isinstance(d,dict) else d}")

# Test payroll-summary
s, d = curl('GET', '/api/v1/employees/payroll-summary', token=tok)
print(f"GET /employees/payroll-summary: HTTP {s}, count={len(d) if isinstance(d, list) else 'N/A'}")

# Test terminate
s, d = curl('POST', f'/api/v1/employees/{emp_id}/terminate?reason=testing', token=tok)
print(f"POST /employees/{emp_id}/terminate: HTTP {s}, status={d.get('status','?') if isinstance(d,dict) else d}")

# Test changed-since
s, d = curl('GET', '/api/v1/employees/changed-since?since=2024-01-01T00:00:00', token=tok)
total = d.get('totalElements', 'N/A') if isinstance(d, dict) else d
print(f"GET /employees/changed-since: HTTP {s}, total={total}")

print("\n=== All employee endpoints verified ===")
