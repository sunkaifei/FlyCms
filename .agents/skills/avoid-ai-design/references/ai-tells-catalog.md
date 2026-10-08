# AI design tells: catalog

The reference catalog for the `avoid-ai-design` skill. Each tell carries a **detection signal**, a one-line reason it **reads as AI**, and a **fix** for both plain HTML/CSS and React/Tailwind/shadcn.

Percentages cited below come from a Playwright analysis of ~1,400 recent Show HN sites ([Krebs, "design slop"](#sources)): 16 deterministic CSS/DOM heuristics, a ~5-10% false-positive rate per the author, on a sample that skews toward solo, AI-built projects. Read them as relative commonness in a slop-prone corner of the web, not ground truth.

**Detectability.** Each tell is marked by how you can confirm it:

- **⚙ scanner**: `scripts/detect.mjs` finds it in source, deterministically, with no model call.
- **👁 render**: needs the rendered pixels (palette dominance, spacing rhythm, hierarchy, motion feel). Without a render, report it as inferred and lower-confidence.
- **🧠 judgment**: needs a read of intent against the brief.

**A tell is evidence of a default nobody chose, not proof that a model made the page.** Designers shipped rounded cards and centered heroes long before LLMs, and much of this is simply Tailwind and shadcn left as installed. The fix is the same whoever defaulted. Never accuse; fix.

## Why AI design converges

A model trained on a decade of Tailwind tutorials, shadcn starters, and GitHub snippets regresses to the visual median of that corpus. It does not choose indigo; indigo is the average. The fixes here all do the same thing: replace a default with a decision. Read each "why" as "no one chose this," and each fix as "choose."

## Two orders of default

**First order: the median.** The purple gradient, Inter, the centered hero over three icon cards, untouched shadcn. This is what "AI slop" meant in 2024-25, and what most of this catalog was first built to catch.

**Second order: the tasteful escape.** Once everyone learned to avoid the purple gradient, the escapes converged too. Anthropic's own `frontend-design` skill now lists five clusters that AI design gathers around ([source 14](#sources)):

1. a warm cream ground with a high-contrast serif and a terracotta or clay accent;
2. a near-black ground with a single acid-green or vermilion accent;
3. a broadsheet layout: hairline rules, zero radius, dense newspaper columns;
4. the SaaS card kit: identical rounded cards, one radius, one soft grey shadow;
5. template chrome: tracked ALL-CAPS eyebrows, `A · B · C` meta strings, `WORD — fragment` labels, tinted near-black for black, monospace for small data labels, a `→` on every link.

De-slop passes land here constantly, this skill's own first demo included. Treat the second order as tells in their own right (the **SD** category below). The honest test for any fix: would you have arrived here for any similar page? If yes, it is a default.

**A third order is already forming.** Design forecasts name a handmade "anti-AI" reaction (grain, hand-drawn type, scrapbook collage). It is legitimate when the subject earns it and the next reflex when it is reached for to look human.

**Severity:** **P0**: a layperson recognizes it as AI-made. **P1**: a designer or developer does. **P2**: craft and polish. Fix P0 and P1 on every pass.

## Index

| Category | Tells |
|---|---|
| Second-order defaults | SD1 P1 ⚙ · SD2 P1 ⚙ · SD3 P2 👁 · SD4 P1 ⚙ · SD5 P1 ⚙ · SD6 P1 ⚙ · SD7 P2 ⚙ · SD8 P2 ⚙ |
| Typography | T1 P0 ⚙ · T2 P1 ⚙ · T3 P1 ⚙ · T4 P1 ⚙ · T5 P1 ⚙ · T6 P2 👁 · T7 P1 ⚙ |
| Color & gradients | C1 P0 ⚙ · C2 P1 ⚙ · C3 P1 👁 · C4 P0/P1 👁 · C5 P2 ⚙ · C6 P0 ⚙ · C7 P1 🧠 · C8 P2 ⚙ · C9 P1 ⚙👁 |
| Layout & composition | L1 P0 ⚙ · L2 P0 ⚙ · L3 P1/P2 🧠 · L4 P1 ⚙ · L5 P2 🧠 · L6 P1 ⚙ · L7 P1 ⚙ · L8 P1 ⚙ · L9 P1 ⚙👁 |
| Components | K1 P0 ⚙ · K2 P1 ⚙ · K3 P1 ⚙ · K4 P1 ⚙ · K5 P2 ⚙ · K6 P1 ⚙ · K7 P1 ⚙ · K8 P1 ⚙ · K9 P1 ⚙ · K10 P1 ⚙ · K11 P2 👁 · K12 P2 👁 |
| Spacing | S1 P2 👁 |
| Motion | M1 P2 ⚙ · M2 P2 👁 · M3 P2 👁 · M4 P2 ⚙ · M5 P2 ⚙ · M6 P1 ⚙ |
| Icons | I1 (see K6) · I2 P1 ⚙ · I3 P1 ⚙ |
| Copy & microcopy | CP1 P1 ⚙ · CP2 P2 ⚙ · CP3 P1 ⚙ · CP4 P2 ⚙ · CP5 P2 🧠 · CP6 P2 ⚙ |
| Imagery | IM1 P1 👁 · IM2 P1 👁 · IM3 P2 👁 · IM4 P1 ⚙ |
| Fingerprints | F1 P1 ⚙ |
| Cross-page consistency | X1 P0 👁 |

---

## Second-order defaults

The "tasteful AI" looks: what models produce when told to avoid the first order. Each is a legitimate choice for some briefs. Each is a tell when it appears regardless of the subject.

### SD1: Cream ground + terracotta accent (the Claude look) · P1 · ⚙
**Detection:** A warm cream or parchment page ground (near `#F4F1EA`) with a terracotta, rust, clay, or vermilion accent (near `#D97757`), usually with a high-contrast serif display, sometimes with a clean grotesque instead.
**Why AI:** It is the first cluster in Anthropic's list, which notes that `#D97757` is Claude's own interface accent, "so on a user's brief it reads as a tell." In the HN discussion of Krebs's study, a commenter pointed out the same beige scheme on the study's own blog.
**Fix:** Derive the ground and accent from the subject (its materials, place, era, signage), not from "warm and editorial." If the brief genuinely wants warmth, move off the cluster's coordinates: an accent from another hue family, a ground that is not parchment, a display face that is not the high-contrast serif.

### SD2: Near-black ground + one acid-green or vermilion accent · P1 · ⚙
**Detection:** A page on near-black (`#0B0B0B` to `#111`, often tinted) with a single bright acid-green, lime, or vermilion signal, frequently with a monospace face (SD4).
**Why AI:** The dark twin of SD1: "technical and confident" by default. It is Anthropic's second cluster; one HN commenter singled out "the console-ish font Claude seems to love."
**Fix:** Go dark only when the product earns it (a monitoring tool, a night-use app), and theme it as a palette, not one neon on black. If you keep a single signal color, choose it for meaning (status, brand), not for voltage.

### SD3: Broadsheet cosplay · P2 · 👁
**Detection:** Hairline rules everywhere, zero border radius, dense newspaper-like columns, a masthead feel on something that is not a publication.
**Why AI:** Anthropic's third cluster: "editorial" worn as a costume rather than a structure the content needs.
**Fix:** Use rules and a column grid when the content is editorial (many articles, sections, bylines). For a product page, let the structure follow the product.

### SD4: Template chrome · P1 · ⚙
**Detection:** Chrome that appears whatever the subject: a tracked-out ALL-CAPS eyebrow above every heading; metadata joined with middle dots (`A · B · C`); labels built as `WORD — fragment`; a monospace face for small data labels; tinted near-black (`#0B0B0B`, `#111`) standing in for black; a `→` on every link and button (see CP3). The scanner reports the pieces separately: SD4 (all-caps labels), SD4m (mono labels), SD4d (middle dots), SD4w (`WORD — fragment`).
**Why AI:** Every piece reads as "designed," so models add them everywhere. Anthropic lists the set as its fifth cluster and calls all-caps labels and unnecessary labels above content "the commonest tells of a generated page."
**Fix:** Keep a label only when it carries information the heading does not. Set it in the body face, sentence case. Mono is for code and tabular data a reader actually scans, not decoration. Use your palette's ink on purpose.

### SD5: One word accented in the headline · P1 · ⚙
**Detection:** A headline with one word or short phrase set apart by color, italic, weight, a highlighter mark, or a gradient: "Product analytics that *just works*."
**Why AI:** It is the first item on Anthropic's list of typographic treatments to avoid: the instant-emphasis move of a page with no real type hierarchy. T3 is its serif-italic special case.
**Fix:** Let the whole headline carry the idea through size, weight, width, and the line break. If something must stand out, make it the thing that matters (a number, a name), and build the emphasis into the type system rather than painting one word.

### SD6: Decorative 01 / 02 / 03 markers · P1 · ⚙
**Detection:** Two-digit numerals labeling features, sections, or cards that are not a sequence.
**Why AI:** Numbering encodes order; on an unordered list it imitates rigor. Anthropic: numbered markers are "only appropriate if the content actually is a sequence."
**Fix:** Number steps, timelines, rankings, and references a reader will cite. Everything else gets a real label or none. (L5 covers the three-step "how it works.")

### SD7: Fake window chrome · P2 · ⚙
**Detection:** A product or code panel wearing macOS traffic-light dots as the hero visual, showing nothing a real screen of the product would show.
**Why AI:** The stock product shot when there is no product shot; tell catalogs call it "the fake terminal hero."
**Fix:** Show a real screenshot or a specific, real state of the product. If you draw a mock, draw the product's own interface, not a generic window.

### SD8: The purple-escape accent · P2 · ⚙
**Detection:** Emerald (or teal) as the one accent on an otherwise neutral page when nothing in the brief asked for green.
**Why AI:** With purple ruled out, emerald is the next reflex; tell catalogs now list the "emerald fallback."
**Fix:** As C2: tie the accent to the brand or the subject, and give it a job (action, status, emphasis).

---

## Typography

### T1: Inter (or the system stack) for everything · P0 · ⚙
**Detection:** `font-family` is Inter, `-apple-system`, `system-ui`, Roboto, or Arial, with no second face. No display font, no contrast.
**Why AI:** Inter is the default in nearly every AI tool and component library. A single neutral sans with no pairing means nothing was chosen.
**Fix (HTML/CSS):** Choose faces for this brief, one family or two clearly distinct ones, and ship them via `@font-face` or a font host. Name them by role on `:root`. The names below are placeholders on purpose: copying a face from an example is how a default is born.
```css
:root { --font-display: "Display Face For This Brief", serif; --font-body: "Its Companion", sans-serif; }
h1, h2, h3 { font-family: var(--font-display); }
body { font-family: var(--font-body); }
```
**Fix (React/Tailwind):** Map the faces to `fontFamily` in `tailwind.config` (`font-display`, `font-sans`), load them with `next/font` or a `<link>`, and apply `font-display` to headings. Do not leave Geist or Inter as the only face.

### T2: The "tasteful free font" cluster · P1 · ⚙
**Detection:** Space Grotesk, Geist, Syne, Sora, Instrument Serif, Fraunces, or Cal Sans used as the *only* gesture toward design. ~15.8% of analyzed sites used one of this small set.
**Why AI:** The non-generic choice became generic: models reach for the same few "indie-startup" Google Fonts to look designed. Anthropic calls out **Space Grotesk** by name as overused across its generations.
**Fix:** Keep the face only if the brief earns it, and pair it. Otherwise search from the subject outward: type from the domain's own artifacts (signage, packaging, manuals, period printing), a foundry specimen, or a face you have not seen on a generated page. Do not replace one list with another. The rule: never ship the face the last three projects shipped.

### T3: Serif-italic accent word in a sans headline · P1 · ⚙
**Detection:** A sans headline with one word in italic serif. "The *modern* way to ship."
**Why AI:** A recognizable Claude signature, its go-to move for instant "editorial" flavor; the scanner reports it under SD5 with an italic note.
**Fix:** See SD5. Use a real serif/sans contrast only if the whole design commits to it, not as a one-word garnish.

### T4: Geist untouched on a Next.js site · P1 · ⚙
**Detection:** `GeistSans` / `GeistMono` from `next/font`, unchanged, on a deployed site.
**Why AI:** Geist is the Next.js 15+ default. Shipping it untouched says "deployed the starter, never themed it."
**Fix:** Replace or pair Geist deliberately. Keep Geist Mono for code if you like it, but give headings a face that belongs to the brand.

### T5: Reflexive all-caps eyebrow labels · P1 · ⚙
**Detection:** Every section opens with an uppercase, letter-spaced micro-label. ~10.5% used all-caps headlines.
**Why AI:** Anthropic ranks all-caps labels among the commonest tells, so this moved from P2 to P1. It is part of the template chrome in SD4, where the scanner reports it.
**Fix:** Most sections need no opener. When one helps, make it information (a question the section answers, a count, a date), in sentence case. Do not trade eyebrows for decorative numbers (SD6).

### T6: One family, one weight, a flat scale · P2 · 👁
**Detection:** A single face at one or two weights, with heading sizes that barely step from body size.
**Why AI:** No hierarchy decision was made; the type is a delivery vehicle rather than part of the design.
**Fix:** One family is fine if the scale does the work. Set a real type scale with intentional weights, widths, and spacing, and use the headline treatment itself as a design element.

### T7: Typeface declared but never shipped · P1 · ⚙
**Detection:** The CSS names a face that no `@font-face`, font `<link>`, or `next/font` call loads. The scanner checks self-contained pages; in apps, check that the font files are in the build.
**Why AI:** The design exists only in the stylesheet: every visitor sees the fallback. As one quality-gate write-up puts it, "declaring is not shipping."
**Fix:** Ship the files (or the font-host link), or choose a face the platform already has. Confirm the face in the render, not in the source.

---

## Color & gradients

### C1: The purple/indigo-to-blue diagonal gradient · P0 · ⚙
**Detection:** `linear-gradient(135deg, ...)` from indigo/violet to blue in the hero, CTA, or as a background glow.
**Why AI:** The canonical tell, "the Purple Problem." It traces to Tailwind UI defaulting buttons to `bg-indigo-500`; Adam Wathan publicly owned the downstream effect in 2025. Sailop's 2026 report still names a `blue-600` to `purple-500` gradient as the single most reliable fingerprint. The color was never tied to a brand.
**Fix (HTML/CSS):** Choose one dominant color and one accent with a job, from the subject rather than from a palette you have seen before. If you want a gradient, keep it tonal within one hue, or build it from the brand's own colors.
```css
/* Roles, not values: fill these from the brief. Copying hex codes from an example re-creates a default. */
:root {
  --ground: /* dominant: most of the page */;
  --ink:    /* text: AA or better on --ground */;
  --signal: /* one accent with a job: action, status, or emphasis */;
}
```
**Fix (React/Tailwind):** Replace `bg-gradient-to-br from-indigo-500 to-purple-600` with committed palette tokens via CSS variables. Never use indigo/violet as the unchosen accent. A flat, confident color beats a timid gradient.

### C2: Indigo / violet CTA buttons · P1 · ⚙
**Detection:** Primary buttons are `bg-indigo-600` / `bg-violet-500`. ~10.7% of sites.
**Why AI:** Same root as C1. The accent defaulted instead of being chosen.
**Fix:** Tie the primary action to the brand's dominant or accent color. Give it a real hover and active state (see K7).

### C3: "VibeCode purple" dark theme · P1 · 👁
**Detection:** Dark background, low-contrast medium-grey body text, purple accent.
**Why AI:** The unmodified "modern dark mode" cluster. Low body contrast also fails accessibility (see C9).
**Fix:** Pick a dark palette with intent: a warm or cool near-black chosen for the product, a real text color at AA or better, and an accent that means something. Avoid grey-on-grey body text.

### C4: Timid, evenly distributed palette · P0/P1 · 👁
**Detection:** Several colors at similar weight, no clear dominant, no sharp accent.
**Why AI:** AI spreads color evenly and avoids commitment. Intentional brands do the opposite: one dominant color carries the page, an accent punctuates it.
**Fix:** Use the 60/30/10 discipline. One dominant, one secondary, one accent used sparingly for emphasis. Commit.

### C5: Colored glow box-shadows · P2 · ⚙
**Detection:** Cards or buttons with a vibrant colored shadow (e.g. `shadow-indigo-500/50`). ~4.3% of sites.
**Why AI:** Decoration with no function. A glow that says nothing.
**Fix:** Use shadow for elevation, not color theater. If you want atmosphere, build it into the background, not as a glow under every card.

### C6: Gradient headline text · P0 · ⚙
**Detection:** `bg-clip-text text-transparent bg-gradient-to-r ...` on a heading, often the indigo-to-violet again.
**Why AI:** A 2024-era default flourish that doubles down on C1 and usually weakens legibility and contrast.
**Fix:** Make headings solid ink or the brand color. For emphasis use size and weight in the type system (see SD5), not a gradient fill.

### C7: Dark mode you didn't ask for · P1 · 🧠
**Detection:** A dark UI shipped by default when nothing in the brief called for one, usually the grey-on-grey "modern dark" cluster (see C3, K9, SD2).
**Why AI:** "Dark mode you never asked for" is a recurring 2026 tell. The model reaches for a dark hero because its training data is full of them, not because the product wants one.
**Fix:** Pick light or dark because the brand and context earn it, then commit. If dark is right, theme it with intent (a near-black chosen for the product, AA or better body text, a real accent), not the stock `zinc-950` shell.

### C8: One hue pretending to be two · P2 · ⚙
**Detection:** The primary and the accent sit within about 20° of hue: two swatches, one color. The scanner checks tokens named primary/brand against accent/secondary.
**Why AI:** "A palette needs a second job, not a second swatch." A nudged shade is a palette that never decided what the second color is for.
**Fix:** Give the second color a different job and make it different in hue or value, so it reads as a separate signal.

### C9: Muted text below WCAG AA · P1 · ⚙ 👁
**Detection:** Body or "muted" text under 4.5:1 against its background. The scanner checks muted tokens against the page ground; confirm everything else in the render or with the DESIGN.md linter.
**Why AI:** Barely-passing grey body copy is a systematic generated failure; in the HN discussion of Krebs's study, several commenters flagged low-contrast beige and brown text on dark sites. It is an accessibility defect whatever made it.
**Fix:** Raise the contrast to AA (4.5:1 for body text) or better and check it with a tool, not by eye.

---

## Layout & composition

### L1: The centered hero template · P0 · ⚙
**Detection:** Pill badge, centered H1, centered subhead, one or two centered CTAs (~23.5% of sites center the title). Centering alone is not a tell; the badge + centered hero + three-card combo is.
**Why AI:** The default "landing page" skeleton. The composition makes no spatial decision.
**Fix (HTML/CSS):** Open with the most characteristic thing in the subject's world, in its best form: a live demo, a real screen, a number that matters, a headline. Break symmetry when it serves that: a left-aligned hero with an asymmetric visual, an oversized type-driven hero, a split layout. Let one element be dramatically larger.
**Fix (React/Tailwind):** Replace the `flex flex-col items-center text-center` hero with a `grid` that places headline, supporting text, and media on an intentional grid. Drop the pill badge unless it carries real news.

### L2: Three identical icon-topped feature cards · P0 · ⚙
**Detection:** A row of three (or six) cards, each with a small icon, a short title, and a line of text, all the same height and padding. ~20% of sites.
**Why AI:** The most clichéd SaaS pattern. Identical cards read as machine-laid-out.
**Fix:** Vary the layout. Alternate text-and-visual rows, use a feature with one large showcase and smaller supporting points, or write the features as prose with inline emphasis. If a grid is right, vary card size and content density so it does not read as a template.

### L3: Bento grid as the default composition · P1/P2 · 🧠
**Detection:** A mixed-size tile grid used because it is trendy, not because the content needs it.
**Why AI:** A real 2025 pattern, now saturated. Reads AI when it is the reflex composition rather than a choice driven by the content.
**Fix:** Use a bento grid only when tiles genuinely differ in importance and the sizes encode that. Otherwise pick a layout that fits the content's actual hierarchy.

### L4: The generic stat / social-proof strip · P1 · ⚙
**Detection:** A band of round numbers: "10,000+ users · 99.9% uptime · 4.9★". ~12.2% of sites. Its relatives are fabricated trust chrome: grayscale logo walls of companies that are not customers, invented compliance badges, testimonial grids of generated people. Anthropic also names the default hero version: "a big number with a small label, supporting stats, and a gradient accent."
**Why AI:** The numbers are placeholders, often for a product with no users. Hollow proof.
**Fix:** Use real numbers or cut the strip. A single specific, true metric beats four invented ones.

### L5: Numbered 1-2-3 "How it works" · P2 · 🧠
**Detection:** A three-step sequence with big numerals. ~9.4% of sites.
**Why AI:** Formulaic filler structure.
**Fix:** Keep it only if the process genuinely has ordered steps. Otherwise show the product doing the thing. (Numbers on things that are not steps: SD6.)

### L6: The default page shell · P1 · ⚙
**Detection:** Every section wrapped in `container mx-auto px-4` or `max-w-7xl mx-auto`, nothing else.
**Why AI:** The reflex Tailwind shell: one width, centered, forever. No spatial decision.
**Fix:** Vary container width by section role. Let some content go full-bleed, some stay narrow and editorial. Width is a tool, not a constant.

### L7: Pricing as three tiers with a "Most Popular" ring · P1 · ⚙
**Detection:** Three cards, the middle one scaled or ringed, a "Most Popular" badge.
**Why AI:** The canonical SaaS pricing template, shipped without regard to the actual plans.
**Fix:** Let structure follow the real offer: two plans, a table, or one plan with add-ons. If a highlight is right, earn it with design, not a default ring.

### L8: The default four-column footer · P1 · ⚙
**Detection:** Four equal link columns, a newsletter input, a row of social icons.
**Why AI:** The universal generated footer, the same whether or not the links exist.
**Fix:** Build the footer from what the site actually has. Two columns and a line is often enough. Drop the newsletter box unless it is real.

### L9: The cookie-cutter section order · P1 · ⚙ 👁
**Detection:** Hero, logos, features, how it works, testimonials, stats, pricing, FAQ, CTA, footer, in that order, whatever the product. Sailop calls it the standard section waterfall. The scanner flags a page carrying six or more of these stock sections; confirm with the **silhouette test**: screenshot the page, reduce it to a 200-pixel-wide black-on-white silhouette (blocks for sections, lines for text), do the same for five competitors, and lay them side by side. If you cannot pick yours out, the structure is slop.
**Why AI:** The architecture is where most of the sameness lives. Color and font fixes never touch it, which is why a re-skinned page still reads as generated.
**Fix:** Order sections by what this audience needs to decide, cut the ones that exist only because the template has them, and give the page one structural idea: a live demo first, a comparison table as the spine, a long-form argument, a single scrolling story.

---

## Components

### K1: Untouched shadcn/ui defaults · P0 · ⚙
**Detection:** Default `zinc`/`slate`/`neutral` base from `components.json`, the stock `--primary`, default `--radius`, unstyled Card/Button/Badge. ~23.5% of sites shipped these unmodified.
**Why AI:** The framework-level tell. The starter was deployed without theming. In the Reddit-mined ranking of AI-site complaints, shadcn/Tailwind defaults come first.
**Fix:** Theme shadcn before shipping. Change the base color and radius in `components.json`/CSS variables, restyle the primitives you use most (Button, Card), and set your own type scale. shadcn is a starting point, not a look.

### K2: `rounded-2xl shadow-lg` on everything · P1 · ⚙
**Detection:** Uniform large border-radius and a soft shadow (often ~0.1 opacity) on every surface. The plain `1px` grey border wrapped around *every* card is the same instinct without the shadow, and "the ghost card" (a hairline border plus a wide diffuse shadow) is the same instinct with both. 2026 write-ups single out the grey box on every card as the most common card tell of all.
**Why AI:** Identical radius plus identical padding plus identical card heights flattens hierarchy into a template. Anthropic lists this kit as its fourth cluster.
**Fix:** Use radius and elevation to express hierarchy, not as a global default. Vary radius by element role. Let some surfaces be flat, some sharp. Reserve strong shadows for things that genuinely float.

### K3: Glassmorphism / `backdrop-blur` by reflex · P1 · ⚙
**Detection:** Frosted, semi-transparent nav and cards with `backdrop-blur`. ~17% of sites.
**Why AI:** A genuine trend, but applied to everything without reason. Glass on glass on glass.
**Fix:** Use a blurred translucent surface only where layering is real (a nav over scrolling content). Everywhere else, use a solid surface with a considered color.

### K4: Colored left/top border-accent cards · P1 · ⚙
**Detection:** Cards with a colored left or top border stripe. ~13% of sites.
**Why AI:** As Krebs puts it, "colored left borders are almost as reliable a sign of AI-generated design as em-dashes are for text."
**Fix:** Drop the stripe. If a card needs emphasis, use weight, scale, background, or position. Differentiate by content, not a ribbon.

### K5: Pill badge above the title · P2 · ⚙
**Detection:** A small capsule, often with a sparkle emoji: "✨ New: v2 is here". ~4.7% of sites.
**Why AI:** A default ornament that announces nothing.
**Fix:** Remove it unless it carries real, dated news. If it does, style it to the brand, not the stock pill.

### K6: Icon in a tinted rounded-square chip · P1 · ⚙
**Detection:** An icon (usually Lucide) centered in a tinted `rounded-xl` square, one per feature.
**Why AI:** Lucide ships with shadcn; the rounded-square chip is the stock "feature icon" treatment, used unedited.
**Fix:** Choose an icon set that fits the direction (or draw simple custom marks). Drop the chip, or make the icon treatment a real design decision (line weight, size, color, position). Consider no icons at all.

### K7: Missing component states · P1 · ⚙
**Detection:** Hover states that do nothing, buttons that snap with no transition, no visible keyboard focus, forms with no error/required/disabled/loading states. The scanner checks self-contained pages for any focus style at all.
**Why AI:** The polish gap. Generated UI renders the happy path and skips the states a craftsperson would build. Missing focus is also an accessibility defect.
**Fix (HTML/CSS):** Add `:hover`, `:focus-visible`, `:active`, and `:disabled` styles, and a `transition`. Design error and empty states.
**Fix (React/Tailwind):** Implement `hover:`, `focus-visible:`, `disabled:`, and loading/error variants. Wire real validation states into forms.

### K8: Untouched `--radius` · P1 · ⚙
**Detection:** shadcn's default `--radius` (`0.5rem`, or `0.625rem` in newer themes) left as-is across every component.
**Why AI:** The radius is a fingerprint; the default one says the theme was never touched. (Related to K1.)
**Fix:** Set a radius that fits the direction: sharp for brutalist or editorial, soft for playful. Vary it by element role.

### K9: The default dark SaaS card · P1 · ⚙
**Detection:** `bg-zinc-950` / `bg-gray-900` surfaces with `border-white/10` hairlines and a faint shadow.
**Why AI:** The unmodified "modern dark" card that every generated dark UI ships.
**Fix:** Choose a real dark palette (a near-black chosen for the product) and separate surfaces by more than a 10%-white border.

### K10: Stock effect-library components (Aceternity / Magic UI) · P1 · ⚙
**Detection:** Effects dropped in as shipped: Spotlight, Background Beams, Animated Beam, Shimmer Button, 3D Card, Meteors, Border Beam, Number Ticker, Marquee, Sparkles.
**Why AI:** Copy-paste libraries put the same effects on every launch; one 2026 comparison notes you will see "the same spotlight effects, 3D card flips, and animated beams" across Product Hunt's top products.
**Fix:** If a moment deserves an effect, build one tied to the product (animate its own data or interface). Otherwise cut it. Anything you keep, customize until it no longer reads as the library demo.

### K11: Cards inside cards · P2 · 👁
**Detection:** Boxed content nested in more boxes, two or three levels deep.
**Why AI:** Containers standing in for hierarchy; anti-pattern detectors and tell catalogs both flag the nesting.
**Fix:** One level of containment. Use space, alignment, and type for the rest.

### K12: Status-chip soup · P2 · 👁
**Detection:** Badges and chips on every row and card ("New", "Beta", "Pro", colored status pills) with no real state behind them.
**Why AI:** Chips signal "app-like" without carrying state.
**Fix:** Show a chip only where it reflects real, changing state, and keep one visual language for it.

---

## Spacing

### S1: Uniform padding, no rhythm · P2 · 👁
**Detection:** The same `gap` and `p-*` on most things; whitespace distributed evenly.
**Why AI:** Even spacing makes a flat hierarchy. Nothing is emphasized because everything breathes the same.
**Fix:** Use a spacing scale to create rhythm. Give sections distinct vertical space by importance. Use whitespace as composition: crowd some things, isolate others. Squint test: shrink the page to 25%; if every section weighs the same, density is flat.
**Note:** This is the softest category. Weight it below the font, color, and component tells.

---

## Motion

### M1: The same fade-up-on-scroll on everything · P2 · ⚙
**Detection:** Every section reveals with an identical fade-and-rise. Code-certain forms: Framer Motion `initial={{ opacity: 0, y: 20 }}` with `whileInView` on each section, AOS `data-aos="fade-up"`, a `fadeInUp` keyframe reused everywhere.
**Why AI:** Reflexive, not choreographed. Anthropic: fade-and-slide-up entrances on each section "read as AI-generated."
**Fix:** Pick one or two high-impact moments. One well-staggered page-load entrance delivers more than a uniform reveal on every block. Vary easing and intent.

### M2: Scattered micro-interactions, no orchestration · P2 · 👁
**Detection:** Many small random hovers and bounces, a hover transition on every card, no coherent motion language.
**Why AI:** Motion sprinkled on rather than designed.
**Fix:** Define a motion language: shared easing, shared duration scale, a clear entrance. Motion that answers a person's action (opening, confirming) is welcome when it shows what changed.

### M3: The copied "Linear glow" · P2 · 👁
**Detection:** A dark hero with a blurred animated gradient glow behind a product shot.
**Why AI:** "The Linear effect," lifted wholesale onto an unrelated product.
**Fix:** Borrow the principle (atmosphere, depth), not the exact effect. Build atmosphere that fits your own direction.

### M4: Bounce and elastic easing · P2 · ⚙
**Detection:** Overshooting `cubic-bezier` curves, spring `bounce`, `animate-bounce`, `easeOutBack`: dialogs that spring in, cards that overshoot.
**Why AI:** Springy interface motion reads dated and generic; anti-pattern detectors flag it.
**Fix:** Ease out without overshoot for interface motion. Keep spring physics for genuinely playful brands.

### M5: Count-up stat animation · P2 · ⚙
**Detection:** Numbers that tick up on scroll (`NumberTicker`, `CountUp`), usually on the L4 strip.
**Why AI:** Theater around hollow numbers.
**Fix:** Show the number. Make it true.

### M6: Motion that ignores reduced-motion · P1 · ⚙
**Detection:** Animation with no `prefers-reduced-motion` handling. The scanner checks self-contained pages; in apps, look for a global rule or `MotionConfig reducedMotion="user"`.
**Why AI:** Part of the quality floor generated pages skip; Anthropic's list includes "reduced motion respected." It fails people who asked for less movement.
**Fix:** Wrap non-essential motion in `@media (prefers-reduced-motion: no-preference)`, or stop it under `reduce`. In React, use `useReducedMotion` or `MotionConfig`.

---

## Icons

### I1: Lucide untouched
See **K6**. The default set used as-is, one per feature card.

### I2: Emoji as feature bullets or in the nav · P1 · ⚙
**Detection:** Emoji standing in for icons in features or navigation. ~3.8% had emoji in nav.
**Why AI:** A lazy substitute for real iconography.
**Fix:** Use a real icon set chosen for the direction, or custom marks. Reserve emoji for genuinely casual, human contexts, never as the system's iconography.

### I3: The overused Lucide glyph set · P1 · ⚙
**Detection:** `Sparkles` (beside "AI"), `ArrowRight`, `Zap`, `Rocket`, `Shield`, `BarChart3`, `Check`/`CheckCircle2`, `Star`, straight from `lucide-react`; a four-point sparkle (✦, ✨) as the universal "AI" badge.
**Why AI:** The same handful of icons in the same roles, unedited. `Sparkles` for "AI" is the most worn of all.
**Fix:** Pick icons for meaning, not availability. Retire `Sparkles` for AI. Match weight and style to the direction, or draw a few simple custom marks.

---

## Copy & microcopy

### CP1: Vague aspirational headline · P1 · ⚙
**Detection:** "Build the future of work." "Your all-in-one platform." "Scale without limits." "Elevate your workflow." "Transform your X." "Unleash the power of." The weightless pair: "Build faster. Ship smarter."
**Why AI:** Brand-agnostic filler that could front any product. Says nothing specific.
**Fix:** Write what the product actually does, for whom, in concrete terms. Specificity is the opposite of slop.

### CP2: Generic superlatives and hedging · P2 · ⚙
**Detection:** "best-in-class," "cutting-edge," "seamless," "powerful," "may help you."
**Why AI:** The microcopy equivalent of beige.
**Fix:** Replace with a concrete claim, a number, or a verb. For full prose, run the text through the `avoid-ai-writing` skill.

### CP3: Arrow glyphs stapled to text · P1 · ⚙
**Detection:** Unicode arrows (→ ← ↑ ↓) pasted into button labels, links, or headings: "Get started →", "Learn more →", "Read the docs →".
**Why AI:** The typographic cousin of the em dash, and part of Anthropic's template chrome (SD4). A real button does not need an arrow character glued to its label.
**Fix:** Drop the glyph. If a control genuinely needs a directional affordance, use a real icon component sized and aligned to the text, only where it adds meaning.

### CP4: Tricolon slogans · P2 · ⚙
**Detection:** Three single-word adjectives or comparatives in a row: "Faster, smarter, simpler."
**Why AI:** People use the rule of three sparingly; generated copy reaches for it by reflex, usually as three one-word adjectives.
**Fix:** Replace the three with one concrete claim.

### CP5: Title Case Everything · P2 · 🧠
**Detection:** Every heading, button, and label in Title Case with total uniformity.
**Why AI:** Uniform casing is a generated habit; interface copy reads better in sentence case.
**Fix:** Sentence case for interface text. Title Case only where a house style requires it.

### CP6: Generic CTA labels · P2 · ⚙
**Detection:** Buttons that say "Get started", "Learn more", "Submit", "Click here".
**Why AI:** A CTA should say exactly what happens; Anthropic's example is "Save changes," not "Submit."
**Fix:** Name the action and its object ("Start a 14-day trial", "Compare plans"), and keep the same verb through the flow: the button that says "Publish" produces a toast that says "Published."

---

## Imagery

### IM1: Stock "diverse team at a laptop" · P1 · 👁
**Detection:** A bright open-plan office, a smiling team around a screen.
**Why AI:** The visual default for "company."
**Fix:** Use real product screenshots, real photography, or a considered illustration style that fits the direction.

### IM2: AI 3D glossy blobs · P1 · 👁
**Detection:** Plastic-looking abstract 3D shapes as hero or section art, with a tell-tale glossy sheen.
**Why AI:** Generated filler with no subject.
**Fix:** Show the actual product, or commission art with a point of view. If you use abstract forms, make them specific to the brand.

### IM3: Corporate Memphis blob-people · P2 (precursor) · 👁
**Detection:** Flat illustrations of people with tiny heads, long bendy limbs, no faces, flat bright fills.
**Why AI:** A pre-AI corporate trend (Buck's "Alegria," 2017) now widely declared dead, but still reproduced by image tools as "friendly corporate." Lineage, not a fresh AI tell.
**Fix:** Choose an illustration style with a real voice, or skip illustration for photography or product UI.

### IM4: Placeholder identities and media · P1 · ⚙
**Detection:** DiceBear / boring-avatars / `pravatar.cc` avatars; `aspect-video bg-muted rounded-xl` standing in for a real demo or screenshot; testimonial walls of generated faces.
**Why AI:** Generated stand-ins where real content belongs. Code-detectable, and a strong "nothing real here yet" signal.
**Fix:** Use real avatars and a real product screenshot or recording. If you must use a placeholder, make it obviously intentional, not a stock avatar service.

---

## Fingerprints

### F1: Generator signature left in · P1 · ⚙
**Detection:** `lovable-tagger`, `gpt-engineer` leftovers, v0 / Bolt / Lovable meta tags or attribution in the shipped page.
**Why AI:** It names the tool that shipped the page.
**Fix:** Remove build-time taggers and generator meta before shipping, unless a builder's terms require the attribution.

---

## Cross-page consistency

### X1: Design-system drift across pages · P0 · 👁
**Detection:** Two pages of the same product that do not share a design system. The font, palette, radius, nav, or footer changes from one page to the next; tokens are renamed or reinvented (`--color-primary-500` on one page, `--brand-action-bg` on another); a new page ignores a font or color the user already chose. This tell only appears when you compare pages **side by side**, never when you judge one in isolation.
**Why AI:** Distinct from ordinary slop. Slop is *convergence*: every site looks the same. Drift is *divergence from your own system*: a product can dodge the clichés and still fall apart across pages. A model has no memory of your design and no access to your real tokens, so a page regenerated from scratch (a new prompt, a fresh chat, "now build the FAQ") is reconstructed from the training median instead of from your site. The 2026 name for it is **AI design system drift**: "the slow divergence between your real design system and what an AI agent generates against it," which can happen "between two consecutive prompts, by the same tool" (sources 11-12).
**Fix (HTML/CSS):** Establish one token contract and reuse it verbatim on every page: the same `:root` variables, the same nav and footer markup, the same type and spacing scale. Factor shared chrome into an include, or copy it exactly; never re-pick fonts or colors per page. If a token you need does not exist, add it to the contract or ask. Do not invent a one-off.
**Fix (React/Tailwind):** Keep tokens in one place (`tailwind.config` plus CSS variables), import shared `Nav` / `Footer` / `Button` components rather than re-authoring them, and constrain new pages to the existing component APIs. Lint for raw hex values and off-scale spacing so drift surfaces in review.
**The contract file:** Google's open **DESIGN.md** format (Apache-2.0, open-sourced April 2026) has become the common one: YAML tokens for exact values plus prose for the reasons, read by Stitch and a growing set of agent skills. `npx @google/design.md lint DESIGN.md` validates it and checks WCAG AA contrast; `npx @google/design.md diff old.md new.md` flags token regressions between versions, which is drift caught in review. Start from [`design-md-template.md`](design-md-template.md). The file is the floor; re-reading it and re-checking each new page is the fix.
**Note:** A design the user chose themselves (a specific font, a brand color) *is* the contract, not a starting suggestion. Match it exactly and carry it to every new page. Overriding it is drift too, even when the result is prettier.

---

## What not to over-flag

A pattern is a tell when it is a **default reached for without reason**, not whenever it appears. Calibrate:

- **The brief wins.** If it asks for cream and terracotta, a terminal look, or a centered hero, deliver that well. Anthropic's own rule: "the brief's own words always win."
- **Judge combinations, not single hits.** One accented he