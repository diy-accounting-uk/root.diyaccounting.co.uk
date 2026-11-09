// SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
// Copyright (C) 2006-2026 DIY Accounting Limited
//
// install.mjs — point git at the tracked hooks in .githooks. Never fails an install.

import { spawnSync } from "child_process";

const r = spawnSync("git", ["config", "core.hooksPath", ".githooks"], { encoding: "utf8" });
console.log(r.status === 0 ? "hooks: core.hooksPath set to .githooks" : "hooks: not a git working tree, core.hooksPath left alone");
