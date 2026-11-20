# Superd Power Guard — Development Standards

Status: Proposed for review on `docs/adopt-development-standards`  
Scope: Android application source, UI/UX, diagnostics, data, testing, and AI-assisted development.

## 1. Source-of-truth hierarchy

When guidance conflicts, use this order:

1. Explicit user-approved task scope and product decisions.
2. Existing project safety contracts and verified runtime behavior.
3. `DESIGN.md` for visual identity and UI direction.
4. `AGENTS.md` and the engineering policies in `docs/engineering/`.
5. Relevant upstream Anti-Slop and Android Skills references.
6. General conventions or an agent's assumptions.

Upstream references inform decisions; they do not automatically override project-specific requirements.

## 2. Product and engineering principles

- **Diagnostic integrity:** measured facts, estimates, unavailable readings, and interpretations must be clearly distinguished.
- **Evidence before claims:** do not assert charging protocol, fast-charge technology, hardware failure, or anomaly cause without sufficient evidence.
- **Minimalist Appearance, Advanced Diagnostics:** retain the approved Industrial Instrument identity in `DESIGN.md`.
- **One source of truth:** avoid duplicated state, controls, calculations, and data pipelines.
- **Lifecycle correctness:** service, receiver, coroutine, Flow, and UI state must respect Android lifecycle and platform restrictions.
- **Small, reversible changes:** fix verified root causes with the narrowest safe diff.
- **Testable behavior:** important behavior should have repeatable tests where practical.
- **Honest delivery:** implementation, CI, and physical-device verification are separate statuses.

## 3. Architecture boundaries

The current project uses Kotlin, Jetpack Compose, MVVM, repositories, Room, DataStore, WorkManager, and Android services/receivers. Preserve the existing architecture and naming conventions.

- UI renders state and emits user intent; it should not own business rules or blocking I/O.
- ViewModels coordinate screen state and use existing domain/repository paths.
- Repositories remain the boundary for data access and persistence coordination.
- Domain/engine code owns measurement and diagnostic rules.
- Services and receivers own platform lifecycle integration already assigned to them.
- Room is for relational/session/history data; DataStore is for small persistent preferences.
- Do not introduce DI frameworks, networking libraries, modularization, or new persistence layers without a concrete requirement and approval.

## 4. Change workflow

1. **Inspect:** branch, status, existing code, tests, design contract, and relevant history.
2. **Trace:** follow the data/control flow to the actual source of the behavior.
3. **Plan:** identify the root cause, intended behavior, scope, risks, and affected files.
4. **Implement:** make only the approved change.
5. **Review:** inspect the full diff and check for duplicated logic, regressions, accessibility, and scope creep.
6. **Verify:** run focused tests, broader tests/build as appropriate, and CI when available.
7. **Document:** update `CHANGELOG.md` for every meaningful project change.
8. **Report:** separate code implementation, automated verification, and device QA.

## 5. Change classification

- **UI-only:** must not alter sensor acquisition, polling cadence, monitoring lifecycle, persistence, charging calculations, or alert logic.
- **Behavioral:** requires a clear before/after behavior contract and tests where feasible.
- **Platform/lifecycle:** requires review of Android API-level constraints, service/receiver triggers, permissions, and real-device verification where applicable.
- **Data/schema:** requires migration/backward-compatibility review and persistence tests.
- **Documentation-only:** must not modify app source, build configuration, or runtime behavior.

## 6. Definition of done

A task is complete only when:
- the requested scope is implemented;
- the diff has been reviewed;
- relevant tests/build checks have actual recorded outcomes;
- unresolved risks are disclosed;
- `CHANGELOG.md` is updated;
- the quality checklist is reported;
- no unapproved main-branch operation has occurred.

A passing CI run does not establish physical-device correctness. Device QA must be reported separately.
