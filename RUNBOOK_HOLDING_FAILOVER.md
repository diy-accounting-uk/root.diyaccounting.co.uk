# Holding Failover Runbook

Procedure for moving live traffic onto a maintenance page and back, across all four
`diyaccounting.co.uk` services. Architecture and design rationale live in
`PLAN_HOLDING_ARCHITECTURE.md`; this document is the operational procedure only.

## What this repo owns

This repo hosts the apex holding page in the management account (887764105431) and, because the
gateway CloudFront distribution serves the apex, `www` and `prod-gateway` names off one
distribution, this repo also runs the gateway's failover. `www.diyaccounting.co.uk` has no
failover workflow of its own — everything for the gateway domains happens here.

Two CloudFront distributions, one per environment, both `ApexStack`:

| Environment | Stack                 | Holding domain                   | `OriginFor` tag                  | Live domains it takes over                                                           |
| ----------- | --------------------- | -------------------------------- | -------------------------------- | ------------------------------------------------------------------------------------ |
| prod        | `prod-root-ApexStack` | `holding.diyaccounting.co.uk`    | `holding.diyaccounting.co.uk`    | `diyaccounting.co.uk`, `www.diyaccounting.co.uk`, `prod-gateway.diyaccounting.co.uk` |
| ci          | `ci-root-ApexStack`   | `ci-holding.diyaccounting.co.uk` | `ci-holding.diyaccounting.co.uk` | `ci-gateway.diyaccounting.co.uk`                                                     |

Each distribution serves a single static page titled "Maintenance – DIY Accounting" with the
heading "We'll be right back". Every response carries a `Server: DIY-Accounting` header. A 403 or
404 on the distribution also renders the holding page, so deep links fail over cleanly instead of
showing an S3 error.

The certificate is self-issued and DNS-validated against the management hosted zone
(`Z0315522208PWZSSBI9AL`), with the live domains above as its subject alternative names. There is
no bootstrap workflow to run for this repo — CDK issues and validates the certificate on first
deploy of `ApexStack`, because this account can resolve the zone directly.

## When to fail over

This is a last resort, for when the site cannot be fixed quickly or is under attack. It is not a
performance measure and not a fix for one broken route — it replaces the whole site with a static
page.

Reach for it when a deploy has left the origin broken and rolling forward is not immediate, when
an attack is in progress, or when an AWS incident is affecting the origin.

What makes it worth having: the holding page is a static file in S3 behind CloudFront, with no
dynamic AWS deployment anywhere in the request path. There is nothing to inject into, because
nothing is served from a database or a function, and CloudFront absorbs volume that would
overwhelm an origin. So it stays up in exactly the situations that take the real site down.

Expect the switch to be slow, and accept it. Users cannot work either way while the site is
broken, so several minutes of blank responses during the cutover costs nothing that the incident
has not already cost.

## Who authorises

The operator. There is no standing delegation — nobody dispatches `deploy-holding.yml` without
the operator's direct instruction.

## How to fail over

Dispatch `deploy-holding.yml` on this repo:

- `target`: `holding`
- `environment-name`: `ci` or `prod` (or `(auto)`, which derives `prod` from `main` and `ci`
  otherwise)

The workflow strips the live aliases (`diyaccounting.co.uk`, `www.diyaccounting.co.uk`,
`prod-gateway.diyaccounting.co.uk` for prod) off the gateway distribution in the gateway account
(283165661847), waits for that change to deploy, then adds those same aliases to the apex holding
distribution in the management account and waits again. Finally it UPSERTs the Route53 A/AAAA
records for the live domains to alias the holding domain. No DNS TTL wait is involved — the alias
record change is what a resolver sees on its next lookup.

## Expected time to take effect

Around 20 minutes, dominated by two `aws cloudfront wait distribution-deployed` calls. Measured on
a real ci exercise, not estimated.

**The live domain serves nothing for most of that window.** CloudFront enforces alias uniqueness
globally, so the name has to be fully released from the live distribution and propagated before
the holding distribution can claim it. There is no overlap and no progressive cutover: the site
goes blank, then the holding page appears. That is inherent to the mechanism, not a fault.

## How to fail back

Dispatch `deploy-holding.yml` again with `target: restore` and the same `environment-name`. This
reverses both steps: the live aliases come off the holding distribution and go back onto the
gateway distribution, then the Route53 records are UPSERTed back to alias the gateway
distribution directly.

Confirm the live aliases are back on the gateway distribution:

```bash
aws cloudfront get-distribution --id <gateway-distribution-id> \
  --query 'Distribution.DistributionConfig.Aliases.Items'
```

## How to verify while failed over

```bash
curl -sI https://diyaccounting.co.uk/
```

Expect `200`, a `Server: DIY-Accounting` header, and a body that is the holding page, not the live
site.

## Rehearsal

Exercise the `ci` environment twice a year. Never rehearse against `prod`.

## Deploying while failed over

`deploy.yml` refuses to run while a failover is live. Before touching `RootDnsStack` or either
`ApexStack`, it checks both holding distributions: if a holding distribution carries any alias
other than its own holding domain, that means it is currently serving live traffic, and the run
stops with an error naming which domains are claimed.

This guard exists because a normal deploy would otherwise undo the failover in both directions at
once: `RootDnsStack` would UPSERT the live records back to the gateway distribution, and
`ApexStack` would reset the holding distribution's aliases to just its own holding domain —
leaving the site reachable from neither. If you hit this error, run `deploy-holding.yml` with
`target: restore` for that environment first, confirm the live aliases are back on the gateway
distribution, then re-run `deploy.yml`.

## Certificate coverage for new domains

Failover adds a live domain as an alias to the holding distribution, and CloudFront refuses an
alias the distribution's certificate does not cover. If a new domain is added to the gateway's
live traffic (a new subdomain, a new brand name), it must be added to `ApexStack`'s live domain
list (`RootEnvironment.apexFailoverDomainNames`) before it can ever be failed over — otherwise
`deploy-holding.yml` fails at the `update-distribution` step for that domain. Because the
certificate is self-issued and DNS-validated, redeploying `ApexStack` after adding the domain
reissues and revalidates it automatically; no manual certificate request is needed.

## SSM parameter naming convention

One convention, used by every service in this workspace: `/<service>/<env>/last-known-good-deployment`,
where `<service>` is `gateway`, `spreadsheets` or `submit`, and `<env>` is `ci` or `prod`. The
parameter lives in the service's own AWS account, type `String`, and is written only by that
service's own `deploy.yml` after a green smoke test.

| Service        | Parameter                                        | Written by                                      | Value                                 | Consumed by                                                                                      |
| -------------- | ------------------------------------------------ | ----------------------------------------------- | ------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `submit`       | `/submit/<env>/last-known-good-deployment`       | `submit.diyaccounting.co.uk` `deploy.yml`       | deployment name (e.g. `prod-2078c71`) | `submit.diyaccounting.co.uk` `set-origins.yml` (`domain-source: last-known-good`)                |
| `spreadsheets` | `/spreadsheets/<env>/last-known-good-deployment` | `spreadsheets.diyaccounting.co.uk` `deploy.yml` | commit SHA                            | not yet consumed — spreadsheets fails back with `deploy-holding.yml`'s `target: restore` instead |
| `gateway`      | `/gateway/<env>/last-known-good-deployment`      | not yet written                                 | commit SHA                            | not yet consumed — gateway fails back with this repo's `deploy-holding.yml` `target: restore`    |

Only submit deploys a new CloudFront distribution per commit, so only submit has a meaningful
last-known-good value to redeploy at. Gateway and spreadsheets each run one long-lived
distribution per environment, so their reverse operation is `restore`, which puts the live
aliases back on the distribution they came from rather than rolling forward to a named
deployment. Do not namespace this parameter by deployment or by region — the deploy tooling in
the submit repo already reads this exact path.
