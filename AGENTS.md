# AI Agent Instructions — Superd Power Guard

These instructions apply to every AI coding agent and every task in this repository.

## Required reading

Before changing code or project documentation, inspect the relevant current files and read:

1. `docs/engineering/DEVELOPMENT_STANDARDS.md`
2. `docs/engineering/QUALITY_CHECKLIST.md`
3. `DESIGN.md` for any UI, UX, copy, theme, or visual change.
4. `docs/engineering/ANTI_SLOP_POLICY.md` for UI, UX, content, accessibility, or quality-review work.
5. `docs/engineering/ANDROID_ENGINEERING_GUIDE.md` for Android, Kotlin, Compose, service, persistence, concurrency, or testing work.
6. `docs/engineering/ANDROID_SKILLS_INDEX.md` to identify the exact upstream Android Skills relevant to the task.
7. `docs/engineering/CHATGPT_WORKFLOW.md` when ChatGPT is performing repository work through the connected GitHub tools.

Read the relevant upstream skill source when a task needs detailed guidance. Do not assume a skill's full contents from its name.

## Project-specific safeguards

- Preserve the existing Kotlin + Jetpack Compose Android architecture unless an approved task explicitly changes it.
- Treat `DESIGN.md` as the source of truth for the app's visual identity. Do not replace its Industrial Instrument direction with a generic design system.
- Protect battery-monitoring lifecycle and service behavior. Do not alter monitoring cadence, sensor acquisition, persistence, alert logic, charging calculations, or startup policy during UI-only work.
- Never infer charging protocol or fast-charge technology from current, voltage, wattage, labels, or thresholds. Report unknown/unavailable when the device does not expose reliable evidence.
- Distinguish measured values, estimates, inferred indicators, unavailable values, and actual service state.
- Do not add duplicate controls, duplicate sources of truth, decorative controls, fake metrics, or unsupported feature claims.
- Inspect existing implementation before creating a new abstraction or duplicating functionality.
- Prefer the smallest change that correctly addresses the verified root cause.
- Do not perform broad refactors as a side effect of a focused task.

## Required workflow

1. Establish the current branch, working-tree/repository state, and relevant implementation.
2. Trace the affected data/control flow and identify the root cause before editing.
3. State the intended scope and files likely to change.
4. Implement only the approved scope.
5. Review the complete diff for regressions, unrelated changes, and duplicated logic.
6. Run relevant tests and build checks; report commands and actual results.
7. Complete `docs/engineering/QUALITY_CHECKLIST.md` in the task report.
8. Update `CHANGELOG.md` for every meaningful SPG change. Preserve historical entries and distinguish implementation from CI and physical-device verification.
9. Report unverified items explicitly. A green CI build is not physical-device validation.

## Git and approval boundaries

- Never commit, push, merge, or open a pull request targeting `main` without explicit user approval.
- Do not change app versioning/build numbering unless explicitly approved.
- Do not overwrite user changes or discard unrelated work.
- Keep changes focused and explain any deviation from the requested scope.
- Documentation-only work must not modify application source, build configuration, or runtime behavior.

## Completion report

Include:
- branch and commit SHA(s);
- files changed;
- concise summary of behavior/documentation changes;
- tests/build checks and their exact status;
- physical-device status (tested / not tested);
- checklist PASS / FAIL / N/A items;
- known limitations or follow-up findings.

A task is not complete while its required changelog entry is missing.
