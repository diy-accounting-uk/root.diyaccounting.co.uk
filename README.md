# root.diyaccounting.co.uk

Root AWS account (887764105431) infrastructure for diyaccounting.co.uk. Manages the Route53 hosted zone, DNS alias records, cross-account delegation role, and the holding/maintenance page.

## Account Structure

```
AWS Organization Root (887764105431) ── Management
├── gateway ─────────── 283165661847 ── Workloads OU (S3 + CloudFront)
├── spreadsheets ────── 064390746177 ── Workloads OU (S3 + CloudFront)
├── submit-ci ──────── 367191799875 ── Workloads OU (Lambda, DDB, Cognito, API GW)
├── submit-prod ────── 972912397388 ── Workloads OU (Lambda, DDB, Cognito, API GW)
└── submit-backup ──── 914216784828 ── Backup OU (cross-account vault)
```

The management account (887764105431) retains only: AWS Organizations, IAM Identity Center, Route53 zone (`diyaccounting.co.uk`), consolidated billing, root DNS stack, and holding page. No application workloads.

## Domain Convention

| Service      | CI                                    | Prod                                    | Prod Alias                                       |
| ------------ | ------------------------------------- | --------------------------------------- | ------------------------------------------------ |
| Submit       | `ci-submit.diyaccounting.co.uk`       | `prod-submit.diyaccounting.co.uk`       | `submit.diyaccounting.co.uk`                     |
| Gateway      | `ci-gateway.diyaccounting.co.uk`      | `prod-gateway.diyaccounting.co.uk`      | `diyaccounting.co.uk`, `www.diyaccounting.co.uk` |
| Spreadsheets | `ci-spreadsheets.diyaccounting.co.uk` | `prod-spreadsheets.diyaccounting.co.uk` | `spreadsheets.diyaccounting.co.uk`               |
| Holding      | `ci-holding.diyaccounting.co.uk`      | `prod-holding.diyaccounting.co.uk`      |                                                  |

## What This Repo Manages

- **Route53 hosted zone** for `diyaccounting.co.uk` (all DNS records)
- **RootDnsStack**: Alias A/AAAA records pointing to gateway/spreadsheets CloudFront distributions
- **Holding page**: Maintenance page at `{env}-holding.diyaccounting.co.uk`
- **Cross-account delegation role**: `root-route53-record-delegate` for submit accounts to create DNS records

**What this repo does NOT have**: Lambda, DynamoDB, Cognito, API Gateway, Docker, ngrok, HMRC, Stripe, or any application code.

## Build Commands

```bash
npm install                      # Install CDK CLI and dev dependencies
./mvnw clean verify              # Build CDK JARs
npm run cdk:synth                # Synthesize CloudFormation templates
npm run cdk:diff                 # Show pending changes
```

## Development Tools

```bash
npm run formatting               # Check formatting (Prettier + Spotless)
npm run formatting-fix           # Auto-fix formatting
npm run lint:workflows           # Validate GitHub Actions workflow syntax
npm run update:java              # Update Maven dependencies to latest versions
npm run update:node              # Update npm dependencies (latest non-alpha)
npm run update-to-minor          # Update npm dependencies (minor versions only)
npm run diagram:root             # Generate draw.io architecture diagram from CDK
```

## CDK Architecture

**Single CDK application** (`cdk-root/`):

- Entry point: `RootEnvironment.java` -> `submit-root.jar`
- Stack: `root-RootDnsStack` (Route53 alias records + delegation role)

**Java packages** (`co.uk.diyaccounting.root`):

- `root` — `RootEnvironment.java` (entry point), `SubmitSharedNames.java` (shared config)
- `root.stacks` — `RootDnsStack.java`, `ApexStack.java`, `SubmitStackProps.java`
- `root.utils` — `Kind.java` (logging), `KindCdk.java` (CDK utilities), `Route53AliasUpsert.java` (DNS alias), `ResourceNameUtils.java` (name conversion)

## Deployment

Deployments are triggered via GitHub Actions workflows:

| Workflow             | Purpose                                        | Trigger              |
| -------------------- | ---------------------------------------------- | -------------------- |
| `test.yml`           | Lint, format check, Maven verify, CDK synth    | Push, daily schedule |
| `deploy.yml`         | Deploy RootDnsStack (DNS records)              | Manual dispatch      |
| `deploy-holding.yml` | Switch apex to holding page or last-known-good | Manual dispatch      |

Both workflows use OIDC authentication with GitHub repository variables.

## GitHub Repository Variables

These variables are configured in **Settings > Secrets and variables > Actions > Variables**:

| Variable                        | Value                                                             | Purpose                            |
| ------------------------------- | ----------------------------------------------------------------- | ---------------------------------- |
| `GATEWAY_ACTIONS_ROLE_ARN`      | `arn:aws:iam::283165661847:role/gateway-github-actions-role`      | OIDC auth for gateway account      |
| `GATEWAY_DEPLOY_ROLE_ARN`       | `arn:aws:iam::283165661847:role/gateway-deployment-role`          | CloudFront lookups in gateway      |
| `SPREADSHEETS_ACTIONS_ROLE_ARN` | `arn:aws:iam::064390746177:role/spreadsheets-github-actions-role` | OIDC auth for spreadsheets account |
| `SPREADSHEETS_DEPLOY_ROLE_ARN`  | `arn:aws:iam::064390746177:role/spreadsheets-deployment-role`     | CloudFront lookups in spreadsheets |
| `ROOT_ACTIONS_ROLE_ARN`         | `arn:aws:iam::887764105431:role/root-github-actions-role`         | OIDC auth for root account         |
| `ROOT_DEPLOY_ROLE_ARN`          | `arn:aws:iam::887764105431:role/root-deployment-role`             | CDK deploy in root account         |
| `ROOT_ACCOUNT_ID`               | `887764105431`                                                    | Root account identifier            |
| `ROOT_HOSTED_ZONE_ID`           | `Z0315522208PWZSSBI9AL`                                           | Route53 hosted zone ID             |

Each OIDC actions role trusts `repo:support-at-diyaccounting/root.diyaccounting.co.uk:*` (and `repo:support-at-diyaccounting/submit.diyaccounting.co.uk:*` for backward compatibility).

## Cross-Repo Coordination

All four repositories depend on root for DNS. When a service changes its CloudFront distribution, root must update the alias records via `deploy.yml`.

```
root.diyaccounting.co.uk ──OIDC──> 887764105431 (DNS + holding only)
submit repo ───────OIDC──> submit-prod + submit-ci
gateway repo ──────OIDC──> gateway (S3 + CloudFront)
spreadsheets repo ─OIDC──> spreadsheets (S3 + CloudFront)
```

## Bootstrapping a New Account

Scripts in `scripts/aws-accounts/` automate the full onboarding flow. Static web projects (S3 + CloudFront) should use `support-at-diyaccounting/www.diyaccounting.co.uk` as a GitHub repository template.

### Prerequisites

- AWS SSO access to the management account (`management` profile)
- GitHub CLI (`gh`) authenticated with repo and variable permissions
- Node.js >= 24, Java >= 25

### Quick Start

```bash
# Step 1: Create the AWS account (Organization + Workloads OU)
./scripts/aws-accounts/create-account.sh \
  --account-name gateway \
  --email aws+gateway@diyaccounting.co.uk

# Step 2: Configure SSO access for the new account in IAM Identity Center
#         (manual — add a permission set in the AWS console)

# Step 3: Bootstrap CDK + OIDC + IAM roles
./scripts/aws-accounts/bootstrap-account.sh \
  --account-id <NEW_ACCOUNT_ID> \
  --account-name gateway \
  --profile gateway \
  --github-repo support-at-diyaccounting/www.diyaccounting.co.uk

# Step 4: Create repo, set GitHub variables, update root trust policy
./scripts/aws-accounts/setup-github-repo.sh \
  --account-id <NEW_ACCOUNT_ID> \
  --account-name gateway \
  --repo support-at-diyaccounting/www.diyaccounting.co.uk

# Step 5: Clone, initialise, and deploy
gh repo clone support-at-diyaccounting/www.diyaccounting.co.uk
cd www.diyaccounting.co.uk
# Update cdk.json, README.md, CLAUDE.md, workflow variable names
npm install && ./mvnw clean verify && npm run cdk:synth
git push origin main
gh workflow run deploy.yml --ref main
```

### What Each Script Does

**`create-account.sh`** — Creates an AWS account and moves it to the correct OU:

- Creates the account in the AWS Organization
- Polls until creation completes
- Moves the account from the root to the Workloads OU (or other specified OU)
- Outputs the new account ID and next steps

**`bootstrap-account.sh`** — Bootstraps a new account for CDK and GitHub Actions:

- CDK bootstraps `us-east-1` and `eu-west-2` (idempotent, skips if already done)
- Creates the GitHub OIDC identity provider (`token.actions.githubusercontent.com`)
- Creates `<name>-github-actions-role` with OIDC trust scoped to the repo
- Creates `<name>-deployment-role` trusted by the actions role (with AdministratorAccess)

**`setup-github-repo.sh`** — Creates the repository and configures GitHub:

- Creates the repository from the template (or skips if it exists)
- Sets GitHub repository variables (OIDC role ARNs + root account constants)
- Creates the `prod` GitHub environment
- Updates `root-github-actions-role` trust policy to allow the new repo

**`export-root-zone.sh`** — Exports the Route53 hosted zone for reference:

- Full JSON export of all DNS records
- BIND-format zone file (human-readable)
- Extracts manually-managed records (email, domain verification)

**`cleanup-zone.sh`** — Removes orphaned DNS records from old deployments:

- Identifies CI branch records and old prod commit-hash records
- Cross-references against live CloudFront distributions
- Generates Route53 change batch and applies deletions (with confirmation)

### Manual Steps After Scripts

1. **Configure SSO access** — Add a permission set for the new account in IAM Identity Center (console only)
2. **Add protection rules** — In the new repo's Settings > Environments > prod, add required reviewers
3. **Initialise the repository** — Update template files with account-specific values:
   - `cdk.json` — account ID, service name, stack-specific context
   - `README.md` — account details, build/deploy commands, GitHub variables
   - `CLAUDE.md` — account structure, build commands, coding conventions
   - GitHub Actions workflows — role variable names and stack names
4. **Update root DNS** — After first deployment, run root repo's `deploy.yml` to point DNS at the new service

### Naming Conventions

| Resource                  | Pattern                                  | Example                            |
| ------------------------- | ---------------------------------------- | ---------------------------------- |
| AWS Account name          | `<service>`                              | `gateway`                          |
| Repository                | `support-at-diyaccounting/<service>.diyaccounting.co.uk` | `support-at-diyaccounting/www.diyaccounting.co.uk` |
| OIDC actions role         | `<service>-github-actions-role`          | `gateway-github-actions-role`      |
| Deployment role           | `<service>-deployment-role`              | `gateway-deployment-role`          |
| CDK stack prefix          | `{env}-{service}-`                       | `prod-gateway-GatewayStack`        |
| CI domain                 | `ci-<service>.diyaccounting.co.uk`       | `ci-gateway.diyaccounting.co.uk`   |
| Prod domain               | `prod-<service>.diyaccounting.co.uk`     | `prod-gateway.diyaccounting.co.uk` |
| GitHub variable (actions) | `<SERVICE>_ACTIONS_ROLE_ARN`             | `GATEWAY_ACTIONS_ROLE_ARN`         |
| GitHub variable (deploy)  | `<SERVICE>_DEPLOY_ROLE_ARN`              | `GATEWAY_DEPLOY_ROLE_ARN`          |

## Related Repositories

| Repository                                  | Purpose                                                |
| ------------------------------------------- | ------------------------------------------------------ |
| `support-at-diyaccounting/submit.diyaccounting.co.uk`       | Submit application (Lambda, Cognito, DynamoDB, API GW) |
| `support-at-diyaccounting/www.diyaccounting.co.uk` (future) | Gateway static site                                    |
| `support-at-diyaccounting/spreadsheets.diyaccounting.co.uk` (future)          | Spreadsheets static site                               |

## AWS CLI Access

Use SSO profiles:

```bash
aws sso login --sso-session diyaccounting
aws --profile management route53 list-hosted-zones
aws --profile management cloudformation describe-stacks --stack-name root-RootDnsStack --region us-east-1
```

## License

AGPL-3.0
