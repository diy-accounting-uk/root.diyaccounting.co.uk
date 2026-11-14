---
name: orient-archive
description: Read before any change, test or plan that touches ../diy-accounting-archive (the pre-migration spreadsheets repository, packages-published cuts, MANIFEST.toml, archive-packages.js): it is history, not a place to develop; which of its docs to follow in place of this repository's CLAUDE.md.
---
<!-- SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0 -->
<!-- Copyright (C) 2006-2026 DIY Accounting Limited -->

# orient-archive — the archive repository is history, not a place to develop

`../diy-accounting-archive` is the pre-migration spreadsheets repository. Its live successor is `../spreadsheets.diyaccounting.co.uk`. Product work goes there.

The rules of that repository win over the rules of the repository you started in, wherever they differ.

## Read first, in order

1. `../diy-accounting-archive/CLAUDE.md`, sections "What this repository is" and "`packages-published/`".
2. `../spreadsheets.diyaccounting.co.uk/.claude/skills/archive-packages/SKILL.md`, the procedure that adds a cut to `packages-published/`.
3. `../diy-accounting-archive/NEXT.md` (no open items; it says why).
4. `../diy-accounting-archive/.claude/skills/`: `do-next`, `plain-prose`.

## Facts that trip a session from another repository

- Nothing deploys from here. There is no deploy workflow.
- `test.yml` and `generate-bst.yml` run on every branch push and exercise the committed data only.
- `MANIFEST.toml` in `packages-published/` is written by `archive-packages.js` and checked by its `--verify` mode. Do not edit it by hand.
- A cut arrives as one reviewed commit, taken from the live repository's generated packages.
- Documentation and layout maintenance are fine here. New features, fixes and package edits are not.
- The corpus index holds this repository's commits only, under the source name `archive`. Its file content is superseded by `spreadsheets`.
- The GitHub archived flag is not set. Do not set it.
