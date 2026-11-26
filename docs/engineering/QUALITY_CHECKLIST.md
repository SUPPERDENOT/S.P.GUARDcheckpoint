## 2026-11-26 — PR #12 delta-frame / theme-container / info-badge precision fix

- [x] Gauge phase is normalized and accumulated from frame-to-frame delta with a 0–64ms clamp and 2,400ms default cycle; disabled animation resets to 0.
- [x] BatteryGaugeView no longer imports or references ValueAnimator, rememberInfiniteTransition, or Compose infinite animation APIs; the only animation driver is LaunchedEffect + withFrameNanos.
- [x] Gauge phase State is read inside each Canvas draw block and converted to radians for the existing render geometry.
- [x] Theme & Gauge preview call site retains forceAnimate = true; regression tests cover Sweep, Segmen, Comet, and Statis.
- [x] Active Overview gauge call site in AppScreens.kt passes the selected live charging state; visibility and thermal eligibility gates remain intact. Legacy ui/dashboard/DashboardScreen.kt has no BatteryGaugeView call and is not routed by MainActivity, so no change was made there.
- [x] All five Material 3 surfaceContainer tokens are explicitly supplied for Basic Terracotta/Emerald/Cobalt in Light and Dark and Midnight Violet/Pink/Cyan/Green.
- [x] ProtocolInfoDialog uses surfaceContainerHigh directly, tonalElevation = 0.dp, and a 1dp outlineVariant border.
- [x] Both Charging info badges use a flat 16dp circle, 11dp Icons.Outlined.Info glyph, 4dp title gap, and no Surface/shadow/tonal elevation.
- [x] Unit tests added for delta accumulation, bounded frame delta, wrap, disabled reset, all animation modes, and all theme accent palettes.
- [x] Protected-boundary review: no Room DB v3, sensor/sampling, calibration default 1x, Foreground Service P0/lifecycle, RECOVERY_HOLD, applicationId/package, Gradle dependencies, SDK, app version, or build number edits intended; final compare pending.
- [ ] Final exact-HEAD diff self-crosscheck: pending.
- [ ] GitHub Actions exact-HEAD Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, and APK upload: pending.
- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-26 — PR #12 precision pass

- [x] Capsule outer frame uses the 1.8dp theme-aware outline; cap remains separate with a 2dp gap.
- [x] Capsule inner geometry uses the outer radius minus (stroke + 1.5dp), with a 4dp minimum radius; track, fill, shimmer, and beam share one inner rounded-rect clip.
- [x] Wave amplitude is 6.5dp, frequency is 1.6 cycles, rear phase is 115°, and the front meniscus is 2dp.
- [x] Wave Sweep uses a rising light ribbon and crest pulse; Segmen uses seven rising bubbles with double ripple rings; Comet uses a 100° tail and 3dp orbit stroke.
- [x] Statis disables moving effects in Busur, Kapsul, Blok, and Wave; Capsule gloss/reflection are also suppressed and Wave retains a fixed sinusoidal curve.
- [x] Frame-clock fallback uses LaunchedEffect + withFrameNanos only when platform animators are disabled and animation is eligible.
- [x] Both Charging info badges use an 18dp theme-colored circle, 12dp icon, 24dp minimum click area, and 3dp title gap.
- [x] Both info popups resolve their container and border from the active Material 3 ColorScheme; light/dark token tests added.
- [x] Regression tests added/updated for Capsule geometry, Wave profiles/amplitude, four-model Statis behavior, frame-clock fallback, popup colors, and badge geometry.
- [x] Full line-by-line diff self-crosscheck from pre-fix HEAD `6c1221e4f11105e8aa6296d9c654536572e97a13` to source/test HEAD `b757b73c6f923f9f37fdb12d0a960540b0695bd9`: PASS — eight changed paths; no protected database, sensor/sampling, calibration, service/lifecycle, RECOVERY_HOLD, Manifest/applicationId, Gradle/dependency, SDK, or version/build-number paths changed.
- [x] Run #705 (`45757589472`) on source/test HEAD `b757b73c6f923f9f37fdb12d0a960540b0695bd9`: SUCCESS — Anti-Slop, testDebugUnitTest, assembleDebug, Upload APK, and unit-test report upload. APK Artifact ID `15088831300`; test report Artifact ID `15088363248`.
- [x] Run #707 (`45757879662`) on documentation HEAD `23187bacec905b7c053ada8eebc5e9a387115c5a`: SUCCESS — Anti-Slop, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload; APK Artifact ID `15088527818`.
- [x] Final checklist closeout is a documentation-only commit; its exact-HEAD CI result and latest APK Artifact ID are recorded in the delivery report.
- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-26 — PR #12 final polish

- [x] Blok segment order starts at 90° (6 o'clock) and advances clockwise; 50% spans bottom → left → top. Sweep/Segmen/Comet head coordinates share the same positive-angle origin — source review and regression tests added.
- [x] Capsule Comet is a vertical rising beam with a fading tail constrained to the liquid column and a bright meniscus flash; no floating dot remains — source review and geometry tests added.
- [x] Wave Sweep, Segmen, Comet, and Statis map to distinct feature profiles; Statis has fixed phase and no moving sweep/strata/orbit — source review and profile tests added.
- [x] Wave Comet uses a 90° orbital tail with a 3dp stroke and a white accent head — source review.
- [x] Charging label is exactly "Kemampuan sumber"; both info affordances are 24dp clickable Boxes adjacent to their labels, with button semantics — source review and label test added.
- [x] Full-diff self-crosscheck: PASS — all six changed paths reviewed; protected database/schema, sensor/sampling, calibration, service/lifecycle, RECOVERY_HOLD, Manifest/applicationId, Gradle/dependency, SDK, and version/build-number paths are absent.
- [x] Run #690 (45734904830) on source/test HEAD 8c150d546cbc3d83c4254250cc0729d52af3fe20: SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, Upload APK, and unit-test report upload; APK Artifact ID 15078965622; report Artifact ID 15078163363.
- [ ] CI for the final documentation-only HEAD: triggered by the final checklist update; exact result is recorded in the delivery report.
- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-25 — PR #12 final visual pass

- [x] Busur uses a clockwise -225° to +45° C-arc, track, active gradient, and endpoint halo — source review.
- [x] Kapsul has a distinct terminal with 2dp gap, glossy outline, vertical fill gradient, and bottom-to-top Sweep/Comet phase mapping — source review.
- [x] Blok uses 24 radial blocks from -90° clockwise with a dimming trail behind Sweep/Comet heads — source review.
- [x] Wave retains battery-level fill when idle or charging and renders two phase-offset waves, 0.82-alpha front fill, crest highlight, glass rim, and animated outer orbit — source review.
- [x] `BatteryGaugeView.forceAnimate` defaults false; Theme & Gauge preview passes true; static selection and visibility/thermal gates remain respected.
- [x] Percentage text outline token is 1.2dp; both Canvas text passes use one measured layout and the same origin.
- [x] Theme preview passes the full Basic draft (appearance mode + accent) and gauge configuration into `PowerGuardTheme`.
- [x] Charging information icons are adjacent to their headings; both explanatory dialogs use opaque `surfaceContainerHigh` surfaces with outline/elevation.
- [x] Disabled Save text uses `onSurface`, matching Cancel; enabled Save remains primary/onPrimary.
- [x] Status Bar interval presets are 32dp and aligned with the 32dp custom field; scroll body and sticky footer are separate, with imePadding retained.
- [x] Capacity dirty-state compares the effective (capacityMah, isManualMode) pair; unchanged Automatic state remains clean.
- [x] Regression tests added/updated for gauge motion, dual Wave layers, forceAnimate, outline width, live Basic accent preview, and capacity dirty-state. Existing Status Bar preset/dirty-state tests retained.
- [x] Final full-diff self-crosscheck: PASS — 20/20 requested checks; 14 changed paths; no protected Room, sensor/sampling, calibration, service/lifecycle, RECOVERY_HOLD, Manifest, Gradle/dependency, or applicationId paths changed.
- [x] Run #683 (`45625532677`) on source HEAD `e3f61d1290ccb87c42da833c21be0cf4a876d465`: SUCCESS — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload. APK Artifact ID `15027787319`; report Artifact ID `15028747474`.
- [ ] Exact final test/documentation HEAD CI: pending; Run #683 predates the final Wave boundary assertion and documentation-only commits.
- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-25 — PR #12 combined hotfix

- [x] Wave liquid fill follows battery level independently of charging animation eligibility; all four animation modes retain the fill.
- [x] Wave/Capsule percentage uses one measured TextLayoutResult and identical Canvas coordinates for 1.8dp dark rounded outline and pure-white ExtraBold fill.
- [x] Theme & Gauge preview model label is width-bounded and ellipsized.
- [x] History uses the shared numeric range formatter with U+2192; straight/curly apostrophe-n regressions are covered.
- [x] Basic Light preserves DESIGN.md Warm Ivory / Paper / Mineral colors and strengthens outline contrast.
- [x] OS ETA is captured only when API 28+ exposes a positive value; implausibly low estimates below 90% are sanity-checked against remaining design capacity and active current.
- [x] Overview, Charging, and foreground notification consume the same resolved ETA field.
- [x] The only new platform reading is BatteryManager's OS ETA API; existing voltage/current/temperature reads, normalization, and sampling cadence are unchanged.
- [x] Capacity dirty-state compares numeric value and manual/automatic mode; automatic action is compact and disabled when already automatic.
- [x] Status Bar interval presets reflect the effective active interval; initial draft state follows the saved value.
- [x] Added regression coverage for Wave, outline, History, theme contrast, ETA, capacity dirty-state, interval presets, and notification ETA.
- [x] Full diff review: PASS — 22 files; database/schema, manifest, Gradle, applicationId, existing sensor normalization/sampling cadence, calibration default, and service lifecycle/startup remain unchanged. Engine/service changes are isolated to OS ETA capture/resolution.
- [x] Run #661 (45608819047): SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, Upload APK, and Upload unit-test reports.
- [x] Debug APK Artifact ID 15018512787; unit-test report Artifact ID 15018722461.
- [x] Run #659 compile failure was traced to the drawText import package and corrected in the follow-up source commit.
- [x] Run #662 (45609359837) on final combined source/documentation HEAD: SUCCESS — all jobs passed; APK Artifact ID 15018364086; unit-test report Artifact ID 15019132838.

- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD; no merge or auto-merge.

---

## 2026-11-25 — PR #12 follow-up: smoother button feedback and thinner gauge outline

- [x] `powerButtonClickEffect` uses 0.95 press scale with MediumBouncy / MediumLow spring.
- [x] Accent glow peaks at 0.20 and uses 150ms press / 180ms release tweens.
- [x] Shared action wrappers support a 90ms delay before closing-dialog callbacks; Save, Batal, Tutup, and equivalent confirmation actions use it.
- [x] Atur indikator, Ubah Kapasitas Desain, Buka Pengaturan Awal, Tes, Hitung Sekarang, Dashboard actions, Analytics actions, theme-package cards, and option pills use the shared interaction effect.
- [x] Wave and Capsule percentage outline is 1.8dp with StrokeJoin.Round; both text layers retain the same centered alignment.
- [x] Added `InteractionVisualTokensTest` for scale, glow alpha/timing, close delay, and gauge outline width.
- [x] Source diff review: PASS — 10 files changed; only UI source, focused unit test, and documentation files changed. Protected database, sensor, calibration, service, manifest, and build configuration paths are unchanged.
- [x] `testDebugUnitTest`: SUCCESS — Run #657.
- [x] `assembleDebug`: SUCCESS — Run #657.
- [x] Anti-Slop UI Audit Gate: SUCCESS — Run #657.
- [x] Debug APK Artifact ID `15014994471` and unit-test report Artifact ID `15014969066` uploaded — Run #657.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-25 — PR #12 final polish: outlined gauge text and uniform interaction system

- [x] Wave and Capsule percentage badges have no backing Surface/pill; the percentage is white ExtraBold text with a 3.5dp rounded black outline token centralized in ui/theme/.
- [x] Capsule terminal cap is separated from the body by a visible gap; the positive (+) mark is removed.
- [x] Shared `powerButtonClickEffect` uses a shared MutableInteractionSource, 0.94 spring bounce, and 0.24 primary-accent flash.
- [x] Dialog action buttons and interactive choice pills use 36dp CircleShape; Save is outlined/muted when unchanged and primary/onPrimary when enabled.
- [x] Theme & Gauge dirty-state compares the full config; footer is right-aligned with 10dp spacing and no equal-width weights.
- [x] Capacity Save requires a valid value different from the saved value; Use Automatic is enabled only for a manual override.
- [x] Status Bar Indicator dirty-state compares effective mode, metrics, scale, and interval; custom interval input has 56dp minimum width and 32dp CircleShape.
- [x] Capacity and Status Bar dialogs keep the footer outside the scrollable body and apply imePadding().
- [x] Info popup header/footer compacted; header information icon follows MaterialTheme primary.
- [x] Analysis Hitung Sekarang uses the shared 36dp circular action button.
- [x] Existing SessionLevelRangeFormatterTest asserts U+2192 exactly once and rejects the malformed 'n sequence.
- [x] Added capacity dirty-state and automatic/manual eligibility tests.
- [x] Added Status Bar Indicator dirty-state tests for mode, metrics, scale, and interval.
- [x] Added Theme & Gauge dirty-state tests for gauge model, animation, accent, and dual-tint.
- [x] Source diff review: PASS — 15 files changed in this polish; protected database, sensor, calibration, service, manifest, and build-configuration paths are unchanged.
- [x] `testDebugUnitTest`: SUCCESS — Run #655.
- [x] `assembleDebug`: SUCCESS — Run #655.
- [x] Anti-Slop UI Audit Gate: SUCCESS — Run #655.
- [x] Debug APK Artifact ID `15002199739` and unit-test report Artifact ID `15003148539` uploaded — Run #655.
- [ ] Physical-device QA on itel P55: NOT RUN.
- [ ] PR #12 merge: HOLD — no merge or auto-merge.

---

## 2026-11-25 — PR #12 final polish

- [x] Midnight display name and all four accent summary labels use Midnight; internal config and persisted keys are unchanged.
- [x] Midnight VIOLET/PINK/CYAN/GREEN use the requested per-accent background, surface, outlineVariant, and primary tokens; all explicit color literals remain in ui/theme/.
- [x] Theme picker exposes the four matching accent swatches.
- [x] Wave idle contour uses a static 2.5dp amplitude override, including endpoint battery levels; charging animation path remains separate.
- [x] Wave and Capsule percentage badges use 8dp × 3dp padding.
- [x] Info popup header/footer compacted and close action follows active theme.
- [x] Dashboard and Settings capacity dialogs share compact layout; Save requires valid changed value/mode, Automatic action is available only for a manual override.
- [x] Status Bar Indicator Save is disabled until a valid draft differs from persisted settings.
- [x] Custom clickable surfaces and choice pills use animated press scale; Material 3 buttons retain default ripple/pressed feedback; no indication=null introduced.
- [x] Session range formatter uses Unicode U+2192; unit test asserts exactly one arrow and rejects the malformed 'n sequence.
- [x] testDebugUnitTest: SUCCESS — baseline Run #653.
- [x] assembleDebug: SUCCESS — baseline Run #653.
- [x] Anti-Slop UI Audit Gate: SUCCESS — baseline Run #653.
- [x] APK Artifact ID `15000545345` and unit-test report Artifact ID `15000465571` uploaded — baseline Run #653.
- [ ] Physical-device QA on itel P55: NOT RUN.

---

## 2026-11-25 — PR #12 polish: Midnight accents and gauge readability

- [x] Added MidnightAccent with safe ungu/pink/cyan/green parsing and Violet fallback.
- [x] Persisted/restored the Midnight accent in the existing theme SharedPreferences; legacy records without the key resolve to Violet.
- [x] Added four 32dp accent choices to the Midnight dialog; draft preview and Save flow use the same ThemePackConfig.
- [x] Centralized the Pink, Cyan, and Green hex literals in ui/theme/Theme.kt.
- [x] Wave gauge now has a high-contrast 1.5dp outline, two charging wave layers, edge-damped amplitude, and a percentage badge.
- [x] Capsule gauge now has a stronger frame/terminal and a percentage badge.
- [x] Updated Status Bar Indicator dialog button shape/size and Indonesian History/SOH copy.
- [x] Added unit tests for Midnight accent parsing, invalid-key fallback, persistence round-trips, and summary formatting.
- [x] Protected boundaries reviewed: Room DB v3, sensor/sampling, calibration 1x, P0 foreground monitoring channel/lifecycle, RECOVERY_HOLD, and applicationId remain outside the intended diff.
- [x] GitHub Actions Run #650 on implementation HEAD `0c2d87d0e473db59e1d66a6b9cd3ccb7d904e1b1`: all jobs SUCCESS.
- [x] `testDebugUnitTest`: SUCCESS — Run #650.
- [x] `assembleDebug`: SUCCESS — Run #650.
- [x] Anti-Slop UI Audit Gate: SUCCESS — Run #650.
- [x] Upload APK: SUCCESS — artifact ID `14989161654`.
- [x] Upload unit-test reports: SUCCESS — artifact ID `14988424665`.
- [x] Source diff review: PASS — 11 changed files; protected DB, sensor, sampling, calibration, service lifecycle, manifest, and build-configuration paths are unchanged.
- [ ] Physical-device QA on itel P55: NOT RUN; required before merge.
- [ ] PR #12 merge: HOLD; no merge or auto-merge.

### Accessibility note
- The requested 32dp accent pills and 36dp dialog buttons are smaller than the 48dp touch-target guidance in DESIGN.md. CI cannot verify thumb usability; retain physical review as a release gate.

---
## 2026-11-25 — PR #12: theme packs and animated battery gauges

- [x] Created `feat/theme-system-and-battery-gauges` directly from verified `main` HEAD `229081788c01ebbe1b20d9a0b4c9ce2dac2d122d`.
- [x] Added Basic and Midnight Violet theme-pack configuration; SYSTEM mode uses reactive `isSystemInDarkTheme()`.
- [x] Added safe key parsers and dedicated SharedPreferences persistence for package, mode, accent, dual tint, gauge model, and charging animation.
- [x] Exposed theme config as a ViewModel StateFlow and applied it at the Compose root.
- [x] Added four Canvas gauge models and four charging animation modes; infinite transition is only composed while charging, visible, and thermally normal.
- [x] Enforced semantic color priority: critical thermal > warm > charging > normal theme accent. Existing analyzer thresholds 40°C / 38°C are reused.
- [x] Added the two-layer Settings picker, draft live preview, sticky footer, and two-line summary.
- [x] Centralized explicit color hex literals in `ui/theme/`; added an Anti-Slop audit guard for literals outside that package.
- [x] Added unit tests for preference parser/fallback/round-trip, semantic color priority, segment boundaries, and summary formatting.
- [x] Follow-up source review: thermal animation is gated in the reusable gauge, capsule terminal has a plus marker, dual-tint OFF removes both tints, preview thresholds reuse shared analyzer constants, idle gauges suppress charging overlays, capsule label contrast follows fill luminance, and animated phase reads occur in draw scopes.
- [x] Corrected the Anti-Slop color-literal regex and verified its match behavior against `Color(0xFF112233)`; final CI rerun pending.
- [x] No intended changes to Room DB v3, sensor acquisition/cadence, calibration default 1x, foreground monitoring P0 channel/lifecycle, RECOVERY_HOLD, or applicationId.
- [x] Full diff review: PASS — 15 changed files; protected Room, sensor, sampling, calibration, P0 lifecycle, RECOVERY_HOLD, and build configuration files are unchanged.
- [x] `testDebugUnitTest`: SUCCESS — Run #646.
- [x] `assembleDebug`: SUCCESS — Run #646.
- [x] Anti-Slop UI Audit Gate: SUCCESS — Run #646; corrected color-literal regex was exercised by CI.
- [x] Upload APK: SUCCESS — Run #646; debug APK Artifact ID `14955470895`.
- [x] Upload unit-test reports: SUCCESS — Run #646; Artifact ID `14955331593`.
- [x] Final implementation CI Run #646 HEAD: `14261dba3660d1ee24a8a61dee16a02b6748fa71`.
- [ ] Physical-device QA on itel P55 360dp / 720p: NOT RUN.
- [ ] PR #12 merge: HOLD until CI and physical-device QA are confirmed.

### Accessibility exception
- Requested 32dp choice pills and 36dp sticky-footer buttons are below the 48dp touch-target guidance in DESIGN.md. Keep this as an explicit QA item; do not report full accessibility compliance before device review.

---

## 2026-11-24 — PR #11 final polish: sticky popup, state-aware footer, and alert repetition

- [x] Charge Guard dialog has a fixed compact header, weighted scrollable body, and sticky compact footer.
- [x] Tone picker and draft Tes action share one row.
- [x] Footer Batal is an outlined 36dp CircleShape button; Simpan uses the same size and is enabled only when the draft differs from persisted configuration/tone settings.
- [x] Removed the outer Settings-card Tes action; retained the chevron and full-width two-line ellipsized summary.
- [x] Added persisted repeat modes Sekali (default), Tiap 1 mnt, and Tiap 3 mnt; summary displays Ulang 1m / Ulang 3m when applicable.
- [x] Added repeat policy with maximum five repeats per incident after the initial alert, independently tracked for charge-limit and temperature alerts.
- [x] Added notification deleteIntent and non-exported lightweight dismissal receiver; dismissal suppresses further repeats for that active incident without changing service lifecycle.
- [x] Charger disconnect receiver immediately clears active repeat state. Existing service evaluation also reconciles inactive conditions on samples.
- [x] Temperature repeat remains active until temperature is strictly below threshold minus 2°C; exact hysteresis boundary does not re-arm.
- [x] Added unit tests for repeat mode persistence, interval boundaries, five-repeat cap, dismiss suppression, unplug reset, hysteresis stop, and summary formatting.
- [x] No Room DB v3, sensor acquisition/cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, RECOVERY_HOLD, or applicationId change intended.
- [x] Diff review: PASS — changes since prior HEAD are limited to Charge Guard dialog/settings, repeat policy/persistence, dismiss receiver/service integration, focused tests, manifest receiver declaration, and documentation.
- [x] Anti-Slop UI Audit Gate: SUCCESS (Run #634).
- [x] testDebugUnitTest: SUCCESS (Run #634).
- [x] assembleDebug: SUCCESS (Run #634).
- [x] Upload APK: SUCCESS (Run #634); debug APK Artifact ID 14940768566; unit-test report Artifact ID 14940269848.
- [ ] Physical-device QA on itel P55: NOT RUN; verify sticky footer reachability, 36dp/32dp touch usability, notification dismissal, repeat cadence, charger unplug, and temperature hysteresis.
- [ ] PR #11 merge: HOLD until CI SUCCESS and physical-device QA confirmation.

### Accessibility trade-off
- Footer buttons are 36dp and selection pills are 32dp per the requested compact design; DESIGN.md specifies 48dp minimum touch targets. This is a documented exception, not an accessibility PASS, and must be reviewed on device.

---

## 2026-11-24 — PR #11 follow-up: audio fallback, custom vibration, and compact switches

- [x] Replaced all four Settings/Charge Guard Material 3 Switch controls with reusable CompactSwitch (42×24dp track, 18dp thumb, animated movement, 36dp interaction height).
- [x] Added READ_MEDIA_AUDIO for API 33+ and READ_EXTERNAL_STORAGE with maxSdkVersion=32; runtime permission is requested after an external MediaStore URI is selected and permission is missing.
- [x] Custom URI validation checks openAssetFileDescriptor(..., "r") and rejects null/zero-length descriptors. SecurityException and FileNotFoundException are treated as inaccessible.
- [x] Preview player tries the selected custom URI, then Settings.System.DEFAULT_NOTIFICATION_URI, then RingtoneManager's notification default. Missing ringtone or play exception advances to the next candidate.
- [x] Preview player retains the Ringtone instance, stops the previous preview, and auto-stops after four seconds. Hening stops active audio; Off cancels prior vibration.
- [x] Added Off, Singkat, Ganda (default), SOS, and Kustom vibration patterns. Custom segments are sanitized to 50–1500ms, limited to 15 pulse/gap segments (maximum eight pulses), and persisted in Charge Guard SharedPreferences.
- [x] Notification-channel ID hashes the effective sound URI and complete vibration waveform; channel vibration uses the selected timings.
- [x] Settings summary is split into two ellipsized single-line rows.
- [x] All History session level-range labels route through SessionLevelRangeFormatter; unavailable end values use the same Unicode arrow formatter.
- [x] Unit tests added for built-in waveform definitions, custom sanitization/persistence, channel hash variation, inaccessible/missing audio fallback, and summary formatting.
- [x] No Room schema/entity/migration, sensor acquisition/cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, RECOVERY_HOLD, SDK, dependency, version, or applicationId change intended.
- [x] Anti-Slop UI Audit Gate: SUCCESS (Run #620).
- [x] testDebugUnitTest: SUCCESS (Run #620).
- [x] assembleDebug: SUCCESS (Run #620).
- [x] APK upload: SUCCESS (Run #620); debug APK Artifact ID 14937035355; unit-test report Artifact ID 14936547835.
- [ ] Physical-device QA on itel P55: NOT RUN; required before merge.
- [ ] PR #11 merge: HOLD until CI SUCCESS and physical-device QA confirmation.

### Accessibility note
- CompactSwitch interaction height is 36dp as explicitly requested. DESIGN.md sets a 48dp minimum target; this deviation is recorded for physical usability review rather than being reported as fully compliant.

---

## 2026-11-24 — PR #11 follow-up: compact Charge Guard popup and 50°C threshold

- [x] Replaced the expanded Settings Charge Guard controls with a compact one-row `Peringatan & Nada` card, dynamic single-line summary, quick `Tes`, and chevron.
- [x] Added scrollable and IME-aware popup using the Status Bar Indicator dialog container pattern.
- [x] Charge target and temperature thresholds use uniform 32dp selectable pills (5 charge choices; 4 temperature choices including 50°C).
- [x] Popup edits are draft-only; `Batal` discards changes and `Simpan` persists both Charge Guard configuration and alert tone/vibration.
- [x] Ringtone picker uses `TYPE_NOTIFICATION`, default/silent flags, and the current draft URI. Draft `Tes` previews tone/vibration without persisting the draft.
- [x] 50°C threshold is accepted by configuration normalization and SharedPreferences round-trip.
- [x] Hysteresis regression test verifies 48°C does not re-arm at a 50°C threshold and 47.9°C does.
- [x] Compact summary formatter tests active thresholds/tone, inactive state, and normalization of unsupported thresholds.
- [x] History formatter source retains `\\u2192`; History test verifies `44% → 54%` and excludes malformed apostrophe-`n` text.
- [x] Reviewed protected boundaries: Room DB v3, sensor acquisition/cadence, calibration default 1x, P0 foreground monitoring, `RECOVERY_HOLD`, and applicationId unchanged.
- [x] GitHub Actions CI Run #608 on branch HEAD `ba7110bfe41f8e77ac05c4e424ad91fc766a265c`: Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all SUCCESS. APK artifact `14933876095`; test report `14933741143`.
- [ ] Physical-device QA on itel P55: NOT RUN; required before merge.
- [ ] PR #11 merge: HOLD until physical-device QA is confirmed.

---

## 2026-11-24 — PR #11 follow-up: Charge Guard alert tone controls

- [x] Settings placeholder replaced by a functional `Nada peringatan` row inside the existing Charge Guard card; selected tone and vibration state are summarized in one line.
- [x] `Tes` previews the configured ringtone and vibration without running on the UI thread.
- [x] Android ringtone picker supports notification tones, default, silent, and custom local audio selection; no storage permission added.
- [x] Tone selection marker / URI, display title, and vibration preference persist in the dedicated Charge Guard SharedPreferences file.
- [x] Android 8+ alert channel ID is configuration-derived (`charge_guard_alert_<hash>`); old alert channel is deleted on configuration change. Foreground monitoring channel remains separate.
- [x] Invalid or inaccessible custom audio URI falls back to `Settings.System.DEFAULT_NOTIFICATION_URI` for preview and notification channel setup.
- [x] Unit tests cover preference defaults/persistence, invalid-URI fallback, and channel ID stability/configuration changes.
- [x] Reviewed service integration: alert notification dispatch uses the service IO scope; alert evaluation remains after the active-sample state is built and does not enter the `RECOVERY_HOLD` early-return path.
- [x] Room DB v3, sensor acquisition/cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, and applicationId remain unchanged by this follow-up.
- [x] PR CI Run #601 on head `747f12e98511e7d4ca1a11e5a1e070d3ca06f501`: Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all SUCCESS.
- [x] Debug APK artifact ID: `14920844863`; unit-test report artifact ID: `14921524240`.
- [ ] Physical-device QA on itel P55: NOT RUN; required before merge.
- [ ] PR #11 merge: HOLD until physical-device QA is confirmed.
- [x] Documentation follow-up CI Run #602: Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all SUCCESS; APK artifact `14925414575`, test report `14926982019`.

---

## 2026-11-24 — PR #11: data consistency, History polish, and Charge Guard alarms

- [x] Baseline PR #10 merged; latest main SHA `18d771a9fca0d51703b01f3c81d8a00c350febc5`; main CI Run #578 and Dynamic Run #14 SUCCESS.
- [x] Created `feat/data-consistency-and-charge-guard` from the verified main SHA; branch is not behind main at implementation start.
- [x] History range formatter uses explicit `\u2192`; compact duration formatter is one line; generic anomaly badge is suppressed when specific anomaly badges exist.
- [x] Selected-session highlight and curve/filter ordering updated.
- [x] Current chart scale excludes absolute current values above 25,000 mA without mutating raw sample values.
- [x] Overview and Insights use one shared mapper over the same persisted SOH snapshot.
- [x] Seven-day chart has distinct Indonesian weekday abbreviations, selectable bars, and daily mAh summary formatting.
- [x] Existing temperature ribbons verified present in Charging and curve session UI; Status Bar dialog adds IME padding and retains vertical scrolling.
- [x] Charge Guard uses separate SharedPreferences; both new toggles default OFF; target and temperature choices are constrained to the requested values.
- [x] Alarm policy uses one alert per charge session/target, temperature hysteresis, and a 15-minute cooldown. Service integration is after the existing active-sample state construction; RECOVERY_HOLD early-return path is unchanged.
- [x] Unit tests added for formatters, outlier scaling, SOH snapshot mapping, daily trend summary/labels, anomaly badge suppression, charge/temperature alarm policy, and SharedPreferences defaults/persistence.
- [x] Room v3 schema and entities unchanged; sensor acquisition, sampling cadence, calibration default 1x, P0 lifecycle transitions, RECOVERY_HOLD, and applicationId unchanged by intended diff.
- [x] Final diff review: PASS — 21 changed files; no Room entity/schema/migration, Gradle, manifest, package/version, sensor acquisition, sampling cadence, or calibration files changed. The service diff is limited to the post-sample alert evaluation and notification helpers.
- [x] Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload: SUCCESS — PR CI Run #591 (`45390892432`).
- [x] Debug APK artifact ID: `14917414475` (`android-debug-apk`); unit-test report artifact ID: `14919250772`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN; required before merge.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN; required before merge.
- [ ] PR #11 merge: HOLD until CI SUCCESS and physical QA confirmation.

---

## 2026-11-24 — PR #10: visual presentation, diagnostic summaries, and live status-bar preview

- [x] Baseline: PR #9 merged at `6fbea39ea0fa694f32b3185b7218f8e5a5238675`; main CI Run #559 SUCCESS. Latest main after the documentation-only restoration is `4140b41313959087a1b97c4a823ff4f176376ed7`; main CI Run #560 SUCCESS. PR #10 targets that main SHA.
- [x] Branch: `feat/visual-presentation-and-ux-polish` already existed when this task was resumed; verified aligned with PR #10 base and zero commits behind latest main. No duplicate branch was created.
- [x] Header: 38dp brand row retains status-bar insets and replaces duplicate page name with a live service-state pill; bottom navigation identifies the active tab.
- [x] Overview: one compact hero card groups the circular battery arc, four live readings, and valid ETA; SOH and system-readiness cards follow it.
- [x] Charging / curve: segmented sample-weighted temperature band (Aman <38°C, Hangat 38–<40°C, Tinggi ≥40°C) and CC/CV-like battery-side pattern heuristic added. UI copy explicitly says this is not direct protocol identification; unavailable/ambiguous data and sample gaps over two minutes remain unknown.
- [x] Voltage presentation: V and mV shown together; battery-side power explanation moved into a tap dialog.
- [x] Insights: seven-day trend aggregation, high-temperature marker, and Indonesian date formatter added.
- [x] History: floating table header removed; mAh units localized inline; level range widened; selected session highlighted; unavailable average power omitted; legacy fixed-voltage watt estimate removed.
- [x] Accessory comparison: normalized +%/jam, average peak temperature, horizontal bars, Tercepat and Cenderung Panas badges added.
- [x] Settings: production-renderer bitmap preview (32dp container / 24dp bitmap) follows draft single/custom mode, selected rotation parameters and interval, and calibration scale; compact summary and Indonesian copy added.
- [x] Focused unit tests added for thermal zones / CC-CV-like mapping, seven-day aggregation/date labels, accessory ranking/peak temperature, and Settings summary formatting.
- [x] Protected scope by source review: Room v3, sensor acquisition/cadence, calibration default 1x, P0 BatteryMonitorService lifecycle, RECOVERY_HOLD, startup policy, and applicationId remain unchanged.
- [x] No package, dependency, SDK, app-version, or build-number changes; no report export/copy feature added.
- [x] Final full diff review: PASS — 21 changed files; no Room, service, sensor acquisition, calibration, Gradle, manifest, package, or version files changed.
- [x] Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK and test-report uploads: SUCCESS — final PR CI Run #577 (`45360617867`).
- [x] Debug APK artifact ID: `14904452445` (`android-debug-apk`); unit-test report artifact ID: `14903734041`.
- [x] Physical-device QA on itel P55 at 360dp / 720p: PASS — user confirmed CI #577 APK artifact `14904452445`.
- [x] PR #10 squash-merged at `18d771a9fca0d51703b01f3c81d8a00c350febc5`; main CI Run #578 and Dynamic Run #14 SUCCESS.

---

## 2026-11-24 — PR #9: compact five-tab header, History anomaly filters, and crisp rotation

- [x] Baseline: PR #8 merged and main CI Run #550 SUCCESS on `79c5523704ba62e431d30b31bbdb31da14ecbba0`; branch created from that SHA.
- [x] Header: 38dp row, statusBarsPadding, 16dp horizontal padding, 0.5dp divider, brand/page label.
- [x] Subheaders: removed from Charging, Insights, History, Settings; first content inset normalized to 8dp.
- [x] History: five LazyRow filter chips, default < 1 mnt; large Switch row removed.
- [x] Domain: anomaly evaluator, presentation mapper, date grouping, and deterministic filter matching implemented.
- [x] Recovery provenance: written only during existing checkpoint reconciliation; Room v3 and isAnomaly / RECOVERY_HOLD state unchanged.
- [x] Session metadata: source and temperature shown; power is — W because session-average voltage/power is not persisted. Missing end-level remains unknown rather than being treated as zero gain.
- [x] Renderer: condensed bold typeface and bounded bitmap cache retained; five-character values and 8% alpha boost added.
- [x] Rotation policy: 1,000ms for enabled custom rotation while interactive; 3,000ms otherwise; sample loop unchanged.
- [x] Focused unit tests added/updated for anomaly evaluation, filters, alpha boost, and refresh policy.
- [x] Diff review: completed for all 19 changed files against the PR #9 base.
- [x] Anti-Slop UI Audit Gate: SUCCESS — CI Run #557 (`45339264157`).
- [x] `testDebugUnitTest`: SUCCESS — 258 tests, 0 failures; `assembleDebug`: SUCCESS; APK upload: SUCCESS.
- [x] Debug APK artifact ID: `14895517683` (`android-debug-apk`); test report artifact ID: `14895093496`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN; required before merge.
- [ ] PR #9 merge: HOLD until physical QA is confirmed.

---

## 2026-11-24 — PR #8 follow-up: uniform 32dp Status Bar dialog pills

- [x] Baseline: PASS — branch `feat/crisp-statusbar-indicator` was verified at `907a90a0f3c0668e3e3199688109bf386b1ad6cc`; PR #8 was OPEN / not merged.
- [x] Display mode: PASS by source review — two equal-weight 32dp Surface pills, 6dp gap, 8dp corners, centered text with font padding disabled, matching selected container/border colors, and no check icon.
- [x] Parameter grid: PASS by source review — six 32dp Surface pills in a 2 × 3 grid, 6dp gaps, 8dp corners, zero vertical padding, and centered text with font padding disabled.
- [x] Current-scale calibration: PASS by source review — three equal-weight 32dp Surface pills, 6dp gap, matching selected styling, centered text, and no segmented control/check icon.
- [x] Rotation interval: PASS by source review — one horizontal row with four equal-weight 32dp presets plus a 52dp × 32dp numeric BasicTextField; preset taps synchronize the input and custom 1–60 input updates the draft and active border.
- [x] Keyboard and validation: PASS by source review — numeric keyboard with IME Done, no placeholder, existing 1–60 validation and Simpan gate retained.
- [x] Protected scope: PASS by source review — only dialog layout/state presentation and documentation changed; draft/save semantics, renderer, sensor, calibration default 1x, Room v3, monitoring lifecycle / `RECOVERY_HOLD`, and `applicationId` remain unchanged.
- [x] Anti-Slop UI Audit Gate: PASS — Run #547 (`45323370940`).
- [x] `testDebugUnitTest` and `assembleDebug`: PASS — Build and Unit Tests job completed successfully; Gradle reported `BUILD SUCCESSFUL`.
- [x] APK and unit-test report uploads: PASS — debug APK artifact `14888163325`; unit-test report artifact `14888802071`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- [!] Accessibility trade-off: 32dp visual control height is below the 48dp touch-target minimum in `DESIGN.md`; retained only because the task explicitly specifies 32dp. Confirm usability on device and consider larger hit regions if the layout permits.
- [ ] PR #8 merge: HOLD — do not merge before latest APK physical QA.

## 2026-11-24 — PR #8 follow-up: compact parameter cells and custom interval input

- [x] Baseline: PASS — branch `feat/crisp-statusbar-indicator` was verified at requested base `d81a016c55b5e5d2dcfa716c901d6d9d02f409b9`; PR #8 was OPEN / not merged.
- [x] Parameter grid: PASS by source review — six option pills use `Modifier.height(36.dp)`, 8dp horizontal / 4dp vertical content padding, and 6dp horizontal / vertical cell gaps.
- [x] Custom interval: PASS by source review — `BasicTextField` is centered, single-line, placeholder-free, and contained in a 56dp × 34dp `Surface` with 8dp rounded corners and validation-aware border color. Existing numeric filtering and 1–60 validation are retained.
- [x] Dialog density: PASS by source review — title/content horizontal inset is 16dp and content section spacing is 8dp.
- [x] Protected scope: PASS by source diff review — only `AppScreens.kt`, `CHANGELOG.md`, and this checklist are changed; draft/save behavior, renderer, sensor, calibration default 1x, Room v3, service lifecycle / `RECOVERY_HOLD`, and `applicationId` are untouched.
- [x] Anti-Slop UI Audit Gate: PASS — GitHub Actions Run #542.
- [x] `testDebugUnitTest`: PASS — GitHub Actions Run #542.
- [x] `assembleDebug`: PASS — GitHub Actions Run #542.
- [x] APK and unit-test report uploads: PASS — debug APK artifact `14886369907`; unit-test report artifact `14886509930`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- [ ] PR #8 merge: HOLD — do not merge before latest APK physical QA.

## 2026-11-24 — PR #8 follow-up: compact Settings and Status Bar controls

- [x] Baseline: PASS — branch `feat/crisp-statusbar-indicator` was verified at requested base `446996fed5200bd20179990ced292a5596f8392e`; PR #8 was OPEN / not merged.
- [x] Settings action: PASS by source review — `Atur indikator` uses full-width `OutlinedButton` with Tune icon, matching the neighboring Settings OutlinedButtons.
- [x] Parameter grid: PASS by source review — trailing check icon and reserved spacer removed; selected container, stronger border, and selected text weight/color remain; label receives the full cell width.
- [x] Custom interval: PASS by source review — one horizontal row, concise left label, centered placeholder-free 72dp × 48dp numeric field, and existing 1–60 validation retained.
- [x] Scope: PASS by diff review — only `AppScreens.kt`, `CHANGELOG.md`, and this checklist changed; draft/save behavior, renderer, sensor, calibration default 1x, Room v3, service lifecycle / `RECOVERY_HOLD`, and `applicationId` untouched.
- [ ] Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: NOT RUN — pending CI for latest branch commit.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- [ ] PR #8 merge: HOLD — do not merge before latest APK physical QA.

## 2026-11-24 — PR #8 follow-up: calibration draft and Auto-Start diagnostic UI removal

- [x] Baseline and merge gate: PASS — branch `feat/crisp-statusbar-indicator`; PR #8 verified OPEN / not merged before changes.
- [x] Calibration storage: PASS by source review — `BatteryCurrentScaleSettings` remains SharedPreferences; `BatteryMonitorEngine` synchronous read path and 1x fallback are unchanged.
- [x] Draft lifecycle: PASS by source review — metric, display mode, rotation selection/interval, and calibration are local dialog state; only Simpan invokes the combined ViewModel save action. Batal and outside dismissal do not persist draft values.
- [x] Persistence boundaries: PASS by source review — metric/mode/rotation/interval are written in one DataStore edit; current scale is written through the existing SharedPreferences setting. These are one UI save action, not an atomic cross-store transaction.
- [x] Settings summary: PASS by source review — active scale (including default 1x) is displayed in the Status Bar Indicator card subtitle.
- [x] Auto-Start diagnostic UI: PASS by source review — Settings export card and share action removed; `AutoStartDiagnosticLog` implementation and internal receiver/coordinator/service references are retained.
- [x] Protected constraints: PASS by diff scope — Room v3, `BatteryMonitorService`, `RECOVERY_HOLD`, background recovery mechanism, `applicationId`, package, dependencies, SDK, and app version unchanged.
- [x] Regression coverage: PASS by source review — combined DataStore configuration persistence test added; existing SharedPreferences default/persistence/fallback tests retained.
- [ ] Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: NOT RUN — pending CI on latest branch commit.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- [ ] PR #8 merge: HOLD — do not merge before latest APK physical QA.

## 2026-11-24 — PR #8 follow-up: compact Status Bar settings dialog

- [x] Baseline: PASS — PR #8 was OPEN / not merged; branch head matched requested base `6640b05cee520fb79c8231ade79f611548c3c716` before edits.
- [x] Parameter selection: PASS by source review — six parameters are arranged in a fixed 2 × 3 grid with concise labels, 8dp gaps, 48dp minimum target height, selected border/tone, check icon, and radio/checkbox semantics.
- [x] Mode hierarchy: PASS by source review — full-width Material 3 `SingleChoiceSegmentedButtonRow` at the top of the dialog.
- [x] Rotation interval: PASS by source review — four equal-weight preset chips share one row; custom numeric input is horizontal, labelled above the field, has no placeholder, and validates 1–60 seconds.
- [x] Footer and draft behavior: PASS by source review — divider-separated Batal / Simpan actions; local draft state is applied only on Simpan, while Batal and outside dismissal discard it. Invalid custom interval disables Simpan in custom mode.
- [x] Scope: PASS — no renderer, sensor, calibration, rotation cadence, service lifecycle, Room v3, package, dependency, SDK, or app-version changes.
- [ ] Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: NOT RUN — pending CI for commit `bac5c77d8f4d9b6c698b0a8ab6d0968df087e04c`.
- [ ] Physical-device QA at 360dp / 720p on itel P55: NOT RUN — required before merge.
- [ ] PR #8 merge: HOLD — do not merge before physical-device QA.

## 2026-11-24 — PR #8 follow-up: resource-source glyph, custom rotation, accessory label cleanup

- [x] Base/branch verification: PASS — `feat/crisp-statusbar-indicator` was identical to requested base `6d2c04727701c48a87cf9279fbbd1f0ce731e4f2` before edits.
- [x] Renderer: PASS by source review — numeric/unit allocations 58% / 42%, 0.75dp target gap, unit width capped at 88% of the 24dp canvas; resource source has a separate one-line full-canvas path and retains the bounded bitmap cache.
- [x] Resource-source provenance: PASS by source review — only `EXTRA_PLUGGED` metadata is mapped to AC / USB / WL / BAT; missing or unsupported metadata maps to an em dash.
- [x] Settings: PASS by source review — single metric / custom rotation modes, six selectable metrics, minimum two selected rotation parameters, preset and custom 1–60 second interval, and DataStore persistence.
- [x] Rotation timing: PASS by source review — selected metric derives deterministically from monotonic elapsed realtime in the existing notification refresh path; no timer, alarm, or service was added.
- [x] Accessory labels: PASS by source review — DAO updates only `accessory_label`; group and reset-all actions leave session/sample rows untouched and are protected by confirmation dialogs.
- [x] Protected constraints: PASS by diff scope — Room v3, default 1x current calibration, P0 lifecycle / RECOVERY_HOLD, `applicationId`, package, dependencies, SDK, and app version unchanged.
- [x] Focused tests added for glyph ratios, one-line AC/USB/WL/BAT rendering, deterministic rotation, DataStore persistence, and DAO label clearing with session/sample retention.
- [x] `testDebugUnitTest`, `assembleDebug`, Anti-Slop UI Audit Gate, and APK artifact upload: PASS — Run #528 (`45247445044`), commit `717bd714ff75e05b8551df8996c8326629daea71`.
- [x] Debug APK artifact `14854875799`; unit-test report artifact `14855115433`.
- [x] Follow-up renderer assertions cover all four resource-source codes; notification summary verifies the selectable resource-source metric and legacy charging-status compatibility.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before merge.
- [ ] PR #8 merge: HOLD — do not merge before physical-device QA.

## 2026-11-23 — PR #8 follow-up: Status Bar glyph size and tight bounds

- [x] Root-cause review: PASS — existing 48dp bitmap is intended for a system small-icon surface and uses fixed 22dp/11dp text plus FontMetrics baseline centering; the system-sized glyph therefore has excessive apparent whitespace after scaling.
- [x] Bitmap target changed to 24dp; numeric and unit rows use bold sans-serif-condensed and are fitted within the full-bleed horizontal bound (95.8% maximum; 0.5dp side insets).
- [x] Both baselines use Paint.getTextBounds; numeric visual top and unit visual bottom are anchored at 0.5dp; target inter-row gap is 1dp.
- [x] Numeric renderer input is limited to four characters; adaptive rounding regression tests include negative decimal values and rounding across 10.
- [x] Existing bitmap cache identity, bounded eviction, and synchronization retained.
- [x] No sensor acquisition, current calibration, Room schema/data, `applicationId`, notification/service lifecycle, monitoring behavior, dependency, SDK, or app version change.
- [x] Focused renderer unit tests / `testDebugUnitTest`: PASS — GitHub Actions Run #521, commit `01d8c8886d22211b9c25e5c457bf9358648babd8`.
- [x] `assembleDebug`: PASS — GitHub Actions Run #521.
- [x] Anti-Slop UI Audit Gate: PASS — GitHub Actions Run #521.
- [x] Debug APK and unit-test report uploads: PASS — artifact IDs `14812579421` and `14813028690`.
- [x] Documentation follow-up commit `e49986f63afe8d9e6c9bac0bcc0a803bcc42251f`: GitHub Actions Run #522 SUCCESS — both jobs and all steps passed; debug APK artifact `14813983055`, unit-test report artifact `14813488060`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- [ ] PR #8 merge: HOLD — do not merge before the user confirms physical-device QA on the latest APK.

## 2026-11-23 — Stacked Status Bar Indicator and global current-scale calibration (PR #8)

- [x] Base commit verified: Run #514 SUCCESS on `1427664eccc69bb3c0f701b9e0b238ffef20d63e`; feature branch points to this exact SHA.
- [x] Status-bar renderer source updated to two-line value/unit layout with 62% / 35% vertical regions, bold typography, and no border.
- [x] Adaptive precision and current unit switching (mA below 1 A, A at/above 1 A) covered by tests.
- [x] Bitmap cache key includes metric type, value text, unit text, and density; hit, invalidation, bounded eviction, and concurrent requests covered by tests.
- [x] Settings exposes exactly five requested metrics; fresh install defaults to W while legacy `CHARGING_STATUS` and unknown stored values fall back safely to W.
- [x] Sensor calibration defaults to 1x and persists 1x / 1000x / 0.001x. The multiplier applies only to `CURRENT_NOW`; battery-side power uses calibrated current with Double intermediate. Voltage, temperature, level, and source-capability metadata remain unchanged.
- [x] Calibration is injected into BatteryMonitorEngine instances used for service snapshots and repository/fallback reads; new Room session/sample writes consume the same calibrated LiveBatteryState. Historical Room v3 rows and schema are not changed.
- [x] No service lifecycle, polling cadence, RECOVERY_HOLD, protocol Tier 2 classification, dependency, SDK, app version, package namespace, or `applicationId` change intended.
- [x] GitHub Actions Run #515 (run ID `45160871480`) on implementation commit `baad432e0f990a56f281f51ffaacca0caafd8543`: SUCCESS — `testDebugUnitTest` and `assembleDebug` passed.
- [x] Anti-Slop UI Audit Gate: SUCCESS in Run #515.
- [x] Debug APK upload: SUCCESS in Run #515; artifact `android-debug-apk`, ID `14810242816`.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before merge.
- [ ] PR #8 merge: HOLD — do not merge before physical-device QA.

## 2026-11-23 — PR #7 Charging Curve header follow-up

- Root-cause source review: PASS — the chart previously placed title and all legend items in a single non-wrapping Row; this could force the rightmost legend outside a 360dp viewport.
- Header layout: PASS — title moved to a dedicated line; legend items use FlowRow with 12dp horizontal / 4dp vertical spacing and the full Baterai (%) label.
- Chart top inset: PASS — explicit 10dp top inset and 6dp bottom inset; no 45–50dp spacer was found inside ChargingCurveChart during source inspection. Physical screenshot confirmation remains required.
- Chart rendering, selection gestures, sensor acquisition, charging calculations, Room schema/data, and monitoring lifecycle: PASS — no changes intended outside chart layout.
- Unit tests and debug build: NOT RUN — pending CI for the follow-up commit.
- Anti-Slop UI Audit Gate and APK upload: NOT RUN — pending CI.
- 360dp / 720p physical-device screenshot QA on itel P55: NOT RUN — required before PR #7 can be merged.
- PR #7 remains OPEN / HOLD MERGE.

## 2026-11-23 — History curve compact-focus polish (PR #7)

- [x] Baseline: branch created from post-PR #5/#6 main commit `d9e489f0e9d7c9a61d3948a58a2f782a12186086`.
- [x] Compact curve-focus header shows visible/total session count, `<1m` filter, and `Tutup` in one row; full History header/filter restored when closed: source implemented.
- [x] Curve remains outside the independently scrolling session LazyColumn; scrolling/selecting sessions does not scroll the chart away: source structure verified.
- [x] Accessory comparison card hidden only during curve-focus mode and restored after closing: source implemented.
- [x] Chart heading/legend and vertical spacing compacted; selected-point date/time, current, temperature, and battery values shown in one horizontal strip. Peak/min current, max temperature, and sample count retained: source implemented.
- [x] Missing curve readings render as unavailable marker and do not become measured zero: presentation helper and unit test added.
- [x] Four accessory quick choices use wrapping FlowRow without fixed-width ellipsis; free-text field remains without placeholder: source implemented.
- [x] No sensor, charging-calculation, Room v3, DAO/repository, AccessoryBenchmarkCalculator, monitoring lifecycle, dependency, SDK, package, applicationId, or app-version changes: source diff scope review.
- [ ] Unit tests and assembleDebug: NOT RUN — pending PR CI.
- [ ] Anti-Slop UI Audit Gate: NOT RUN — pending PR CI.
- [ ] APK artifact upload: NOT RUN — pending PR CI.
- [ ] Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before merge.
- [ ] PR #7 merge: HOLD — do not merge before physical-device QA.

## 2026-11-23 — PR #6 post-merge verification

- [x] Run #510 on exact PR #6 head `a50dd540445379b95cd2d2a4b4a2ca833cdb77af`: SUCCESS — Anti-Slop UI Audit Gate and Build and Unit Tests completed successfully; `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all succeeded.
- [x] User confirmed physical QA on itel P55 was completed before merge.
- [x] PR #6 squash-merged to main; merge commit `d9e489f0e9d7c9a61d3948a58a2f782a12186086`.
- [x] Main baseline for PR #7: `d9e489f0e9d7c9a61d3948a58a2f782a12186086`.

## 2026-11-23 — Overview source-label consistency and drawable cleanup

- [x] Active Overview route verified: `MainActivity` renders `SuperdDashboardScreen`; Overview now labels the shared `chargingProtocol` value as `Protokol / kemampuan sumber`, matching Charging.
- [x] Related surfaces reviewed: Dashboard capsule and monitoring notification already render the same shared label/value without conflicting source wording; legacy `DashboardScreen` is not routed from `MainActivity` and was left unchanged.
- [x] `ic_status_turbo_ring.xml` reference audit: PASS — repository code search for `ic_status_turbo_ring` and `turbo_ring` returned no results; the PR #5 diff does not add references. Removed the unused drawable.
- [x] Existing detector/catalog tests already cover the standardized capability labels; no new business-logic test added because this polish changes only Overview copy and removes an unreferenced resource.
- [x] No sensor acquisition, power formula, Tier 2 classification/evidence, `applicationId`, monitoring lifecycle, persistence, dependency, SDK, or app version changes.
- [ ] Anti-Slop UI Audit Gate: NOT RUN — pending CI after this polish.
- [ ] `testDebugUnitTest` and `assembleDebug`: NOT RUN — pending CI after this polish.
- [ ] APK artifact upload: NOT RUN — pending CI.
- [ ] Physical-device QA on itel P55: NOT RUN — wait for successful CI, then perform the planned single device test.
- [ ] PR #5 merge: HOLD — do not merge before itel P55 physical QA.

## 2026-11-23 — Charging source-capability label clarification

- [x] Main base CI Run #500: PASS — Anti-Slop UI Audit Gate and Build and Unit Tests on `77c8de31bf700e47b00717b85fb398070f2e7f5d`.
- [x] Root-cause audit: PASS — BatteryManager maximum-capability metadata and battery-side measured/calculated power use separate data paths; ambiguity was in user-facing labels and explanation.
- [x] AC Tier 2 labels explicitly describe source capability; no punctuation artifacts: IMPLEMENTED — requires branch CI.
- [x] Protocol info dialog distinguishes BatteryManager source capability from battery-side voltage × current and includes transient/stable-comparison guidance: IMPLEMENTED — requires branch CI and UI review.
- [x] Charging metric explicitly says `Daya baterai`: IMPLEMENTED — requires branch CI and UI review.
- [x] Regression tests cover updated detector labels and explanation guidance: IMPLEMENTED — requires branch CI.
- [x] No battery-side power formula, detector thresholds, protocol evidence rules, service lifecycle, persistence, Room schema, dependency, SDK, package, or app version changes: PASS by scoped diff review.
- [ ] Feature branch unit tests and debug build: NOT RUN at checklist update time.
- [ ] Anti-Slop UI Audit Gate: NOT RUN at checklist update time.
- [ ] Physical-device QA on itel P55: NOT RUN — required after CI passes.
- [ ] PR review and merge: NOT STARTED.

## 2026-11-23 — Accessory Benchmark in History (parallel feature branch)

- [x] Feature branch feat/accessory-benchmark-history created from exact main baseline 77c8de31bf700e47b00717b85fb398070f2e7f5d: PASS — branch creation used that commit SHA directly.
- [x] PR #5 isolation: PASS — fix/charging-source-capability-labels and its head 39563ea7b85731c03292dae698d430490bc4e428 were not modified; PR #5 remains OPEN and unmerged.
- [x] Room schema version 2 → 3 uses explicit additive MIGRATION_2_3 with ALTER TABLE charging_sessions ADD COLUMN accessory_label TEXT DEFAULT NULL: PASS — source review.
- [x] Migration chain 1 → 2 → 3 is registered; the migration regression test now opens a v1 database and verifies preserved session/sample rows plus a null accessory label after migration: PASS — test source added; execution pending CI.
- [x] DAO supports label updates, labeled-session query, and completed labeled-session/sample relation query: PASS — source review.
- [x] No fallbackToDestructiveMigration() added: PASS — repository source search.
- [x] Recovery checkpoint, sample flush transaction, and atomic session-finalization implementations were not edited: PASS — changed-file scope review.
- [x] Pure Kotlin domain/benchmark calculator groups trimmed/case-insensitive labels and computes per-session averages from measured battery-side data; missing metrics remain null: PASS — source review.
- [x] Current average uses the persisted session aggregate only when hasCurrentData is true; temperature rise uses first available in-session temperature and recorded session peak; power uses available persisted battery-side samples and excludes samples explicitly marked not charging: PASS — source review.
- [x] Charge-rate calculation requires a completed session lasting at least 5 minutes; comparability notes use group mean start SOC (>15 percentage points) and mean start temperature (>4°C): PASS — source review.
- [x] Unit tests added for aggregation, duration boundary, unavailable vs measured-zero values, incomplete sessions, label normalization, comparability warnings, charging-state filtering, DAO label persistence/relation, and Room migration: PASS — test source added; execution pending CI.
- [x] History detail includes optional label add/edit/remove, four quick choices, and a free-text field with a heading above it and no placeholder: PASS — source review.
- [x] Comparison card is conditional on at least two completed, distinct labels and uses vertically stacked metric pairs to avoid horizontal scrolling: PASS — source review; 360dp / 720p device review pending.
- [x] applicationId (com.superdenot.superdguard), package namespace, dependencies, SDK, app version, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, and charging-protocol classification were not changed: PASS — scope review.
- [x] GitHub Actions Run #506 on PR head 5dcd3a4f1bcfde1d4d5b8af2fcf98a6096ac9cbd: SUCCESS — testDebugUnitTest, assembleDebug, Anti-Slop UI Audit Gate, APK upload, and unit-test report upload all passed. Debug APK artifact: 14793940833; test report artifact: 14793138821.
- [ ] Physical-device QA on itel P55, including 360dp / 720p layout and Room upgrade from an existing install: NOT RUN — required before PR #6 merge.
- [ ] PR #6 merge: HOLD — do not merge before itel P55 physical QA.

## 2026-11-23 — PR #4 merged to main

- [x] PR #4 `feat/ui-polish-and-session-soh` -> `main`: PASS — squash merge completed; merge commit `94a814479fae8b803b7a27835686712b9408cbab`.
- [x] Launcher adaptive resources synchronized to the new 3D icon assets: PASS — source and APK audit; legacy Transsion scanner display is deferred for separate investigation.
- [x] Charging protocol explanation catalog/info action: PASS — implementation included in merged PR.
- [x] History micro-session filter: PASS — default ON, display-only, no Room data deletion or mutation.
- [x] Session-based SOH estimator: PASS — valid completed sessions and existing design-capacity setting; implementation covered by unit tests.
- [x] CI Run #497 on PR head `6326dc5489e2b5f558ac352127662b902af17135`: PASS — 214/214 unit tests, `assembleDebug`, Anti-Slop UI Audit Gate, APK and test-report uploads.
- [x] Physical-device QA on itel P55: PASS — user confirmed the physical test is complete. Detailed test observations were not supplied in this merge request.
- [x] Transsion security-scanner legacy icon investigation: DEFERRED — separate follow-up; does not block this merge.
- [ ] Post-merge CI on `main`: NOT RUN at the time this entry was written; verify the workflow triggered by the merge/documentation commits separately.
- [x] No changes to Room schema, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, RECOVERY_HOLD, package, SDK, dependencies, or app version in this scope.

## 2026-11-22 — Run #490 Robolectric SDK version-conflict correction

- Run #490 logs and XML artifact 14756126935 inspected: PASS — exact failure is `LocalDependencyResolver.validateFile` with `Path is not a file` for the API 34 jar; XML confirms 80 API 34 failures and 1 API 26 failure, 214 total tests / 81 failed / 0 skipped.
- Root cause identified: API 26/34/35 versions of the same Maven module were placed in one Gradle configuration; Gradle version conflict resolution retained only one module version. The Sync task's success did not guarantee all required jars were present.
- Pre-warm configuration corrected to three independent Gradle configurations, one per SDK version: PASS — source change committed.
- Pre-warm task now validates the exact API 26, API 34, and API 35 jar filenames after Sync: PASS — source change committed.
- Test assertions, test enablement, production behavior, app runtime dependencies, package, app version, and Android SDK: unchanged.
- New CI verification for testDebugUnitTest 214/214, assembleDebug, Anti-Slop gate, and APK upload: NOT RUN — pending workflow triggered by correction.
- Physical-device QA on itel P55: NOT RUN — required before PR #4 merge.
- PR #4 remains OPEN and unmerged; main untouched.


## 2026-11-22 — Initial UI polish and session SOH analytics

- [x] Branch created from exact main baseline c955aa2888e5b6fcd6e650ba2f2791b7e3692adc: PASS.
- [x] Both mipmap-anydpi-v26 adaptive launcher XML files reference the shared 3D foreground PNG: PASS — source inspection.
- [x] Obsolete green lightning vector removed; density-specific 3D launcher PNGs retained: PASS — resource tree review.
- [x] Protocol explanation catalog covers all fixed detector labels (including detection-disabled, unavailable, wireless, and explicit sysfs/HAL protocol outputs) with an honest fallback: PASS — source/test review.
- [x] Charging info action uses a 48dp touch target and keeps the protocol value in a separate wrapping row: PASS — source review; 360dp device review pending.
- [x] History micro-session filter defaults ON, uses saveable UI state, and applies the requested OR predicate (<60 seconds OR zero SoC delta with charged mAh <=0.5): PASS — source/test review.
- [x] Filter is applied to the displayed session list only; no Room delete/update/query/schema change: PASS — source review.
- [x] Session SOH estimator uses completed segments, delta SoC >=15%, duration >=60 seconds, valid positive charge, plausible capacity bounds, and median aggregation; separate disconnect segments are never combined to satisfy the threshold: PASS — source/test review.
- [x] Insights uses existing configurable design-capacity setting and 5000 mAh fallback; no duplicate Settings preference added: PASS — source review.
- [x] Existing domain/engine/BatteryHealthEstimator, BatteryDao health log path, and Dashboard snapshot behavior left unchanged: PASS — scope review.
- [x] No CSV/JSON export, dependency, SDK, app version, Room schema, or package change: PASS — scope review.
- [x] testDebugUnitTest: SUCCESS — 214 tests, 0 failures/errors/skipped; GitHub Actions Run #449.
- [x] assembleDebug: SUCCESS — GitHub Actions Run #449.
- [x] Anti-Slop UI Audit Gate: SUCCESS — GitHub Actions Runs #449 and #450.
- [x] Debug APK upload: SUCCESS — artifact 14720034341, Run #449.
- [x] Unit-test report upload: SUCCESS — artifact 14720158754, Run #449.
- [x] PR workflow: SUCCESS — Run #450 on the same code SHA 6eb97cf6ac9311055494c0c36ebe96bfb68eb3ed.
- [ ] Physical-device QA at 360dp / 720p (Charging dialog and History toggle/list): NOT RUN — required before merge.
- [x] PR #4 remains OPEN and unmerged; main remains at c955aa2888e5b6fcd6e650ba2f2791b7e3692adc.
- [x] No merge performed; physical QA is still a hard prerequisite.

---

## 2026-11-21 — P0 Unisoc detection and protocol label clipping follow-up

- [x] Read-only allowlist includes battery/charge_type, sprd-charger/type, sc27xx-fgu/type, and USB usb_type/real_type/type: PASS — source review.
- [x] Generic HAL values Fast / Adaptive / FASTCHARGE map only to "Fast Charge (HAL)"; no PD/QC/VOOC identity is inferred: PASS — source review and unit tests added.
- [x] AC source capability uses >=8,500mV for the 9V+ capability label and >=10W from max-current × max-voltage metadata for high power: PASS — source review and unit tests added.
- [x] Battery-side measured current/power is not used as charger capability metadata: PASS — source review.
- [x] Charging tab value wraps to two lines; Dashboard protocol capsule has bounded width and two-line wrapping: PASS — source review; Anti-Slop and CI pending.
- [x] Unisoc allowlist, generic HAL labels, and 2A × 5V threshold regression tests added: PASS — test source review.
- [x] CI #415 Anti-Slop UI Audit Gate and assembleDebug: PASS; unit tests: FAIL only because one Wireless expected-string assertion still used the prior label (198/199 passed); XML report inspected and exact mismatch confirmed.
- [x] Corrected the Wireless assertion to "Nirkabel (Standar Tak Terverifikasi)".
- [x] CI #417 on commit 95ff2d4: PASS — 199/199 tests, assembleDebug, Anti-Slop UI Audit Gate, APK artifact 14711582078, report artifact 14711875931.
- [x] Specific protocol sysfs paths precede generic battery/charge_type to avoid masking; regression assertion added.
- [x] Final branch CI #419 on commit 9ceea4c: PASS — Anti-Slop UI Audit Gate, 199/199 unit tests (0 failures/errors/skipped), assembleDebug, APK artifact 14712121172, and unit-test report artifact 14711607719.
- [ ] Physical-device retest on itel P55 (720p/360dp clipping and Unisoc FASTCHARGE exposure): NOT RUN.
- [x] No Battery Monitoring lifecycle, HUD_ONLY, RECOVERY_HOLD, P1 protection, ChargingEtaEstimator, or CcCvStagnantWatchdog algorithm changes intended: PASS — change scope.
- [x] main untouched; no merge performed.

---

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
- HUD_ONLY isolation: PASS — protocol/sysfs detection is opt-in and service sampling enables it only in Battery Monitoring active states; indicator-only snapshots use the explicit “Deteksi protokol nonaktif” state.
- RECOVERY_HOLD, P1 protections, ChargingEtaEstimator, and CcCvStagnantWatchdog: PASS — no algorithm changes intended; confirm by final diff.
- Focused unit tests: PASS — CI #413, 197/197 tests, 0 failures/errors/skipped; report artifact ID 14710610822.
- Android build and Anti-Slop UI Audit Gate: PASS — CI #413 `assembleDebug` and Anti-Slop UI Audit Gate both SUCCESS.
- APK artifact upload: PASS — `android-debug-apk` artifact ID 14710545989; expires 2026-12-05T14:05:19Z.
- Unit-test report artifact: PASS — `android-unit-test-reports` artifact ID 14710610822.
- Physical-device QA on itel P55: NOT RUN — requires installation and observation; do not infer protocol availability from CI.
- Package, SDK, dependencies, app version, and build number: PASS — unchanged.

# SPG Quality Checklist

## 2026-11-24 — PR #11 follow-up: Charge Guard alert tone

- [x] Existing Charge Guard preference file reused; sound selection/title/vibration are persisted separately from Room and calibration.
- [x] Settings row and dialog are wired to persisted state; picker exposes system default and silent choices.
- [x] Dedicated notification channel ID is derived from effective sound URI + vibration setting; previous Charge Guard channel is deleted when replaced.
- [x] Invalid custom URI resolution falls back to system notification URI; pure fallback and channel-ID regression tests added.
- [x] Preview and Charge Guard notification posting use IO coroutines and do not execute in the sampling call stack.
- [x] Source review confirms no intended changes to Room v3, sensor acquisition/cadence, calibration default 1x, foreground monitoring channel, P0 lifecycle, or RECOVERY_HOLD.
- [x] `testDebugUnitTest`, `assembleDebug`, Anti-Slop UI Audit Gate, APK upload, and unit-test report upload: SUCCESS — CI Run #600 on `b152498c7b6a93b383c30cdd312fb484a8852ba4`.
- [x] Debug APK artifact ID: `14921189893`; unit-test report artifact ID: `14922038659`.
- [ ] Physical-device QA on itel P55: NOT RUN — required before PR #11 merge.
- [ ] PR #11 merge: HOLD until latest CI succeeds and physical-device QA is confirmed.

---

## 2026-11-22 — Robolectric runtime SDK resolution in CI

- Run #476 XML report inspected: PASS — all 80 reported failures were caused by Robolectric `MavenArtifactFetcher` failing to fetch the API 34 instrumented SDK jar with HTTP 429 from `repo1.maven.org`; this was a shared test-environment failure, not 80 distinct assertion mismatches.
- Robolectric 4.14.1 SDK mapping checked against its pinned `DefaultSdkProvider`: PASS — API 34 uses `14-robolectric-10818077-i7`; API 26 uses `8.0.0_r4-robolectric-r1-i7`; API 35 uses `15-robolectric-12650502-i7`.
- Gradle-resolved pre-warm configuration now covers API 26, 34, and 35: PASS — source/configuration review; the first attempt omitted default SDK 35 and was corrected after Run #484.
- `prepareRobolectricSdk` stages artifacts locally and supplies `robolectric.dependency.dir` to test workers: PASS — source/configuration review; CI execution pending.
- Existing `testImplementation` dependencies and Robolectric version retained; no app runtime dependency added: PASS.
- Test assertions, production Kotlin behavior, service lifecycle, persistence, and diagnostic/business logic unchanged: PASS — diff scope.
- CI workflow pre-warms Robolectric artifacts before `testDebugUnitTest`; setup-gradle dependency cache retained: PASS — source review.
- `testDebugUnitTest` target 214/214: NOT RUN — pending CI on the corrected API 26/34/35 configuration. Run #484 failed before test execution because the initial local SDK directory lacked API 35.
- `assembleDebug` on Run #484: completed before the unit-test task; APK upload SKIPPED because the combined command failed. Anti-Slop UI Audit Gate SUCCESS; unit-test report upload SUCCESS (artifact 14755513728). Full verification of the corrected configuration: NOT RUN — pending next CI.
- Physical-device QA on itel P55: NOT RUN — still required for the overall PR #4 before merge.
- PR #4 remains OPEN and unmerged; `main` untouched.


### Run #484 follow-up
- XML report artifact 14755513728 inspected: FAIL confirmed as missing local API 35 jar, not an application assertion mismatch.
- Root cause: `AutoStartDiagnosticLogTest` has no explicit SDK in its Robolectric `@Config`; Robolectric 4.14.1 selects API 35 by default.
- Added API 35 pre-instrumented jar coordinate to the Gradle pre-warm configuration. Test assertions and application source remain unchanged.
- Next CI run for API 26/34/35 coverage: NOT RUN — pending.


## 2026-11-22 — Launcher resource tree cleanup

- All 10 density launcher PNGs plus the 432×432 xxxhdpi adaptive foreground are present under `app/src/main/res/mipmap-*`: PASS — GitHub path checks.
- Temporary `ASSET_NOTES.json` and `preview.jpg` removed from repository root and `app/src/main/res/`: PASS — cleanup commits.
- No runtime Kotlin/manifest/Gradle behavior changed: PASS — documentation and resource-file cleanup only.
- Unit tests / Android build / Anti-Slop UI Audit Gate: NOT RUN — new CI run pending.
- Physical-device launcher rendering on itel P55: NOT RUN — required before merge.
- `CHANGELOG.md` and this checklist updated; no version, SDK, dependency, package, or main-branch change.


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
