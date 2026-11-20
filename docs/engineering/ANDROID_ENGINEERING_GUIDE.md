# Android Engineering Guide — Superd Power Guard

Status: Proposed for review  
Adaptation basis: user-provided `android-skills-main` archive. This document selects relevant guidance for the current native Android project; it is not a mandate to adopt every skill or dependency in the archive.

## 1. Architecture and boundaries

Relevant source skill: `android-dev`.

- Preserve the existing Kotlin + Jetpack Compose + MVVM architecture and established package boundaries.
- Keep composables focused on rendering state and emitting user actions.
- Keep business rules outside composables.
- Reuse existing ViewModels, repositories, domain components, and services where they already own the responsibility.
- Do not introduce Koin, Retrofit, Ktor, KMP, modularization, or another architecture solely because a reference skill describes it.

## 2. Debugging and root-cause analysis

Relevant source skill: `android-debugging`.

Before a fix:
1. Reproduce or precisely characterize the issue.
2. Identify expected versus observed behavior.
3. Trace the data/control path through engine, service/receiver, repository, ViewModel, and UI as applicable.
4. Find the first point where state or behavior diverges.
5. Check logs, lifecycle, threading, persistence, and platform restrictions relevant to the path.
6. Propose the smallest change that addresses the verified cause.
7. Add or update a regression test when feasible.

Do not mask a state-flow, lifecycle, or data-layer defect by changing only the UI display.

## 3. Testing strategy

Relevant source skill: `android-testing`.

- Prefer behavior-focused tests over tests that only mirror implementation details.
- Use the existing test framework and conventions before adding new libraries.
- For bug fixes, create a regression test that fails for the defect when practical, then passes after the fix (RED → GREEN → REFACTOR).
- Test boundaries and failure cases: missing data, unavailable sensors, permission denied, service stopped, persistence errors, and lifecycle transitions as relevant.
- Keep unit tests deterministic; isolate Android/platform dependencies behind existing abstractions where practical.
- Report test counts and actual outcomes. Do not infer device behavior from unit tests.

## 4. Coroutines and Flow

Relevant source skills: `kotlin-coroutines` and `kotlin-flows`.

- Respect structured concurrency and cancellation.
- Do not swallow `CancellationException` in broad exception handling.
- Bound retries and define retry conditions; avoid unbounded loops.
- Keep side effects out of pure state-combination transformations.
- Understand the semantics of StateFlow, SharedFlow, and Channel; a Channel is not a broadcast mechanism.
- Ensure callback-based Flow adapters release callbacks/resources when collection ends.
- Collect UI state with lifecycle-aware Compose APIs where appropriate and consistent with the project's existing versions.
- Avoid creating duplicate collectors or triggering repeated work during recomposition.

## 5. Jetpack Compose

Relevant source skill: `compose` and its references.

Consult the specific reference that matches the task:
- `state-management.md`: state ownership, hoisting, and UI state.
- `side-effects.md`: lifecycle-aware effects and one-shot events.
- `lists-scrolling.md`: long session/history lists and nested scrolling.
- `performance.md`: recomposition and rendering cost.
- `accessibility.md`: semantics and accessible controls.
- `navigation.md`: destination and navigation behavior.
- `theming-material3.md`: Material 3 theme integration.
- `screen-structure.md`: screen composition.

Project-specific rules:
- Preserve Industrial Instrument styling from `DESIGN.md`.
- Do not apply generic Material 3 audit suggestions blindly if they conflict with the explicit project design contract.
- Avoid unnecessary state reads and side effects in composables.
- Ensure scrollable content remains reachable when charts, bottom navigation, or system insets are present.
- Give interactive controls meaningful labels and at least the project's specified 48dp touch target where feasible.

## 6. Data layer and persistence

Relevant source skills: `android-data-layer` and `datastore`.

- Keep Room for relational session, sample, rollup, and health data.
- Keep DataStore for small persistent preferences and settings.
- Do not move relational/history data into DataStore.
- Handle expected I/O failures explicitly; do not convert every exception into an empty/default state.
- Preserve cancellation and propagate unexpected failures rather than silently hiding them.
- Maintain one application-scoped DataStore instance per backing file.
- Do not perform blocking database or DataStore operations on the main thread.
- Review schema migrations and backward compatibility before changing Room entities or database versions.

## 7. Build and performance

Relevant source skill: `gradle-build-performance`.

- Inspect existing Gradle configuration before changing build logic.
- Avoid dependency additions that do not solve a demonstrated need.
- Do not change Gradle, Kotlin, AGP, or library versions as incidental cleanup.
- Use existing CI as the authoritative automated build check for the branch.
- Make performance claims only when supported by measurement or a reproducible comparison.

## 8. SPG-specific protected behavior

The following are product-specific constraints and take precedence over generic examples:

- Battery Monitoring master-control behavior and tested service lifecycle must not be changed casually.
- Monitoring is charging-triggered according to the approved product behavior; do not introduce always-on unplugged monitoring.
- Dashboard state must reflect actual service state, not merely the saved master toggle.
- Status Bar Indicator and Monitoring Notification are distinct surfaces; do not conflate their purpose.
- Unknown charging protocol must remain unknown unless a reliable device/API source is established.
- Do not infer protocol identity from current, voltage, or power thresholds.
- Keep measured battery-side current, source type, charging progress, and protocol as distinct concepts.
- UI-only changes must not alter acquisition cadence, persistence, alert thresholds, or calculations.

## 9. Upstream skill index

Use the supplied Android Skills source archive for detailed task-specific guidance. High-priority skills for SPG:

- `android-dev`
- `android-debugging`
- `android-testing`
- `android-data-layer`
- `kotlin-coroutines`
- `kotlin-flows`
- `compose`
- `android-ux`
- `datastore`
- `gradle-build-performance`

Other skills in the archive (KMP, Koin, Retrofit, Ktor, Paging, Coil, PDF annotations, RxJava migration, and modularization) are out of scope unless a concrete product requirement makes them relevant.

## 10. Source and attribution

Adapted from the user-provided Android Skills archive by Ricardo Costeira. Upstream repository: https://github.com/ricardocost/android-skills . The supplied archive includes an MIT License; see `THIRD_PARTY_NOTICES.md`.
