# Anti-Slop Policy — Superd Power Guard

Status: Proposed for review  
Adaptation basis: uploaded `anti-slop-main` source archive, including `antislop.md`, `skills/antislop-ui/SKILL.md`, `skills/antislop-human/SKILL.md`, and `skills/antislop-code/SKILL.md`.

Anti-Slop is a quality filter, not a visual style guide. The project's visual direction remains defined by `DESIGN.md`. The source rulebook describes five craftsmanship principles (C-1 through C-5), three rule tiers (R-01 through R-40), task-specific skills, and a mandatory Delivery Gate. This policy selects and adapts relevant ideas for a native Android app; it does not claim that every web-specific rule applies to Compose.

## 1. Craftsmanship principles

- **C-1 — Intentionality:** each visual, interaction, and code decision must serve a real product purpose.
- **C-2 — Functional Completeness:** visible controls and navigation must work as represented.
- **C-3 — Content-Driven Composition:** layout follows diagnostic content and user tasks, not generic card templates.
- **C-4 — Resilience:** screens handle loading, empty, unavailable, normal, caution, and error/anomaly states as applicable.
- **C-5 — Evidence Over Claims:** displayed facts and diagnostic claims must be supported by actual data or explicitly qualified.

## 2. Selected upstream rules for SPG

The identifiers below retain the names used in the supplied Anti-Slop rulebook.

### Hard-gate rules
- **R-17 — Data & Numbers:** no fabricated readings, statistics, chart values, or diagnostic numbers.
- **R-24 — Navigation:** destinations and navigation actions must be real and coherent.
- **R-25 — Color Contrast:** text and meaningful visual controls must remain legible.
- **R-26 — Interactive Elements:** buttons, switches, tabs, and clickable rows must perform their represented action.
- **R-27 — UI States:** do not design only for the ideal populated state; handle relevant loading, empty, unavailable, and failure states.
- **R-34 — Every Theme You Ship Must Work:** any theme/appearance option exposed by SPG must be implemented and checked.
- **R-35 — Verify Before You Deliver:** verify the actual result; do not report unrun checks as passing.
- **R-36 — No Fabricated Claims:** do not invent product capabilities, sensor facts, protocol support, or diagnostic certainty.
- **R-37 — Design Direction Required:** UI work must follow the existing `DESIGN.md`.
- **R-38 — Real Content or Honest Placeholder:** unfinished or unavailable functionality must be represented honestly.
- **R-39 — Accessible Semantics & Interaction Affordances:** controls need meaningful labels, semantics, and clear interaction affordances.
- **R-40 — Performance Mechanisms:** avoid unnecessary work and expensive UI behavior; validate performance claims.

### Purpose and consistency rules
- **R-01 — Color & Gradients:** use visual effects only when they serve the established design and hierarchy.
- **R-04 — Icons:** icons must communicate a clear action or meaning.
- **R-05 — Layout & Page Structure:** structure must support the actual screen task.
- **R-06 — Typography:** preserve readable, consistent hierarchy.
- **R-11 — Border Radius:** avoid arbitrary shape changes that weaken the visual system.
- **R-12 — Shadow:** use elevation deliberately, not decoratively.
- **R-13 — Glow:** avoid decorative glow that conflicts with the Industrial Instrument design.
- **R-19 — Animations:** motion must communicate state or navigation and must not obstruct input.
- **R-20 — Visual Identity:** maintain SPG's distinct product identity.
- **R-21 — Dark Mode:** if a dark theme is shipped, verify it as a real theme rather than an untested variant.
- **R-29 — Color Palette:** keep colors restrained and semantically consistent.
- **R-30 — Do Not Clone Popular Products:** use benchmarks for learning, not copying.
- **R-31 — Every Decision Must Have a Reason (Write It Down):** record non-obvious design/technical trade-offs when they affect future work.

## 3. Native Android interpretation

- Apply responsive-layout and mobile-overflow principles to Compose layouts; do not copy web CSS/HTML requirements literally.
- Apply accessibility principles through Compose semantics, labels, focus behavior, contrast, and touch targets.
- Follow `DESIGN.md` even when an upstream example recommends a different visual style.
- Do not add decorative cards, gradients, glowing borders, badges, or animation merely to make a screen look busier.
- Do not remove useful diagnostic density merely to make the UI look sparse.
- Preserve technical precision: unknown protocol stays unknown; missing sensor data is unavailable, not zero; anomaly text describes an indication rather than asserting hardware failure.
- A threshold-based heuristic must be named and documented as a heuristic, not presented as protocol identification.

## 4. Anti-Slop review gate

For every UI/UX change, report PASS / FAIL / N/A for:

1. Clear hierarchy and a justified focal point.
2. Components justified by diagnostic content.
3. Consistency with `DESIGN.md`.
4. Readable typography, contrast, and touch targets.
5. Narrow-screen layout, long values, and scrolling behavior.
6. Loading, empty, unavailable, and error states where relevant.
7. Functional controls and clear feedback.
8. No duplicated information or competing sources of truth.
9. No invented values, unsupported protocol labels, or overconfident diagnoses.
10. No unrelated service, database, or monitoring behavior changed.
11. Relevant automated checks completed and accurately reported.
12. Physical-device review recorded separately when needed.

A FAIL blocks delivery unless explicitly accepted by the user with the limitation documented. N/A requires a short reason.

## 5. Task-specific use

- UI/visual work: consult the upstream `antislop-ui` skill.
- Accessibility and interaction: consult `antislop-human`.
- Code-comment cleanup: consult `antislop-code` only when comments are in scope.
- Copywriting skill is generally low priority for SPG; use only when user-facing text is being revised.
- Mobile-layout guidance is applicable conceptually; translate it to native Compose behavior.

Do not install the upstream CLI/plugin or duplicate its CI gate as part of this policy adoption. First inspect the repository's existing Anti-Slop UI Audit Gate and avoid overlapping checks.

## 6. Source and attribution

Adapted from the user-provided Anti-Slop archive by Miqdad Badjuber. Upstream repository: https://github.com/miqdadbadjuber/anti-slop . The supplied archive includes an MIT License; see `THIRD_PARTY_NOTICES.md`.
