---
name: orient-spreadsheets
description: Read before any change, test, workflow or plan in ../spreadsheets.diyaccounting.co.uk (Excel packages, generate-*.yml workflows, reconciliation checks, diya-gl and its npm release, the spreadsheets and diya-gl.co.uk sites, the npm test router, the pre-push hook): which of that repository's docs and skills to follow in place of this repository's CLAUDE.md.
---
<!-- SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0 -->
<!-- Copyright (C) 2006-2026 DIY Accounting Limited -->

# orient-spreadsheets — work in the spreadsheets repository by its own rules

`../spreadsheets.diyaccounting.co.uk` holds the Excel package pipeline, the spreadsheets and diya-gl.co.uk static sites (S3 and CloudFront) and the `diya-gl` npm package.

The rules of that repository win over the rules of the repository you started in, wherever they differ.

## Read first, in order

1. `../spreadsheets.diyaccounting.co.uk/CLAUDE.md`, sections "Context Survival", "Testing", "Reconciliation-bug method", "Commands only the operator can run", "Package Pipeline", "Deployment".
2. The `CONTEXT_*.md` file of the product you touch, in `../spreadsheets.diyaccounting.co.uk/`: `CONTEXT_BASIC_SOLE_TRADER.md`, `CONTEXT_TAXI.md`, `CONTEXT_SELF_EMPLOYED.md`, `CONTEXT_LIMITED_COMPANY.md`. The cell maps in them have been wrong before; check the XML.
3. `../spreadsheets.diyaccounting.co.uk/.claude/skills/excel/SKILL.md` for any workbook change.
4. `../spreadsheets.diyaccounting.co.uk/.claude/skills/package-updates/SKILL.md` for the annual tax data update.
5. `../spreadsheets.diyaccounting.co.uk/.claude/skills/archive-packages/SKILL.md` for a cut into `diy-accounting-archive`.
6. `../spreadsheets.diyaccounting.co.uk/.claude/skills/plain-prose/SKILL.md` before human-facing text.

## Facts that trip a session from another repository

- This repository's board and plans live in `../submit.diyaccounting.co.uk/NEXT.md` and its `PLAN_*.md` files. Its own `NEXT.md` only points there.
- `npm test` is the router `scripts/test-scope.mjs`. Name `npm test` and nothing narrower in a brief: CLAUDE.md "Testing".
- `TEST_SCOPE_TIERS` splits the tiers between CI jobs; `SKIP_LIBREOFFICE=1` makes the verdict PARTIAL: same section.
- `.githooks/pre-push` runs the router over everything the push adds. It can take up to about 55 minutes, so push with SSH keepalives (memory note "Spreadsheets pre-push runs the scoped suite"). `SKIP_PUSH_TESTS=1` runs the gates tier only.
- Reconciliation checks follow "Reconciliation-bug method". A check that compares a value with itself can never fail.
- `update.yml` runs nightly and pushes formatting and dependency commits to main. `publish-diya-gl.yml` pushes a version commit to main after a prod deploy. Pull before you commit.
- `deploy.yml` runs on push to main. DNS for the sites is in `../root.diyaccounting.co.uk`.
- Further changes you find here that are not in your brief: ask the operator one plain question (memory note "Spreadsheets is stable").
- Branch removal, SSO login and Stripe test keys are the operator's: "Commands only the operator can run".
