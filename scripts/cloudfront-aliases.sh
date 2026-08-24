#!/bin/bash
# SPDX-License-Identifier: AGPL-3.0-only
# Copyright (C) 2025-2026 DIY Accounting Ltd
#
# Add or remove alternate domain names on a CloudFront distribution and wait for the change to
# deploy. Used by deploy-holding.yml to move the live aliases between the gateway distribution and
# the apex holding distribution.
#
# Usage: ./scripts/cloudfront-aliases.sh <add|remove> <distribution-id> <domain> [<domain>...]
#
# CloudFront refuses an alias that the distribution's certificate does not cover, and refuses an
# alias already claimed by another distribution, so the caller must remove before it adds.

set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "usage: $0 <add|remove> <distribution-id> <domain> [<domain>...]" >&2
  exit 1
fi

ACTION="$1"
DISTRIBUTION_ID="$2"
shift 2

case "$ACTION" in
  add | remove) ;;
  *)
    echo "ERROR: action must be 'add' or 'remove', got [${ACTION}]" >&2
    exit 1
    ;;
esac

WORK=$(mktemp -d)
trap 'rm -rf "${WORK}"' EXIT

# One call for both the config and its ETag, so an update cannot race a separate read.
aws cloudfront get-distribution-config --id "${DISTRIBUTION_ID}" --output json >"${WORK}/current.json"
ETAG=$(jq -r '.ETag' "${WORK}/current.json")
jq '.DistributionConfig' "${WORK}/current.json" >"${WORK}/config.json"

DOMAINS=$(printf '%s\n' "$@" | jq -R . | jq -s .)

if [ "${ACTION}" = "add" ]; then
  jq --argjson domains "${DOMAINS}" \
    '.Aliases.Items = ((.Aliases.Items // []) + $domains | unique) | .Aliases.Quantity = (.Aliases.Items | length)' \
    "${WORK}/config.json" >"${WORK}/updated.json"
else
  jq --argjson domains "${DOMAINS}" \
    '.Aliases.Items = ((.Aliases.Items // []) - $domains) | .Aliases.Quantity = (.Aliases.Items | length)' \
    "${WORK}/config.json" >"${WORK}/updated.json"
fi

echo "Distribution [${DISTRIBUTION_ID}] aliases before: $(jq -c '.Aliases.Items' "${WORK}/config.json")"
echo "Distribution [${DISTRIBUTION_ID}] aliases after:  $(jq -c '.Aliases.Items' "${WORK}/updated.json")"

if jq -e --slurpfile before "${WORK}/config.json" '.Aliases.Items == $before[0].Aliases.Items' \
  "${WORK}/updated.json" >/dev/null; then
  echo "Aliases already as required; leaving distribution [${DISTRIBUTION_ID}] untouched."
  exit 0
fi

aws cloudfront update-distribution \
  --id "${DISTRIBUTION_ID}" \
  --if-match "${ETAG}" \
  --distribution-config "file://${WORK}/updated.json" >/dev/null

echo "Waiting for distribution [${DISTRIBUTION_ID}] to deploy..."
aws cloudfront wait distribution-deployed --id "${DISTRIBUTION_ID}"
echo "Distribution [${DISTRIBUTION_ID}] deployed with aliases: $(jq -c '.Aliases.Items' "${WORK}/updated.json")"
