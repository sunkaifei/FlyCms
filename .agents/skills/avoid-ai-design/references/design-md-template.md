# DESIGN.md template

Use this when the work spans more than one page or more than one session. The file is the contract every later page reads first, so page two cannot drift from page one (catalog **X1**).

It follows Google's open [DESIGN.md format](https://github.com/google-labs-code/design.md): machine-readable tokens in YAML front matter, the reasons in Markdown prose, sections in the spec's order. Other tools (Google Stitch, several agent skills) read the same format, so the contract travels.

**Rules for filling it in**

- If a `DESIGN.md` already exists (written by a person, Stitch, or another skill), do not replace it. It is the contract: read it, follow it, and add to it only with the user's agreement.
- Fill every `<…>` from the brief and the committed direction. Do not copy values from examples, including the ones in Google's README: a copied value is how a default is born.
- A font or color the user chose goes in as-is and is listed under "Don't change."
- Name the refused defaults (the catalog's SD list and any first-order tells you fixed), so later passes cannot slide back into them.

**Check it**

```bash
npx @google/design.md lint DESIGN.md                 # structure, broken token references, WCAG AA contrast
npx @google/design.md diff DESIGN.md DESIGN.next.md  # token regressions between versions: drift, caught in review
```

The CLI is optional. Without it, re-read the file before each new page and compare the page's tokens against it by hand.

---

```md
---
version: alpha
name: <product name>
description: <one sentence: what the product is, for whom, and the feeling it should leave>
colors:
  ground: "<hex>"   # the dominant surface
  ink: "<hex>"      # body text, AA or better on ground
  muted: "<hex>"    # secondary text, still 4.5:1 or better on ground
  signal: "<hex>"   # one accent, one job (say which below)
typography:
  display:
    fontFamily: <face chosen for this brief>
    fontSize: <size>
    fontWeight: <weight>
    letterSpacing: <tracking>
  body:
    fontFamily: <companion face, or the same family>
    fontSize: <size>
    lineHeight: <leading>
rounded:
  sm: <radius>
  md: <radius>
spacing:
  sm: <step>
  md: <step>
  lg: <step>
components:
  button-primary:
    backgroundColor: "{colors.signal}"
    textColor: "{colors.ground}"
    rounded: "{rounded.sm}"
    padding: <padding>
---

## Overview

<Subject and audience in two sentences. Then the source of the look: the materials, place,
era, or artifacts of this product's world that the choices come from. This paragraph is what
keeps a later page, or a later session, from reinventing the direction.>

## Colors

- **Ground (<hex>):** <why this ground, traced to the subject>
- **Ink (<hex>):** <contrast ratio on ground>
- **Muted (<hex>):** <contrast ratio on ground>
- **Signal (<hex>):** <its single job: action, status, or emphasis>

## Typography

<Faces and their roles, the type scale, and the one place the display treatment may be loud.>

## Layout

<The grid, alignment, container width by section role, and the site's one structural idea.>

## Elevation & Depth

<When a surface lifts and when it stays flat.>

## Shapes

<Radius by element role.>

## Components

<The shared nav, footer, buttons, and cards. Every page reuses these verbatim.>

## Do's and Don'ts

- Do reuse the nav, footer, and tokens above on every page, unchanged.
- Do add a token here before a page uses it. Never invent a one-off inside a page.
- Don't change: <the user's own choices, e.g. their typeface or brand color>
- Don't use: <the defaults this design refuses, e.g. SD1 cream + terracotta, SD5 one accented headline word, SD6 decorative 01/02/03>
```
