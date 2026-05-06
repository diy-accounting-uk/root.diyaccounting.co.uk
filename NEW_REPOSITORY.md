# NEW_REPOSITORY.md — migration record

> This repository was migrated from `antonycc/root.diyaccounting.co.uk` to `support-at-diyaccounting/root.diyaccounting.co.uk` on **2026-05-06** after the suspension of `antonycc`.

## What happened

- The personal account `antonycc` was first org-flagged on 2026-05-03 and fully suspended on 2026-05-06.
- A new GitHub Pro account `support-at-diyaccounting` was created and authenticated.
- Cross-repo migration plan: see `PLAN_GITHUB_MIGRATION.md` and `PLAN_FLAGGED.md` in the parent workspace (`/Users/antony/projects/diy-accounting-limited/`).
- This repo manages the **management AWS account (887764105431)** — Route53 DNS for `diyaccounting.co.uk` and the holding page.

## How this repo was created in the new home

```bash
gh repo create support-at-diyaccounting/root.diyaccounting.co.uk \
  --public \
  --description "Root AWS account — Route53 DNS and holding page for diyaccounting.co.uk"

git -C root.diyaccounting.co.uk remote add newhome \
  git@github.com:support-at-diyaccounting/root.diyaccounting.co.uk.git
git -C root.diyaccounting.co.uk push newhome --all
git -C root.diyaccounting.co.uk push newhome --tags
```

## What was migrated

- **3 branches**: `main`, `colourin`, `import`.
- **0 tags**.
- **All repository content** including CDK Java code, scripts, holding-page content.

## Code rewrites in this branch

This branch (`claude/migrate-to-support-at-diyaccounting`) updates stale `antonycc` references. Replacement rules applied:

| Old reference | New reference |
|---|---|
| `antonycc/root.diyaccounting.co.uk` | `support-at-diyaccounting/root.diyaccounting.co.uk` |
| `antonycc/submit.diyaccounting.co.uk` | `support-at-diyaccounting/submit.diyaccounting.co.uk` |
| `antonycc/www.diyaccounting.co.uk` | `support-at-diyaccounting/www.diyaccounting.co.uk` |
| `antonycc/diy-accounting` | `support-at-diyaccounting/spreadsheets.diyaccounting.co.uk` (renamed during migration) |
| `@antonycc/root-diyaccounting-co-uk` (npm scope) | `@support-at-diyaccounting/root-diyaccounting-co-uk` |

Files affected:
- `CLAUDE.md`, `README.md`
- `package.json`
- `infra/main/java/co/uk/diyaccounting/root/stacks/ApexStack.java` — CDK stack tags
- `infra/main/java/co/uk/diyaccounting/root/stacks/RootDnsStack.java` — CDK stack tags
- `scripts/aws-accounts/bootstrap-account.sh` — default `GITHUB_REPO`
- `scripts/aws-accounts/create-account.sh` — generated repo URL hint
- `scripts/aws-accounts/setup-github-repo.sh` — default `TEMPLATE_REPO`, examples, post-setup hint

## What was deliberately NOT rewritten

- `cdk-submit-root.out/manifest.json` — build artifact; will be regenerated next `cdk synth`.
- `package-lock.json` — regenerated.

## What still needs setup before deploys work

### 1. AWS OIDC trust policy (BLOCKING for first deploy)

The IAM roles in account **887764105431 (management)** that GitHub Actions assumes have `sub` claim trust pinned to `repo:antonycc/root.diyaccounting.co.uk:*`. They must be updated to `repo:support-at-diyaccounting/root.diyaccounting.co.uk:*` before any workflow in this new repo can `aws-actions/configure-aws-credentials@v4`.

- Apply via CDK redeploy from local SSO:
  ```bash
  aws sso login --sso-session diyaccounting
  cd cdk-root
  ./mvnw clean verify
  cdk deploy --profile management --all
  ```

### 2. GitHub Actions Variables

Set on this repo via `gh variable set`:

| Variable | Value source |
|---|---|
| `ROOT_ACCOUNT_ID` | `887764105431` |
| `ROOT_HOSTED_ZONE_ID` | `aws --profile management route53 list-hosted-zones-by-name --dns-name diyaccounting.co.uk --query 'HostedZones[0].Id' --output text` (strip `/hostedzone/` prefix) |
| `ROOT_ACTIONS_ROLE_ARN` | `aws --profile management iam get-role --role-name root-github-actions-role --query Role.Arn --output text` |
| `ROOT_DEPLOY_ROLE_ARN` | `aws --profile management iam get-role --role-name root-deployment-role --query Role.Arn --output text` |
| `GATEWAY_ACTIONS_ROLE_ARN` | `aws --profile gateway iam get-role --role-name gateway-github-actions-role --query Role.Arn --output text` |
| `GATEWAY_DEPLOY_ROLE_ARN` | `aws --profile gateway iam get-role --role-name gateway-deployment-role --query Role.Arn --output text` |
| `SPREADSHEETS_ACTIONS_ROLE_ARN` | `aws --profile spreadsheets iam get-role --role-name spreadsheets-github-actions-role --query Role.Arn --output text` |
| `SPREADSHEETS_DEPLOY_ROLE_ARN` | `aws --profile spreadsheets iam get-role --role-name spreadsheets-deployment-role --query Role.Arn --output text` |
| `SUBMIT_REGIONAL_CERTIFICATE_ARN` | `aws --profile submit-prod acm list-certificates --region eu-west-2 --query "CertificateSummaryList[?DomainName=='*.submit.diyaccounting.co.uk'].CertificateArn" --output text` |

Cross-account variables (gateway, spreadsheets, submit) are set on root because the `deploy.yml` workflow does cross-account NS-record updates from the management account.

### 3. GitHub Actions Secrets

None required. All values are non-secret config.

### 4. GitHub Environments

None required. No environment-scoped variables for this repo.

## Sequence to restore deploys

1. Merge this PR.
2. `aws sso login --sso-session diyaccounting`.
3. From local: `cd cdk-root && cdk deploy --profile management --all` — applies the OIDC trust policy update with the new sub claim.
4. Set the GitHub Actions Variables listed above (`gh variable set ...`).
5. Push a trivial commit (or re-run the latest deploy workflow) to verify the new repo can assume the management role.
6. Verify Route53 zone is intact and the holding page still serves at https://diyaccounting.co.uk.

## How to obtain values

### Role ARNs
```bash
aws --profile <profile> iam list-roles \
  --query "Roles[?contains(RoleName, 'github-actions') || contains(RoleName, 'deployment')].[RoleName,Arn]" \
  --output table
```

### Hosted Zone ID
```bash
aws --profile management route53 list-hosted-zones \
  --query "HostedZones[?Name=='diyaccounting.co.uk.'].Id" --output text
```

### Certificate ARNs (regional, for SUBMIT_REGIONAL_CERTIFICATE_ARN)
```bash
aws --profile submit-prod acm list-certificates --region eu-west-2 \
  --query "CertificateSummaryList[].[DomainName,CertificateArn]" --output table
```
