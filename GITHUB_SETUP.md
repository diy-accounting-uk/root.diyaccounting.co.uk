# GITHUB_SETUP.md — what a fresh GitHub repo needs

This document captures the GitHub-side configuration this repo depends on. If you're setting it up in a new GitHub account or org, this is the checklist.

## What this repo deploys

The DNS root for `diyaccounting.co.uk` and the holding/maintenance page. CDK (Java) deploys Route53 records and a cross-account delegation role to the management AWS account:

| Account | AWS Account ID | Profile |
|---|---|---|
| `management` | 887764105431 | `management` |

The hosted zone for `diyaccounting.co.uk` lives here (zone ID `Z0315522208PWZSSBI9AL`); records point at gateway/spreadsheets/submit CloudFront distributions in their respective accounts.

## AWS-side prerequisites

The management AWS account needs a GitHub OIDC provider and these IAM roles:

| Role | Trusted by | Purpose |
|---|---|---|
| `root-github-actions-role` | GitHub OIDC | Workflow entry — allowed `sub` claim `repo:<org>/root.diyaccounting.co.uk:*` |
| `root-deployment-role` | `root-github-actions-role` | CDK deploy role; assumed via STS chain |
| `root-route53-record-delegate` | submit-ci (367191799875), submit-prod (972912397388) | Cross-account: lets submit deploys upsert Route53 records |

These were created by `submit.diyaccounting.co.uk/scripts/aws-accounts/bootstrap-account.sh` originally. The OIDC trust on `root-github-actions-role` is **not CDK-managed** — if the GitHub org changes, update the trust on that role directly with `aws iam update-assume-role-policy`. CDK only manages the cross-account delegation roles.

## GitHub Environments

`deploy-root-dns` job declares `environment: prod`. Create the `prod` environment under **Settings → Environments**.

## GitHub Actions Variables

Repo-level. The values below are per-account ARNs and IDs that the deploy workflows use to assume cross-account roles for Route53 alias updates and CloudFront lookups.

| Variable | How to obtain |
|---|---|
| `ROOT_ACCOUNT_ID` | `887764105431` |
| `ROOT_HOSTED_ZONE_ID` | `aws --profile management route53 list-hosted-zones-by-name --dns-name diyaccounting.co.uk --query 'HostedZones[0].Id' --output text` (strip `/hostedzone/`) |
| `ROOT_ACTIONS_ROLE_ARN` | `aws --profile management iam get-role --role-name root-github-actions-role --query Role.Arn --output text` |
| `ROOT_DEPLOY_ROLE_ARN` | `aws --profile management iam get-role --role-name root-deployment-role --query Role.Arn --output text` |
| `GATEWAY_ACTIONS_ROLE_ARN` | `aws --profile gateway iam get-role --role-name gateway-github-actions-role --query Role.Arn --output text` |
| `GATEWAY_DEPLOY_ROLE_ARN` | `aws --profile gateway iam get-role --role-name gateway-deployment-role --query Role.Arn --output text` |
| `SPREADSHEETS_ACTIONS_ROLE_ARN` | `aws --profile spreadsheets iam get-role --role-name spreadsheets-github-actions-role --query Role.Arn --output text` |
| `SPREADSHEETS_DEPLOY_ROLE_ARN` | `aws --profile spreadsheets iam get-role --role-name spreadsheets-deployment-role --query Role.Arn --output text` |
| `SUBMIT_REGIONAL_CERTIFICATE_ARN` | `aws --profile submit-prod acm list-certificates --region eu-west-2 --query "CertificateSummaryList[?DomainName=='*.submit.diyaccounting.co.uk'].CertificateArn" --output text` (used by deploy-holding workflow) |

Cross-account variables (`GATEWAY_*`, `SPREADSHEETS_*`, `SUBMIT_*`) are set on this repo because the deploy workflow does cross-account NS-record updates from the management account.

## GitHub Actions Secrets

None required.

## Workflows

| File | Trigger | Notes |
|---|---|---|
| `test.yml` | push (paths-filtered), workflow_dispatch, daily schedule | Lint + Maven verify + CDK synth |
| `deploy.yml` | workflow_dispatch | Deploy `RootDnsStack` (Route53 records + cross-account roles) |
| `deploy-holding.yml` | workflow_dispatch | Switch the apex CloudFront alias to a holding page or back to last-known-good. **Currently has a known architectural gap** — the holding-page CloudFront distribution it expects to find in management doesn't exist yet (`ApexStack.java` exists in code but isn't wired into `RootEnvironment.java`). See `../PLAN_HOLDING_ARCHITECTURE.md` in the parent workspace for the full plan. |

## Sequence to bring a new repo online

1. Create the repo on GitHub. Push code.
2. Update OIDC trust on `root-github-actions-role` to include `repo:<new-org>/root.diyaccounting.co.uk:*` (via the AWS console or `aws iam update-assume-role-policy`).
3. Create the `prod` GitHub Environment.
4. Set the repo-level variables listed above. Cross-account variables require SSO into each profile (`gateway`, `spreadsheets`, `submit-prod`).
5. Push a trivial commit on a feature branch — `test.yml` should pass (proves OIDC for management).
6. `gh workflow run deploy.yml` to apply the `RootDnsStack` (DNS + cross-account delegation roles).
7. Verify https://diyaccounting.co.uk resolves and Route53 records are intact.

## How to obtain values quickly

```bash
# All role ARNs in management
aws --profile management iam list-roles \
  --query "Roles[?contains(RoleName, 'github-actions') || contains(RoleName, 'deployment') || contains(RoleName, 'route53')].[RoleName,Arn]" \
  --output table

# Route53 hosted zone ID
aws --profile management route53 list-hosted-zones \
  --query "HostedZones[?Name=='diyaccounting.co.uk.'].Id" --output text

# Verify trust on root-github-actions-role
aws --profile management iam get-role --role-name root-github-actions-role \
  --query 'Role.AssumeRolePolicyDocument.Statement[].Condition.StringLike'
```
