#!/usr/bin/env node
// SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
// Copyright (C) 2006-2026 DIY Accounting Limited

// scripts/check-action-pins.mjs
//
// Every external `uses: owner/repo@ref` in .github/workflows and .github/actions must name a
// 40-character commit SHA, with the version as a trailing comment. A tag can move; a SHA cannot.
// Local actions (./...) and docker:// references are exempt.
//
// Usage: node scripts/check-action-pins.mjs [directory ...]   (default: .github)

import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

const USES_LINE = /^\s*(?:-\s+)?uses:\s*["']?([^\s"'#]+)["']?\s*(#.*)?$/;
const SHA_REF = /^[^@]+@[0-9a-f]{40}$/;

function workflowFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return workflowFiles(path);
    return /\.ya?ml$/.test(entry.name) ? [path] : [];
  });
}

export function unpinnedRefs(text) {
  const found = [];
  text.split("\n").forEach((line, index) => {
    const match = USES_LINE.exec(line);
    if (!match) return;
    const ref = match[1];
    if (ref.startsWith("./") || ref.startsWith("docker://")) return;
    if (!SHA_REF.test(ref)) found.push({ line: index + 1, ref });
    else if (!match[2])
      found.push({ line: index + 1, ref: `${ref} (no version comment)` });
  });
  return found;
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const dirs = process.argv.slice(2).length
    ? process.argv.slice(2)
    : [".github"];
  let bad = 0;
  let total = 0;
  for (const dir of dirs) {
    for (const file of workflowFiles(dir)) {
      const text = readFileSync(file, "utf8");
      total += text.split("\n").filter((l) => USES_LINE.test(l)).length;
      for (const { line, ref } of unpinnedRefs(text)) {
        console.error(`${file}:${line}: not pinned to a commit SHA: ${ref}`);
        bad++;
      }
    }
  }
  if (bad) {
    console.error(`${bad} unpinned action reference(s)`);
    process.exit(1);
  }
  console.log(
    `All external action references are pinned to a commit SHA (${total} uses lines checked)`,
  );
}
