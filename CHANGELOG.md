# Changelog

All notable changes to Superd Power Guard are documented here. Implementation and device-testing status are tracked separately.

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
