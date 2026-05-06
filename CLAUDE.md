# Claude Code Memory - DIY Accounting Root

> **Shared conventions** (git workflow, AWS accounts, code quality, confirm behavior, security): See `../CLAUDE.md`

## Context Survival (CRITICAL — read this first after every compaction)

**After compaction or at session start:**

1. Read all `PLAN_*.md` files in the project root — these are the active goals
2. Run `TaskList` to see tracked tasks with status
3. Do NOT start new work without checking these first

**During work:**

- When the user gives a new requirement, add it to the relevant `PLAN_*.md` or create a new one
- Track all user goals as Tasks with status (pending -> in_progress -> completed)
- Update `PLAN_*.md` with progress before context gets large

**PLAN file pattern:**

- Active plans live at project root: `PLAN_<DESCRIPTION>.md`
- Each plan has user assertions verbatim at the top (non-negotiable requirements)
- Plans track problems, fixes applied, and verification criteria
- If no plan file exists for the current work, create one before starting
- Never nest plans in subdirectories — always project root

## Quick Reference

This repository manages the **root AWS account** (887764105431) for diyaccounting.co.uk:

- **Route53 hosted zone** for `diyaccounting.co.uk` (all DNS records)
- **RootDnsStack**: Alias records pointing to gateway/spreadsheets CloudFront distributions
- **Holding page**: Maintenance page at `{env}-holding.diyaccounting.co.uk`
- **Cross-account delegation role**: `root-route53-record-delegate` for submit accounts

**What this repo does NOT have**: Lambda, DynamoDB, Cognito, API Gateway, Docker, ngrok, HMRC, Stripe, or any application code.

## Git Workflow

See `../CLAUDE.md` for full rules. Branch naming: `claude/<short-description>`.

## Build Commands

```bash
npm install                    # Install CDK CLI and dev dependencies
./mvnw clean verify            # Build CDK JARs
npm run cdk:synth              # Synthesize CloudFormation templates
npm run cdk:diff               # Show pending changes
```

## Development Tools

```bash
npm run formatting             # Check formatting (Prettier + Spotless)
npm run formatting-fix         # Auto-fix formatting
npm run lint:workflows         # Validate GitHub Actions workflow syntax (uses actionlint)
npm run update-to-minor        # Update npm dependencies (minor versions)
npm run update-to-greatest     # Update npm dependencies (latest non-alpha)
npm run update:java            # Update Maven dependencies to latest versions
npm run update:node            # Update npm dependencies to latest non-alpha versions
npm run diagram:root           # Generate draw.io architecture diagram from CDK synth output
```

## CDK Architecture

**Single CDK application** (`cdk-root/`):

- Entry point: `RootEnvironment.java` -> `submit-root.jar`
- Stack: `root-RootDnsStack` (Route53 alias records + delegation role)

**Java packages** (`co.uk.diyaccounting.root`):

- `root` — `RootEnvironment.java` (entry point), `SubmitSharedNames.java` (shared config)
- `root.stacks` — `RootDnsStack.java`, `ApexStack.java`, `SubmitStackProps.java`
- `root.utils` — `Kind.java` (logging), `KindCdk.java` (CDK utilities), `Route53AliasUpsert.java` (DNS alias), `ResourceNameUtils.java` (name conversion)

## Formatting

- Spotless with Palantir Java Format (100-column width)
- Runs during Maven `install` phase
- Fix: `./mvnw spotless:apply` (only when asked)

## IAM Best Practices

- Follow least privilege principle
- Avoid `Resource: "*"` wildcards
- Use specific ARNs where possible

## Deployment

Deployments are triggered via GitHub Actions workflows:

| Workflow             | Purpose                                        | Trigger              |
| -------------------- | ---------------------------------------------- | -------------------- |
| `test.yml`           | Lint, format check, Maven verify, CDK synth    | Push, daily schedule |
| `deploy.yml`         | Deploy RootDnsStack (DNS records)              | Manual dispatch      |
| `deploy-holding.yml` | Switch apex to holding page or last-known-good | Manual dispatch      |

Both workflows use OIDC authentication with these GitHub repository variables:

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

Each OIDC actions role trusts both `repo:support-at-diyaccounting/root.diyaccounting.co.uk:*` and `repo:support-at-diyaccounting/submit.diyaccounting.co.uk:*`.

**GitHub Environment**: The `deploy-root-dns` job uses `environment: prod`. This environment must exist in Settings > Environments.

## AWS CLI Access

Use SSO profiles:

```bash
aws sso login --sso-session diyaccounting
aws --profile management route53 list-hosted-zones
aws --profile management cloudformation describe-stacks --stack-name root-RootDnsStack --region us-east-1
```

**Read-only AWS operations are always permitted.** Ask before any write operations.

## AWS Write Operations

See `../CLAUDE.md` — always ask before any mutating AWS operation.

## Confirm Means Stop and Wait

See `../CLAUDE.md` — present the command, STOP, wait for explicit approval before executing.

## Code Quality Rules

See `../CLAUDE.md` for shared rules. Root-specific: only run `./mvnw spotless:apply` when specifically asked.

## Security Checklist

See `../CLAUDE.md` for shared rules. Root-specific: Route53 delegation role is scoped to specific hosted zone only.
