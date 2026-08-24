# NEXT — current state & kickoff

Living handover for this repository. Rules and shape: `../NEXT.md` (DONE or OPEN only, nothing
deferred; a bug found fixing item A is A's remainder, not a new item; this file holds ONLY what
to do next — completed work lives in `git log`). Plans of record: `PLAN_*.md` at this root.

## Open items

- [ ] **Holding-page architecture — code complete, AWS cutover remains.** `ApexStack` is wired
      into `RootEnvironment.java` for `ci-root-ApexStack` and `prod-root-ApexStack`, deployable
      through `deploy.yml`; the prod holding domain is `holding.diyaccounting.co.uk` (renamed
      from `prod-holding.diyaccounting.co.uk`). What remains, all mutating AWS operations needing
      operator approval: deploy the two stacks, cut the Route53 aliases over to the new
      management-account distributions, re-run `deploy-holding.yml` to confirm it finds them, then
      decommission the old distributions in submit-ci (`E22NFQM9UZRBRC`) and submit-prod
      (`E2IWOXX8ANG33N`). See `../PLAN_HOLDING_ARCHITECTURE.md`.

## Discipline

(none repo-specific yet — see `../NEXT.md`)
