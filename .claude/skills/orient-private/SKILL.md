---
name: orient-private
description: Read before reading, writing or moving any file in ../private.diyaccounting.co.uk (regulator correspondence, strategy, finance, security findings, session reports, archived engineering plans, operator briefs): that repository has no CLAUDE.md, so this says which workspace docs apply.
---
<!-- SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0 -->
<!-- Copyright (C) 2006-2026 DIY Accounting Limited -->

# orient-private — the private documents repository

`../private.diyaccounting.co.uk` holds documents only: correspondence, evidence, strategy, finance packs, security reviews, session reports and archived engineering plans. It has no build, no tests and no workflows.

It has no CLAUDE.md. The workspace rules in `../CLAUDE_WORKSPACE.md` apply, and they win over the rules of the repository you started in, wherever they differ.

## Read first, in order

1. `../CLAUDE_WORKSPACE.md`, sections "Context Survival", "Workspace Structure", "Git Workflow", "Writing style".
2. `../private.diyaccounting.co.uk/README.md`, the folder map and the list of what lives elsewhere.
3. The folder for the job: `sessions/` (session reports), `security/` (findings), `operator/` (briefs, runbook), `engineering/` (archived plans and backlog), `strategy/`, `finance/`, `hmrc/`, `companies-house/`.

## Facts that trip a session from another repository

- Documents that give too much away (identifiers, correspondence, security findings, finance, strategy) are written here. They never go in a public repository or loose at the workspace root.
- Secrets and credentials are never written here. Run `git config core.hooksPath .githooks` once per clone; the hooks run a secret scan on commit and push.
- A commit that touches only `.md` files may be pushed straight to `main` (Shared conventions, "Git Workflow").
- Plans and the board are not here. They are `PLAN_*.md` and `NEXT.md` in `../submit.diyaccounting.co.uk`.
- Session reports use the name `REPORT_SESSION_SUBMIT_<id>_<date>.md` in `sessions/`.
- Source data for the books is in `../staging/`, the Drive mirror in `../drive/`, mail in `../mail/`: README "Not here".
- No Actions run here. Nothing is deployed from this repository.
