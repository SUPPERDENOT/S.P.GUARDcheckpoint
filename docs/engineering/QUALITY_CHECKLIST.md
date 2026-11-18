# SPG Quality Checklist

Complete this checklist for every meaningful code change. For documentation-only changes, mark code/runtime checks N/A and state why.

Use only:
- **PASS** — checked and supported by evidence.
- **FAIL** — checked and did not meet the requirement.
- **N/A** — not relevant to this change, with a reason.
- **NOT RUN** — relevant but not executed; this blocks a claim of full verification.

## 1. Scope and code review

- [ ] Scope matches the approved request.
- [ ] Root cause or reason for change is documented.
- [ ] Existing implementation was inspected before adding new logic.
- [ ] Diff contains no unrelated edits.
- [ ] No duplicated state, control, calculation, or data source was introduced.
- [ ] Existing behavior and compatibility were considered.

## 2. Product and diagnostic integrity

- [ ] Measured values, estimates, unavailable values, and interpretations are distinguished.
- [ ] No fabricated sensor readings, metrics, protocol names, or diagnostic certainty.
- [ ] Missing sensor data is not silently represented as zero.
- [ ] Unknown charging protocol remains unknown without reliable evidence.
- [ ] Anomaly wording describes evidence/indications rather than asserting unproven hardware failure.

## 3. UI/UX (mark N/A for non-UI work)

- [ ] Follows `DESIGN.md` and Industrial Instrument direction.
- [ ] Information hierarchy is clear and content-driven.
- [ ] No unnecessary cards, decorative effects, badges, or animation.
- [ ] Text, values, and controls remain readable on target phone sizes.
- [ ] Relevant loading, empty, unavailable, normal, caution, and error states are handled.
- [ ] Interactive controls work and provide feedback.
- [ ] Accessibility semantics, labels, contrast, and touch targets were considered.
- [ ] Scrolling and system insets do not hide important content.

## 4. Android architecture and lifecycle

- [ ] Responsibilities remain in the appropriate UI/ViewModel/domain/repository/service layer.
- [ ] Coroutines and Flow respect cancellation and lifecycle.
- [ ] No blocking I/O was added to the main thread.
- [ ] Service/receiver/permission behavior is correct for relevant Android versions.
- [ ] Persistence and migration compatibility were considered when applicable.

## 5. Verification

- [ ] Relevant unit tests executed.
- [ ] Relevant instrumentation/UI tests executed, or N/A with reason.
- [ ] Android build/CI executed.
- [ ] Existing regression-sensitive behavior was checked.
- [ ] Physical-device test performed where needed, or explicitly marked NOT RUN.
- [ ] Test/build results are reported accurately with run/link/commit when available.

## 6. Delivery and documentation

- [ ] `CHANGELOG.md` updated and historical entries preserved.
- [ ] Implementation status is separate from CI status and device QA status.
- [ ] No unapproved version/build number change.
- [ ] No unapproved commit, push, merge, or main-branch operation.
- [ ] Known limitations and follow-up work are disclosed.

## Required completion report

Provide a compact report with:
- Branch and commit SHA.
- Files changed.
- Checklist status (PASS / FAIL / N/A / NOT RUN).
- Tests and build checks with actual result.
- Physical-device QA status.
- Known risks or remaining work.

A task with a relevant NOT RUN or FAIL item is not fully verified. Do not describe it as complete without stating the limitation.


## 2026-11-18 — Background Auto Start CI #257 follow-up

- Root cause identified from CI log: stale unit-test references to the renamed `canStartFromPowerConnected` helper: PASS
- Production Kotlin compilation and debug APK assembly in CI #257: PASS
- Unit-test source compilation in CI #257: FAIL — four stale helper references; corrected in follow-up source change, rerun pending
- Anti-Slop UI Audit Gate in CI #257: PASS
- Follow-up unit-test/CI verification: NOT RUN — awaiting new Actions run
- UI/UX review: N/A — no UI change
- Physical-device QA: NOT RUN — requires itel P55 NFC
- Runtime Battery Monitoring lifecycle and RECOVERY_HOLD regression checks: NOT RUN


## 2026-11-18 — Background Auto Start implementation checklist

- Scope matches approved Auto Start receiver/WorkManager change: PASS
- Root cause and existing implementation inspected: PASS
- Diff limited to approved platform lifecycle and regression-test scope: PASS
- No duplicate service, preference, or sensor source introduced: PASS
- UI/UX checklist: N/A — no UI changes
- Android service/receiver and background-start paths reviewed: PASS
- Relevant unit tests: NOT RUN — pending branch CI
- Android build/CI: NOT RUN — pending branch CI
- Physical-device QA: NOT RUN — requires the user's itel P55 NFC
- Changelog updated; app version/build number and dependencies unchanged: PASS
