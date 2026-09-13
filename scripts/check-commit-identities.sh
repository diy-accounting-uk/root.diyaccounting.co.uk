#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
# Copyright (C) 2006-2026 DIY Accounting Limited

# Fails when a commit on a pull request is authored by an email address that
# is not on the allow-list in .github/allowed-commit-identities.yml.
#
# This checks the commit AUTHOR only (git log --format='%ae'). It never reads
# the commit message body, so a `Co-Authored-By:` trailer never affects the
# result. Every Claude-authored commit in this repository carries a
# `Co-Authored-By: ... <noreply@anthropic.com>` trailer by convention — that
# trailer is not an author and must never be added to the allow-list or
# treated as a failure.
#
# Usage: check-commit-identities.sh <base-sha> <head-sha> [allowlist-file]

set -euo pipefail

base_sha="${1:?usage: check-commit-identities.sh <base-sha> <head-sha> [allowlist-file]}"
head_sha="${2:?usage: check-commit-identities.sh <base-sha> <head-sha> [allowlist-file]}"
allowlist_file="${3:-.github/allowed-commit-identities.yml}"

if [ ! -f "$allowlist_file" ]; then
  echo "missing allow-list file: $allowlist_file" >&2
  exit 1
fi

# The allow-list is a flat YAML list under one key, so a plain line match
# for "  - <value>" is enough and keeps this script free of a YAML parser.
allowed_emails="$(grep -E '^[[:space:]]*-[[:space:]]*[^[:space:]]' "$allowlist_file" | sed -E 's/^[[:space:]]*-[[:space:]]*//; s/[[:space:]]*#.*$//')"

if [ -z "$allowed_emails" ]; then
  echo "allow-list $allowlist_file parsed to zero entries — refusing to run with an empty allow-list" >&2
  exit 1
fi

commit_list="$(git log --format='%H%x09%ae%x09%an' "${base_sha}..${head_sha}")"
if [ -z "$commit_list" ]; then
  echo "git log ${base_sha}..${head_sha} returned no commits — cannot determine the pull request's commit range" >&2
  exit 1
fi

bad_commits=0
while IFS=$'\t' read -r sha email name; do
  [ -z "$sha" ] && continue
  if ! grep -qxF "$email" <<< "$allowed_emails"; then
    echo "::error::commit $sha is authored by '$name <$email>', which is not on the allow-list in $allowlist_file"
    bad_commits=$((bad_commits + 1))
  fi
done <<< "$commit_list"

if [ "$bad_commits" -ne 0 ]; then
  echo "$bad_commits commit(s) on this pull request are authored by an identity outside the allowed set." >&2
  echo "Add the address to $allowlist_file if it is a legitimate new committer, or fix the commit's author." >&2
  exit 1
fi

echo "every commit author on this pull request is on the allow-list"
