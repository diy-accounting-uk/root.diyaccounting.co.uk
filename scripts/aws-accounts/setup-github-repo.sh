#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Copyright (C) 2025-2026 DIY Accounting Ltd
#
# scripts/aws-accounts/setup-github-repo.sh
#
# Creates a GitHub repository from the static-web template, sets repository
# variables for OIDC authentication, and updates the root account trust policy.
#
# Usage:
#   ./scripts/aws-accounts/setup-github-repo.sh \
#     --account-id <id> --account-name <name> --repo <owner/repo> \
#     [--template <owner/template-repo>] [--root-profile <profile>]
#
# Prerequisites:
#   - GitHub CLI (gh) authenticated with repo, variable, and admin permissions
#   - AWS CLI configured with a profile for the root/management account
#   - bootstrap-account.sh already run for the target account

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# --- Defaults ---
ACCOUNT_ID=""
ACCOUNT_NAME=""
REPO=""
TEMPLATE_REPO="support-at-diyaccounting/www.diyaccounting.co.uk"
ROOT_PROFILE="management"
ROOT_ACCOUNT_ID="887764105431"
ROOT_HOSTED_ZONE_ID="Z0315522208PWZSSBI9AL"
SKIP_CREATE_REPO=false

# --- Parse arguments ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    --account-id)
      ACCOUNT_ID="$2"
      shift 2
      ;;
    --account-name)
      ACCOUNT_NAME="$2"
      shift 2
      ;;
    --repo)
      REPO="$2"
      shift 2
      ;;
    --template)
      TEMPLATE_REPO="$2"
      shift 2
      ;;
    --root-profile)
      ROOT_PROFILE="$2"
      shift 2
      ;;
    --skip-create-repo)
      SKIP_CREATE_REPO=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 --account-id <id> --account-name <name> --repo <owner/repo>"
      echo ""
      echo "Creates a GitHub repository and configures it for OIDC deployments."
      echo ""
      echo "Required:"
      echo "  --account-id <id>          AWS account ID (12 digits)"
      echo "  --account-name <name>      Account name for role naming (e.g., 'gateway')"
      echo "  --repo <owner/repo>        GitHub repository (e.g., 'support-at-diyaccounting/www.diyaccounting.co.uk')"
      echo ""
      echo "Options:"
      echo "  --template <owner/repo>    Template repository (default: support-at-diyaccounting/www.diyaccounting.co.uk)"
      echo "  --root-profile <profile>   AWS CLI profile for root account (default: management)"
      echo "  --skip-create-repo         Skip repo creation (use if repo already exists)"
      echo ""
      echo "What it does:"
      echo "  1. Creates GitHub repository from template"
      echo "  2. Sets GitHub repository variables (OIDC role ARNs, root account constants)"
      echo "  3. Creates 'prod' GitHub environment"
      echo "  4. Updates root-github-actions-role trust policy to trust the new repo"
      echo ""
      echo "Example:"
      echo "  $0 --account-id 283165661847 --account-name gateway --repo support-at-diyaccounting/www.diyaccounting.co.uk"
      exit 0
      ;;
    *)
      echo -e "${RED}ERROR: Unknown argument: $1${NC}"
      echo "Run $0 --help for usage."
      exit 1
      ;;
  esac
done

# --- Validate ---
if [[ -z "${ACCOUNT_ID}" ]]; then
  echo -e "${RED}ERROR: --account-id is required${NC}"
  exit 1
fi
if [[ -z "${ACCOUNT_NAME}" ]]; then
  echo -e "${RED}ERROR: --account-name is required${NC}"
  exit 1
fi
if [[ -z "${REPO}" ]]; then
  echo -e "${RED}ERROR: --repo is required${NC}"
  exit 1
fi
if ! [[ "${ACCOUNT_ID}" =~ ^[0-9]{12}$ ]]; then
  echo -e "${RED}ERROR: Account ID must be 12 digits (got: '${ACCOUNT_ID}')${NC}"
  exit 1
fi

ACCOUNT_NAME_UPPER=$(echo "${ACCOUNT_NAME}" | tr '[:lower:]-' '[:upper:]_')
ACTIONS_ROLE_NAME="${ACCOUNT_NAME}-github-actions-role"
DEPLOYMENT_ROLE_NAME="${ACCOUNT_NAME}-deployment-role"

# --- Header ---
echo -e "${GREEN}=== Setup GitHub Repository ===${NC}"
echo "  Account ID:      ${ACCOUNT_ID}"
echo "  Account name:    ${ACCOUNT_NAME}"
echo "  Repository:      ${REPO}"
echo "  Template:        ${TEMPLATE_REPO}"
echo "  Root profile:    ${ROOT_PROFILE}"
echo ""

# --- Verify gh is authenticated ---
echo "Verifying GitHub CLI..."
GH_USER=$(gh auth status 2>&1 | grep -o 'Logged in to github.com account [^ ]*' | awk '{print $NF}' || echo "")
if [[ -z "${GH_USER}" ]]; then
  echo -e "${RED}ERROR: GitHub CLI not authenticated. Run: gh auth login${NC}"
  exit 1
fi
echo -e "  Authenticated as: ${GREEN}${GH_USER}${NC}"
echo ""

# ============================================================================
# Step 1: Create Repository from Template
# ============================================================================
echo -e "${CYAN}Step 1: Create Repository from Template${NC}"

if [[ "${SKIP_CREATE_REPO}" == "true" ]]; then
  echo -e "  ${YELLOW}Skipped (--skip-create-repo)${NC}"
elif gh repo view "${REPO}" &>/dev/null; then
  echo -e "  ${GREEN}Repository already exists${NC}: ${REPO}"
else
  echo -n "  Creating ${REPO} from ${TEMPLATE_REPO}..."
  gh repo create "${REPO}" \
    --template "${TEMPLATE_REPO}" \
    --public \
    --clone=false
  echo -e " ${GREEN}OK${NC}"
fi
echo ""

# ============================================================================
# Step 2: Set GitHub Repository Variables
# ============================================================================
echo -e "${CYAN}Step 2: Set GitHub Repository Variables${NC}"

set_var() {
  local name="$1" value="$2"
  echo -n "  ${name}..."
  if gh variable set "${name}" --repo "${REPO}" --body "${value}" 2>/dev/null; then
    echo -e " ${GREEN}OK${NC}"
  else
    echo -e " ${RED}FAILED${NC} (set manually in Settings > Variables)"
    return 1
  fi
}

# Account-specific roles
set_var "${ACCOUNT_NAME_UPPER}_ACTIONS_ROLE_ARN" \
  "arn:aws:iam::${ACCOUNT_ID}:role/${ACTIONS_ROLE_NAME}" || true
set_var "${ACCOUNT_NAME_UPPER}_DEPLOY_ROLE_ARN" \
  "arn:aws:iam::${ACCOUNT_ID}:role/${DEPLOYMENT_ROLE_NAME}" || true

# Root account constants
set_var "ROOT_ACTIONS_ROLE_ARN" \
  "arn:aws:iam::${ROOT_ACCOUNT_ID}:role/root-github-actions-role" || true
set_var "ROOT_DEPLOY_ROLE_ARN" \
  "arn:aws:iam::${ROOT_ACCOUNT_ID}:role/root-deployment-role" || true
set_var "ROOT_ACCOUNT_ID" "${ROOT_ACCOUNT_ID}" || true
set_var "ROOT_HOSTED_ZONE_ID" "${ROOT_HOSTED_ZONE_ID}" || true
echo ""

# ============================================================================
# Step 3: Create 'prod' GitHub Environment
# ============================================================================
echo -e "${CYAN}Step 3: Create 'prod' GitHub Environment${NC}"

# gh api to create environment (idempotent PUT)
REPO_OWNER="${REPO%%/*}"
REPO_NAME="${REPO##*/}"
if gh api "repos/${REPO}/environments/prod" \
  --method PUT \
  --input /dev/null \
  &>/dev/null; then
  echo -e "  ${GREEN}Environment 'prod' created/confirmed${NC}"
  echo -e "  ${YELLOW}NOTE: Add protection rules (required reviewers) in Settings > Environments > prod${NC}"
else
  echo -e "  ${YELLOW}Could not create environment via API — create 'prod' manually in Settings > Environments${NC}"
fi
echo ""

# ============================================================================
# Step 4: Update Root Trust Policy
# ============================================================================
echo -e "${CYAN}Step 4: Update Root Account Trust Policy${NC}"

echo "  Checking root-github-actions-role trust policy..."

# Verify root profile works
ROOT_VERIFIED=$(aws sts get-caller-identity --profile "${ROOT_PROFILE}" --query 'Account' --output text 2>/dev/null) || ROOT_VERIFIED=""
if [[ "${ROOT_VERIFIED}" != "${ROOT_ACCOUNT_ID}" ]]; then
  echo -e "  ${YELLOW}WARNING: Cannot authenticate with root profile '${ROOT_PROFILE}'${NC}"
  echo "  Run manually:"
  echo "    aws --profile ${ROOT_PROFILE} iam get-role --role-name root-github-actions-role --query 'Role.AssumeRolePolicyDocument'"
  echo "    # Add \"repo:${REPO}:*\" to the StringLike condition, then:"
  echo "    aws --profile ${ROOT_PROFILE} iam update-assume-role-policy --role-name root-github-actions-role --policy-document file://trust.json"
else
  # Get current trust policy
  TRUST_POLICY=$(aws iam get-role \
    --profile "${ROOT_PROFILE}" \
    --role-name root-github-actions-role \
    --query 'Role.AssumeRolePolicyDocument' \
    --output json 2>/dev/null) || TRUST_POLICY=""

  if [[ -z "${TRUST_POLICY}" ]]; then
    echo -e "  ${RED}ERROR: Could not read root-github-actions-role trust policy${NC}"
  elif echo "${TRUST_POLICY}" | grep -q "repo:${REPO}:"; then
    echo -e "  ${GREEN}Repository already trusted${NC}: repo:${REPO}:*"
  else
    echo "  Adding repo:${REPO}:* to trust policy..."

    # Update the StringLike condition to include the new repo
    UPDATED_POLICY=$(echo "${TRUST_POLICY}" | python3 -c "
import json, sys
policy = json.load(sys.stdin)
for stmt in policy.get('Statement', []):
    cond = stmt.get('Condition', {}).get('StringLike', {})
    key = 'token.actions.githubusercontent.com:sub'
    if key in cond:
        existing = cond[key]
        new_entry = 'repo:${REPO}:*'
        if isinstance(existing, str):
            cond[key] = [existing, new_entry]
        elif isinstance(existing, list) and new_entry not in existing:
            existing.append(new_entry)
json.dump(policy, sys.stdout)
")

    if aws iam update-assume-role-policy \
      --profile "${ROOT_PROFILE}" \
      --role-name root-github-actions-role \
      --policy-document "${UPDATED_POLICY}" 2>/dev/null; then
      echo -e "  ${GREEN}Trust policy updated${NC}"
    else
      echo -e "  ${RED}ERROR: Failed to update trust policy${NC}"
      echo "  Update manually — add \"repo:${REPO}:*\" to the StringLike condition"
    fi
  fi
fi
echo ""

# ============================================================================
# Summary
# ============================================================================
echo -e "${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "  Repository:  ${REPO}"
echo "  Template:    ${TEMPLATE_REPO}"
echo ""
echo "Next steps:"
echo "  1. Clone the repository:"
echo "       gh repo clone ${REPO}"
echo "       cd ${REPO##*/}"
echo ""
echo "  2. Initialise the repository:"
echo "       - Update cdk.json with account ID and service-specific context"
echo "       - Update workflow variable names and stack names"
echo "       - Write README.md and CLAUDE.md"
echo ""
echo "  3. Build and verify:"
echo "       npm install"
echo "       ./mvnw clean verify"
echo "       npm run cdk:synth"
echo ""
echo "  4. Push and deploy:"
echo "       git push origin main"
echo "       gh workflow run deploy.yml --ref main"
echo ""
echo "  5. Update root DNS (from root.diyaccounting.co.uk repo):"
echo "       gh workflow run deploy.yml --ref main -R support-at-diyaccounting/root.diyaccounting.co.uk"
