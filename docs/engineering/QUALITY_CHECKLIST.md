## 2026-11-21 — Sprint P2 physical QA and collapsed ETA layout

- [x] User-reported itel P55 QA: Real-Time Charging ETA appears after at least three valid samples.
- [x] User-reported itel P55 QA: ETA clears automatically after unplug; collapsed state reports “Tidak mengisi”.
- [x] User-reported itel P55 QA: Battery Monitoring OFF / HUD_ONLY shows “Menunggu pemantauan” without a fabricated ETA.
- [x] User-reported itel P55 QA: 11-minute USB session (59% → 60%, average +194 mA, peak +305 mA) produced no false-positive CC/CV stagnant warning.
- [x] Collapsed notification ID 1001 moves ETA to the content title; contentText remains the three-metric row.
- [x] Expanded BigTextStyle keeps the full “Estimasi penuh: ...” line.
- [x] Unit regression coverage added for warm-up title, compact numeric ETA title, clean collapsed metrics, and expanded ETA.
- [x] Updated ChargingEtaPresentationTest to assert the new collapsed-title contract instead of the superseded ETA-in-contentText contract.
- [x] ETA estimator, watchdog logic, Battery Monitoring lifecycle, HUD_ONLY isolation, and Status Bar Indicator path remain unchanged.
- [x] Run #400 failure traced to the obsolete presentation assertion; corrected on the branch.
- [x] Run #402 push CI — PASS: Anti-Slop UI Audit Gate, 183/183 unit tests (0 failed/errors/skipped), assembleDebug, APK upload, and unit-test report upload.
- [x] Run #403 pull_request CI — PASS on attempt 2; attempt 1 had a transient KSP plugin-resolution failure before compilation.
- [x] Run #402 artifact IDs: APK 14703432370; unit-test reports 14703611094.
- [x] Diff review against main baseline 8655255 — PASS; 14 changed files, all within the approved P2 scope.
- [ ] Squash merge to main — authorized by user; pending final merge operation and post-merge main CI.

---

## 2026-11-21 — Persistent notification icon test fixture determinism

- [x] CI #397 XML report inspected; exact failure is expected icon type 1 versus observed type 2 at test line 564.
- [x] Root cause verified: the test's local `LiveBatteryState` fixture was not passed to either notification build; the one-argument helper read the service snapshot.
- [x] Both test builds now pass the same explicit fixture through the existing two-argument helper; assertions remain unchanged.
- [x] Source contract reviewed: ID 1001 uses the static drawable when Status Bar Indicator is OFF and the dynamic metric bitmap when ON and renderable.
- [x] Battery Monitoring / ETA separation reviewed: `HUD_ONLY` and Battery Monitoring OFF reset ETA tracking and clear the estimate; no change made.
- [x] P2 estimator/watchdog algorithms and P0/P1 protected paths unchanged.
- [x] `testDebugUnitTest` — PASS, 183/183 tests, 0 failed/errors/skipped (GitHub Actions #398).
- [x] `assembleDebug` — PASS (GitHub Actions #398).
- [x] Anti-Slop UI Audit Gate — PASS (GitHub Actions #398; no UI/layout changed).
- [x] `android-debug-apk` and `android-unit-test-reports` — both uploaded successfully (GitHub Actions #398).
- [ ] Physical-device QA on itel P55 — NOT RUN.
- [ ] Merge to `main` — HOLD / NOT DONE.

---

## 2026-11-21 — Sprint P2: charging ETA and CC/CV stagnant watchdog

- [x] Main baseline verified at 8655255710111c7e0070460d795f3abc80a834c2; feature branch created from exact SHA.
- [x] CI push triggers include main and the active P2 feature branch; existing triggers retained.
- [x] ETA uses positive available battery-side current only while charging and externally connected.
- [x] ETA uses a bounded 120-second timestamp-weighted EMA with a 30-second half-life.
- [x] ETA requires at least three valid samples; stale samples and EMA at or below 50 mA produce no numeric estimate.
- [x] Capacity validates stored estimated capacity; otherwise uses configured/system design capacity and 5000 mAh fallback.
- [x] The 1.25x taper applies only to the remaining 80–100% CV segment.
- [x] Expanded notification ID 1001 and Charging tab show “Estimasi penuh”; collapsed title carries compact ETA / “Menghitung ETA...” and the three-metric content body stays clean.
- [x] CC watchdog: SoC below 80%, current below 150 mA, no SoC gain for 10 minutes.
- [x] CV watchdog: SoC 80–99%, current below 120 mA, no SoC gain for 20 minutes; 100% excluded.
- [x] Watchdog resets on unplug/non-charging, invalid sensors, SoC gain, session reset/new session, and sustained recovery >=300 mA for 30 seconds.
- [x] STAGNANT_CHARGING uses existing anomaly ID 1002 and persisted session dedupe.
- [x] Unit tests added for ETA and watchdog boundaries/reset behavior.
- [x] ETA integer-boundary rounding compensates only four ULPs of floating-point drift; a fractional-minute regression test still rounds upward.
- [x] Diff review against base SHA 8655255710111c7e0070460d795f3abc80a834c2 — PASS; 14 changed files, all in the approved P2 scope.
- [x] ./scripts/ui-audit.sh — PASS in GitHub Actions Run #402.
- [x] gradle --no-daemon testDebugUnitTest assembleDebug — PASS in GitHub Actions Run #402; 183/183 unit tests.
- [x] Debug APK and test report artifacts — PASS in Run #402 (artifact IDs 14703432370 and 14703611094).
- [x] Physical-device QA for P2 ETA/watchdog — PASS (user-reported itel P55; detailed cases in the 2026-11-21 follow-up above).
- [ ] Merge to main — authorized; pending final CI and squash merge.

---
## 2026-11-21 — Anomaly bitmap cache identity fix

- [x] Root cause verified: anomaly cache key used only icon resource and severity accent color, so distinct critical anomaly types collided.
- [x] Bitmap cache key includes semantic anomaly identity, icon resource, accent, label, severity, and all renderer surface/arc colors.
- [x] Cache lookup derives key from the supplied spec's current rendered properties, so altered spec copies cannot reuse an incompatible bitmap.
- [x] Regression tests cover identical-spec instance reuse, distinct anomaly-code separation, and changes to icon, accent, label, and severity.
- [x] ID 1001 LargeIcon omission and live Status Bar Indicator path left untouched by this fix.
- [x] P0/P1 lifecycle, persistence, RECOVERY_HOLD, current normalization, notification gate, and auto-start logic left untouched.
- [ ] `./scripts/ui-audit.sh` — pending GitHub Actions.
- [ ] `gradle --no-daemon testDebugUnitTest assembleDebug` — pending GitHub Actions.
- [ ] Debug APK and unit-test report artifacts — pending GitHub Actions.
- [ ] Physical-device QA — NOT RUN; no physical retest is claimed for this cache-only correction.
- [ ] Five pre-merge gates and squash merge — pending CI and final diff audit.

---

## 2026-11-21 — Final notification cleanup and anomaly copy

- [x] Monitoring notification ID 1001 no longer attaches a circular LargeIcon; its resource/dynamic small icon remains configured.
- [x] Monitor-specific badge bitmap rendering was removed; the separate anomaly notification LargeIcon remains in use.
- [x] Live Status Bar Indicator still renders its selected parameter through `LiveStatusBarRenderer` and `setSmallIcon`.
- [x] Centralized anomaly formatter covers every code emitted by the branch's `AnomalyDetector`, requested voltage/drain/low-current/stagnation aliases, and unknown-code fallback.
- [x] History session details and anomaly notifications use the centralized Indonesian formatter; anomaly notification no longer sets subtext.
- [x] Unit tests cover mappings, thermal peak/no-peak, multi-code display, fallback humanization, ID 1001 LargeIcon omission, and retained small-icon behavior.
- [x] `applicationId = "com.superdenot.superdguard"` confirmed; `namespace = "com.superdenot.powerguard"` retained.
- [x] `app/debug.keystore` remains intact and configured in the debug signing config.
- [x] CI #361 on baseline `4898f5a`: PASS across Anti-Slop, unit tests, assembleDebug, and artifact uploads.
- [x] Physical QA on itel P55 for baseline layout, permission refresh, and notification states: PASS (user-confirmed).
- [ ] CI #362: FAIL — one anomaly bitmap cache test failed; Anti-Slop and assembleDebug passed; report artifact uploaded.
- [x] P0/P1 core logic untouched: Room transaction/finalization, recovery checkpoint ownership/durability, current normalizer, 250ms notification gate/sequence/dedup/unplug suppression, service operation policy, and auto-start coordinator.
- [x] No SDK, dependency, app version, build number, namespace, application ID, or Room schema change; confirmed application ID is documented above.

---

## 2026-11-20 — Notification parameter label and font-safe History range

- [x] HUD_ONLY notification ID 1001 body is formatted from the selected StatusBarParameter using Indonesian labels and the matching measured reading.
- [x] All six parameter selections have unit coverage; unavailable readings use <Label> • —.
- [x] Charging status text distinguishes Mengisi (AC), Tidak Mengisi, and exposed USB/Wireless sources.
- [x] History session range formatter uses ASCII ->; regression test covers 38% -> 38% and a changing range.
- [x] Scope review: no intended edits to Battery Monitoring / Status Bar Indicator lifecycle, normalizer, notification gate, sequence guard, deduplication, cooldown, persistence, or sensor cadence.
- [x] `./scripts/ui-audit.sh` — PASS in GitHub Actions CI #361.
- [x] `gradle --no-daemon testDebugUnitTest assembleDebug` — PASS in GitHub Actions CI #361.
- [x] Physical-device QA on itel P55 for layout, permission refresh, and collapsed/expanded notifications — PASS (user-confirmed).
- [x] No SDK, dependency, package, application version, build number, or Room schema change; main untouched.

---

## 2026-11-20 — Physical QA follow-up: layout, permission refresh, and notification content (CI and device QA verified)

- [x] Charging header, Hero padding, divider spacing, 2×2 metric grid (8dp row gap; 10dp metric-card vertical padding), and session summary compacted.
- [x] Charging protocol value has a weighted, single-line value column.
- [x] Overview, Charging, Insights, History, and Settings scroll containers reserve at least 96dp plus navigation-bar inset at the bottom.
- [x] Insights and History nested Analytics lists also receive the bottom content reserve.
- [x] Removed duplicate manual permission refresh buttons and their Snackbar UI.
- [x] Added ON_RESUME refresh to Initial Setup and active Overview; activity refresh remains for other tabs and charger-start lifecycle behavior is retained.
- [x] History range formatter was already correct; repository code search found zero literal `'n` matches in `app/src/main`.
- [x] Monitor notification ID 1001 no longer sets visual subtext; compact body and expanded BigTextStyle cover level, current, power, voltage, temperature, net charge, charging status, source, and protocol.
- [x] App activity title resolves from PackageManager with package-name fallback; package subtitle is one-line ellipsized.
- [x] P0/P1 service lifecycle, normalizer, notification post gate, sequence guard, deduplication, and cooldown logic unchanged.
- [x] `./scripts/ui-audit.sh` — PASS in GitHub Actions CI #361.
- [x] `gradle --no-daemon testDebugUnitTest assembleDebug` — PASS in GitHub Actions CI #361.
- [x] Physical-device QA on itel P55 for 360×800dp layout, permission return refresh, and collapsed/expanded notification — PASS (user-confirmed).
- [x] No SDK, dependency, version, namespace, application ID, or Room schema change; `main` untouched.

---

## 2026-11-20 — Physical QA UI polish (implementation review; CI verified)

- [x] Initial Setup refresh action now shows a Snackbar after refreshing Android permission state.
- [x] Overview Kesiapan Sistem refresh action now shows the same Snackbar summary.
- [x] Snackbar reports notification permission, battery-optimization exemption, and usage-access state from refreshed values.
- [x] Session level ranges consistently render as `start% → end%` through an explicit `\u2192` formatter.
- [x] Unit test verifies the exact session level range string.
- [x] Foreground notification title is state-aware and no longer duplicates the app brand in the content title.
- [x] Notification title formatting has charging, idle/discharging, and unavailable-level test coverage.
- [x] P0/P1 service lifecycle, current normalizer, notification lock/sequence guard/deduplication/cooldown logic unchanged; only notification title presentation inputs changed.
- [x] Anti-Slop UI audit — PASS in GitHub Actions run #358.
- [x] `testDebugUnitTest` and `assembleDebug` — PASS in GitHub Actions run #358; APK and unit-test report artifacts uploaded.
- [ ] Physical-device QA for these three polish changes — not yet confirmed as passing; follow-up screen-recording review identified additional issues.
- [x] No SDK, dependency, version, namespace, application ID, or Room schema change.
- [x] `main` untouched; no merge performed.

---

## 2026-11-20 — P0/P1 audit fixes ported onto latest UI baseline (implementation review; CI verified)

- [x] Target branch created directly from `fix/auto-start-coordinator-timing` at `d7dbec01d412b95a8129659a0991a2d43bca6519`.
- [x] Latest Industrial Instrument UI, five-tab telemetry components, AutoStartCoordinator changes, `DESIGN.md`, `AGENTS.md`, `DEVELOPMENT_WORKFLOW.md`, and `scripts/ui-audit.sh` preserved.
- [x] Application ID is `com.superdenot.superdguard` in Gradle, README, and About; Kotlin namespace remains `com.superdenot.powerguard`.
- [x] Current polarity is preserved while external power is connected; unplugged current remains normalized as discharge.
- [x] Regression tests cover positive charge, negative net drain while plugged, unavailable current, and ordinary unplugged discharge.
- [x] Net-drain classification requires external power, available current data, and sustained near-zero/negative current through the existing three-minute no-input window; no protocol is inferred.
- [x] Protocol metadata defaults to `Not Exposed by Device`; no voltage/current/wattage threshold is used as a protocol identity.
- [x] Battery Monitoring and Status Bar Indicator remain independent; both fresh-install defaults are OFF.
- [x] Existing OR-gate lifecycle and RECOVERY_HOLD precedence are retained; existing tests cover all toggle combinations and finalization transition.
- [x] Existing service logic finalizes an active Room session when Battery Monitoring transitions ON→OFF.
- [x] Notification ID 1001 uses a shared post/publication lock, sequence guard, duplicate gate, and 250 ms power-transition cooldown; stable metric payload updates remain supported.
- [x] Obsolete root `INSTALL-INSTRUCTIONS.txt` removed; historical changelog/checklist entries preserved.
- [x] Anti-Slop UI audit script — PASS in [GitHub Actions run #356](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/44712530728).
- [x] `testDebugUnitTest` — PASS in GitHub Actions run #356.
- [x] `assembleDebug` — PASS in GitHub Actions run #356; debug APK artifact uploaded.
- [ ] Physical-device QA — NOT RUN; required before treating Android OEM lifecycle/notification behavior as validated.
- [x] No SDK, dependency, application version, namespace, application ID, or Room schema change made.
- [x] CI verified exact source head `0ccb48aada965c748760c7723622810456bad367`; local build was not run.
- [x] `main` untouched; no merge performed.

---

## 2026-11-19 — Unified service lifecycle and persistent notification (implementation review)

- Source/diff review: PASS — lifecycle call paths, Room-write gates, notification ID/channel references, and changed-file scope reviewed.
- Approved scope and branch target `fix/auto-start-coordinator-timing`: PASS — changes limited to service lifecycle, preference synchronization, notification consolidation, regression coverage, and documentation.
- Keep-alive rule: PASS — policy resolves Monitoring OR Status Bar Indicator OR RECOVERY_HOLD.
- Four toggle combinations: PASS — policy regression tests added for STOPPED, HUD_ONLY, and MONITORING_ACTIVE.
- RECOVERY_HOLD precedence: PASS — policy test covers both toggles OFF while recovery remains required; service handlers retain the recovery-first path.
- Immediate Room write gate: PASS — Monitoring OFF closes the in-memory recording gate synchronously before the actor performs finalization.
- HUD_ONLY Room safety: PASS — charger-connect handler does not call `recoverOrStartChargingSession()` when Monitoring is OFF; monitoring loop bypasses sample aggregation/buffering/persistence while in HUD_ONLY.
- Monitoring ON while unplugged: PASS — service remains in MONITORING_STANDBY with the sensor loop stopped when Status Bar Indicator is OFF; when the indicator is ON, it uses HUD_ONLY to keep live metrics updated without Room writes. Explicit manual start remains a separate RUNNING_MANUAL path.
- ON→OFF finalization: PASS — settings actor and explicit toggle-stop path finalize under `sharedSessionMutex`; successful finalization clears active session state so repeated sync commands do not re-finalize an already completed session.
- Recovery retry: PASS — checkpoint retry path no longer creates a new charging session when Monitoring is OFF.
- Charger connect/disconnect: PASS — receiver/coordinator synchronize an active service; disconnect finalizes the charging session and returns to MONITORING_STANDBY or HUD_ONLY according to the toggles. The charging-constrained WorkManager fallback is scheduled when either feature toggle is ON.
- Persistent notification: PASS — ID 1001 is the sole persistent service card; ID 1003 is cancelled during startup and its old channel is deleted; ID 1002 remains separate.
- Dynamic/static icon selection: PASS — LiveStatusBarRenderer is used only for ID 1001 when Status Bar Indicator is ON; static NotificationVisualRenderer icon is the fallback.
- Anti-flicker: PASS — signature deduplication, 4-second body throttle, and cache reset on toggle/metric/mode changes implemented.
- Charging fallback scheduler: PASS — periodic and one-time charging-constrained work is retained when either feature toggle is ON.
- Android background-start policy: PASS — coordinator retains foreground/background eligibility checks; no attempt to bypass platform restrictions.
- Unit/regression tests: NOT RUN — pending GitHub Actions.
- Android build / assembleDebug: NOT RUN — pending GitHub Actions.
- Anti-Slop UI Audit Gate: NOT RUN — no Compose screen/layout changed; notification-specific visual behavior is covered by Robolectric regression tests.
- Physical-device QA: NOT RUN — target-device validation still required.
- CHANGELOG updated; package, application ID, SDK, dependencies, version, and release signing unchanged.
- Branch `main` untouched; merge and push remain prohibited.

## 2026-11-19 — Reproducible debug signing (verified)

- Dedicated development keystore added at `app/debug.keystore`: PASS — alias and credentials verified with keytool; SHA-1 fingerprint `AF:4C:A2:92:11:1D:E4:58:2F:A2:18:76:C3:EF:D4:F9:7C:0C:7C:B2`.
- Only `buildTypes.debug` is explicitly assigned the repository debug signing config: PASS — source review.
- Release signing configuration and release build type: N/A — untouched.
- `assembleDebug`: PASS — GitHub Actions #322, attempt 2.
- `testDebugUnitTest`: PASS — 136 passed, 0 failed, 0 errors, 0 skipped; XML reports checked from workflow artifact.
- Anti-Slop UI Audit Gate: PASS — GitHub Actions #322.
- CI attempt 1: FAIL — one assertion in the existing `NotificationVisualRendererTest` (line 331); rerun attempt 2 passed without source changes.
- Physical install/update test: NOT RUN — requires device validation.
- Debug signing key is development-only and must never be used for production/release signing.
- One-time migration note: an already-installed APK signed with a different debug certificate must be uninstalled before installing this fixed-signature build; subsequent local/CI debug updates use the same certificate.
- Branch `main` untouched; merge remains HOLD.

## 2026-11-19 — Tahap 2 lifecycle fallback dan Status Bar Indicator (completed)

- Scope sesuai persetujuan Tahap 2: PASS.
- Audit MainActivity lifecycle: PASS — hook ON_START/ON_STOP yang sudah ada mengirim status foreground; tidak diperlukan perubahan source pada MainActivity.
- FallbackSensorPoller lifecycle gate: PASS — polling hanya membaca ketika app foreground, Battery Monitoring ON, dan data service tidak fresh; gate diperiksa sebelum/sesudah snapshot.
- Pencegahan loop ganda: PASS — satu job disimpan pada ViewModel dan start guard memeriksa job aktif; perubahan foreground/background hanya mengubah gate, bukan membuat loop baru.
- ViewModel coroutine cancellation: PASS — operasi suspend memakai helper try/catch yang melempar ulang CancellationException; error operasional biasa tetap fail-closed.
- Monitoring Notification: PASS — small icon berasal dari NotificationVisualRenderer dan tidak lagi bergantung pada LiveStatusBarRenderer.
- Status Bar Indicator: PASS — notification ID 1003 dan channel terpisah; channel IMPORTANCE_LOW, tanpa suara/getar; update hanya saat kedua toggle ON dan service aktif.
- Cleanup indikator: PASS — status toggle OFF, Battery Monitoring OFF, actor STOPPED, dan service onDestroy membatalkan notification indikator.
- Kebenaran data: PASS — formatter menampilkan N/A untuk metric/status yang unavailable dan mempertahankan measured zero yang valid.
- Regression/unit tests: PASS — 136 tests pada workflow final #320; test harness memberikan POST_NOTIFICATIONS dan memakai ShadowNotificationManager untuk verifikasi notification posting/cancel.
- Android build: PASS — assembleDebug pada workflow #320; APK debug berhasil diunggah.
- Anti-Slop UI Audit Gate: PASS — workflow #320.
- UI screen review: N/A — tidak ada perubahan layout/screen Compose; perubahan visual terbatas pada notification surface yang tidak dicakup DESIGN.md.
- Pengujian perangkat fisik: NOT RUN — status ikon di System UI, suara/getar channel pada OEM, serta lifecycle polling di perangkat nyata masih perlu diuji.
- CHANGELOG dan checklist diperbarui; app version/build number, package, SDK, dan dependencies tidak diubah.
- Branch main tidak disentuh; merge tetap HOLD.

## 2026-11-19 — Tahap 1 availability persistence (completed)

- Scope Tahap 1 dan branch target `fix/auto-start-coordinator-timing`: PASS.
- Availability domain Engine/Normalizer/Integrator: PASS — sensor/status availability diteruskan tanpa mengubah measured zero menjadi unavailable.
- BatteryMonitorService: PASS — mAh integrator, current average, temperature maximum, weighted buffer, Room sample, dan final sample digate oleh availability.
- `RECOVERY_HOLD`: PASS — checkpoint lama default availability false; aggregate availability disimpan; same-session merge dan flags sampel konsisten; validasi ownership dan read-back verification tetap dipertahankan.
- Room v1→v2: PASS — migrasi additive `ALTER TABLE ... ADD COLUMN ... DEFAULT 0`; data sesi/sampel lama dipertahankan; schema export aktif.
- DAO aggregates: PASS — current/temperature hanya memakai sampel valid; mAh tidak mengintegrasikan interval yang melintasi sampel unavailable; nilai 0 valid tetap diterima.
- Charging curve: PASS — path terputus pada unavailable samples; tooltip menunjukkan “Tidak tersedia”; measured zero tetap diplot jika available.
- Unit/regression tests: PASS — 129 tests, 0 failed/errors/skipped; `AppDatabaseMigrationTest` termasuk.
- GitHub Actions #306: PASS — `testDebugUnitTest`, `assembleDebug`, dan Anti-Slop UI Audit Gate.
- Debug APK dan unit-test report artifacts: PASS — diunggah oleh workflow.
- UI/UX review: PASS — hanya perilaku data chart yang diubah; arah visual dan komponen yang ada dipertahankan.
- Pengujian perangkat fisik: NOT RUN — tidak diklaim sebagai verifikasi source/CI.
- Tahap 2 dan merge ke `main`: HOLD.


---

## 2026-11-21 — P0 charging protocol detection

- Approved scope and conservative evidence tiers: PASS — exact protocol labels require an allowlisted explicit sysfs/HAL value; BatteryManager labels remain source-capability classifications.
- Fixed read-only sysfs allowlist and exception handling: PASS — source review; SecurityException is distinguished from IOException/missing-node outcomes.
- Unit conversion: PASS — µA/µV converted to mA/mV in the detector; deterministic tests added.
- Dashboard, Charging screen, and expanded monitoring notification detail: PASS — source review.
- Battery Monitoring lifecycle, HUD_ONLY, RECOVERY_HOLD, P1 protections, ChargingEtaEstimator, and CcCvStagnantWatchdog: PASS — no algorithm or lifecycle edits intended; confirm by final diff.
- Focused unit tests: NOT RUN — pending branch CI.
- Android build and Anti-Slop UI Audit Gate: NOT RUN — pending branch CI.
- APK artifact upload: NOT RUN — pending branch CI.
- Physical-device QA on itel P55: NOT RUN — requires installation and observation; do not infer protocol availability from CI.
- Package, SDK, dependencies, app version, and build number: PASS — unchanged.

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
