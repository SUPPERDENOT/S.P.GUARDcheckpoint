## 2026-11-23 — Charging source-capability labels and battery-side power clarity

### Changed
- Renamed AC Tier 2 classifications so each label explicitly describes source capability: `AC — Kemampuan Sumber Standar`, `AC — Kemampuan Sumber Tinggi (≥10 W)`, `AC — Kemampuan Tegangan 9V+`, and `AC — Kemampuan Sumber Tidak Tersedia`.
- Kept the classification inputs unchanged: BatteryManager maximum charging-current / maximum charging-voltage metadata and the existing thresholds. Battery-side measured power is not used to classify source capability.
- Expanded the Charging protocol information dialog to distinguish source capability metadata from battery-side power (battery voltage × battery current), explain transient adjustment after connection, and recommend comparing stable readings under similar battery level, temperature, and device-load conditions.
- Renamed the Charging metric to `Daya baterai` and added a concise note that it is calculated from battery voltage × current.
- Added regression assertions for the standardized AC labels and the explanatory guidance.

### Protected behavior
- No protocol is inferred from actual wattage, voltage, or current. Explicit protocol names still require explicit sysfs/HAL evidence.
- No sensor acquisition, battery-side power calculation, charging thresholds, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, persistence, Room schema, dependency, SDK, package, or app-version changes.

### Verification
- Main CI Run #500 on base `77c8de31bf700e47b00717b85fb398070f2e7f5d`: SUCCESS — Anti-Slop UI Audit Gate and Build and Unit Tests.
- Feature branch implementation and regression tests: pending CI.
- Physical-device verification on itel P55: pending; do not treat CI as device validation.

## 2026-11-23 — PR #4 squash merge: UI polish and session-based SOH

- Squash-merged `feat/ui-polish-and-session-soh` into `main` (PR #4); merge commit: `94a814479fae8b803b7a27835686712b9408cbab`.
- Included the 3D adaptive launcher asset synchronization, Charging protocol explanation catalog/info action, default-ON display-only History micro-session filter, and completed-session SOH estimator using the existing configurable design capacity.
- CI Run #497 on PR head `6326dc5489e2b5f558ac352127662b902af17135`: SUCCESS — 214/214 unit tests, `assembleDebug`, Anti-Slop UI Audit Gate, and APK/test-report artifact uploads. This is PR-head CI, not yet a post-merge `main` CI result.
- Physical QA on itel P55: completed and confirmed by the user.
- The legacy green icon still shown by the Transsion security scanner is explicitly deferred for a separate investigation; no further launcher resource change was made as part of this merge.
- No Room schema, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, RECOVERY_HOLD, package, SDK, dependency, or app-version changes were part of the approved scope.

## 2026-11-22 — Run #490 Robolectric SDK configuration conflict fix

### Root cause
- Inspected Run #490 job logs and all XML test reports from artifact 14756126935. The suite completed 214 tests with 81 failures and 0 skipped; failures were infrastructure setup errors, not assertion mismatches.
- Exact exception: `java.lang.IllegalArgumentException: Path is not a file: .../app/build/robolectric-sdk/android-all-instrumented-14-robolectric-10818077-i7.jar`, from `LocalDependencyResolver.validateFile(LocalDependencyResolver.java:50)`.
- XML reports show 80 failures requesting API 34 (`14-robolectric-10818077-i7`) and 1 requesting API 26 (`8.0.0_r4-robolectric-r1-i7`). API 35 was not the missing artifact in this run.
- Root cause in the pre-warm setup: all three versions of the same Gradle module, `org.robolectric:android-all-instrumented`, were declared in one resolvable configuration. Gradle conflict resolution selects one version for a module within a configuration, so the Sync task could succeed while omitting the API 26 and API 34 jars. The prior task did not validate that every required file had actually been staged.

### Changed
- Split API 26, API 34, and API 35 into three independent resolvable Gradle configurations, preventing cross-version conflict resolution.
- The Sync task now stages all three configurations into `app/build/robolectric-sdk`.
- Added a post-sync assertion that checks all three exact Robolectric jar filenames exist. A missing SDK artifact now fails the pre-warm task immediately, before unit tests start.
- No test assertion, test enablement, production Kotlin behavior, app runtime dependency, SDK, package, app version, or business logic was changed.

### Verification
- Run #490 Anti-Slop UI Audit Gate: SUCCESS.
- Run #490 `testDebugUnitTest`: FAIL — 214 tests completed, 81 infrastructure failures (80 API 34 missing local jar; 1 API 26 missing local jar); 133 passed, 0 skipped.
- Run #490 APK upload: SKIPPED because the combined test/build command stopped at the failed unit-test task.
- New three-configuration staging fix: committed; CI verification pending.
- Physical-device QA on itel P55: NOT RUN; still required before PR #4 merge.
- PR #4 remains OPEN and unmerged; `main` untouched.

## 2026-11-22 — Robolectric SDK pre-warm for CI reliability

### Root cause
- Downloaded the Run #476 unit-test report artifact (14755063530) and inspected the XML reports. The 80 failed test invocations across Robolectric test classes share the same infrastructure failure: Robolectric `MavenArtifactFetcher` received HTTP 429 Too Many Requests from `https://repo1.maven.org/maven2` while requesting `org.robolectric:android-all-instrumented:14-robolectric-10818077-i7`.
- This is a runtime SDK-jar fetch failure, not evidence of 80 independent application assertion regressions. The original test assertions remain unchanged.
- Source inspection of Robolectric 4.14.1 `DefaultSdkProvider` confirms the pre-instrumented SDK artifact coordinates. The test suite uses API 34 class-level configurations, a method-level API 26 override, and API 35 as Robolectric's default for a class without an explicit SDK.

### Changed
- Added a dedicated, resolvable Gradle configuration for the exact Robolectric 4.14.1 pre-instrumented Android framework jars required by API 26, API 34, and API 35.
- Added `prepareRobolectricSdk` Sync task to stage those resolved artifacts in `app/build/robolectric-sdk`; all Gradle `Test` tasks depend on this preparation.
- Configured Robolectric's supported `robolectric.dependency.dir` system property to use the staged local artifacts. This prevents `MavenArtifactFetcher` from making runtime HTTP requests to Maven Central during tests.
- Added an explicit CI pre-warm step before `testDebugUnitTest` / `assembleDebug`. The existing `gradle/actions/setup-gradle@v4` cache retains Gradle-resolved artifacts between workflow runs.
- No application runtime dependency, SDK, Gradle/AGP/Kotlin version, package, versionCode/versionName, test assertion, or business logic was changed.

### Verification
- Source/configuration review: PASS — exact Robolectric 4.14.1 API 26/API 34 coordinates confirmed from the upstream version-pinned source; project test annotations inspected.
- Run #476 report inspection: PASS — HTTP 429 and exact missing artifact URL confirmed from XML test reports.
- `testDebugUnitTest` after this fix: NOT RUN — awaiting branch CI.
- `assembleDebug`, Anti-Slop UI Audit Gate, APK upload, and test-report upload after this fix: NOT RUN — awaiting branch CI.
- Physical-device QA on itel P55: NOT RUN — this CI dependency fix does not replace the required PR #4 physical QA.
- PR #4 remains OPEN and unmerged; no operation on `main`.

### Follow-up after Run #484
- The first pre-warm run successfully resolved/staged API 26 and API 34, but the XML report artifact (14755513728) showed Robolectric selecting API 35 for `AutoStartDiagnosticLogTest`, which has no explicit `sdk` value in its `@Config`.
- Run #484 therefore failed with `LocalDependencyResolver`: the expected local API 35 jar was absent. This is a test-environment SDK-coverage gap; the application assertions remain unchanged.
- Added the exact Robolectric 4.14.1 API 35 artifact `org.robolectric:android-all-instrumented:15-robolectric-12650502-i7` to the Gradle pre-warm configuration. The expected SDK set is now API 26, 34, and 35.
- Run #484: `prepareRobolectricSdk` SUCCESS; `testDebugUnitTest` FAIL (214 tests, 82 failed due to missing API 35 local jar); APK upload SKIPPED; report upload SUCCESS. Anti-Slop UI Audit Gate SUCCESS.
- Verification of the API 26/34/35 correction: NOT RUN — awaiting the next CI run.
\n---\n\n## 2026-11-22 — Launcher resource tree cleanup

### Changed
- Kept the 11 launcher raster PNG assets only in their Android resource density folders under `app/src/main/res/mipmap-*`.
- Removed the temporary `ASSET_NOTES.json` and `preview.jpg` files from the repository root and Android resource directory; neither file is an Android runtime resource.

### Verification
- GitHub source audit: all 11 expected launcher PNG paths exist under `app/src/main/res/`; root-level mipmap files were not present at audit time.
- Documentation/resource cleanup only; no Kotlin, manifest, Gradle, dependency, SDK, package, version, or runtime lifecycle changes.
- GitHub Actions for the cleanup commit: pending.
- Physical-device launcher verification on itel P55: NOT RUN; required before PR #4 merge.

## 2026-11-22 — Initial UI polish and session-based SOH analytics

### Changed
- Confirmed both Android 8.0+ adaptive launcher XML resources use the latest 3D cyan-purple neon energy-core foreground PNG. Removed the obsolete green lightning vector drawable; density-specific launcher PNG assets remain the 3D brand icon.
- Added a protocol explanation catalog and an accessible 48dp information action beside the protocol/capability label in Charging. Explanations distinguish explicit device labels from BatteryManager source-capability metadata and do not claim that metadata is measured charger output or a negotiated protocol.
- Added the default-enabled History filter "Sembunyikan sesi < 1 menit". It hides rows shorter than 60 seconds or rows with zero SoC delta and no more than 0.5 mAh delivered. Filtering is presentation-only; Room records are unchanged.
- Added a separate session-based SOH estimator under domain/analytics. It uses completed session segments with at least 15% SoC gain, at least 60 seconds duration, valid positive delivered charge, and plausible effective-capacity bounds. A median across eligible sessions reduces sensitivity to one noisy session.
- Added the Estimated SOH / effective mAh summary to Insights. It reuses the existing Settings design-capacity preference and falls back to 5000 mAh when no capacity is available. The pre-existing engine estimator and persisted Dashboard health snapshot path remain unchanged.
- Added unit tests for protocol explanation labels/fallback, micro-session filter boundaries, and SOH eligibility including the 14% rejection / 15% acceptance boundary.
- Added this feature branch to the existing Android CI push trigger; no parallel workflow or new dependency was introduced.

### Verification
- GitHub Actions Run #449 (push) and Run #450 (pull_request) on code SHA 6eb97cf6ac9311055494c0c36ebe96bfb68eb3ed: SUCCESS. Both passed Anti-Slop UI Audit Gate, Build and Unit Tests, testDebugUnitTest (214 tests, 0 failures, 0 errors, 0 skipped), assembleDebug, debug APK upload (artifact 14720034341), and unit-test report upload (artifact 14720158754).
- Earlier Run #425 initially exposed two test expectation issues (SOH boundary floating-point rounding and catalog fallback wording). The estimator now applies a small numerical tolerance at the 110% plausibility boundary; the fallback assertion matches the catalog. The later final-code runs #449/#450 completed successfully.
- Source inspection and focused diff review: PASS; 13 files changed, no Room/service lifecycle scope expansion.
- Local Gradle/UI audit: NOT RUN in the GitHub-only editing environment.
- Local Gradle/UI audit: NOT RUN in the GitHub-only editing environment.
- Physical-device QA at 360dp / 720p: NOT RUN; required before merge.
- No Room schema, service lifecycle, Battery Monitoring, Status Bar Indicator, RECOVERY_HOLD, P0/P1 safeguards, or existing engine SOH persistence logic changes intended.
- No CSV/JSON export feature added. No merge to main.

## 2026-11-21 — P0 Unisoc sysfs coverage, capability thresholds, and narrow-screen protocol layout

- Expanded the fixed read-only sysfs allowlist with Unisoc/Spreadtrum sprd-charger and sc27xx-fgu nodes, battery charge_type, and USB usb_type / real_type / type metadata paths. Specific protocol nodes are read before generic battery charge_type so a generic Fast indication cannot mask a more specific exposed protocol. Existing SecurityException and IOException handling remains in place.
- Explicit generic HAL values Fast, Adaptive, and FASTCHARGE map only to "Fast Charge (HAL)"; no proprietary protocol name is inferred.
- Updated BatteryManager source-capability wording and thresholds: AC 8,500mV+ -> "AC — Kemampuan 9V+"; maximum capability power >=10W -> "AC — Kemampuan Daya Tinggi"; lower known capability -> "AC — Daya Standar". USB and wireless labels remain explicitly unverified. AC wattage requires both max-current and max-voltage metadata; battery-side measured current/power is not used.
- Charging diagnostic value now wraps to two lines with a wider value column. Dashboard protocol capsule is constrained to 190dp and wraps to two lines without repeating the source label.
- Added tests for Unisoc allowlist paths, charge_type Fast/Adaptive, FASTCHARGE, and 2,000,000µA × 5,000,000µV = 10W capability.
- CI #415 exposed one stale Wireless expected-string assertion (198/199 passed); the test fixture was aligned to the approved concise label. The detector result itself matched the requested label. CI #417 on commit 95ff2d42ae325bd44466d7d70005aaeb8a70b929: SUCCESS — 199/199 unit tests (0 failures/errors/skipped), assembleDebug, Anti-Slop UI Audit Gate, APK artifact 14711582078, and unit-test report 14711875931. Final branch CI #419 on commit 9ceea4cb553e48e90d326b2e99fda72b250da909: SUCCESS — Anti-Slop UI Audit Gate, 199/199 unit tests (0 failures/errors/skipped), assembleDebug, APK artifact 14712121172, and test report artifact 14711607719. Physical-device retest on itel P55 remains pending. Physical-device retest on itel P55: pending.
- Battery Monitoring lifecycle, HUD_ONLY gating, RECOVERY_HOLD, P1 safeguards, ChargingEtaEstimator, and CcCvStagnantWatchdog: no intended changes.

## 2026-11-21 — P0 charging protocol detection audit and conservative classification

- Added a dedicated detector with strict evidence tiers: explicit allowlisted sysfs/HAL protocol values first; BatteryManager source-capability labels second; bounded unknown-source fallback last.
- Added read-only access to a fixed allowlist of power_supply metadata nodes. SecurityException and explicit EACCES/permission-denied IOException are distinguished from ordinary missing-node/I/O failures; the SELinux suffix is emitted only when an access denial is observed.
- HUD_ONLY is isolated from protocol detection: sysfs and hidden charging-capability extras are read only on Battery Monitoring-enabled paths. The status-bar indicator can continue updating without protocol reads; its UI state says detection is inactive.
- Optional AOSP ACTION_BATTERY_CHANGED keys `max_charging_current` and `max_charging_voltage` are hidden from the public SDK; the engine reads them by literal key only when present. Values are converted from µA/µV to mA/mV and presented as source capabilities, never as proof of USB SDP/CDP, DCP, Qi, or a negotiated fast-charge protocol. Missing extras remain unavailable.
- Updated Dashboard, Charging diagnostics, and expanded monitoring notification protocol/source detail.
- Added deterministic detector tests for explicit protocol aliases, metadata conversion, USB/AC/wireless categories, unplugged state, and SELinux/unknown fallback.
- GitHub Actions run #413 on commit `8decd883c2c89d25c6e54d7e27f1c93df0bf3af6`: SUCCESS — Anti-Slop UI Audit Gate, 197/197 unit tests (0 failures/errors/skipped), `assembleDebug`, APK artifact `14710545989`, and unit-test report artifact `14710610822`.
- P0 branch CI trigger added. Physical-device protocol exposure on itel P55: NOT RUN — requires APK installation and runtime observation.
- Battery Monitoring lifecycle, HUD_ONLY behavior, RECOVERY_HOLD, P0/P1 protections outside protocol display, ChargingEtaEstimator, and CcCvStagnantWatchdog: unchanged by scope.

## 2026-11-21 — Collapsed notification ETA summary and P2 physical QA

### Changed
- Moved the charging ETA summary for monitoring notification ID 1001 into the collapsed content title, after charging state and battery percentage.
- The title uses a compact approximate form (for example, ±58 mnt); while the estimator is warming up it shows "Menghitung ETA...".
- Kept the collapsed content body limited to current, power, temperature, and net-charge metrics. The expanded BigTextStyle retains the complete "Estimasi penuh: ..." line.
- Added regression coverage for warm-up and numeric ETA title suffixes, clean collapsed metrics, and the unchanged expanded ETA line.
- Recorded user-reported itel P55 physical verification for Sprint P2: ETA warm-up/clear-on-unplug/HUD_ONLY isolation PASS; CC/CV watchdog 11-minute USB observation PASS without false positive.

### Verification
- Run #400 exposed one obsolete assertion in ChargingEtaPresentationTest (182/183 tests passed); the assertion was corrected to the collapsed-title contract.
- GitHub Actions Run #402 (push, current branch head 85c8a8f): SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest (183/183; 0 failures/errors/skipped), assembleDebug, and both artifact uploads.
- GitHub Actions Run #403 (pull_request, current branch head 85c8a8f): first attempt failed during external KSP plugin resolution before compilation; attempt 2 rerun: SUCCESS across both jobs, including build/tests and artifact uploads.
- Run #402 debug APK SHA-256: 6f08785e820def6eca37d57fe4404d11097cb27df74af222af4d0bbfb6a79e94.
- Local Gradle/UI-audit commands: NOT RUN in this GitHub-only editing environment; the configured commands ran in GitHub Actions.
- Physical-device QA for Sprint P2 on itel P55: PASS (user-reported; details recorded in the P2 quality checklist).
- Estimator math, CC/CV watchdog thresholds/reset rules, Battery Monitoring lifecycle, HUD_ONLY isolation, and Status Bar Indicator behavior are unchanged.

---

## 2026-11-21 — Persistent notification icon test fixture determinism

### Root cause and correction
- CI #397 exposed that `persistentNotificationUsesDynamicMetricIconOnlyWhenIndicatorToggleIsEnabled` created a deterministic `LiveBatteryState` fixture but called the one-argument test helper for both notifications. That overload reads the service's current sensor snapshot, so the fixture was unused and the expected dynamic bitmap depended on ambient service state.
- Both OFF and ON notification builds now pass the prepared state through the existing two-argument helper. The behavioral assertions are unchanged: indicator OFF expects a resource icon; indicator ON expects a bitmap icon.
- No production renderer or service behavior was changed. ID 1001 continues to use the static `NotificationVisualRenderer` drawable when Status Bar Indicator is OFF and `LiveStatusBarRenderer` bitmap only when it is ON.
- Battery Monitoring remains independent: the P2 ETA path resets and clears ETA in `HUD_ONLY` / Battery Monitoring OFF; ETA tracking is enabled only for an active charging session.

### Verification
- Root cause confirmed from Run #397 XML report: expected icon type 1, observed type 2 at test line 564.
- GitHub Actions #398 (`d01c5d5`): PASS — `testDebugUnitTest` 183/183, 0 failed/errors/skipped; `assembleDebug` PASS; Anti-Slop UI Audit Gate PASS.
- Artifacts: PASS — `android-debug-apk` and `android-unit-test-reports` uploaded successfully (Run #398).
- Local Gradle/UI-audit commands: NOT RUN — no local project checkout/Gradle worktree in this execution environment; CI ran the configured workflow command.
- `ChargingEtaEstimator` (four-ULP correction, timestamp-weighted EMA, CV-only 1.25x taper) and `CcCvStagnantWatchdog` (SoC below 100%) unchanged.
- P0/P1 service lifecycle, persistence, RECOVERY_HOLD, normalization, notification render gate, and auto-start logic unchanged.
- Physical-device QA on itel P55: NOT RUN. No merge to `main`.

---

## 2026-11-21 — ETA floating-point rounding correction

### Changed
- Corrected integer-boundary ETA rounding by subtracting a four-ULP tolerance derived from the calculated minute value before applying `ceil()`. This targets IEEE-754 representation drift without suppressing meaningful fractional minutes.
- Added a regression test confirming a genuinely fractional ETA (75.075 minutes) still rounds up to 76 minutes. Existing 75- and 165-minute exact-boundary tests remain in place.
- Preserved timestamp-weighted EMA, the 1.25x CV-only taper, minimum-sample/current/SoC guards, and the CC/CV stagnant watchdog's 0–99% SoC range.

### Verification
- Source and test changes committed to the P2 feature branch; automated verification is pending the new GitHub Actions run.
- Local `./scripts/ui-audit.sh` and `gradle --no-daemon testDebugUnitTest assembleDebug`: NOT RUN because this execution environment has GitHub repository access but no local project checkout/Gradle worktree.
- Physical-device QA on itel P55 for P2 ETA/watchdog: NOT RUN.
- No change to P0/P1 lifecycle, persistence, RECOVERY_HOLD, sensor cadence, normalization, notification gate, or auto-start behavior. No merge to `main`.

---

## 2026-11-21 — Sprint P2: charging ETA and CC/CV stagnant watchdog

### Changed
- Added timestamp-weighted EMA over a bounded 120-second positive battery-side current sample window; ETA requires at least three valid charging samples and EMA current above 50 mA.
- Capacity selection validates the latest stored estimated-capacity snapshot; otherwise it uses configured/system design capacity, with a 5000 mAh fallback when unavailable or invalid.
- The 1.25x taper applies only to remaining capacity in the 80–100% CV segment; the below-80% CC segment is not multiplied.
- The expanded notification ID 1001 and Charging tab retain the full “Estimasi penuh” line; collapsed notification title carries a compact ETA or “Menghitung ETA...” warm-up suffix.
- Added a CC/CV stagnant watchdog: below 80%, current below 150 mA for 10 minutes; 80–99%, current below 120 mA for 20 minutes, without SoC gain.
- Watchdog resets on unplug/non-charging, invalid current/level, SoC gain, new/reset sessions, and sustained current recovery of at least 300 mA for 30 seconds. It emits STAGNANT_CHARGING through existing anomaly ID 1002 and session deduplication.
- Added main and the active P2 feature branch to Android CI push triggers.

### Verification
- Added unit tests for timestamp-weighted ETA, minimum samples, current/capacity/staleness guards, CC/CV thresholds, SoC progress, invalid sensors, unplug, and sustained recovery.
- GitHub Actions Run #402 (push): PASS — Anti-Slop UI Audit Gate, 183/183 unit tests, assembleDebug, and APK/test-report artifact uploads.
- GitHub Actions Run #403 (pull_request): PASS on attempt 2 after attempt 1 encountered a transient KSP plugin-resolution failure before compilation.
- Local ./scripts/ui-audit.sh and gradle --no-daemon testDebugUnitTest assembleDebug: NOT RUN in the GitHub-only editing environment; equivalent workflow commands passed in CI.
- Physical-device QA for P2 ETA/watchdog on itel P55: PASS (user-reported).
- P0/P1 lifecycle, RECOVERY_HOLD ownership, current normalization, notification post gate, sensor cadence, persistence schema, and service start policy remain protected.

---

## 2026-11-21 — Anomaly bitmap cache identity fix

### Changed
- Fixed anomaly LargeIcon bitmap cache collisions by including anomaly code/message identity, icon resource, accent color, notification label, severity, and rendered surface/arc colors in the bitmap cache key.
- Cache lookup derives the complete key from the supplied spec at render time, preventing stale keys after a spec copy changes a rendered property.
- Identical specs retain exact Bitmap instance reuse; distinct anomaly identities or visual properties receive separate cache entries.
- Monitoring notification ID 1001, Status Bar Indicator rendering, and P0/P1 lifecycle, persistence, recovery ownership, and sensor behavior remain unchanged.

### Verification
- Regression tests cover identical-spec reuse, distinct anomaly-code separation, and changes to icon, accent, label, and severity.
- Local `./scripts/ui-audit.sh`: NOT RUN in this GitHub-only editing environment; pending CI.
- Local `gradle --no-daemon testDebugUnitTest assembleDebug`: NOT RUN in this GitHub-only editing environment; pending CI.
- CI for this cache fix: PENDING.
- Physical-device QA for this cache-only correction: NOT RUN; no runtime service behavior was intentionally changed.

---

## 2026-11-21 — Notification width and centralized anomaly labels

### Changed
- Removed the redundant circular LargeIcon badge from monitoring notification ID 1001; retained its resource/dynamic small icon path and live Status Bar Indicator rendering.
- Removed the now-unused monitor-specific circular badge bitmap renderer. The separate anomaly notification retains its own large icon.
- Centralized Indonesian anomaly-code formatting for History session details and anomaly notifications, including the persisted session peak temperature when available.
- Removed anomaly notification subtext so its header and content are not reduced by a redundant secondary line.
- Kept `namespace = "com.superdenot.powerguard"` and confirmed `applicationId = "com.superdenot.superdguard"`; the debug keystore remains wired to the debug signing config.

### Verification
- Added unit coverage for all anomaly codes emitted by the branch's `AnomalyDetector`, requested voltage/drain/low-current/stagnation aliases, unknown-code fallback, multiple codes, and thermal peak/unavailable cases.
- Added notification regression coverage confirming ID 1001 has no LargeIcon while its small-icon and live Status Bar Indicator paths remain intact.
- CI #361 on baseline `4898f5a`: PASS (Anti-Slop UI Audit, unit tests, debug APK build, and artifact uploads).
- Physical QA on itel P55 for baseline layout, permission refresh, and collapsed/expanded notification checks: PASS (user-confirmed).
- GitHub Actions #362 on `212ee35`: FAIL — 164 tests passed and one anomaly bitmap cache identity test failed; `assembleDebug` and Anti-Slop passed, APK upload was skipped, test reports uploaded.
- No P0/P1 persistence, recovery ownership, current normalization, notification gate, service policy, or auto-start coordinator logic changed.
- No SDK, dependency, application version, build number, Room schema, or package namespace changes.

---

## 2026-11-20 — Notification parameter labels and font-safe History range

### Changed
- In Status-Bar-Indicator-only (HUD_ONLY) mode, notification ID 1001 now shows the selected parameter's Indonesian label and full formatted reading: temperature, voltage, current, power, battery percentage, or charging status.
- Unavailable readings use an em dash (—) with the corresponding parameter label; charging status distinguishes Mengisi (AC) from Tidak Mengisi and preserves exposed USB/Wireless source distinctions.
- History session level ranges now use the font-safe ASCII arrow format start% -> end%; removed the stale Unicode-arrow comment.

### Verification
- Added unit coverage for all six selected parameter labels, unavailable fallbacks, and charging/not-charging status text.
- Updated the session range formatter test for 38% -> 38% and a changing level range.
- GitHub Actions CI #361 on commit `4898f5a`: PASS — Anti-Slop UI audit, unit tests, debug APK build, and artifact uploads.
- Physical-device QA on itel P55 for the five-tab layout, permission refresh, and collapsed/expanded notification checks: PASS (user-confirmed).
- No Battery Monitoring / Status Bar Indicator lifecycle, normalizer, notification post gate, sequence guard, deduplication, cooldown, sensor cadence, persistence, SDK, dependency, package, or version changes intended.
- main untouched; merge remains HOLD.

---

## 2026-11-20 — Physical QA follow-up: five-tab layout, permission refresh, and notification content (implementation; CI pending)

### UI layout and navigation
- Tightened the Charging header, Hero card, battery-reading grid, and session summary to reduce the short-screen overflow on 360×800dp devices while retaining a scrollable list for accessibility; the 2×2 grid row gap is 8dp and each metric card keeps 10dp vertical padding.
- Added a 96dp bottom content reserve plus system navigation-bar inset to the scrollable content in Overview, Charging, Insights, History, and Settings.
- Applied the reserve to the nested Analytics lists used by Insights and History, so the final row remains reachable above bottom navigation.
- Reduced the Charging page to the concise title "Analisis Pengisian"; the protocol value is constrained to one line with a wider value column.

### Permission refresh and history text
- Removed the manual "Periksa ulang status" controls and their Snackbar feedback from Initial Setup and Overview.
- Permission state now refreshes on ON_RESUME in the active Overview screen or Initial Setup modal; other tabs retain the activity-level refresh path. The existing charger-start lifecycle action remains in MainActivity.
- The History session row already used the Unicode arrow formatter on the audited base. Repository code search for literal `'n` under `app/src/main` returned zero matches, so no redundant source edit was made.

### Monitoring notification and app activity labels
- Removed the monitor notification's redundant subtext so the dynamic title has more available space.
- Replaced the long "Terisi:" body wording with a compact single-line battery-side telemetry summary and added expanded BigTextStyle details for level, current, power, voltage, temperature, net charge, charging status, source, and protocol.
- App activity titles now resolve through PackageManager with package-name fallback; package subtitles are single-line ellipsized.
- P0/P1 service lifecycle, current normalizer, notification post gate, sequence guard, deduplication, and cooldown algorithms were not changed.

### Verification
- GitHub Actions CI #361 on commit `4898f5a`: PASS — Anti-Slop UI audit, `testDebugUnitTest`, `assembleDebug`, and artifact uploads.
- Physical-device re-test on itel P55 for 360×800dp fit, permission return refresh, and collapsed/expanded notification rendering: PASS (user-confirmed).
- No SDK, dependency, version, namespace, application ID, Room schema, or main-branch change.

## 2026-11-20 — Physical QA UI polish (implementation; CI verified)

### Readiness feedback
- Added a Snackbar confirmation to the Initial Setup sheet and the active Overview screen's Kesiapan Sistem card. The message reports refreshed notification permission, battery-optimization exemption, and usage-access states.
- Reused one readiness-summary formatter so both entry points present the same status vocabulary.

### History and notification presentation
- Centralized session battery-level range formatting as `start% → end%` using the explicit Kotlin Unicode escape `\u2192`; added a unit test for the rendered range.
- Replaced the duplicated foreground notification title with a state-aware title: `Mengisi Daya • level%` while charging, or `Pemantauan Baterai Aktif` while discharging/idle. Unavailable level is shown as `—`.
- Kept notification publication, sequence validation, deduplication, cooldown, service lifecycle, sensor normalization, and P0/P1 policy logic unchanged. Only notification presentation title inputs were updated.

### Verification
- GitHub Actions run #358 on commit `ad890ff02cfb843aa88d04219940f5d0e632db50`: Anti-Slop UI audit PASS, `testDebugUnitTest` PASS, and `assembleDebug` PASS; both APK and unit-test report artifacts uploaded.
- Physical-device QA for the three polish changes remains separate from CI; the later itel P55 screen-recording review reported direct APK overwrite success and identified the follow-up UI issues recorded in the newer entry at the top of this changelog.
- No local Gradle build was run in this GitHub-only editing environment.

---

## 2026-11-20 — P0/P1 audit fixes ported to latest Industrial Instrument baseline (CI verified)

### Scope and compatibility
- New integration branch is based directly on `fix/auto-start-coordinator-timing` at `d7dbec01d412b95a8129659a0991a2d43bca6519`; only the six approved P0/P1 audit areas are included.
- Preserved the latest Industrial Instrument UI, five-tab telemetry structure, AutoStartCoordinator implementation, `DESIGN.md`, `AGENTS.md`, `DEVELOPMENT_WORKFLOW.md`, Anti-Slop scripts, and existing CI jobs.
- Confirmed `applicationId = "com.superdenot.superdguard"` and Kotlin namespace `com.superdenot.powerguard`; README already contains the correct Application ID and the About screen metadata was corrected.

### Diagnostics and lifecycle
- Battery current normalization preserves signed battery-side current while external power is connected; sustained negative current with available sensor data is classified as `NET_DRAIN_CHARGING` after the existing three-minute no-input window.
- Charging protocol remains `Not Exposed by Device`; no V/A/W threshold is used to infer a fast-charging protocol.
- Retained the independent Battery Monitoring and Status Bar Indicator toggles, fresh-install OFF defaults, OR-gate service keep-alive policy (including RECOVERY_HOLD), and active Room-session finalization when Battery Monitoring turns OFF.

### Notification durability
- Added `MonitorNotificationPostGate` for notification ID 1001 with synchronized state publication/posting, snapshot sequence validation, duplicate suppression, and a 250 ms power-transition bounce cooldown.
- Metric payload refreshes remain possible in a stable state and do not extend the power-transition cooldown.

### Repository hygiene and verification
- Removed the obsolete root `INSTALL-INSTRUCTIONS.txt`.
- Anti-Slop UI audit gate: PASS; `testDebugUnitTest`: PASS; `assembleDebug`: PASS in [GitHub Actions run #356](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/44712530728), exact tested head `0ccb48aada965c748760c7723622810456bad367`.
- CI uploaded `android-debug-apk` (17,059,413 bytes) and `android-unit-test-reports` artifacts.
- No local build was run in this environment; the build/test verification above is from GitHub Actions.
- Physical-device QA: not run; CI does not establish OEM notification or sensor behavior.

---

## 2026-11-19 — Unified service lifecycle and persistent notification (implementation; verification pending)

### Scope
- Consolidate the persistent foreground-service notification to ID `1001`; preserve transient anomaly notification ID `1002`.
- Apply the approved service keep-alive rule: Battery Monitoring OR Status Bar Indicator OR RECOVERY_HOLD.
- Separate `HUD_ONLY`, `MONITORING_STANDBY`, charging recording, and `RECOVERY_HOLD` actor modes.
- Prevent charger transitions in HUD-only mode from starting Room sessions or appending new samples.
- Preserve Android background Foreground Service start restrictions.

### Implementation
- Added `ServiceOperationPolicy` to derive STOPPED / HUD_ONLY / MONITORING_ACTIVE / RECOVERY_HOLD and centralize the keep-alive and Room-recording gates.
- `BatteryMonitorService` now reconciles both preferences through its lifecycle actor. The Monitoring OFF request closes the in-memory Room-write gate synchronously before the next sensor-loop tick. Monitoring OFF finalizes an existing session under `sharedSessionMutex`; if Status Bar Indicator remains ON, the service transitions to HUD_ONLY. If Monitoring remains ON while unplugged, the actor uses HUD_ONLY when Status Bar Indicator is ON (live sensor display without Room writes), or MONITORING_STANDBY when the indicator is OFF (sensor loop stopped). A successful recovery retry cannot create a new charging session while Monitoring is OFF.
- HUD_ONLY reads live sensor state for display but bypasses session creation, sample buffering, and Room sample writes. RECOVERY_HOLD remains the highest-priority safety state and keeps the service alive until the pending data is safely finalized/reconciled.
- Removed the active ID `1003` notification path and stopped creating its old channel. Startup cancels any stale ID `1003` notification and deletes the legacy channel. The single ID `1001` notification uses `LiveStatusBarRenderer` when Status Bar Indicator is ON and the static `NotificationVisualRenderer` icon otherwise.
- ID `1001` is ongoing, only-alert-once, and silent. `PersistentNotificationRenderGate` deduplicates the `iconKey|title|body` signature and throttles body-only updates to 4 seconds; toggle, metric, and actor-mode changes reset the render cache. ID `1002` remains independent.
- Auto Start and the charging-constrained WorkManager fallback now check both feature toggles. An already-running service receives an actor synchronization command rather than another start request. Boot recovery also considers HUD-only mode. Platform background-start policy remains in force.

### Regression coverage added/updated
- Four toggle combinations and RECOVERY_HOLD precedence.
- Room recording gate for HUD_ONLY versus charging Monitoring mode.
- ON→OFF session-finalization transition policy.
- Charger transitions in HUD_ONLY.
- Notification ID `1001`, legacy ID/channel cleanup, dynamic/static small-icon selection, and independent anomaly ID `1002`.
- Persistent-notification signature deduplication and 4-second body throttle.

### Verification status
- Source/diff review: **PASS** — lifecycle branches, Room-write gates, notification ID/channel references, and branch diff reviewed.
- Unit/regression tests: **NOT RUN** — pending branch CI.
- Android debug build: **NOT RUN** — pending branch CI.
- Anti-Slop UI Audit Gate: **NOT RUN** — notification surface only; no Compose screen layout changed.
- Physical-device validation: **NOT RUN** — verify all four toggle combinations, HUD-only charger connect/disconnect, single notification card, icon visibility, and recovery-hold behavior on the target device.
- Branch `main`: untouched. No merge or push requested.

---

## 2026-11-19 — Reproducible debug signing (selesai; CI PASS)

### Perubahan
- Menambahkan `app/debug.keystore` tetap dengan alias `androiddebugkey`, store/key password standar `android`, RSA 2048, dan sertifikat pengembangan.
- Mengarahkan hanya `buildTypes.debug` ke `signingConfigs.debug` pada `app/build.gradle.kts`. Konfigurasi release tidak diubah.
- Fingerprint sertifikat debug tetap: SHA-1 `AF:4C:A2:92:11:1D:E4:58:2F:A2:18:76:C3:EF:D4:F9:7C:0C:7C:B2`; SHA-256 `9B:46:72:71:9E:99:5A:93:32:FF:DD:FF:4B:8B:07:36:45:0D:31:4F:C9:C0:D9:14:4B:F8:95:F7:A1:A8:63:1F`.
- Keystore ini khusus pengembangan dan berada di repository; jangan gunakan untuk release/production signing.

### Status verifikasi
- Source/configuration review: PASS.
- GitHub Actions #322, attempt 2, pada commit `e4e26dfe2cff4bb25379583c80138fc95414b329`: **PASS** — `testDebugUnitTest`, `assembleDebug`, dan Anti-Slop UI Audit Gate.
- Unit test: **136 passed, 0 failed, 0 errors, 0 skipped**; laporan XML workflow diverifikasi. Attempt 1 sempat gagal pada satu assertion di `NotificationVisualRendererTest` baris 331; rerun job (attempt 2) lulus tanpa perubahan source.
- Debug APK dan unit-test report berhasil diunggah sebagai artifact workflow.
- Pengujian instalasi/update pada perangkat fisik: NOT RUN.
- Catatan kompatibilitas: APK lama yang ditandatangani dengan debug key lain tidak dapat diperbarui dengan key baru. Jika signature lama berbeda, uninstall satu kali sebelum memasang APK bertanda tangan tetap ini; setelah itu build lokal dan CI dapat saling memperbarui selama menggunakan keystore repository yang sama.
- Branch `main` tidak disentuh; merge tetap HOLD.

---

## 2026-11-19 — Tahap 2: lifecycle fallback dan pemisahan Status Bar Indicator (selesai)

### Scope disetujui
- Membatasi fallback sensor polling pada kondisi App Foreground = TRUE, Battery Monitoring = ON, dan data service tidak fresh.
- Memastikan coroutine suspend di ViewModel meneruskan `CancellationException` dan tetap fail-closed untuk error operasional.
- Memisahkan Status Bar Indicator dari ikon Monitoring Notification melalui notification ID dan channel tersendiri.
- Membersihkan indikator ketika salah satu toggle terkait OFF atau service berhenti, serta menampilkan unavailable ketika data terpilih tidak tersedia.

### Implementasi
- `MainActivity` sudah memiliki lifecycle hook `ON_START` / `ON_STOP` untuk mengirim status foreground. Integrasi yang sudah benar dipertahankan; `PowerGuardViewModel` kini menyimpan status itu dalam `StateFlow` yang menjadi gate poller.
- `FallbackSensorPoller` membaca snapshot hanya jika aplikasi foreground, Battery Monitoring aktif, dan data service tidak fresh. Kondisi diperiksa sebelum dan sesudah pembacaan; satu job poller dijaga agar transisi lifecycle tidak membuat loop ganda.
- Preferensi Battery Monitoring memakai eager collection di ViewModel agar nilai gate tetap aktual meskipun tidak ada composable yang sedang mengoleksi StateFlow.
- Operasi suspend yang sebelumnya dibungkus `runCatching` menggunakan penanganan yang melempar ulang `CancellationException`; error operasional biasa tetap ditangani secara fail-closed.
- `buildMonitorNotification()` memakai ikon dari `NotificationVisualRenderer` secara independen. `LiveStatusBarRenderer` hanya digunakan oleh Status Bar Indicator.
- Status Bar Indicator memakai notification ID `1003` dan channel `powerguard_status_bar_indicator` dengan `IMPORTANCE_LOW`, tanpa suara/getar. Indikator hanya diposting saat Battery Monitoring dan Status Bar Indicator sama-sama ON serta service aktif; indikator dibersihkan saat toggle OFF atau service dihentikan.
- Formatter indikator tetap membedakan nilai 0 yang valid dari sensor/status charging yang unavailable (`N/A`).

### Status verifikasi
- GitHub Actions #320 pada commit `87794ef755cb6a4a78d8d008aba9619e8d38afc1`: **PASS** — `testDebugUnitTest`, `assembleDebug`, dan Anti-Slop UI Audit Gate.
- Unit test: **136 tests** — seluruhnya lulus pada workflow final. Commit aplikasi utama `c1a679079bee52d050a3f73a12f60cc529990ba3` juga lulus pada workflow #317.
- Debug APK dan unit-test report berhasil diunggah sebagai artifact workflow.
- Iterasi CI awal menemukan dua masalah pada test harness notification: pembacaan `Notification.EXTRA_TEXT` dan inspeksi `activeNotifications` tidak stabil pada Robolectric. Regression test diperbaiki dengan menguji formatter secara langsung, memberi izin `POST_NOTIFICATIONS`, dan memeriksa hasil melalui `ShadowNotificationManager`. Workflow final #320 PASS.
- Pengujian perangkat fisik: **NOT RUN** — visibilitas ikon di System UI, perilaku channel OEM, dan polling pada transisi foreground/background tetap memerlukan validasi pada perangkat.
- Branch `main` tidak disentuh. Merge tetap **HOLD** sampai Tahap 2 dan pengujian fisik disetujui.

[GitHub Actions #320](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/44487118987)

---

## 2026-11-19 — Tahap 1: propagasi availability dan integritas riwayat (selesai)

### Scope disetujui
- Propagasi availability sensor dari Engine/Normalizer/Integrator/Service sampai Room dan checkpoint `RECOVERY_HOLD`.
- Migrasi Room v1→v2 secara non-destruktif dengan default availability `false` untuk data lama.
- Pemutusan segmen kurva pada sampel unavailable serta status unavailable pada tooltip.
- Regression/unit tests untuk migrasi, persistence, recovery, dan perlindungan nilai nol yang benar-benar terukur.

### Implementasi
- Room schema dinaikkan ke v2; seluruh kolom availability baru memiliki Kotlin default `false` dan SQL default `0`.
- `BatteryDao` menghitung agregat current/temperature hanya dari data valid, menjaga data lama tanpa menandainya sebagai hasil pengukuran yang terverifikasi, dan mengintegrasikan mAh hanya pada pasangan sampel arus yang valid.
- `SessionRecoveryStore` mempertahankan validasi payload, ownership `sessionId`, merge same-session, serta verifikasi read-back pasca-`commit()`. Checkpoint lama membaca availability sebagai `false`; merge sampel memilih flags sesuai snapshot terbaru dan menggabungkan flags batch berbeda secara konsisten.
- `BatteryMonitorService` meneruskan availability ke integrator, agregasi berbobot, persistence, sampel final, dan checkpoint `RECOVERY_HOLD`.
- `ChargingCurveChart` memutus garis di titik unavailable dan menampilkan “Tidak tersedia” pada tooltip; nilai 0 tetap ditampilkan jika flag availability bernilai true.
- Commit 2A: `7339b1ec` — Room schema, migrasi, dan DAO. Commit 2B: rangkaian implementasi/recovery/regression test hingga `71f438bb`. Commit 3: `48d6c6c0` — finalisasi chart availability.

### Status verifikasi
- GitHub Actions #306 pada commit `48d6c6c0f3354f1e1d80ac1e6b74a9f552542e2c`: **PASS** — `testDebugUnitTest`, `assembleDebug`, dan Anti-Slop UI Audit Gate.
- Unit test: **129 passed, 0 failed, 0 errors, 0 skipped**; termasuk `AppDatabaseMigrationTest`.
- Debug APK dan unit-test report berhasil diunggah sebagai artifact workflow.
- Pengujian perangkat fisik: **NOT RUN** — perubahan ini diverifikasi pada level source, unit test, migrasi, dan build; belum dilakukan validasi runtime perangkat.
- Tahap 2 belum dimulai. Branch `main` tidak disentuh; HOLD terhadap merge tetap berlaku.

[GitHub Actions #306](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/44479587142)

---

# Changelog

## 2026-11-19 — Physical-device Auto Start validation (itel P55 NFC)

### Test results
- Dynamic receiver while the SPG process is alive: **PASS** — `SPG_AutoStart_Filtered.txt` records two successful charging-triggered coordinator paths reaching `RUNNING_CHARGING`; T0→T2 was 403 ms and 146 ms respectively.
- Auto Start after swipe/remove-task process death: **FAIL** — ActivityManager records the SPG process killed with reason `remove task`; no subsequent SPG receiver, coordinator, or Worker execution is evidenced in the supplied capture. Charger-event rejection messages at that point belong to other applications.
- JobScheduler `CHARGING` constraint: **FAIL / BLOCKED** — in `spg_job_and_battrey.txt`, SPG WorkManager jobs `#u0a283/4` and `#u0a283/6` both require charging, list `CHARGING` as unsatisfied, and report `Ready: false`.
- Worker execution: **NOT VERIFIED** — the not-ready JobScheduler snapshots do not prove that `ChargingAutoStartWorker.doWork()` started or returned a result.
- BatteryService snapshot: reports `AC powered: true`, `status: 2`, and level 79% at that capture. A separate capture reported 83% at a different time; these values are not treated as simultaneous readings.

### Capture identity and limitations
- The Auto Start log uses UID `u0a282`; the later JobScheduler dump uses `u0a283`. The user confirms an APK reinstall occurred between test sessions, explaining the UID change. The files must not be interpreted as one continuous process session.
- The battery and JobScheduler commands in `spg_job_and_battrey.txt` were captured in the same script with a 3-second gap. BatteryService's connected/charging snapshot and JobScheduler's unsatisfied `CHARGING` constraint therefore coexist in that capture; this proves the observed state mismatch, not its underlying cause.
- Attribution to a Unisoc HAL defect or a missing/delayed `BatteryManager.ACTION_CHARGING` broadcast is **NOT VERIFIED**. No HAL/framework trace in these files establishes that mechanism.
- Locking SUPERD POWER GUARD in Recent Apps remains an **UNTESTED mitigation option**, not a validated workaround or guaranteed solution.
- No application source, build configuration, dependency, package, SDK, app version, or runtime behavior was changed for this documentation entry. No merge to `main` was performed.

## 2026-11-19 — Hybrid charging Auto Start recovery

### Changed
- Added a unique one-time WorkManager request constrained by `requiresCharging=true` as the process-dead recovery path. It uses the existing `ChargingAutoStartWorker` and a separate unique name, `powerguard_charging_auto_start_once`.
- Preserved the existing periodic unique work name `powerguard_charging_auto_start`, its 15-minute minimum interval, charging constraint, and retry/backoff behavior.
- One-time work uses `ExistingWorkPolicy.KEEP`; it is armed only when Battery Monitoring is ON and the sticky battery intent explicitly reports `EXTRA_PLUGGED == 0`. Unknown or missing plug state fails closed.
- Arm checks are integrated at application process creation, boot, and charger disconnect. On charger connect, only queued one-time work is cancelled; a RUNNING worker is not deliberately cancelled. The foreground/background fast path remains the dynamic receiver + existing coordinator.
- Battery Monitoring OFF now cancels both periodic and one-time work through the existing scheduler cancellation API. The Worker independently checks the toggle and retains its existing retry mapping.
- No expedited work is used with the charging constraint. The Worker does not re-arm itself.

### Verification and limitations
- CI #271 on commit `36d067508441a9d89b87223e4da489488503037c`: PASS — `gradle --no-daemon testDebugUnitTest assembleDebug`, including debug APK assembly and unit tests; Anti-Slop UI Audit Gate also PASS. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/44362064874).
- WorkManager/JobScheduler execution remains best-effort and OEM-dependent; this architecture does not guarantee process wake-up or real-time start on XOS.
- Physical-device validation on itel P55 NFC is still required, including process removal, unplugged arming, charger reconnect, toggle OFF cancellation, and duplicate-start checks.
- BatteryMonitorService, RECOVERY_HOLD, sensor cadence, persistence schema, dependencies, SDK, package, app version, and build number are unchanged.


## 2026-11-18 — Dynamic power-event receiver export experiment

### Changed
- Changed only the application-lifetime dynamic receiver registration in `PowerGuardApp.kt` from `ContextCompat.RECEIVER_NOT_EXPORTED` to `ContextCompat.RECEIVER_EXPORTED` for the protected system power-connect and power-disconnect broadcasts.
- This is a controlled compatibility experiment to compare delivery on the target itel/XOS device. It does not change the manifest receiver, WorkManager fallback, AutoStartCoordinator, BatteryMonitorService, service policy, or Battery Monitoring lifecycle.
- Because an exported dynamic receiver can receive other unprotected broadcasts, keep the filter restricted to the two system power actions and do not add unrelated actions.

### Verification and limitations
- Source change committed on the development branch. Android CI/build and physical-device A/B verification are pending.
- `RECEIVER_EXPORTED` does not bypass Android cached-process scheduling, OEM background restrictions, or Foreground Service start policy.
- On Android 14/API 34 and later, some context-registered broadcasts may be queued while the app is cached and delivered after it leaves cached state; record delayed delivery as a distinct outcome rather than assuming the broadcast was dropped.
- Physical-device scenarios remain required: (A) plug in shortly after Home; (B) wait until the process is cached/frozen, then plug in and observe whether delivery occurs after thaw/resume. Compare receiver-entry and coordinator events in `AutoStartDiagnosticLog`.

All notable changes to Superd Power Guard are documented here. Implementation and device-testing status are tracked separately.

## 2026-11-18 — Periodic charging Auto Start recovery

### Changed
- Replaced the one-time charging-constrained fallback with unique periodic WorkManager work using the platform minimum interval of 15 minutes and the existing `setRequiresCharging(true)` constraint.
- Kept the same unique work name and cancellation path so Battery Monitoring OFF continues to cancel the fallback; ON schedules/replaces the periodic work.
- Corrected Worker outcome handling: `REQUESTED`, `ALREADY_IN_FLIGHT`, `SERVICE_ALREADY_ACTIVE`, `MONITORING_DISABLED`, and `NOT_CHARGING` complete the current run. `DEFERRED_BY_PLATFORM` and `FAILED` use WorkManager retry/backoff.
- Added unit tests for the Worker outcome mapping.
- No BatteryMonitorService, RECOVERY_HOLD, sensor cadence, persistence schema, dependency, SDK, package, app version, or build number changes.

### Limitations and verification
- Periodic WorkManager is a best-effort recovery check, not a real-time trigger. Its 15-minute interval is a minimum scheduling interval; Android and OEM scheduling may delay execution further.
- A passing Worker test or CI run cannot prove XOS background scheduling or service startup on the physical itel P55 NFC.
- Residual edge case: if a start request is accepted but its process-local pending-start marker remains stale without the service reaching RUNNING, the coordinator may report SERVICE_ALREADY_ACTIVE on later checks. This change deliberately does not add a pending-start timeout because that could create duplicate service starts; resolving it needs a separate lifecycle change and regression coverage.
- Automated verification and physical-device QA are pending for this change.

## 2026-11-18 — Background Auto Start CI follow-up

### Fixed
- Updated the existing BatteryMonitoringSettings unit test to call the renamed `canStartFromBackgroundTrigger` helper. The production helper had already been renamed as part of the background-start policy change, but the older test references were missed.
- No production behavior, dependency, SDK, app version, or build number changed.

### Verification
- CI run #257 confirmed `:app:compileDebugKotlin` and `:app:assembleDebug` succeeded, but `:app:compileDebugUnitTestKotlin` failed because four test calls still referenced the removed helper name.
- This follow-up corrects those stale test references. CI rerun and physical-device validation remain pending.

## 2026-11-18 — Background Auto Start charging trigger

### Changed
- Removed manifest registration for implicit charger connect/disconnect broadcasts, which Android restricts for apps targeting API 26+.
- Registered charger connect/disconnect events dynamically for the lifetime of the application process; retained the manifest receiver only for BOOT_COMPLETED.
- Added a unique WorkManager one-time charging-constrained worker as a process-death/reboot fallback. The worker rechecks the Battery Monitoring preference, charging state, service state, and background Foreground Service eligibility before requesting the existing BatteryMonitorService.
- Kept a retrying charging-constrained work request while Battery Monitoring remains ON, using a 15-minute linear backoff. This preserves a pending trigger across unplug/replug and unexpected process death; execution timing remains subject to Android/WorkManager scheduling.
- Toggle OFF cancels the unique charging work and still requests service stop. Toggle ON schedules/replaces the work and retains the existing immediate foreground start path when charging.
- Added regression coverage for the WorkManager background-start policy. No new dependency, SDK, package, version, sensor cadence, persistence format, or monitoring service was added.

### Verification
- Source implementation committed on the development branch. Android CI and unit-test outcomes are tracked separately and must not be inferred from this changelog entry.
- Physical-device validation on itel P55 NFC: pending. Confirm background charger connect, process death, unplug/replug, toggle OFF, and RECOVERY_HOLD behavior.


## 2026-11-18 — Auto Start receiver diagnostic durability

### Changed
- Added a serialized critical-log write barrier with filesystem sync for BroadcastReceiver lifecycle markers. The receiver records entry, worker dispatch, coordinator result, and completion before releasing its asynchronous broadcast result.
- Added process ID and foreground-state context to receiver diagnostics, plus bounded exception details for coordinator start failures and Foreground Service promotion failures.
- Added a Robolectric regression test verifying queued events are written before a durable marker returns and are present in an exported snapshot.
- Isolated the logger test with a dedicated no-op test Application so production WorkManager scheduling does not run during this focused unit test.
- No Auto Start policy, Battery Monitoring lifecycle, RECOVERY_HOLD, sensor cadence, session persistence, SDK, dependency, or app-version behavior was intentionally changed.

### Verification
- Initial CI run #252: `assembleDebug` passed and Anti-Slop UI Audit Gate passed; `testDebugUnitTest` failed only in the new logger test because production Application startup attempted to access WorkManager while the Robolectric test manifest disabled its initializer.
- Test harness isolation applied in commit `a83018e0aa83599424a533e6fac59c688f06c2ca`; rerun CI pending.
- Physical-device reproduction on itel P55 NFC pending; a passing unit test/build does not prove broadcast delivery on the device.

## 2026-11-18 — Auto Start diagnostic export

### Added
- Added bounded, app-private Auto Start diagnostic logging for receiver delivery, Battery Monitoring preference, charging-state checks, background-start policy, service request, Foreground Service promotion, and RUNNING_CHARGING transition.
- Added a temporary Settings action to create and share a diagnostic log snapshot through Android's system Sharesheet. Exported files are stored in app cache; active logs remain in app-private storage and rotate at a bounded size.
- Added FileProvider access limited to the diagnostic export cache directory. The diagnostic logger records technical markers only; it does not collect battery sensor readings, account identifiers, or user content.

### Verification
- Android CI run #251 passed on commit `ea4f6fd7e4e7b204ee72871844237e238adbd534`: `testDebugUnitTest`, `assembleDebug`, and the Anti-Slop UI Audit Gate succeeded; debug APK and unit-test report artifacts were uploaded.
- Physical-device export/share and Auto Start reproduction: pending; CI success does not verify FileProvider sharing or runtime log capture on the itel P55 NFC.
- No Auto Start policy, Battery Monitoring lifecycle, RECOVERY_HOLD, sensor polling, Room schema, app version, or dependency changes intended.

## 2026-11-18 — Automatic Battery Monitoring startup

### Changed
- Added a single Auto Start coordinator shared by charger/boot broadcasts, foreground app checks, the Battery Monitoring toggle, and fallback Charging State snapshots.
- Connected the existing 1.5-second fallback sensor path to the Auto Start gate while the app is foregrounded. Repeated signals are deduplicated against one in-flight request and the Service's existing STARTING/RUNNING state.
- Preserved the existing background-start restriction for charger broadcasts when the app is not foregrounded; boot-start behavior remains subject to Android foreground-service rules.
- Added monotonic T0/T1/T2 diagnostic logs: T0 charging detection, T1 successful Foreground Service entry, and T2 RUNNING_CHARGING. Logs include elapsed intervals and attempt/charging-epoch identifiers.
- Added unit coverage for charging-epoch timestamp retention/reset and request-gate deduplication.
- Enabled the existing Android CI workflow for this focused development branch.

### Verification
- Android CI run #245 passed on source commit `5902933395b88d81b0a4e688eb44bb205a91a23e`: `testDebugUnitTest`, `assembleDebug`, and the Anti-Slop UI Audit Gate all succeeded.
- CI uploaded the debug APK and unit-test report artifacts.
- Physical-device verification: pending. The user's earlier delay estimate is not treated as a measured baseline; T0/T1/T2 logs will provide actual elapsed-time evidence.
- Charger-disconnect shutdown and RECOVERY_HOLD behavior were not intentionally changed and remain regression-sensitive device checks.


## 2026-11-17

### Changed
- **Top bar:** reduced app-bar container height from 56dp to 48dp and removed the secondary “BATTERY DIAGNOSTIC SYSTEM” line. The SUPERD POWER GUARD brand title remains unchanged.
- **History / Charging Curve:** reduced the vertical gap between the curve and session table so the Waktu, Level, and Durasi header and session rows sit closer to the chart.
- **Settings:** removed the separate “Akses Android” panel because its shortcuts duplicated permission actions available through Initial Setup. The “Persiapan Izin” entry remains as the single in-app access point.

### Previously implemented in this development branch
- **P3 — Charging Curve:** compacted chart padding, legend spacing, chart height, and selected-sample card spacing to preserve more room for the session list. Commit: `ca72b3b033a4f9cf1a5f532682c341abc22c03ca`.
- **P4 — App chrome:** reduced bottom navigation container height to 64dp and set top app bar expanded height to 56dp without changing brand typography. Commit: `42f8f3119b450343005f9f01facf6612983f5db3`.

### Verification
- These source changes are not yet device-tested. Android CI and physical-device verification are pending; do not treat these UI adjustments as verified until the relevant build and user QA pass.


## 2026-11-17 — Follow-up UI refinement

### Changed
- **Settings header alignment:** reduced the Settings content header top inset from 12dp to 4dp so it visually aligns with the shared compact app bar used by Overview and Charging. Existing text and typography are preserved.
- **History heading spacing:** reduced the bottom inset below “Sesi dan kurva pengisian” from 12dp to 3dp, bringing “Riwayat Sesi Pengisian” closer to its parent heading.

### Verification
- Source changes only; Android CI and physical-device verification are pending.


## 2026-11-17 — ChatGPT workflow and Android skill routing

### Added
- Added `docs/engineering/CHATGPT_WORKFLOW.md` describing the repository workflow for ChatGPT-assisted inspection, implementation, branch safety, diff review, verification, changelog maintenance, and delivery reporting.
- Added `docs/engineering/ANDROID_SKILLS_INDEX.md` mapping SPG work areas to the exact upstream Android Skills and relevant Compose references.
- Updated `AGENTS.md` to point to the new workflow and task-to-skill index, and corrected the `main` branch approval-boundary typo.
- Removed `CLAUDE.md` because the repository workflow is now documented for ChatGPT and the general agent instructions remain in `AGENTS.md`.

### CI and verification
- Inspected `.github/workflows/android-ci.yml`: the existing `Anti-Slop UI Audit Gate` runs `bash scripts/ui-audit.sh`; no duplicate Anti-Slop workflow was added.
- Documentation-only changes. No application source, build configuration, dependency, version, or runtime behavior changed.
- Android build and app tests: N/A for this documentation-only update.
- GitHub Actions for this documentation branch: not triggered by the current workflow branch filters; no CI pass is claimed.
- Physical-device testing: N/A.
- Branch diff and documentation checks remain subject to final review; nothing was merged to `main`.

## 2026-11-17 — Engineering standards adoption (documentation only)

### Added
- Added repository-wide AI agent instructions in `AGENTS.md` and Claude-specific workflow guidance in `CLAUDE.md`.
- Added `docs/engineering/DEVELOPMENT_STANDARDS.md` defining project-specific precedence, architecture boundaries, change workflow, and definition of done.
- Added `docs/engineering/ANTI_SLOP_POLICY.md` adapting relevant Anti-Slop principles and rules for native Android/Compose work while preserving `DESIGN.md` as the visual source of truth.
- Added `docs/engineering/ANDROID_ENGINEERING_GUIDE.md` selecting Android Skills guidance relevant to architecture, debugging, testing, Coroutines/Flow, Compose, persistence, and build performance.
- Added `docs/engineering/QUALITY_CHECKLIST.md` for implementation, diagnostic integrity, UI, Android lifecycle, verification, and delivery review.
- Added `THIRD_PARTY_NOTICES.md` recording attribution and upstream MIT license references for the two supplied source archives.

### Verification
- Documentation-only change. No Android application source, build configuration, dependencies, version, or runtime behavior changed.
- Android build and app tests were not run because no application code changed.
- Documentation content was prepared against the supplied Anti-Slop and Android Skills archives and the existing `DESIGN.md` / `CHANGELOG.md`.
- Physical-device testing: not applicable to this documentation-only change.
- Branch review and user approval are pending; nothing was merged to `main`.
