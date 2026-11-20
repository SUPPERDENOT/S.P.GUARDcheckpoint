# SUPERD POWER GUARD — UI Design System

Version: 1.0
Direction: Industrial Instrument
Scope: Android app screens only. Notification visuals and future widgets are out of scope.

## Product character
A precise, calm battery diagnostic instrument. The interface should feel engineered and trustworthy, not like a generic fintech dashboard or a decorative gaming HUD. Show measured facts first; label estimates and unavailable values honestly.

## Visual language
- Canvas: warm ivory `#F4F0E7`.
- Primary surface: paper `#FFFCF6`.
- Secondary surface: mineral `#E9E3D8`.
- Main text: graphite `#252923`.
- Secondary text: slate `#62665F`.
- Primary action / instrument accent: safety orange `#D96B2B`.
- Positive state: restrained deep green `#39765B`.
- Caution: ochre `#A87524`.
- Anomaly: brick red `#B8473D`.
- Avoid neon cyan/green, pure-black panels, decorative gradients, glowing borders, and color as the only status cue.

## Layout and hierarchy
1. Each screen has one clear page title and one concise purpose statement.
2. The most decision-relevant battery value is the visual anchor.
3. Related measurements are grouped by diagnostic meaning, not placed in equally weighted generic cards.
4. Use consistent 8dp spacing increments, 16–20dp screen gutters, and 12–16dp component radii.
5. Prefer flat paper surfaces, thin neutral dividers, and selective borders over nested cards.
6. Avoid horizontal layouts that force small labels or clipped values on narrow phones.
7. Touch targets must be at least 48dp; body copy should remain comfortably readable.

## Typography
- Use Material typography roles consistently.
- Numeric readings may use headline styles and tabular-friendly alignment.
- Section labels are sentence case; avoid excessive all-caps micro-labels.
- Use Indonesian consistently for user-facing copy, retaining standard technical units and terms where useful.

## Interaction and states
- Every interactive row/button has a clear affordance and accessible description.
- Provide loading, empty, unavailable, normal, caution, and anomaly states where applicable.
- Never use color alone to communicate charging or anomaly state.
- Do not render fake disabled/coming-soon controls as if they were functional features.
- Settings are grouped by user intent: Monitoring, Status Bar, Battery Calibration, Android Access, About.

## Data integrity
- Distinguish source, protocol, measured battery-side values, and charging progress.
- Unknown protocol remains explicitly unknown; never infer a protocol from current, voltage, or wattage thresholds.
- Missing sensor values display as unavailable, not zero.
- Anomaly copy must describe an indication, not claim hardware failure.
- UI-only work must not alter monitoring cadence, sensor acquisition, persistence, alert logic, or charging calculations.

## Anti-Slop review gate
A UI change is not ready unless all are true:
- [ ] The screen has a clear information hierarchy and one primary focal point.
- [ ] Components are justified by content; no card grid added merely for visual symmetry.
- [ ] Color has semantic purpose and remains restrained.
- [ ] Labels are concise, specific, and consistent in Indonesian.
- [ ] Small-screen layout, long values, empty states, and unavailable data are considered.
- [ ] Controls are functional and have clear interaction feedback.
- [ ] No unsupported claims, fake metrics, or invented protocol labels.
- [ ] No unrelated feature, service, database, or monitoring behavior changed.
- [ ] Android CI build and unit tests pass.
- [ ] Device visual review is recorded separately from CI; CI is not device validation.

## Out of scope
- Notification/status-bar icon renderer and notification layout.
- Home-screen widgets.
- Charger cutoff / charge limiting.
- Battery monitoring engine, service lifecycle, sensor polling, repository, and database behavior.
