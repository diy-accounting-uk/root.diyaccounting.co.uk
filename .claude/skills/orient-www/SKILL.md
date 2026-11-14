---
name: orient-www
description: Read before any change, test, workflow or plan in ../www.diyaccounting.co.uk (the gateway static site for diyaccounting.co.uk, its CloudFront redirects, GatewayStack, deploy.yml, the holding-page failover): which of that repository's docs and skills to follow in place of this repository's CLAUDE.md.
---
<!-- SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0 -->
<!-- Copyright (C) 2006-2026 DIY Accounting Limited -->

# orient-www — work in the gateway repository by its own rules

`../www.diyaccounting.co.uk` is the gateway static site for www.diyaccounting.co.uk (S3, CloudFront, a redirect function, one CDK stack) in the gateway AWS account. It is the simplest CDK static site template here.

The rules of that repository win over the rules of the repository you started in, wherever they differ.

## Read first, in order

1. `../www.diyaccounting.co.uk/CLAUDE.md`, sections "Context Survival", "Quick Reference", "Testing", "Deployment".
2. `../www.diyaccounting.co.uk/NEXT.md`, the open work of that repository.
3. `../www.diyaccounting.co.uk/RUNBOOK_HOLDING_FAILOVER.md` before any failover to the holding page.
4. `../www.diyaccounting.co.uk/TEMPLATE.md` when you copy the stack for a new static site.
5. `../www.diyaccounting.co.uk/.claude/skills/`: `do-next`, `plain-prose`.

## Facts that trip a session from another repository

- `npm test` runs vitest SEO and smoke tests only. Browser and behaviour tests are separate scripts: CLAUDE.md "Testing".
- Local behaviour runs skip the redirect tests, because CloudFront Functions do not run locally.
- `./mvnw clean verify` includes the Spotless check. Run `spotless:apply` only when asked: "Code Quality Rules".
- The pre-push hook runs a secret scan only. CI runs the tests.
- `deploy.yml` deploys GatewayStack on push to main and on manual dispatch: "Deployment". After a deploy, root's DNS workflow takes the new CloudFront domain.
- DNS records and the holding-page distribution are in `../root.diyaccounting.co.uk`; this repository has no Route53.
- The repository has no Lambda, DynamoDB, Cognito, HMRC or Stripe code: "Quick Reference".
- Ask before any AWS write; the account is gateway (283165661847), SSO profile `diya-gateway`: "AWS CLI Access".
