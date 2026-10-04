#!/bin/bash
# SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
# Copyright (C) 2006-2026 DIY Accounting Limited
# Export Route53 hosted zone records to annotated files.
#
# Outputs:
#   root-zone/zone.json           — full zone export (all records, raw AWS format)
#   root-zone/zone.bind           — BIND-format zone file (human-readable)
#   root-zone/manual-records.json — records NOT managed by any CDK stack (email, legacy, etc.)
#
# Usage:
#   ./scripts/aws-accounts/export-root-zone.sh
#   AWS_PROFILE=diya-management ./scripts/aws-accounts/export-root-zone.sh

set -euo pipefail

PROFILE="${AWS_PROFILE:-diya-management}"
HOSTED_ZONE_ID="Z0315522208PWZSSBI9AL"
ZONE_NAME="diyaccounting.co.uk"
OUT_DIR="root-zone"

mkdir -p "$OUT_DIR"

echo "Exporting zone ${ZONE_NAME} (${HOSTED_ZONE_ID}) from profile ${PROFILE}..."

# 1. Full JSON export
aws --profile "$PROFILE" route53 list-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --output json > "${OUT_DIR}/zone.json"

TOTAL=$(python3 -c "import json; print(len(json.load(open('${OUT_DIR}/zone.json'))['ResourceRecordSets']))")
echo "Exported ${TOTAL} records to ${OUT_DIR}/zone.json"

# 2. Generate BIND-format zone file
python3 - "${OUT_DIR}/zone.json" "${OUT_DIR}/zone.bind" "$ZONE_NAME" <<'PYEOF'
import json, sys

zone_json_path, bind_path, zone_name = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(zone_json_path))
records = sorted(data["ResourceRecordSets"], key=lambda r: (r["Name"], r["Type"]))

with open(bind_path, "w") as f:
    f.write(f"; Zone file for {zone_name}\n")
    f.write(f"; Exported from Route53 hosted zone\n")
    f.write(f"; Records: {len(records)}\n")
    f.write(f";\n")
    f.write(f"$ORIGIN {zone_name}.\n\n")

    for r in records:
        name = r["Name"]
        rtype = r["Type"]

        # Make name relative to zone
        if name == f"{zone_name}.":
            display_name = "@"
        elif name.endswith(f".{zone_name}."):
            display_name = name[: -(len(zone_name) + 2)]
        else:
            display_name = name

        if "AliasTarget" in r:
            alias = r["AliasTarget"]
            f.write(f"; ALIAS (Route53-specific, not standard BIND)\n")
            f.write(f"{display_name:<60} {rtype:<6} ALIAS {alias['DNSName']}\n\n")
        elif "ResourceRecords" in r:
            ttl = r.get("TTL", 300)
            for rr in r["ResourceRecords"]:
                f.write(f"{display_name:<60} {ttl:<6} IN {rtype:<6} {rr['Value']}\n")
            f.write("\n")

print(f"Generated BIND-format zone file: {bind_path}")
PYEOF

# 3. Extract manually-managed records (not managed by CDK or ACM)
python3 - "${OUT_DIR}/zone.json" "${OUT_DIR}/manual-records.json" "$ZONE_NAME" <<'PYEOF'
import json, sys

zone_json_path, manual_path, zone_name = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(zone_json_path))

def categorize(name, rtype):
    if rtype == "MX":
        return "email (Google Workspace)"
    if "domainkey" in name:
        return "email (DKIM)"
    if rtype == "TXT" and name == zone_name:
        return "email (SPF) + domain verification"
    if rtype == "TXT":
        return "domain verification"
    if "webmail" in name:
        return "email (webmail redirect)"
    return "unknown — review manually"

manual = []
for r in data["ResourceRecordSets"]:
    name = r["Name"].rstrip(".")
    rtype = r["Type"]
    if rtype in ("NS", "SOA"):
        continue
    if rtype == "CNAME" and name.startswith("_"):
        continue
    if rtype in ("A", "AAAA") and "AliasTarget" in r:
        continue

    entry = {
        "name": name,
        "type": rtype,
        "owner": categorize(name, rtype),
    }
    if "TTL" in r:
        entry["ttl"] = r["TTL"]
    if "ResourceRecords" in r:
        entry["values"] = [rr["Value"] for rr in r["ResourceRecords"]]
    manual.append(entry)

with open(manual_path, "w") as f:
    json.dump(manual, f, indent=2)
    f.write("\n")

print(f"Found {len(manual)} manually-managed records → {manual_path}")
for m in manual:
    print(f"  {m['type']:<6} {m['name']:<50} ({m['owner']})")
PYEOF

echo ""
echo "Summary:"
echo "  ${OUT_DIR}/zone.json           — full zone (${TOTAL} records, raw AWS format)"
echo "  ${OUT_DIR}/zone.bind           — BIND-format zone file"
echo "  ${OUT_DIR}/manual-records.json  — records not managed by CDK or ACM"
