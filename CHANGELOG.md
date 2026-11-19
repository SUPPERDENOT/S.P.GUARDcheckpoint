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
