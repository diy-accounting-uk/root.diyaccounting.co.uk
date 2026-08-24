---
name: plain-prose
description: Holds this repo's writing rules for plain, human prose and the LLM-voice tells to cut. Load it before writing any human-facing text — docs, code comments, or chat replies.
---

# plain-prose — write plain, human prose; keep the proof out of the shop window

The stock LLM writing voice reads as generic and machine-made. This skill is the standing style
guide for this repo. It has two jobs: make prose read as if a person wrote it, and keep the
supporting evidence out of the way so reader-facing surfaces stay short enough that someone
actually reads them.

**Scope: everything written for a human reader** — `README.md`, `AWS_RESOURCES.md`,
`GITHUB_SETUP.md`, the `PLAN_*.md` design docs, other skill docs, code comments, and the
assistant's own chat responses. This repo is DNS/CDK infrastructure only — Route53 records, the
holding page, cross-account delegation — no application code, so there is no product-facing
"shop window" of the kind a SaaS README has; the same discipline still applies to keeping
`README.md` and `AWS_RESOURCES.md` short and pushing detail into `PLAN_*.md`.

The base rules in section 1 are the Plain English Campaign's, who have promoted plain English and
fought gobbledygook since 1979 (plainenglish.co.uk). Section 2 adds the LLM-voice tells to cut on
top of them.

---

## 1. Plain English base rules

The foundation. Apply these before worrying about anything else.

- **Short sentences. Average 15–20 words.** Mix short and longer, but if a sentence runs past ~25
  words, split it. One long clause-stacked sentence is the most common wordiness fault.
- **One idea per sentence** (plus perhaps one closely related point). If you are joining two ideas
  with a dash or a semicolon, they usually want to be two sentences.
- **Active voice, not passive.** "The stack creates the alias record," not "the alias record is
  created by the stack." Passive hides who does what and adds words.
- **Everyday words.** Use the simplest word that fits. Cut jargon a first-time reader can't parse,
  or define it in three words the first time.
- **Write to the reader as "you"; call ourselves "we".** "You run `./mvnw clean verify`," not "the
  build is run by the user."
- **Cut nominalisations** (an abstract noun hiding a verb). "We discussed it," not "we had a
  discussion about it." "It fails," not "it results in a failure."
- **Use lists** when you have three or more parallel points. A bullet list scans; a comma-spliced
  sentence does not.
- **Cut every word that earns nothing.** Delete redundant openers ("It is important to note that",
  "In order to"), doubled words ("each and every"), and filler adverbs.

Common substitutions (Plain English Campaign's A-to-Z, the ones that recur here):

| instead of | write |
| --- | --- |
| additional | extra |
| commence / initiate | start |
| ensure | make sure |
| in excess of | more than |
| prior to | before |
| subsequent to | after |
| terminate | end |
| utilise | use |
| in order to | to |
| approximately | about |
| demonstrate | show |
| sufficient | enough |
| require | need |
| regarding / with regard to | about |
| whilst | while |
| in the event that | if |

---

## 2. The LLM-voice tells to cut

On top of the Plain English rules, scan every draft for these machine-voice fingerprints and
remove them.

- **Em-dash sprinkling as fake sophistication.** Do not bolt clauses together with `—`. Use a
  period, a comma, or restructure. Reserve em-dashes for rare, deliberate use.
- **The "not X, it's Y" / "not X but Y" / "not only X but also Y" negation-contrast.** State what
  the thing is, not what it isn't. "The zone has one alias record," not "it is not a complex
  setup."
- **Announced-honesty preambles.** Drop "honest current state:", "to be clear," "reported
  honestly." Just report the thing.
- **Colon reveals.** Avoid the dramatic setup-then-colon. Write a plain subject-verb sentence.
- **Anthropomorphizing tools and stacks.** A CDK stack does not "want," a deploy does not
  "struggle." Say what it did or measured.
- **Rule-of-three padding, hedging, and hype.** Cut "powerful", "seamless", "robust", "in the
  ever-evolving landscape", "it's worth noting", "delve", and the reflexive three-item list where
  one item does the job.
- **Listicle bloat and promotional filler.** Don't inflate two real points into a bulleted five.
  Don't restate the headline three ways. One concrete claim beats three decorated ones.

Default to short declarative sentences a person would write. Say the thing once, plainly.

---

## 3. Proofs and evidence: keep the shop window short

`README.md` is this repo's front door — the account structure, domain convention, and what this
repo does and does not manage. It is not the place to reproduce every operational detail.

- Lead with what the repo manages, in one or two short sentences.
- One small table at most (the account structure and domain convention tables already earn their
  place). Don't stack more tables and caveat paragraphs on top.
- Push detail into `AWS_RESOURCES.md` (the concrete stack/resource inventory), `GITHUB_SETUP.md`
  (the one-time GitHub setup steps), and `PLAN_*.md` (design detail behind work in progress).
  `CLAUDE.md` is not reader-facing; it is working guidance for a Claude Code session, so this rule
  applies more loosely there.

The rule in one line: **the claim lives in the window, the proof lives in the back room, and a
link connects them.**

---

## 4. Related principles (same spirit)

- **No delta-framing.** Describe the work on its own terms. Don't frame a change as a rebuttal to
  a prior approach.
- **Dependency pragmatism.** Never frame work around avoiding dependencies. State what a choice
  does positively.
- **"NOT" sections stay factual.** `CLAUDE.md`'s "What this repo does NOT have" note is fine
  because each line states a positive scope decision (DNS and a holding page, not application
  infrastructure). Keep those grounded.

All three are the same instinct as this skill: say what the thing is, positively and plainly,
without scaffolding it against something else.

---

## 5. Workflow — edit before you ship

After drafting any human-facing text:

1. **Cut length first.** Split every sentence over ~25 words. Delete redundant openers and filler.
   Turn a three-plus-point sentence into a list. Run the substitution table over it.
2. **Cut the tells.** Search for `—`, "not just", "not only", "not X, it's Y", "honest"/
   "transparent" self-labels, "delve", "it's worth noting", and hype adjectives. Remove each one.
3. **Read it as a stranger.** If a clause sounds like a press release or a model's default voice,
   rewrite it as the sentence a person would say out loud.
4. **On `README.md`, check the order.** What this repo manages first, then the account/domain
   structure, then a link to `AWS_RESOURCES.md` for the resource-level detail.
5. **Match the surrounding voice.** A paragraph that suddenly turns formal and three-adjectived is
   a tell even if every word is fine.

This applies to the assistant's own chat responses too, not only the artefacts it produces.

---

## 6. One-paragraph TL;DR

Write plain, direct prose a person would recognise as human. Short sentences (15–20 words), one
idea each, active voice, everyday words, "you"/"we", no nominalisations, lists for parallel
points. On top of that, cut the LLM tells: em-dash sprinkling, "not X it's Y", announced-honesty,
colon reveals, anthropomorphized tools, hype, rule-of-three padding, listicle bloat. On
`README.md`, lead with what the repo manages and link to `AWS_RESOURCES.md`/`GITHUB_SETUP.md`/
`PLAN_*.md` for the detail rather than reproducing it there. Base rules are the Plain English
Campaign's (plainenglish.co.uk). Applies to docs, code comments, and chat.
