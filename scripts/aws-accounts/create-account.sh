#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Copyright (C) 2025-2026 DIY Accounting Ltd
#
# scripts/aws-accounts/create-account.sh
#
# Creates a new AWS account in the Organization and moves it to the Workloads OU.
# Waits for account creation to complete and outputs the new account ID.
#
# Usage:
#   ./scripts/aws-accounts/create-account.sh --account-name <name> --email <email> [--ou-name <ou>]
#
# Prerequisites:
#   - AWS CLI configured with a profile that has Organizations access (management account)

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# --- Defaults ---
ACCOUNT_NAME=""
EMAIL=""
PROFILE="management"
OU_NAME="Workloads"

# --- Parse arguments ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    --account-name)
      ACCOUNT_NAME="$2"
      shift 2
      ;;
    --email)
      EMAIL="$2"
      shift 2
      ;;
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    --ou-name)
      OU_NAME="$2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 --account-name <name> --email <email>"
      echo ""
      echo "Creates a new AWS account in the Organization."
      echo ""
      echo "Required:"
      echo "  --account-name <name>   Account name (e.g., 'gateway', 'spreadsheets')"
      echo "  --email <email>         Root email for the account (e.g., 'aws+gateway@diyaccounting.co.uk')"
      echo ""
      echo "Options:"
      echo "  --profile <profile>     AWS CLI profile with Organizations access (default: management)"
      echo "  --ou-name <ou>          Organizational Unit to move account to (default: Workloads)"
      echo ""
      echo "What it does:"
      echo "  1. Creates a new account in the AWS Organization"
      echo "  2. Waits for creation to complete"
      echo "  3. Moves the account to the specified OU"
      echo "  4. Outputs the new account ID and next steps"
      echo ""
      echo "Example:"
      echo "  $0 --account-name gateway --email aws+gateway@diyaccounting.co.uk"
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
if [[ -z "${ACCOUNT_NAME}" ]]; then
  echo -e "${RED}ERROR: --account-name is required${NC}"
  exit 1
fi
if [[ -z "${EMAIL}" ]]; then
  echo -e "${RED}ERROR: --email is required${NC}"
  exit 1
fi

# --- Header ---
echo -e "${GREEN}=== Create AWS Account ===${NC}"
echo "  Account name:  ${ACCOUNT_NAME}"
echo "  Email:         ${EMAIL}"
echo "  Profile:       ${PROFILE}"
echo "  Target OU:     ${OU_NAME}"
echo ""

# --- Verify credentials ---
echo "Verifying credentials..."
CALLER_ACCOUNT=$(aws sts get-caller-identity --profile "${PROFILE}" --query 'Account' --output text 2>/dev/null) || {
  echo -e "${RED}ERROR: Cannot authenticate with profile '${PROFILE}'${NC}"
  echo "  Run: aws sso login --sso-session diyaccounting"
  exit 1
}
echo -e "  Authenticated to account: ${GREEN}${CALLER_ACCOUNT}${NC}"
echo ""

# ============================================================================
# Step 1: Look up Organization structure
# ============================================================================
echo -e "${CYAN}Step 1: Look up Organization structure${NC}"

# Get the Organization root ID
ROOT_ID=$(aws organizations list-roots \
  --profile "${PROFILE}" \
  --query 'Roots[0].Id' \
  --output text)
echo "  Organization root: ${ROOT_ID}"

# Find the target OU
OU_ID=$(aws organizations list-organizational-units-for-parent \
  --profile "${PROFILE}" \
  --parent-id "${ROOT_ID}" \
  --query "OrganizationalUnits[?Name=='${OU_NAME}'].Id | [0]" \
  --output text 2>/dev/null) || OU_ID="None"

if [[ "${OU_ID}" == "None" || -z "${OU_ID}" ]]; then
  echo -e "  ${RED}ERROR: OU '${OU_NAME}' not found under root${NC}"
  echo "  Available OUs:"
  aws organizations list-organizational-units-for-parent \
    --profile "${PROFILE}" \
    --parent-id "${ROOT_ID}" \
    --query 'OrganizationalUnits[].Name' \
    --output text
  exit 1
fi
echo -e "  Target OU: ${GREEN}${OU_NAME}${NC} (${OU_ID})"
echo ""

# ============================================================================
# Step 2: Create the account
# ============================================================================
echo -e "${CYAN}Step 2: Create Account${NC}"

# Check if account already exists
EXISTING_ACCOUNT=$(aws organizations list-accounts \
  --profile "${PROFILE}" \
  --query "Accounts[?Name=='${ACCOUNT_NAME}' && Status=='ACTIVE'].Id | [0]" \
  --output text 2>/dev/null) || EXISTING_ACCOUNT="None"

if [[ "${EXISTING_ACCOUNT}" != "None" && -n "${EXISTING_ACCOUNT}" ]]; then
  echo -e "  ${GREEN}Account already exists${NC}: ${EXISTING_ACCOUNT} (${ACCOUNT_NAME})"
  NEW_ACCOUNT_ID="${EXISTING_ACCOUNT}"
else
  echo -n "  Creating account '${ACCOUNT_NAME}' (${EMAIL})..."

  REQUEST_ID=$(aws organizations create-account \
    --profile "${PROFILE}" \
    --email "${EMAIL}" \
    --account-name "${ACCOUNT_NAME}" \
    --iam-user-access-to-billing ALLOW \
    --query 'CreateAccountStatus.Id' \
    --output text)

  echo -e " ${GREEN}submitted${NC} (request: ${REQUEST_ID})"

  # Poll until complete
  echo -n "  Waiting for account creation"
  while true; do
    STATUS=$(aws organizations describe-create-account-status \
      --profile "${PROFILE}" \
      --create-account-request-id "${REQUEST_ID}" \
      --query 'CreateAccountStatus.State' \
      --output text)

    case "${STATUS}" in
      SUCCEEDED)
        echo -e " ${GREEN}OK${NC}"
        break
        ;;
      FAILED)
        REASON=$(aws organizations describe-create-account-status \
          --profile "${PROFILE}" \
          --create-account-request-id "${REQUEST_ID}" \
          --query 'CreateAccountStatus.FailureReason' \
          --output text)
        echo -e " ${RED}FAILED: ${REASON}${NC}"
        exit 1
        ;;
      IN_PROGRESS)
        echo -n "."
        sleep 10
        ;;
    esac
  done

  NEW_ACCOUNT_ID=$(aws organizations describe-create-account-status \
    --profile "${PROFILE}" \
    --create-account-request-id "${REQUEST_ID}" \
    --query 'CreateAccountStatus.AccountId' \
    --output text)

  echo -e "  Account ID: ${GREEN}${NEW_ACCOUNT_ID}${NC}"
fi
echo ""

# ============================================================================
# Step 3: Move to target OU
# ============================================================================
echo -e "${CYAN}Step 3: Move Account to ${OU_NAME} OU${NC}"

# Check current parent
CURRENT_PARENT=$(aws organizations list-parents \
  --profile "${PROFILE}" \
  --child-id "${NEW_ACCOUNT_ID}" \
  --query 'Parents[0].Id' \
  --output text 2>/dev/null) || CURRENT_PARENT=""

if [[ "${CURRENT_PARENT}" == "${OU_ID}" ]]; then
  echo -e "  ${GREEN}Already in ${OU_NAME} OU${NC}"
else
  echo -n "  Moving from ${CURRENT_PARENT} to ${OU_ID}..."
  aws organizations move-account \
    --profile "${PROFILE}" \
    --account-id "${NEW_ACCOUNT_ID}" \
    --source-parent-id "${CURRENT_PARENT}" \
    --destination-parent-id "${OU_ID}"
  echo -e " ${GREEN}OK${NC}"
fi
echo ""

# ============================================================================
# Summary
# ============================================================================
echo -e "${GREEN}=== Account Created ===${NC}"
echo ""
echo "  Account ID:   ${NEW_ACCOUNT_ID}"
echo "  Account name: ${ACCOUNT_NAME}"
echo "  Email:        ${EMAIL}"
echo "  OU:           ${OU_NAME}"
echo ""
echo "Next steps:"
echo ""
echo "  1. Configure SSO access for the new account in IAM Identity Center"
echo ""
echo "  2. Bootstrap the account for CDK and OIDC:"
echo "       ./scripts/aws-accounts/bootstrap-account.sh \\"
echo "         --account-id ${NEW_ACCOUNT_ID} \\"
echo "         --account-name ${ACCOUNT_NAME} \\"
echo "         --profile <sso-profile-for-new-account>"
echo ""
echo "  3. Set up the GitHub repository:"
echo "       ./scripts/aws-accounts/setup-github-repo.sh \\"
echo "         --account-id ${NEW_ACCOUNT_ID} \\"
echo "         --account-name ${ACCOUNT_NAME} \\"
echo "         --repo support-at-diyaccounting/${ACCOUNT_NAME}.diyaccounting.co.uk"
