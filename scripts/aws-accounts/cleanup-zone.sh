#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Copyright (C) 2025-2026 DIY Accounting Ltd
#
# scripts/aws-accounts/cleanup-zone.sh
#
# Identifies and deletes orphaned DNS records from the Route53 hosted zone.
# Cross-references ALIAS records against live CloudFront distributions in all
# AWS accounts to find records pointing to deleted distributions.
#
# Usage:
#   ./scripts/aws-accounts/cleanup-zone.sh [--dry-run] [--profile <management-profile>]
#
# Prerequisites:
#   - AWS CLI configured with SSO profiles for all accounts
#   - jq and python3 installed

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# --- Defaults ---
PROFILE="management"
HOSTED_ZONE_ID="Z0315522208PWZSSBI9AL"
DRY_RUN=false
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# AWS account profiles
ACCOUNT_PROFILES=("submit-ci" "submit-prod" "gateway" "spreadsheets" "management")

# --- Parse arguments ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 [--dry-run] [--profile <management-profile>]"
      echo ""
      echo "Identifies and deletes orphaned DNS records from the Route53 hosted zone."
      echo "Cross-references ALIAS records against live CloudFront distributions."
      echo ""
      echo "Options:"
      echo "  --dry-run              Show what would be deleted without making changes"
      echo "  --profile <profile>    AWS CLI profile for management account (default: management)"
      echo ""
      echo "What it does:"
      echo "  1. Queries CloudFront distributions from all AWS accounts"
      echo "  2. Fetches all Route53 records from the hosted zone"
      echo "  3. Identifies ALIAS records pointing to deleted CloudFront distributions"
      echo "  4. Identifies orphaned ACM validation CNAMEs"
      echo "  5. Presents deletion candidates for confirmation"
      echo "  6. Deletes in batches and re-exports the zone"
      exit 0
      ;;
    *)
      echo -e "${RED}ERROR: Unknown argument: $1${NC}"
      exit 1
      ;;
  esac
done

# --- Header ---
echo -e "${GREEN}=== Route53 Zone Cleanup ===${NC}"
echo "  Hosted zone:  ${HOSTED_ZONE_ID}"
echo "  Profile:      ${PROFILE}"
echo "  Dry run:      ${DRY_RUN}"
echo ""

# --- Verify management credentials ---
echo "Verifying management credentials..."
MGMT_ACCOUNT=$(aws sts get-caller-identity --profile "${PROFILE}" --query 'Account' --output text 2>/dev/null) || {
  echo -e "${RED}ERROR: Cannot authenticate with profile '${PROFILE}'${NC}"
  echo "  Run: aws sso login --sso-session diyaccounting"
  exit 1
}
echo -e "  Authenticated: ${GREEN}${MGMT_ACCOUNT}${NC}"
echo ""

# ============================================================================
# Step 1: Collect live CloudFront domains from all accounts
# ============================================================================
echo -e "${CYAN}Step 1: Collecting live CloudFront distributions${NC}"

LIVE_CF_FILE=$(mktemp)
LIVE_ALIASES_FILE=$(mktemp)
ZONE_FILE=$(mktemp)
trap 'rm -f "${LIVE_CF_FILE}" "${LIVE_ALIASES_FILE}" "${ZONE_FILE}"' EXIT

for ACCT_PROFILE in "${ACCOUNT_PROFILES[@]}"; do
  echo -n "  ${ACCT_PROFILE}... "

  # Check if profile works
  if ! aws sts get-caller-identity --profile "${ACCT_PROFILE}" &>/dev/null; then
    echo -e "${YELLOW}skipped (auth failed)${NC}"
    continue
  fi

  # Get CloudFront distributions
  DIST_JSON=$(aws --profile "${ACCT_PROFILE}" cloudfront list-distributions \
    --query 'DistributionList.Items[]' \
    --output json 2>/dev/null) || DIST_JSON="[]"

  if [[ "${DIST_JSON}" == "null" || "${DIST_JSON}" == "[]" ]]; then
    echo -e "${GREEN}0 distributions${NC}"
    continue
  fi

  # Extract CF domain names
  echo "${DIST_JSON}" | python3 -c "
import json, sys
dists = json.load(sys.stdin)
if not dists:
    sys.exit(0)
for d in dists:
    print(d['DomainName'])
" >> "${LIVE_CF_FILE}"

  # Extract alias domain names
  echo "${DIST_JSON}" | python3 -c "
import json, sys
dists = json.load(sys.stdin)
if not dists:
    sys.exit(0)
for d in dists:
    aliases = d.get('Aliases', {}).get('Items', [])
    for a in aliases:
        print(a)
" >> "${LIVE_ALIASES_FILE}"

  COUNT=$(echo "${DIST_JSON}" | python3 -c "import json,sys; print(len(json.load(sys.stdin)))")
  echo -e "${GREEN}${COUNT} distributions${NC}"
done

echo ""
echo "  Live CF domains: $(wc -l < "${LIVE_CF_FILE}" | tr -d ' ')"
echo "  Live aliases:    $(wc -l < "${LIVE_ALIASES_FILE}" | tr -d ' ')"
echo ""

# ============================================================================
# Step 2: Fetch current zone records
# ============================================================================
echo -e "${CYAN}Step 2: Fetching Route53 zone records${NC}"

aws --profile "${PROFILE}" route53 list-resource-record-sets \
  --hosted-zone-id "${HOSTED_ZONE_ID}" \
  --output json > "${ZONE_FILE}"

TOTAL_RECORDS=$(python3 -c "import json; print(len(json.load(open('${ZONE_FILE}'))['ResourceRecordSets']))")
echo "  Total records: ${TOTAL_RECORDS}"
echo ""

# ============================================================================
# Step 3: Identify orphaned records
# ============================================================================
echo -e "${CYAN}Step 3: Identifying orphaned records${NC}"

DELETION_JSON=$(python3 - "${ZONE_FILE}" "${LIVE_CF_FILE}" "${LIVE_ALIASES_FILE}" <<'PYEOF'
import json, sys

zone_file = sys.argv[1]
live_cf_file = sys.argv[2]
live_aliases_file = sys.argv[3]

zone_json = json.load(open(zone_file))
records = zone_json["ResourceRecordSets"]

# Load live CF domains and aliases
with open(live_cf_file) as f:
    live_cf_domains = set(line.strip().rstrip(".") for line in f if line.strip())

with open(live_aliases_file) as f:
    live_aliases = set(line.strip().rstrip(".") for line in f if line.strip())

ZONE = "diyaccounting.co.uk"

# Records that are always kept (infrastructure managed by deploy workflows or essential)
KEEP_NAMES = {
    # Apex and www (gateway)
    f"{ZONE}.",
    f"www.{ZONE}.",
    # Gateway
    f"ci-gateway.{ZONE}.",
    f"prod-gateway.{ZONE}.",
    # Spreadsheets
    f"ci-spreadsheets.{ZONE}.",
    f"prod-spreadsheets.{ZONE}.",
    f"spreadsheets.{ZONE}.",
    # Holding
    f"ci-holding.{ZONE}.",
    f"holding.{ZONE}.",
    # Simulator
    f"ci-simulator.{ZONE}.",
    f"prod-simulator.{ZONE}.",
    # Submit apex aliases (managed by deploy workflow)
    f"ci-submit.{ZONE}.",
    f"prod-submit.{ZONE}.",
    f"submit.{ZONE}.",
    f"ci.submit.{ZONE}.",
}

# Build a map of Name -> AliasTarget.DNSName for chain resolution
alias_map = {}
for r in records:
    if "AliasTarget" in r:
        name = r["Name"].rstrip(".")
        target = r["AliasTarget"]["DNSName"].rstrip(".")
        alias_map[name] = target

def resolve_chain(name, depth=0):
    """Follow alias chains to find the ultimate CF domain."""
    if depth > 5:
        return name
    target = alias_map.get(name)
    if target is None:
        return name
    if target.endswith(".cloudfront.net"):
        return target
    return resolve_chain(target, depth + 1)

def is_live_cf(cf_domain):
    """Check if a CloudFront domain is in the live set."""
    return cf_domain.rstrip(".") in live_cf_domains

def is_live_alias(domain):
    """Check if a domain is configured as a live CloudFront alias."""
    return domain.rstrip(".") in live_aliases

deletions = []
kept = []
skipped = []

for r in records:
    name = r["Name"]
    rtype = r["Type"]

    # Always keep NS, SOA
    if rtype in ("NS", "SOA"):
        skipped.append({"name": name, "type": rtype, "reason": "essential"})
        continue

    # Always keep MX, TXT (email, domain verification)
    if rtype in ("MX", "TXT"):
        skipped.append({"name": name, "type": rtype, "reason": "email/verification"})
        continue

    # Keep non-ALIAS CNAME records (webmail, ACM validation) — but flag orphaned ACM ones
    if rtype == "CNAME":
        record_name = name.rstrip(".")
        # Check for orphaned ACM validation CNAMEs
        if record_name.startswith("_"):
            # ACM validation records — check if they're for live domains
            # Extract the domain part after the hash prefix
            parts = record_name.split(".", 1)
            if len(parts) > 1:
                validated_domain = parts[1]
                # Orphaned if validating a domain that doesn't exist as infrastructure
                orphaned_acm_patterns = [
                    "stage.",            # no stage environment
                    "www.stage.",        # no stage environment
                    "dev.account.",      # no dev.account environment
                ]
                is_orphaned = False
                for pattern in orphaned_acm_patterns:
                    if validated_domain.startswith(pattern):
                        is_orphaned = True
                        break

                # Also check: ACM validation for ci-auth/prod-auth (no longer exist)
                if validated_domain.startswith("ci-auth.") and not validated_domain.startswith("ci-auth.submit."):
                    is_orphaned = True
                if validated_domain.startswith("prod-auth.") and not validated_domain.startswith("prod-auth.submit."):
                    is_orphaned = True

                if is_orphaned:
                    deletions.append({
                        "record": r,
                        "reason": f"orphaned ACM validation for {validated_domain}"
                    })
                    continue

        skipped.append({"name": name, "type": rtype, "reason": "CNAME (kept)"})
        continue

    # Handle ALIAS A/AAAA records
    if rtype in ("A", "AAAA") and "AliasTarget" in r:
        # Always keep infrastructure records
        if name in KEEP_NAMES:
            kept.append({"name": name, "type": rtype, "reason": "infrastructure"})
            continue

        # Check if this is a live alias on a CloudFront distribution
        domain = name.rstrip(".").rstrip(".")
        if is_live_alias(domain):
            kept.append({"name": name, "type": rtype, "reason": "live CF alias"})
            continue

        # Check if the alias target resolves to a live CF distribution
        target = r["AliasTarget"]["DNSName"].rstrip(".")
        if target.endswith(".cloudfront.net"):
            if is_live_cf(target):
                kept.append({"name": name, "type": rtype, "reason": f"live CF: {target}"})
                continue
            else:
                deletions.append({
                    "record": r,
                    "reason": f"dead CF: {target}"
                })
                continue

        # Target is another zone record — follow the chain
        ultimate = resolve_chain(domain)
        if ultimate.endswith(".cloudfront.net") and not is_live_cf(ultimate):
            deletions.append({
                "record": r,
                "reason": f"chain to dead CF: {ultimate}"
            })
            continue

        # If we can't determine, keep it
        kept.append({"name": name, "type": rtype, "reason": f"unknown target: {target}"})
        continue

    # Non-alias A/AAAA (shouldn't exist in this zone, but keep them)
    skipped.append({"name": name, "type": rtype, "reason": "non-alias"})

# Print summary
print(f"=== DELETION CANDIDATES: {len(deletions)} records ===", file=sys.stderr)
for d in deletions:
    r = d["record"]
    print(f"  DELETE {r['Type']:<6} {r['Name']:<60} ({d['reason']})", file=sys.stderr)

print(f"\n=== KEPT: {len(kept)} records ===", file=sys.stderr)
for k in kept:
    print(f"  KEEP   {k['type']:<6} {k['name']:<60} ({k['reason']})", file=sys.stderr)

print(f"\n=== SKIPPED: {len(skipped)} records ===", file=sys.stderr)
for s in skipped:
    print(f"  SKIP   {s['type']:<6} {s['name']:<60} ({s['reason']})", file=sys.stderr)

print(f"\nTotal: {len(deletions)} to delete, {len(kept)} to keep, {len(skipped)} skipped", file=sys.stderr)

# Output deletion records as JSON (for the bash script to process)
deletion_records = [d["record"] for d in deletions]
json.dump(deletion_records, sys.stdout, indent=2)
PYEOF
)

echo "${DELETION_JSON}" > /dev/null  # suppress for now, python printed to stderr

DELETION_COUNT=$(echo "${DELETION_JSON}" | python3 -c "import json,sys; print(len(json.load(sys.stdin)))")
echo ""
echo -e "  ${YELLOW}${DELETION_COUNT} records identified for deletion${NC}"
echo ""

if [[ "${DELETION_COUNT}" == "0" ]]; then
  echo -e "${GREEN}Zone is clean — nothing to delete${NC}"
  exit 0
fi

# ============================================================================
# Step 4: Confirm and delete
# ============================================================================
if [[ "${DRY_RUN}" == "true" ]]; then
  echo -e "${YELLOW}Dry run — no changes made${NC}"
  echo ""
  echo "To execute deletions, run without --dry-run:"
  echo "  $0 --profile ${PROFILE}"
  exit 0
fi

echo -e "${CYAN}Step 4: Delete orphaned records${NC}"
echo ""
echo -e "${RED}This will delete ${DELETION_COUNT} DNS records from the hosted zone.${NC}"
echo -n "Proceed? (yes/no): "
read -r CONFIRM
if [[ "${CONFIRM}" != "yes" ]]; then
  echo "Aborted."
  exit 1
fi

echo ""

# Build change batches (Route53 allows up to 1000 changes per batch)
# Each record deletion is one change
BATCH_SIZE=500

echo "${DELETION_JSON}" | python3 -c "
import json, sys

records = json.load(sys.stdin)
batch_size = ${BATCH_SIZE}
batches = []
current_batch = []

for r in records:
    change = {
        'Action': 'DELETE',
        'ResourceRecordSet': r
    }
    current_batch.append(change)
    if len(current_batch) >= batch_size:
        batches.append(current_batch)
        current_batch = []

if current_batch:
    batches.append(current_batch)

for i, batch in enumerate(batches):
    change_batch = {
        'Comment': f'Zone cleanup batch {i+1}/{len(batches)} - delete orphaned records',
        'Changes': batch
    }
    filename = f'/tmp/zone-cleanup-batch-{i+1}.json'
    with open(filename, 'w') as f:
        json.dump({'ChangeBatch': change_batch}, f, indent=2)
    print(f'{filename}:{len(batch)}')
"

# Execute each batch
BATCH_FILES=$(ls /tmp/zone-cleanup-batch-*.json 2>/dev/null || true)
BATCH_NUM=0
for BATCH_FILE in ${BATCH_FILES}; do
  BATCH_NUM=$((BATCH_NUM + 1))
  BATCH_COUNT=$(python3 -c "import json; print(len(json.load(open('${BATCH_FILE}'))['ChangeBatch']['Changes']))")
  echo -n "  Batch ${BATCH_NUM}: deleting ${BATCH_COUNT} records..."

  CHANGE_ID=$(aws --profile "${PROFILE}" route53 change-resource-record-sets \
    --hosted-zone-id "${HOSTED_ZONE_ID}" \
    --cli-input-json "file://${BATCH_FILE}" \
    --query 'ChangeInfo.Id' \
    --output text 2>&1) || {
    echo -e " ${RED}FAILED${NC}"
    echo "  Error: ${CHANGE_ID}"
    echo "  Batch file: ${BATCH_FILE}"
    exit 1
  }

  echo -e " ${GREEN}OK${NC} (${CHANGE_ID})"

  # Wait for change to propagate before next batch
  echo -n "  Waiting for propagation..."
  aws --profile "${PROFILE}" route53 wait resource-record-sets-changed \
    --id "${CHANGE_ID}" 2>/dev/null || true
  echo -e " ${GREEN}done${NC}"

  rm -f "${BATCH_FILE}"
done

echo ""
echo -e "${GREEN}=== Cleanup Complete ===${NC}"
echo ""

# ============================================================================
# Step 5: Re-export zone
# ============================================================================
echo -e "${CYAN}Step 5: Re-exporting zone${NC}"
if [[ -x "${SCRIPT_DIR}/export-root-zone.sh" ]]; then
  AWS_PROFILE="${PROFILE}" "${SCRIPT_DIR}/export-root-zone.sh"
else
  echo -e "  ${YELLOW}export-root-zone.sh not found — run manually${NC}"
fi

echo ""
echo "Verification:"
echo "  cat root-zone/zone.bind | head -5    # Check record count"
echo "  dig diyaccounting.co.uk              # Verify apex resolves"
echo "  dig submit.diyaccounting.co.uk       # Verify submit resolves"
