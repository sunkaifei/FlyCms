---
name: avoid-ai-design
description: Audit and rewrite frontend UI to remove generic AI design patterns ("AI slop"), both the first-order defaults (purple gradients, Inter, a centered hero over three cards, untouched shadcn) and the second-order "tasteful AI" ones (cream with terracotta, near-black with acid green, all-caps mono chrome, one accented headline word, decorative 01/02/03). Use when asked to "de-slop a UI", "make a design look less AI-generated", "audit a page or component for AI design tells", "fix Claude/Codex-generated frontend that looks generic", or keep a multi-page site on one design system. Covers HTML/CSS and React/Tailwind/shadcn. Has a detection-only mode and a zero-dependency scanner (scripts/detect.mjs) for the code-certain tells.
license: MIT
compatibility: Works in any agent that reads agentskills.io SKILL.md files (Claude Code, Cursor, Codex CLI, VS Code Copilot and others). Optional, Node 18+ to run scripts/detect.mjs, and a screenshot tool to judge the visual tells.
metadata:
  author: ungspirit
  version: "0.4.0"
  tags: design ui frontend ai-slop tailwind shadcn react
  agentskills_spec: "1.0"
---

# Avoid AI Design: Audit & Rewrite

You are reviewing frontend code to find the patterns that make a UI look AI-generated ("AI slop"), then rewriting it around one intentional design direction.

This is the design counterpart to the `avoid-ai-writing` skill. It targets two stacks: plain **HTML/CSS/JS** and **React + Tailwind + shadcn/ui**.

The point is not to chase novelty. It is to replace defaults with decisions. A purple gradient is not bad because purple is bad; it is bad because no one chose it.

**There are two orders of default.** The first is the median: purple gradient, Inter, three icon cards. The second is the tasteful escape that became the new median: cream with terracotta, near-black with one acid-green accent, all-caps mono labels, one colored word in the headline, decorative 01/02/03. Anthropic's own `frontend-design` skill lists these clusters, and de-slop passes (this skill's included) land in them constantly. Treat both orders as tells. See the SD section of `references/ai-tells-catalog.md`.

**Code shows half of it; pixels show the rest.** Some tells live in the source (a literal `from-indigo-500`, `Inter`, a `lucide` import); the scanner finds those. Others are visual: whether a palette has a real dominant color, whether spacing has rhythm, whether the structure is the same as every other page. Render when you can. When you cannot, say which findings are code-certain and which are inferred.

## Modes

**`rewrite`** (default): Audit the code, commit to a direction, then rewrite it.

**`detect`**: Audit and score only. No edits. Use this mode when:
- The user wants to see what is flagged and fix it themselves.
- You are reviewing code you should not change (a dependency, a teammate's work, a reference).
- The user asks for a quick scan.

Trigger `detect` when the user says "just audit", "flag only", "scan", "what's AI about this", or "don't change the code". Default to `rewrite`.

## The scanner

`scripts/detect.mjs` finds the code-certain tells (marked ⚙ in the catalog) with no model call and no dependencies. Run it first whenever Node 18+ is available:

```bash
node <skill-dir>/scripts/detect.mjs <files or folders>        # grouped report, exit 2 on any P0/P1
node <skill-dir>/scripts/detect.mjs --json <files or folders>  # for your own parsing
```

Treat its output as the code-certain half of the audit, not the whole audit: it cannot see palette weight, rhythm, hierarchy, or structure-level sameness. It skips `examples/`, `fixtures/`, `stories/`, and files marked `slop-example` unless given `--include-examples`, and it honors `avoid-ai-design-ignore: <ID>` comments for choices the brief asked for. Without Node, walk the catalog by hand.

## Workflow (rewrite mode)

1. **Scope.** Read the actual files first. Establish what is under review (a component, a page, a whole app), the stack, and the mode. Look for a design system to stay consistent with: a `DESIGN.md` (Google's open format, also written by Stitch and other design skills), a tokens file or `:root`, a component library, sibling pages, or a font and color the user chose themselves. Note whether the work spans **more than one page**. Never judge from the prompt alone.
2. **Scan.** Run the scanner over the files in scope.
3. **Render.** If a screenshot or preview tool is available, render the artifact and judge the 👁 tells from pixels. Run the **silhouette test** on pages: reduce the screenshot to a small black-on-white block silhouette and ask whether it could be any other product's page (catalog L9). Squint at 25% for flat density. Without a render, mark visual tells as **inferred, lower-confidence**.
4. **Audit.** Merge the scan, the render, and a pass through every category in `references/ai-tells-catalog.md`, including the SD section. For each tell: location, ID, severity, one line on *why it reads as AI*, and whether it is code-certain, rendered, or inferred.
5. **Ground and plan.** If a design system exists, it *is* the direction: adopt it as a contract and match it exactly. Otherwise, derive the direction from the subject, not from a style menu. Ask what the product is, who uses it, and what its world is made of (materials, place, era, the domain's own artifacts and vernacular). Write a compact plan: 4-6 named colors with roles, the faces and their roles, the layout idea in a sentence, and the one signature detail. `references/aesthetic-directions.md` is vocabulary for naming moves, not a menu to pick from.
6. **Review the plan against the defaults.** Before touching code, check the plan against the second-order clusters (SD1-SD8) and your own recent passes. For each part, ask: would I arrive here for any similar page? If yes, revise it and say what changed. If a user is present, show the plan plus one or two alternatives in a sentence and pause. If you are running non-interactively, state the assumption in one line, proceed, and keep it easy to override.
7. **Calibrate depth.** A small component, or anything inside a design system, gets a **surgical** pass: swap the tells, keep the structure and the tokens. A standalone page gets a **rebuild** around the direction. A ground-up build overlaps with the `frontend-design` skill; if it is available, hand it the reviewed plan rather than duplicating its job.
8. **Rewrite.** Edit the real files. Preserve functionality, props, state, routing, data flow, accessibility, and the meaning of the copy. Do not add dependencies silently; name any you introduce. For multi-page work, write the plan into a `DESIGN.md` from `references/design-md-template.md` (or extend the existing one), and build every page from it.
9. **Re-scan and judge.** Run the scanner again: no P0, and no SD tell the brief did not ask for. Then judge the result against "What success means." A clean scan is **necessary but not sufficient**.

## What success means

"Less obviously AI" is not the goal. A token-swap (indigo to teal, Inter to a trendier face, drop the emoji) clears every P0 and still leaves a forgettable template. Judge the result against these tests:

1. **Justified.** Every change serves the committed direction, and the direction traces back to the subject.
2. **Coherent.** The type, palette, layout, and signature detail reinforce one another. One idea, executed. Spend the boldness in one place.
3. **Not a second-order default.** It does not land in an SD cluster the brief did not ask for, and it does not repeat your recent passes (the cream-and-terracotta move, the near-black-and-lime move, the mono-chrome move). If you would have produced it for any similar page, it failed.
4. **Consistent** (whenever it is not the only page). It shares its siblings' tokens, nav, footer, and type scale, so it reads as the same product. Vary *across* projects (test 3); never *within* one.

A page can pass the scanner and still fail every one of these. The catalog catches clichés; these tests catch mediocrity.

## Severity tiers

Tiers triage by **who notices**, not by how much the pattern annoys you. Context can move a tell up or down.

- **P0, a layperson recognizes it as AI-made.** The purple-to-blue gradient, Inter for everything, an untouched shadcn base theme, gradient (`bg-clip-text`) headline text, the centered-hero-over-three-cards template. These are the memes.
- **P1, a designer or developer recognizes it.** The second-order clusters (SD), `rounded-2xl shadow-lg` on every surface, the default page shell, icon-in-a-rounded-square, the default four-column footer, stock Aceternity / Magic UI effects, dead hover and focus states, arrow glyphs stapled to CTAs, "Elevate your workflow" copy.
- **P2, craft and polish gaps.** Flat spacing with no rhythm, the same fade-up on everything, bounce easing, generic CTA labels.

Context matters: a centered hero is P0 on a generic SaaS page and fine in a luxury layout. Missing `:focus-visible`, muted text below AA, and motion that ignores reduced-motion are accessibility defects, so treat them as high priority whatever their tier.

## Context profiles

Adjust strictness to where the UI lives. Auto-detect from the stack and structure; state which profile you are using.

| Profile | How to treat it |
|---|---|
| `landing` / `artifact` | Full strength. Reward boldness. This is where a real direction matters most. |
| `marketing-page` | Full strength on type, color, layout, and copy. |
| `app-component` | Surgical. Fix the tells, keep the component's contract and structure. |
| `inside-design-system` | Surgical only. Respect existing tokens and primitives. Do not fight the system; flag system-level tells separately as advice. |
| `dashboard` | Favor density, legibility, and information hierarchy over decoration. |

## Guardrails

- **Never break working code.** Props, state, routing, data fetching, and accessibility survive intact. Behavior is not yours to change.
- **Do not trade one cliché for another.** A second-order default is still a default. The fix for purple is not cream and terracotta, and the fix for cream is not near-black and lime. Vary across runs and justify every choice by the subject.
- **The brief wins.** An existing design system, brand guidelines, a named framework, or a font or color the user picked outranks your taste, including when it asks for one of the SD looks. Match it, never override it, and mark it with an ignore comment so the scanner stops flagging it.
- **Keep one design system across pages.** When the work spans more than one page, lock a single contract (color, type, spacing, radius, nav, footer, shared components) in `DESIGN.md` and reuse it verbatim. Regenerating each page from scratch reverts to the training median, so page two drifts from page one. That is *design-system drift* (catalog **X1**), and it reads as AI as loudly as any single tell. If a token you need does not exist, add it to the contract or ask. Do not invent a one-off.
- **Evidence, not accusation.** A tell shows a default nobody chose. It never proves a model made the page, and much of it is Tailwind and shadcn as installed. Report defaults; do not speculate about authorship.
- **Do not manufacture problems.** If the UI is already distinctive and intentional, say so and stop. A clean audit is a valid result.
- **Keep the copy's meaning.** You may sharpen generic microcopy, but do not invent claims or change what the product says about itself.

## Self-reference escape hatch

When the code is *about* AI design patterns (a demo, a teaching example, a "what not to do" gallery, or this skill's own docs), illustrative slop is intentional. Treat it as exempt only when there is a concrete signal: a sibling comment that marks it (`slop-example`), a path under `examples/`, `fixtures/`, `__mocks__/`, or `stories/`, or text explicitly labeled illustrative. The scanner applies the same rule. Flag patterns in the real interface only.

## Beyond HTML and React

The catalog is written for HTML/CSS and React/Tailwind/shadcn because that is what AI tools emit most. The principle is framework-agnostic: a default left untouched is the tell. Untouched MUI (Roboto and blue), Chakra, Bootstrap (`btn-primary` blue), or Mantine defaults read as AI for the same reason. Apply the same audit, swapping the specific class and token names.

## Output format

### rewrite mode

1. **Audit.** Every tell found, grouped by severity, each with its ID, location, a one-line reason, and a code-certain / rendered / inferred tag. Lead with the scanner summary when you ran it.
2. **Plan.** The direction and where it comes from in the subject, the compact token plan, what you revised in the default review (step 6), and one or two alternatives in a sentence. If interactive, this is where you pause.
3. **Rewrite.** The edited code, at the calibrated depth, plus `DESIGN.md` for multi-page work.
4. **What changed.** A short summary of the meaningful moves, not a line-by-line diff.
5. **Re-scan and judgment.** The scanner's second pass, then the four success tests. Fix anything that fails.

### detect mode

1. **Audit.** Every tell found, grouped by severity (P0/P1/P2), with IDs, locations, and a code-certain / rendered / inferred tag.
2. **Assessment.** For each flag, whether it is a clear problem or a judgment call. Some patterns are fine in context (one gradient, used well, is not slop; a cream palette the brief asked for is a choice). Say which to fix and which to leave.

## Don't over-design

The goal is a UI that looks like a person with taste made it for this subject, not a UI that is loud for its own sake. Restraint executed well beats maximalism applied blindly. If the original is already strong, make the few cuts it needs and stop. Match the intensity of the rewrite to the artifact: a settings panel does not need a hero animation.
