#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Copyright (C) 2025-2026 DIY Accounting Ltd
#
# scripts/aws-accounts/bootstrap-account.sh
#
# Bootstraps a new AWS account for CDK deployments and GitHub Actions OIDC.
# Creates: CDK bootstrap stacks, OIDC provider, github-actions-role, deployment-role.
# Part of Phase 1 (PLAN_ACCOUNT_SEPARATION.md steps 1.x.3-1.x.5).
#
# Usage:
#   ./scripts/aws-accounts/bootstrap-account.sh --account-id <id> --account-name <name> --profile <profile>
#
# Prerequisites:
#   - AWS CLI configured with a named profile that has AdministratorAccess to the target account
#   - CDK CLI installed (npx cdk or global cdk)
#   - jq installed

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# --- Defaults ---
ACCOUNT_ID=""
ACCOUNT_NAME=""
PROFILE=""
GITHUB_REPO="support-at-diyaccounting/submit.diyaccounting.co.uk"
OIDC_PROVIDER_URL="token.actions.githubusercontent.com"
REGIONS=("us-east-1" "eu-west-2")

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
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    --github-repo)
      GITHUB_REPO="$2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 --account-id <id> --account-name <name> --profile <profile>"
      echo ""
      echo "Bootstraps a new AWS account for CDK deployments and GitHub Actions OIDC."
      echo ""
      echo "Required:"
      echo "  --account-id <id>       AWS account ID (12 digits)"
      echo "  --account-name <name>   Account name for role naming (e.g., 'submit', 'gateway', 'spreadsheets', 'submit-ci')"
      echo "  --profile <profile>     AWS CLI profile with AdministratorAccess to the account"
      echo ""
      echo "Options:"
      echo "  --github-repo <repo>    GitHub repo for OIDC trust (default: support-at-diyaccounting/submit.diyaccounting.co.uk)"
      echo ""
      echo "What it does:"
      echo "  1. CDK bootstraps us-east-1 and eu-west-2"
      echo "  2. Creates GitHub OIDC provider (token.actions.githubusercontent.com)"
      echo "  3. Creates {name}-github-actions-role (assumed by GitHub Actions via OIDC)"
      echo "  4. Creates {name}-deployment-role (assumed by github-actions-role for CDK deploys)"
      echo ""
      echo "Account names and resulting roles:"
      echo "  submit-prod  -> submit-prod-github-actions-role, submit-prod-deployment-role"
      echo "  submit-ci    -> submit-ci-github-actions-role, submit-ci-deployment-role"
      echo "  gateway      -> gateway-github-actions-role, gateway-deployment-role"
      echo "  spreadsheets -> spreadsheets-github-actions-role, spreadsheets-deployment-role"
      echo ""
      echo "Example:"
      echo "  $0 --account-id 972912397388 --account-name submit-prod --profile submit-prod"
      echo "  $0 --account-id 234567890123 --account-name gateway --profile gateway"
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
  echo "Run $0 --help for usage."
  exit 1
fi

if [[ -z "${ACCOUNT_NAME}" ]]; then
  echo -e "${RED}ERROR: --account-name is required${NC}"
  echo "Run $0 --help for usage."
  exit 1
fi

if [[ -z "${PROFILE}" ]]; then
  echo -e "${RED}ERROR: --profile is required${NC}"
  echo "Run $0 --help for usage."
  exit 1
fi

if ! [[ "${ACCOUNT_ID}" =~ ^[0-9]{12}$ ]]; then
  echo -e "${RED}ERROR: Account ID must be 12 digits (got: '${ACCOUNT_ID}')${NC}"
  exit 1
fi

ACTIONS_ROLE_NAME="${ACCOUNT_NAME}-github-actions-role"
DEPLOYMENT_ROLE_NAME="${ACCOUNT_NAME}-deployment-role"

# --- Header ---
echo -e "${GREEN}=== Bootstrap AWS Account ===${NC}"
echo "  Account ID:      ${ACCOUNT_ID}"
echo "  Account name:    ${ACCOUNT_NAME}"
echo "  Profile:         ${PROFILE}"
echo "  GitHub repo:     ${GITHUB_REPO}"
echo "  Actions role:    ${ACTIONS_ROLE_NAME}"
echo "  Deployment role: ${DEPLOYMENT_ROLE_NAME}"
echo "  Regions:         ${REGIONS[*]}"
echo ""

# --- Verify credentials ---
echo "Verifying credentials..."
VERIFIED_ACCOUNT=$(aws sts get-caller-identity --profile "${PROFILE}" --query 'Account' --output text 2>/dev/null) || {
  echo -e "${RED}ERROR: Cannot authenticate with profile '${PROFILE}'${NC}"
  echo "  Run: aws sso login --sso-session diyaccounting"
  exit 1
}

if [[ "${VERIFIED_ACCOUNT}" != "${ACCOUNT_ID}" ]]; then
  echo -e "${RED}ERROR: Profile '${PROFILE}' authenticates to account ${VERIFIED_ACCOUNT}, not ${ACCOUNT_ID}${NC}"
  exit 1
fi
echo -e "  Authenticated: ${GREEN}${VERIFIED_ACCOUNT}${NC}"
echo ""

# ============================================================================
# Step 1: CDK Bootstrap
# ============================================================================
echo -e "${CYAN}Step 1: CDK Bootstrap${NC}"

for REGION in "${REGIONS[@]}"; do
  echo -n "  Bootstrapping ${REGION}..."

  # Check if already bootstrapped
  BOOTSTRAP_STATUS=$(aws cloudformation describe-stacks \
    --profile "${PROFILE}" \
    --region "${REGION}" \
    --stack-name CDKToolkit \
    --query 'Stacks[0].StackStatus' \
    --output text 2>/dev/null) || BOOTSTRAP_STATUS=""

  if [[ "${BOOTSTRAP_STATUS}" == "CREATE_COMPLETE" || "${BOOTSTRAP_STATUS}" == "UPDATE_COMPLETE" ]]; then
    echo -e " ${GREEN}already bootstrapped (${BOOTSTRAP_STATUS})${NC}"
    continue
  fi

  npx cdk bootstrap "aws://${ACCOUNT_ID}/${REGION}" \
    --profile "${PROFILE}" \
    --cloudformation-execution-policies "arn:aws:iam::aws:policy/AdministratorAccess" \
    2>&1 | tail -1

  echo -e "  ${GREEN}Bootstrapped ${REGION}${NC}"
done
echo ""

# ============================================================================
# Step 2: GitHub OIDC Provider
# ============================================================================
echo -e "${CYAN}Step 2: GitHub OIDC Provider${NC}"

# Check if OIDC provider already exists
OIDC_ARN="arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${OIDC_PROVIDER_URL}"
EXISTING_OIDC=$(aws iam get-open-id-connect-provider \
  --profile "${PROFILE}" \
  --open-id-connect-provider-arn "${OIDC_ARN}" \
  --query 'Url' \
  --output text 2>/dev/null) || EXISTING_OIDC=""

if [[ -n "${EXISTING_OIDC}" ]]; then
  echo -e "  ${GREEN}OIDC provider already exists${NC}: ${OIDC_ARN}"
else
  echo -n "  Creating OIDC provider for ${OIDC_PROVIDER_URL}..."

  # GitHub's OIDC thumbprint (standard across all accounts)
  # This is the thumbprint of the intermediate CA that signs GitHub's OIDC tokens.
  # AWS now auto-discovers thumbprints for OIDC providers, but we include it for reliability.
  THUMBPRINT="6938fd4d98bab03faadb97b34396831e3780aea1"

  aws iam create-open-id-connect-provider \
    --profile "${PROFILE}" \
    --url "https://${OIDC_PROVIDER_URL}" \
    --client-id-list "sts.amazonaws.com" \
    --thumbprint-list "${THUMBPRINT}" \
    --output json >/dev/null

  echo -e " ${GREEN}OK${NC}"
fi
echo ""

# ============================================================================
# Step 3: GitHub Actions Role (OIDC assumption)
# ============================================================================
echo -e "${CYAN}Step 3: GitHub Actions Role (${ACTIONS_ROLE_NAME})${NC}"

# Check if role already exists
EXISTING_ACTIONS_ROLE=$(aws iam get-role \
  --profile "${PROFILE}" \
  --role-name "${ACTIONS_ROLE_NAME}" \
  --query 'Role.Arn' \
  --output text 2>/dev/null) || EXISTING_ACTIONS_ROLE=""

if [[ -n "${EXISTING_ACTIONS_ROLE}" ]]; then
  echo -e "  ${GREEN}Role already exists${NC}: ${EXISTING_ACTIONS_ROLE}"
else
  echo -n "  Creating ${ACTIONS_ROLE_NAME}..."

  # Trust policy: GitHub Actions OIDC, scoped to this repo
  TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${OIDC_PROVIDER_URL}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${OIDC_PROVIDER_URL}:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "${OIDC_PROVIDER_URL}:sub": "repo:${GITHUB_REPO}:*"
        }
      }
    }
  ]
}
EOF
)

  ACTIONS_ROLE_ARN=$(aws iam create-role \
    --profile "${PROFILE}" \
    --role-name "${ACTIONS_ROLE_NAME}" \
    --assume-role-policy-document "${TRUST_POLICY}" \
    --description "GitHub Actions OIDC role for ${GITHUB_REPO} - assumes ${DEPLOYMENT_ROLE_NAME}" \
    --max-session-duration 7200 \
    --query 'Role.Arn' \
    --output text)

  echo -e " ${GREEN}OK${NC}"
  echo "  ARN: ${ACTIONS_ROLE_ARN}"

  # Attach policy to allow assuming the deployment role
  ASSUME_DEPLOY_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::${ACCOUNT_ID}:role/${DEPLOYMENT_ROLE_NAME}"
    }
  ]
}
EOF
)

  aws iam put-role-policy \
    --profile "${PROFILE}" \
    --role-name "${ACTIONS_ROLE_NAME}" \
    --policy-name "AssumeDeploymentRole" \
    --policy-document "${ASSUME_DEPLOY_POLICY}"

  echo -e "  ${GREEN}Attached AssumeDeploymentRole policy${NC}"

  # IAM is eventually consistent — the new role may not be visible as a principal yet.
  # Wait for propagation before Step 4 tries to reference it in a trust policy.
  echo "  Waiting 10s for IAM propagation..."
  sleep 10
fi
echo ""

# ============================================================================
# Step 4: Deployment Role (CDK deploys)
# ============================================================================
echo -e "${CYAN}Step 4: Deployment Role (${DEPLOYMENT_ROLE_NAME})${NC}"

# Check if role already exists
EXISTING_DEPLOY_ROLE=$(aws iam get-role \
  --profile "${PROFILE}" \
  --role-name "${DEPLOYMENT_ROLE_NAME}" \
  --query 'Role.Arn' \
  --output text 2>/dev/null) || EXISTING_DEPLOY_ROLE=""

if [[ -n "${EXISTING_DEPLOY_ROLE}" ]]; then
  echo -e "  ${GREEN}Role already exists${NC}: ${EXISTING_DEPLOY_ROLE}"
else
  echo -n "  Creating ${DEPLOYMENT_ROLE_NAME}..."

  # Trust policy: allow assumption from the GitHub Actions role
  DEPLOY_TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::${ACCOUNT_ID}:role/${ACTIONS_ROLE_NAME}"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
)

  DEPLOY_ROLE_ARN=$(aws iam create-role \
    --profile "${PROFILE}" \
    --role-name "${DEPLOYMENT_ROLE_NAME}" \
    --assume-role-policy-document "${DEPLOY_TRUST_POLICY}" \
    --description "CDK deployment role for ${ACCOUNT_NAME} - trusted by ${ACTIONS_ROLE_NAME}" \
    --max-session-duration 7200 \
    --query 'Role.Arn' \
    --output text)

  echo -e " ${GREEN}OK${NC}"
  echo "  ARN: ${DEPLOY_ROLE_ARN}"

  # Attach AdministratorAccess for CDK deployments
  # CDK needs broad permissions to create arbitrary resources (Lambda, DDB, CloudFront, etc.)
  aws iam attach-role-policy \
    --profile "${PROFILE}" \
    --role-name "${DEPLOYMENT_ROLE_NAME}" \
    --policy-arn "arn:aws:iam::aws:policy/AdministratorAccess"

  echo -e "  ${GREEN}Attached AdministratorAccess${NC}"
  echo -e "  ${YELLOW}NOTE: Consider creating a scoped-down policy for production${NC}"
fi
echo ""

# ============================================================================
# Summary
# ============================================================================
FINAL_ACTIONS_ARN=$(aws iam get-role --profile "${PROFILE}" --role-name "${ACTIONS_ROLE_NAME}" --query 'Role.Arn' --output text 2>/dev/null) || FINAL_ACTIONS_ARN="(not found)"
FINAL_DEPLOY_ARN=$(aws iam get-role --profile "${PROFILE}" --role-name "${DEPLOYMENT_ROLE_NAME}" --query 'Role.Arn' --output text 2>/dev/null) || FINAL_DEPLOY_ARN="(not found)"

echo -e "${GREEN}=== Bootstrap Complete ===${NC}"
echo ""
echo "  Account:         ${ACCOUNT_ID} (${ACCOUNT_NAME})"
echo "  CDK bootstrap:   us-east-1, eu-west-2"
echo "  OIDC provider:   ${OIDC_PROVIDER_URL}"
echo "  Actions role:    ${FINAL_ACTIONS_ARN}"
echo "  Deployment role: ${FINAL_DEPLOY_ARN}"
echo ""
ACCOUNT_NAME_UPPER=$(echo "${ACCOUNT_NAME}" | tr '[:lower:]-' '[:upper:]_')

echo "GitHub Secrets to add to the repository:"
echo "  ${ACCOUNT_NAME_UPPER}_ACCOUNT_ID=${ACCOUNT_ID}"
echo "  ${ACCOUNT_NAME_UPPER}_ACTIONS_ROLE_ARN=${FINAL_ACTIONS_ARN}"
echo "  ${ACCOUNT_NAME_UPPER}_DEPLOY_ROLE_ARN=${FINAL_DEPLOY_ARN}"
echo ""
echo "Verification commands:"
echo "  # Verify OIDC provider"
echo "  aws iam get-open-id-connect-provider --profile ${PROFILE} --open-id-connect-provider-arn ${OIDC_ARN}"
echo ""
echo "  # Verify roles"
echo "  aws iam get-role --profile ${PROFILE} --role-name ${ACTIONS_ROLE_NAME}"
echo "  aws iam get-role --profile ${PROFILE} --role-name ${DEPLOYMENT_ROLE_NAME}"
echo ""
echo "  # Verify CDK bootstrap"
echo "  aws cloudformation describe-stacks --profile ${PROFILE} --region eu-west-2 --stack-name CDKToolkit --query 'Stacks[0].StackStatus'"
echo "  aws cloudformation describe-stacks --profile ${PROFILE} --region us-east-1 --stack-name CDKToolkit --query 'Stacks[0].StackStatus'"
echo ""
echo "  # Test OIDC from GitHub Actions (add a workflow step):"
echo "  # - uses: aws-actions/configure-aws-credentials@v4"
echo "  #   with:"
echo "  #     role-to-assume: ${FINAL_ACTIONS_ARN}"
echo "  #     aws-region: eu-west-2"
