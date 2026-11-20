# Android Skills Index — Superd Power Guard

Purpose: route each SPG task to the relevant source skills from the user-supplied Android Skills archive. Read the actual skill file before applying it; this index is a routing aid, not a substitute for the source.

## Source location

The supplied archive is `android-skills-main` from the upstream Android Skills project by Ricardo Costeira. The skill identifiers below use the upstream names. The archive is a reference input and is not copied wholesale into this repository.

Upstream source: https://github.com/ricardocost/android-skills

## Task-to-skill map

| SPG work area | Read these skills | Relevant reference files / focus |
|---|---|---|
| General Android/Kotlin changes | `android-dev` | Baseline rules and routing to task-specific skills. |
| Battery Monitoring engine/service lifecycle | `android-dev`, `android-debugging`, `kotlin-coroutines`, `kotlin-flows`, `android-testing` | Platform lifecycle, structured concurrency, cancellation, state propagation, regression tests. |
| Charging data and diagnostics | `android-data-layer`, `android-dev`, `android-debugging`, `android-testing` | Repository boundaries, error handling, data provenance, deterministic tests. Do not infer charging protocol from electrical thresholds. |
| Protocol/source capability investigation | `android-dev`, `android-debugging`, `android-source-search` | Verify whether the platform/device exposes an authoritative source before displaying a protocol claim. |
| Overview / Dashboard state | `compose`, `compose/references/state-management.md`, `compose/references/screen-structure.md`, `kotlin-flows` | State ownership, rendering, lifecycle-aware collection, empty/unavailable states. |
| Charging screen and live measurements | `compose`, `compose/references/state-management.md`, `compose/references/performance.md`, `kotlin-flows` | State freshness, recomposition, collection lifecycle, truthful measurement display. |
| History sessions and charging curve | `compose`, `compose/references/lists-scrolling.md`, `compose/references/screen-structure.md`, `compose/references/performance.md`, `android-data-layer` | Nested scrolling, chart/list viewport, session data, pagination/large lists if present. |
| Settings and persisted preferences | `datastore`, `android-data-layer`, `compose/references/state-management.md` | Single DataStore instance, preference persistence, state synchronization, avoid duplicate settings sources. |
| Status Bar Indicator | `android-dev`, `android-ux`, `android-debugging`, `android-testing` | Platform integration, permissions/notification constraints, accessible settings and state. Keep it distinct from Monitoring Notification. |
| UI/UX and accessibility | `android-ux`, `compose/references/accessibility.md`, `compose/references/focus-navigation.md`, Anti-Slop `antislop-ui`, `antislop-human` | Touch targets, semantics, contrast, focus, responsive layout, functional controls. Always read `DESIGN.md` and `ANTI_SLOP_POLICY.md`. |
| Navigation and screen transitions | `compose/references/navigation.md`, `compose/references/side-effects.md` | Navigation ownership, one-shot effects, lifecycle-aware handling. |
| Build configuration and build performance | `gradle-build-performance`, `android-gradle-logic`, `android-debugging` | Gradle task/configuration behavior, dependency/build changes, build diagnosis. |
| Unit and instrumentation tests | `android-testing`, plus the skill for the code under test | Behavior-based tests, regression coverage, deterministic boundaries. |
| Coroutine/Flow defects | `kotlin-coroutines`, `kotlin-flows`, `android-debugging` | Structured concurrency, cancellation, StateFlow/SharedFlow/Channel semantics, collector lifecycle. |

## Skill selection rules

1. Always read `android-dev` as the Android baseline for Android/Kotlin implementation.
2. Add the most specific skill(s) from the table; do not load unrelated skills by habit.
3. For Compose UI work, inspect the relevant Compose reference file, not only `compose/SKILL.md`.
4. For UI/UX changes, also read `DESIGN.md`, `ANTI_SLOP_POLICY.md`, and the matching Anti-Slop skill.
5. For a bug spanning layers, read the skills for each affected boundary and trace the actual path.
6. Follow the project's established architecture and dependency choices. Upstream defaults do not authorize adopting Koin, Retrofit, KMP, modularization, or new libraries.
7. Keep the exact upstream skill identifier in task notes so later reviews can reproduce the guidance used.

## Not default scope

Skills for KMP, Koin, Retrofit, Ktor, Paging, Coil, PDF annotations, RxJava migration, and modularization are not default SPG requirements. Use them only when the existing implementation or an approved feature genuinely requires them.