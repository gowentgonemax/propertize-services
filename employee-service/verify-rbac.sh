#!/bin/bash

# RBAC Integration Verification Script for Employecraft

echo "========================================="
echo "RBAC Integration Verification"
echo "========================================="
echo ""

# Check if rbac.yml exists
echo "1. Checking RBAC configuration file..."
if [ -f "src/main/resources/rbac.yml" ]; then
    echo "   ✅ rbac.yml found"
    echo "   📊 File size: $(wc -l < src/main/resources/rbac.yml) lines"
else
    echo "   ❌ rbac.yml not found!"
    exit 1
fi

echo ""

# Check RBAC Java classes
echo "2. Checking RBAC Java classes..."
RBAC_CLASSES=(
    "src/main/java/com/employecraft/rbac/RbacConfig.java"
    "src/main/java/com/employecraft/rbac/RbacService.java"
    "src/main/java/com/employecraft/rbac/AuthorizeAspect.java"
    "src/main/java/com/employecraft/rbac/annotation/Authorize.java"
)

for class in "${RBAC_CLASSES[@]}"; do
    if [ -f "$class" ]; then
        echo "   ✅ $(basename $class)"
    else
        echo "   ❌ $(basename $class) not found!"
        exit 1
    fi
done

echo ""

# Check JWT filter enhancement
echo "3. Checking JWT Authentication Filter..."
if grep -q "RbacService" "src/main/java/com/employecraft/auth/JwtAuthenticationFilter.java"; then
    echo "   ✅ JwtAuthenticationFilter enhanced with RBAC"
else
    echo "   ❌ JwtAuthenticationFilter not enhanced!"
    exit 1
fi

echo ""

# Check for employee-specific roles in rbac.yml
echo "4. Checking employee-specific roles..."
EMPLOYEE_ROLES=("HR_MANAGER" "PAYROLL_MANAGER" "DEPARTMENT_MANAGER" "EMPLOYEE")

for role in "${EMPLOYEE_ROLES[@]}"; do
    if grep -q "$role:" "src/main/resources/rbac.yml"; then
        echo "   ✅ $role defined"
    else
        echo "   ❌ $role not found!"
        exit 1
    fi
done

echo ""

# Check documentation
echo "5. Checking documentation files..."
DOCS=(
    "RBAC_INTEGRATION_ANALYSIS.md"
    "RBAC_IMPLEMENTATION_GUIDE.md"
    "RBAC_INTEGRATION_SUMMARY.md"
)

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        echo "   ✅ $doc"
    else
        echo "   ❌ $doc not found!"
    fi
done

echo ""
echo "========================================="
echo "✅ RBAC Integration Verification Complete!"
echo "========================================="
echo ""
echo "Next Steps:"
echo "1. Add @Authorize annotations to your controllers"
echo "2. Test permission enforcement with different roles"
echo "3. Update frontend to request user permissions"
echo "4. Configure audit logging for permission checks"
echo ""
echo "Documentation:"
echo "- Analysis: RBAC_INTEGRATION_ANALYSIS.md"
echo "- Guide: RBAC_IMPLEMENTATION_GUIDE.md"
echo "- Summary: RBAC_INTEGRATION_SUMMARY.md"
echo ""
