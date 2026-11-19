## 2026-11-19 — Tahap 1 availability persistence (in progress)

- Scope Tahap 1 disetujui dan branch target diverifikasi: PASS — `fix/auto-start-coordinator-timing`.
- README/Application ID dan batasan diagnostik protokol charging: PASS — dokumentasi pada commit Tahap 1 docs.
- Propagasi availability Engine → Normalizer → Integrator → Service → Room: NOT RUN — implementasi belum selesai.
- `RECOVERY_HOLD` mempertahankan flags availability: NOT RUN.
- Room migration v1→v2 non-destruktif dan schema export: NOT RUN.
- Chart memutus garis pada sampel unavailable dan tooltip menampilkan unavailable: NOT RUN.
- Unit/regression tests termasuk `AppDatabaseMigrationTest`: NOT RUN.
- `testDebugUnitTest` dan `assembleDebug`: NOT RUN — menunggu implementasi selesai dan CI branch.
- Pengujian perangkat fisik: NOT RUN — bukan target verifikasi source Tahap 1.
- Tahap 2 dan merge ke `main`: HOLD.

---

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

## 2026-11-18 — Periodic charging Auto Start recovery

- Scope limited to charging fallback scheduling, Worker result mapping, regression tests, and documentation: PASS
- Existing Worker, scheduler, coordinator, WorkManager dependency (2.10.0), and project standards inspected: PASS
- Accepted/in-flight/already-active/disabled/not-charging outcomes no longer trigger unnecessary retry: PASS — source review
- Deferred/failed outcomes retain WorkManager retry handling: PASS — source review
- Periodic request constrained to charging with a 15-minute minimum interval: PASS — source review
- BatteryMonitorService, RECOVERY_HOLD, sensor cadence, persistence, package, SDK, dependencies, and app version unchanged: PASS — diff scope
- New Worker outcome unit tests: NOT RUN — awaiting branch CI
- Android build/CI: NOT RUN — awaiting branch Actions run
- Runtime lifecycle / RECOVERY_HOLD regression checks: NOT RUN
- Stale process-local pending-start marker recovery: NOT RUN — intentionally not changed; requires separate lifecycle design to avoid duplicate service starts
- Physical-device QA on itel P55 NFC: NOT RUN
- UI/UX review: N/A — no UI changes
- Changelog updated and historical entries preserved: PASS

## 2026-11-18 — Dynamic power-event receiver export experiment

- Scope limited to changing the application-lifetime dynamic receiver flag to `RECEIVER_EXPORTED` and documenting the controlled device experiment: PASS — source review
- Receiver filter remains limited to `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED`: PASS — source review
- Manifest receiver, WorkManager scheduler/worker, AutoStartCoordinator, BatteryMonitorService, and Battery Monitoring lifecycle unchanged: PASS — change scope
- Android build and unit tests: NOT RUN — awaiting branch CI
- Physical-device A/B test on itel P55 NFC (immediate-after-Home and cached/frozen-process scenarios): NOT RUN
- AutoStartDiagnosticLog comparison for receiver delivery, delayed delivery after thaw, and coordinator outcome: NOT RUN
- Exported receiver exposure reviewed: PASS — only the two protected system power actions are registered; do not add unrelated actions
- Cached-process scheduling and OEM background restrictions: NOT proven resolved by this flag change; physical evidence required
- UI/UX review: N/A — no UI change
- Changelog updated; package, SDK, dependencies, app version, and build number unchanged: PASS


## 2026-11-19 — Hybrid charging Auto Start recovery

- Scope limited to the one-time charging-constrained recovery path, integration points, tests, and documentation: PASS — source review
- Existing periodic unique work name, 15-minute interval, charging constraint, and retry mapping preserved: PASS — source review
- One-time work uses a separate unique name, `KEEP`, and `requiresCharging=true`; no expedited request: PASS — source review
- One-time work is armed only when Battery Monitoring is ON and `EXTRA_PLUGGED == 0`; unknown plug state fails closed: PASS — source review
- Application startup, boot, and disconnect arm paths are integrated; worker does not self-rearm: PASS — source review
- Battery Monitoring OFF cancellation covers periodic and one-time work: PASS — scheduler cancellation API and existing ViewModel call sites
- Pending one-time work is cancelled on charger connect without deliberately cancelling a RUNNING worker: PASS — source review; timing race still requires device observation
- Existing Worker outcome mapping and cancellation semantics preserved: PASS — source review
- Focused unit tests: PASS — CI #271, `testDebugUnitTest`; unit-test report artifact uploaded
- Android build/CI: PASS — CI #271, `assembleDebug`; debug APK artifact uploaded
- Anti-Slop UI Audit Gate: PASS — CI #271
- Physical-device lifecycle / XOS scheduling tests: NOT RUN
- BatteryMonitorService, RECOVERY_HOLD, sensor cadence, persistence, dependencies, SDK, package, app version, and build number unchanged: PASS — diff scope
- UI/UX review: N/A — no UI changes
- Changelog updated and historical entries preserved: PASS



## 2026-11-19 — Physical-device Auto Start validation (itel P55 NFC)

Evidence reviewed: `SPG_AutoStart_Filtered.txt` and `spg_job_and_battrey.txt`. These are separate captures/test sessions; the UID differs (`u0a282` in the Auto Start log and `u0a283` in the JobScheduler dump). The user confirms an APK reinstall occurred between sessions, so the UID difference is expected and must not be treated as one continuous process identity. The battery dump records 79%; 83% was observed in a separate capture at a different time and is not a conflicting same-time measurement.

- Dynamic receiver while the SPG process is alive: **PASS** — the log records T0 charging detection, accepted coordinator request, Foreground Service entry, and `RUNNING_CHARGING` (T0→T2 403 ms for epoch 1; 146 ms for epoch 2).
- Auto Start after swipe/remove-task process death: **FAIL** — ActivityManager records SPG process termination with reason `remove task`; the subsequent charger-event warnings shown are for other packages, with no SPG receiver/coordinator/worker execution evidence in this capture.
- JobScheduler `CHARGING` constraint: **FAIL / BLOCKED** — the two SPG WorkManager jobs (`#u0a283/4` and `#u0a283/6`) both require charging, report `CHARGING` under Unsatisfied constraints, and show `Ready: false`.
- Worker execution: **NOT VERIFIED** — the dump establishes that the jobs were not ready; it does not show the Worker body starting or returning a result.
- BatteryService snapshot: **PASS as a reported system state only** — `AC powered: true`, `status: 2`, level 79% at the capture. This does not establish that JobScheduler's charging constraint was satisfied.
- Locking SUPERD POWER GUARD in Recent Apps: **NOT RUN** — possible operational mitigation only; no result is claimed until separately tested.
- Root cause attribution to Unisoc HAL / a missing or delayed `ACTION_CHARGING` event: **NOT VERIFIED** — the captures prove the unsatisfied JobScheduler constraint, not the underlying framework/HAL cause.
- Android build and unit tests: **N/A** — documentation-only recording of device evidence; no application source or runtime behavior changed in this entry.
- Changelog cross-reference and evidence limitations recorded: **PASS**.
