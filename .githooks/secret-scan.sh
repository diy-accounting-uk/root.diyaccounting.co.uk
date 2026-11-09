#!/bin/sh
# SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
# Copyright (C) 2006-2026 DIY Accounting Limited
#
# secret-scan.sh — refuse a commit or a push that carries a secret.
#
#   secret-scan.sh staged           scan the staged changes (pre-commit)
#   secret-scan.sh push           scan the commits a push adds; reads the pre-push stdin lines
#
# Prints the rule and the path of each finding, never the value.
# A missing gitleaks prints one warning and lets the work through; the CI content scan is the backstop.
# Accepted findings are fingerprints in .gitleaksignore at the top of the working tree.

mode="${1:-staged}"

if ! command -v gitleaks >/dev/null 2>&1; then
  echo "secret-scan: gitleaks is not installed, so this check did not run (brew install gitleaks). CI scans the content." >&2
  exit 0
fi

top=$(git rev-parse --show-toplevel) || exit 0
cd "$top" || exit 0
report=$(mktemp "${TMPDIR:-/tmp}/secret-scan.XXXXXX") || exit 0
trap 'rm -f "$report"' EXIT

scan() {
  gitleaks git "$@" --redact --no-banner --log-level error \
    --gitleaks-ignore-path "$top" --report-format json --report-path "$report" >/dev/null 2>&1
}

show_findings() {
  # json report lines: "RuleID": "x", ... "File": "y"
  awk -F'"' '/"RuleID"/ {rule=$4} /"File"/ {print "  rule " rule "  path " $4}' "$report" | sort -u >&2
}

case "$mode" in
  staged)
    scan --staged
    rc=$?
    ;;
  push)
    rc=0
    zero=0000000000000000000000000000000000000000
    while read -r _local_ref local_sha _remote_ref remote_sha; do
      [ -z "$local_sha" ] && continue
      [ "$local_sha" = "$zero" ] && continue
      if [ -n "$remote_sha" ] && [ "$remote_sha" != "$zero" ] && git rev-parse --verify --quiet "${remote_sha}^{commit}" >/dev/null; then
        range="${remote_sha}..${local_sha}"
      else
        range="${local_sha} --not --remotes"
      fi
      scan --log-opts="$range"
      r=$?
      if [ "$r" -ne 0 ]; then
        rc=$r
        [ "$r" -eq 1 ] && show_findings
      fi
    done
    ;;
  *)
    echo "secret-scan: unknown mode $mode" >&2
    exit 0
    ;;
esac

if [ "$rc" -eq 1 ]; then
  [ "$mode" = staged ] && show_findings
  echo "secret-scan: refused, $mode changes hold a secret. Remove it; a reviewed false positive goes in .gitleaksignore as its fingerprint." >&2
  exit 1
fi
if [ "$rc" -ne 0 ]; then
  echo "secret-scan: gitleaks failed (exit $rc), so this check did not run. CI scans the content." >&2
fi
exit 0
