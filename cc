#!/bin/bash
# SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
# Copyright (C) 2006-2026 DIY Accounting Limited
#
# cc: update Claude Code, then start it in this repository with every sibling
# repository of the workspace added. --add-dir gives file access and loads each
# sibling's skills; their CLAUDE.md files stay unloaded, so a session pays for a
# sibling's rules only when it invokes that sibling's orient-* skill.
# Arguments after cc pass through to claude: ./cc --model opus "fix the board"
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
workspace="$(dirname "$here")"
repos="submit.diyaccounting.co.uk spreadsheets.diyaccounting.co.uk www.diyaccounting.co.uk root.diyaccounting.co.uk diy-accounting-archive private.diyaccounting.co.uk"

add_dirs=()
for repo in $repos; do
  dir="$workspace/$repo"
  if [ "$dir" != "$here" ] && [ -d "$dir" ]; then
    add_dirs+=(--add-dir "$dir")
  fi
done

unset CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD
claude update
cd "$here"
exec claude ${add_dirs[@]+"${add_dirs[@]}"} "$@"
