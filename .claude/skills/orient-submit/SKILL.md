---
name: orient-submit
description: Read before any change, test, workflow, plan or board row in ../submit.diyaccounting.co.uk (Lambda app, CDK, HMRC and Companies House submissions, Cognito, behaviour tests, NEXT.md and the PLAN files, deploy.yml, ci and prod sets): which of that repository's docs and skills to follow in place of this repository's CLAUDE.md.
---
<!-- SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0 -->
<!-- Copyright (C) 2006-2026 DIY Accounting Limited -->

# orient-submit — work in the submit repository by its own rules

`../submit.diyaccounting.co.uk` is the VAT, Income Tax and Companies House submission app (Lambda, DynamoDB, Cognito, CDK in Java) and the home of the one work board for every repository.

The rules of that repository win over the rules of the repository you started in, wherever they differ.

## Read first, in order

1. `../submit.diyaccounting.co.uk/CLAUDE.md`, sections "Context Survival", "NEXT.md holds open work only", "Git Workflow".
2. `../submit.diyaccounting.co.uk/NEXT.md` (the board) and the `PLAN_*.md` files at that root.
3. `../submit.diyaccounting.co.uk/REPORT_CAPABILITIES.md`, before you add a script, workflow, check or skill (the `capabilities` skill says how).
4. The skill for the job: `do-next`, `iterate`, `watch`, `auto-merge`, `fast-deploy`, `visual-review`, `plain-prose` in `../submit.diyaccounting.co.uk/.claude/skills/`.

## Facts that trip a session from another repository

- Test commands: CLAUDE.md "Test Commands" and "Four-Tier Testing Pyramid". `npm test` takes about 80 seconds; behaviour tests have their own rules in "Active Test Monitoring".
- Run only the blast radius mid-task; the full suite runs before a push (Shared conventions, "Test the blast radius").
- The pre-push hook runs a secret scan only. CI runs the tests.
- Deploy: a push to a branch deploys a ci set; the full cycle is in "Deployment & Infrastructure Workflow". A second deploy of one branch needs an explicit deployment name (same section).
- Only ci-set1, ci-set2 and the apex sign in on ci: memory note "Named ci deploys cannot sign in".
- Bots push to main from `deploy.yml`, `publish.yml`, `sync-plugin-repo.yml`, `alarm-triage.yml` and `dashboard-review-nightly.yml`. Pull before you commit.
- A merge goes only through `/auto-merge`. Never run a bare `gh pr merge` (CLAUDE.md "Git Workflow").
- `git worktree remove`, `git branch -D` and an SSO login are the operator's: "Worktree and branch removal is the operator's".
- A change a person sees is done only after the visual steps: "Definition of done for anything a person sees".
- Ask before any AWS write; never scan a customer table; use the Athena lake: "AWS Write Operations" and the memory note "Never scan customer tables".
- `NEXT.md` holds open work only. Delete a row in the commit that closes it.
