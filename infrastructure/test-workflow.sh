#!/bin/bash

# Test workflow logic locally before pushing
# Usage: cd infrastructure && ./test-workflow.sh

set -e

cd "$(dirname "$0")"

echo "🧪 Testing GitOps Workflow Logic"
echo ""

# Test 1: Read regions from cdk.json
echo "Test 1: Reading regions from cdk.json"
REGIONS=$(jq -c '.context.regions' cdk.json)
echo "✅ Regions: $REGIONS"
echo ""

# Test 2: Parse regions as array
echo "Test 2: Parsing regions array"
for REGION in $(echo "$REGIONS" | jq -r '.[]'); do
  echo "  - $REGION"
done
echo ""

# Test 3: Check if regions is valid JSON
echo "Test 3: Validating JSON"
if echo "$REGIONS" | jq empty 2>/dev/null; then
  echo "✅ Valid JSON"
else
  echo "❌ Invalid JSON"
  exit 1
fi
echo ""

# Test 4: Simulate matrix strategy
echo "Test 4: Simulating GitHub Actions matrix"
echo "Matrix would create jobs for:"
for REGION in $(echo "$REGIONS" | jq -r '.[]'); do
  REGION_SHORT=$(echo "$REGION" | tr -d '-')
  ENV_NAME="minisocial-backend-cdk-${REGION_SHORT}"
  echo "  - Region: $REGION"
  echo "    Environment: $ENV_NAME"
  echo ""
done

echo "✅ All tests passed!"
echo ""
echo "Safe to push! The workflow will:"
echo "1. Read regions: $REGIONS"
echo "2. Deploy to each region in sequence"
echo "3. Update Route 53 with latency routing"
