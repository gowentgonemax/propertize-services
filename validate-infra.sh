#!/bin/zsh
# Infrastructure & Services Full Validation Script

echo "============================================"
echo "  PROPERTIZE FULL STACK VALIDATION REPORT"
echo "  $(date)"
echo "============================================"

echo ""
echo "=== DOCKER INFRASTRUCTURE CONTAINERS ==="
docker ps --format "  {{.Names}}: {{.Status}} | {{.Ports}}" 2>&1

echo ""
echo "=== PORT CONNECTIVITY ==="
ports=(
  "5432:PostgreSQL"
  "6379:Redis"
  "27017:MongoDB"
  "9092:Kafka"
  "2181:Zookeeper"
  "8090:Kafka-UI"
  "8089:Mongo-Express"
  "8761:Service-Registry"
  "8081:Auth-Service"
  "8082:Propertize-Core"
  "8083:Employee-Service"
  "8080:API-Gateway"
  "3000:Frontend"
)
for item in "${ports[@]}"; do
  p="${item%%:*}"
  n="${item##*:}"
  nc -z localhost "$p" 2>/dev/null && echo "  ✅  $n (port $p)" || echo "  ❌  $n (port $p)"
done

echo ""
echo "=== SPRING BOOT HEALTH CHECKS ==="
for svc in "8761:Service-Registry" "8081:Auth-Service" "8082:Propertize-Core" "8083:Employee-Service" "8080:API-Gateway"; do
  sp="${svc%%:*}"
  sn="${svc##*:}"
  result=$(curl -s --max-time 5 "http://localhost:${sp}/actuator/health" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('status','?'))" 2>/dev/null)
  if [ "$result" = "UP" ]; then
    echo "  ✅  $sn — UP"
  else
    echo "  ❌  $sn — ${result:-UNREACHABLE}"
  fi
done

echo ""
echo "=== EUREKA REGISTERED SERVICES ==="
curl -s --max-time 5 http://localhost:8761/eureka/apps 2>/dev/null | python3 -c "
import sys, xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.stdin).getroot()
    apps = root.findall('.//application')
    print(f'  Total registered: {len(apps)}')
    for app in apps:
        name = app.find('name').text
        insts = app.findall('instance')
        inst = insts[0]
        ip = inst.find('ipAddr').text if inst.find('ipAddr') is not None else 'unknown'
        print(f'    ✅  {name} — {len(insts)} instance(s) @ {ip}')
except Exception as e:
    print(f'  Error: {e}')
" 2>/dev/null

echo ""
echo "=== REDIS ==="
r=$(docker exec propertize-redis redis-cli -a redis_secure_pass ping 2>/dev/null)
echo "  Ping response: $r"

echo ""
echo "=== MONGODB ==="
m=$(docker exec propertize-mongodb mongosh --eval "db.adminCommand('ping').ok" --quiet 2>/dev/null)
echo "  Ping response: $m (1 = healthy)"

echo ""
echo "=== KAFKA TOPICS ==="
docker exec propertize-kafka kafka-topics --bootstrap-server kafka:29092 --list 2>/dev/null | while read topic; do
  echo "  📢  $topic"
done

echo ""
echo "=== POSTGRESQL DATABASES ==="
psql -h localhost -U ravishah -tA -c "SELECT datname FROM pg_database WHERE datname NOT IN ('template0','template1','postgres') ORDER BY datname;" 2>/dev/null | while read db; do
  echo "  🗄   $db"
done

echo ""
echo "=== FRONTEND ==="
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 http://localhost:3000 2>/dev/null)
if [ "$code" = "200" ]; then
  echo "  ✅  Next.js Frontend — HTTP $code"
else
  echo "  ❌  Next.js Frontend — HTTP ${code:-UNREACHABLE}"
fi

echo ""
echo "============================================"
echo "  VALIDATION COMPLETE"
echo "============================================"

