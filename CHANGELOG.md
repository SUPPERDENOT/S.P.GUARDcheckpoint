## 2026-11-26 — PR #12 precision pass: Capsule contour, Wave motion, and Charging info badges

### Changed
- Rebuilt the Capsule interior as a concentric rounded-rectangle path: the 1.8dp outer stroke plus 1.5dp inner breathing inset determines the inner contour, with a 4dp minimum inner corner radius. The empty track, liquid fill, shimmer, beam, and fixed glass reflection share one inner clip path. The terminal remains a separate cap with a 2dp gap; the outline uses the active theme's `outline` token and 0.85 alpha in dark surfaces.
- Increased the Wave sinusoidal amplitude to 6.5dp and frequency to 1.6 cycles across the gauge. The rear layer uses a 115-degree phase offset; the front liquid uses a theme-derived vertical gradient and a 2dp meniscus.
- Replaced Wave Sweep's diagonal blade with a soft bottom-to-crest light ribbon and a pulsing meniscus. Replaced Segmen's horizontal strata with seven rising luminous bubbles and double ripple rings. Comet now uses a 100-degree fading orbital tail with a 3dp stroke and bright head. Statis retains a fixed sinusoidal surface and disables all moving effects for Busur, Kapsul, Blok, and Wave.
- Added a frame-clock fallback driven by `LaunchedEffect` and `withFrameNanos` when `ValueAnimator.areAnimatorsEnabled()` is false. The fallback is gated by the same animation eligibility rules and is not launched for Statis.
- Centralized popup surface tokens to `surfaceContainerHigh` and `outlineVariant`. Both Charging info affordances now share an 18dp theme-colored badge, 12dp icon, 24dp minimum click area, and 3dp title gap.

### Tests
- Added Capsule inner-contour/stroke geometry tests, Wave amplitude/frequency/phase/profile tests, all-model Statis gating tests, and frame-clock fallback gating tests.
- Added light/dark ColorScheme popup-token tests and info badge size/spacing token tests.
- Updated obsolete Wave profile assertions that previously expected diagonal Sweep and horizontal Segmen strata.

### Protected boundaries
- No Room DB v3/schema/migrations, sensor acquisition or sampling, calibration default 1x, foreground monitoring P0/lifecycle, RECOVERY_HOLD, applicationId/package, SDK, Gradle dependencies, app version, or build number changes are intended.

### Verification status
- Source implementation and regression tests are committed on `feat/theme-system-and-battery-gauges`; full diff review and exact-HEAD CI remain pending.
- Physical-device QA on itel P55: NOT RUN.
- PR #12 remains OPEN; no merge or auto-merge.

---

## 2026-11-26 — PR #12 final polish: Blok orientation, Capsule beam, Wave mode separation, Charging label

### Changed
- Blok's 24 radial segments now start at 90° (6 o'clock) and advance by positive 15° increments clockwise. The first 12 segments cover the left semicircle from bottom through 9 o'clock to top; Sweep, Segmen, and Comet heads use the same angle helper.
- Capsule Comet now renders a vertical, bottom-to-top energy beam with a narrow bright core, soft glow, and a concentrated flash along the liquid meniscus; the previous floating circular head was removed.
- Wave animation modes now have distinct render paths: Sweep adds a diagonal lower-left to upper-right light blade; Segmen adds pulsing horizontal luminous strata rising through the liquid; Comet orbits the glass perimeter with a 90° fading tail and 3dp stroke; Statis keeps the wave phase fixed and disables all moving effects.
- Charging source capability copy is shortened to "Kemampuan sumber". Both adjacent info affordances are 24dp circular clickable Boxes rather than 48dp IconButtons, retaining button semantics and descriptive content labels.

### Tests
- Added assertions for Blok's 90° start and clockwise left-side progression, Capsule vertical-beam path bounds, distinct feature flags for all four Wave modes, and the exact "Kemampuan sumber" label.
- Existing Wave fill, dual-layer, thermal/visibility animation gates, and gauge percentage tests remain.

### Protected boundaries
- No Room DB v3/schema/migrations, sensor acquisition/sampling, calibration default 1x, foreground monitoring channel/P0 lifecycle, RECOVERY_HOLD, applicationId, package, SDK, Gradle dependencies, app version, or build number changes.

### Verification
- Line-by-line diff review: PASS — six changed paths; no Room/database/migration, sensor/sampling, calibration, foreground service/lifecycle, RECOVERY_HOLD, Manifest/applicationId, Gradle/dependency, SDK, or version/build-number paths changed.
- Run #690 (workflow ID 45734904830) on source/test HEAD 8c150d546cbc3d83c4254250cc0729d52af3fe20: SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, APK upload, and unit-test report upload. Debug APK Artifact ID 15078965622 (SHA256 04280b125ad8247fbdcee70c879e2b60fc6c039068ccade0526501f33d1799f2); unit-test report Artifact ID 15078163363.
- This changelog/checklist evidence update is documentation-only. CI for the resulting exact documentation HEAD is tracked in the final delivery report to avoid another documentation commit after the final verification.
- Physical-device QA on itel P55: NOT RUN.
- PR #12 remains OPEN; no merge or auto-merge.

---

## 2026-11-25 — PR #12 final visual pass: four gauge models, live accent preview, solid info dialogs

### Changed
- Reworked the four Canvas gauge renderers: Busur is a clockwise C-arc from -225° through +45° with a gradient fill and soft endpoint halo; Kapsul has a separated terminal, glossy frame, vertical liquid gradient, and bottom-to-top Sweep/Comet motion; Blok uses 24 full-circle segments starting at -90° with clockwise sweep/comet tail shading; Wave uses two offset sinusoidal liquid layers, a bright 0.82-alpha front fill, crest highlight, glass rim reflections, and an animated outer orbit.
- Added `forceAnimate` (default false) to `BatteryGaugeView`; the live Theme & Gauge preview opts in so animation can be inspected while unplugged. Static animation selection and thermal/visibility guards remain respected.
- Reduced Canvas percentage outline from 1.8dp to 1.2dp while preserving the shared measured text layout and exact draw origin.
- Theme preview now receives an explicit copy of the full draft configuration, including Basic appearance mode and accent, so Terracotta / Emerald / Cobalt changes are reflected immediately.
- Moved both Charging information icons directly beside their headings. Protocol and battery-side explanatory popups now use opaque elevated surfaces with outline borders.
- Disabled Save uses the same onSurface text treatment as Cancel; enabled Save remains primary/onPrimary. Status Bar dialog keeps its footer outside the scrolling body, aligns 32dp interval presets with the custom interval field, and retains imePadding.
- Capacity dialog derives draft manual mode from actual edits, so opening Automatic at its existing capacity does not silently mark the form dirty or convert it to Manual.

### Tests
- Added regression tests for 24-block geometry and clockwise ordering, Capsule bottom-to-top phase mapping, Wave fill while charging/idle, dual-wave layer separation and front alpha, forced preview animation, 1.2dp outline, live Basic draft preview propagation, and Automatic/Manual capacity dirty-state.
- Existing Status Bar tests continue to cover preset recognition and dirty-state changes across mode, parameters, scale, and interval.

### Protected boundaries
- No Room DB v3/schema/migration, existing sensor acquisition or sampling, calibration default 1x, foreground monitoring channel/P0 lifecycle, RECOVERY_HOLD, applicationId, Gradle dependencies, SDK, app version, or build number changes intended.
- Changes are limited to Compose Canvas rendering, theme preview state, UI dialogs, and focused unit tests.

### Verification
- Line-by-line self-crosscheck: PASS — 20/20 requirement checks; 14 changed paths in the delta from `551f258b11c981fece8ca45a4e8abed643496894`; no Room/database/migration, sensor/sampling, calibration, service/lifecycle, RECOVERY_HOLD, Manifest, Gradle/dependency, or applicationId path is in the diff.
- Run #683 (`45625532677`) on source HEAD `e3f61d1290ccb87c42da833c21be0cf4a876d465`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload. APK Artifact ID `15027787319` (SHA256 `53472faa500bf568a390808189f444281782354e61c21ffa9b9569dfd7cd5f24`); unit-test report Artifact ID `15028747474`.
- After Run #683, the Wave exact-boundary regression assertions and documentation were updated; production UI/source logic did not change. No workflow run was returned for the final test/documentation HEAD, so exact-HEAD CI verification remains pending.
- Physical-device QA on itel P55: NOT RUN; required to validate animation direction, rendering, contrast, popup opacity, and narrow-screen dialog behavior.
- PR #12 remains OPEN; no merge or auto-merge.

---

## 2026-11-25 — PR #12 combined hotfix: Wave, Canvas percentage text, History arrow, Basic Light, ETA, and dialog state

### Changed
- Wave liquid fill follows the clamped battery fraction independently of animation eligibility; idle rendering keeps a subtle static contour with no artificial endpoint fill.
- Wave/Capsule percentage text uses one TextMeasurer layout and two Canvas drawText passes at the exact same origin: dark 1.8dp rounded outline, then pure-white ExtraBold fill.
- Theme & Gauge preview bounds the model label to remaining row width and ellipsizes it.
- History session percentages use the shared numeric formatter with U+2192; regression coverage rejects straight and curly apostrophe-n variants.
- Basic Light keeps the approved Warm Ivory / Paper / Mineral palette and uses stronger neutral outline tokens for surface/sub-card separation.
- BatteryMonitorEngine captures positive OS ETA values through computeChargeTimeRemaining() on API 28+. A single service resolver rejects OS estimates below 10 minutes when level is below 90%, replacing them with remaining design capacity divided by active battery-side current. Existing EMA remains fallback when OS/current evidence is unavailable. Overview, Charging, and foreground notification use the same resolved LiveBatteryState ETA.
- Capacity dirty-state compares (capacityMah, isManualMode); Gunakan Otomatis remains compact and disabled in automatic mode.
- Status Bar preset selection derives from the effective interval value; initial draft state is keyed to the saved setting and dirty-state compares effective values.

### Tests
- Added Wave fill, Basic Light contrast, ETA sanity, interval preset, and foreground notification ETA regression tests.
- Expanded percentage outline, History arrow, and capacity value/mode dirty-state tests.

### Protected boundaries
- No Room DB v3/schema/migration, existing voltage/current/temperature sensor reads or normalization, sampling cadence, calibration default 1x, foreground monitoring channel/P0 lifecycle, RECOVERY_HOLD, applicationId, package, SDK, dependencies, app version, or build-number changes intended. The new OS ETA read is separate from existing sensor readings.
- History remains numeric in persistence; no schema or stored-session rewrite.

### Verification
- Source diff review: PASS — 22 files changed; no Room entity/DAO/database/migration, manifest, Gradle, applicationId, sensor normalization/sampling cadence, calibration default, or service lifecycle/startup paths changed. Engine/service edits are limited to reading and resolving the OS ETA.
- Run #659 (45608228926): Anti-Slop UI Audit Gate SUCCESS; Kotlin compile initially failed because drawText was imported from the wrong Compose package. Corrected to androidx.compose.ui.text.drawText.
- Run #661 (45608819047), final source HEAD 48227f213e55ec41f530dcb9ba9e98545e4c3595: SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, Upload APK, and Upload unit-test reports all passed.
- Debug APK artifact ID 15018512787 (SHA256 dca36c1326bfb456529e21b6aaa1f8b14b685a5ad5ac761dff8e3a6841b31b7c); unit-test report artifact ID 15018722461.
- Run #662 (45609359837), final combined source/documentation HEAD fb3298c554010026ca428c7b64805997041a963a: SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, Upload APK, and Upload unit-test reports all passed.
- Latest debug APK artifact ID 15018364086 (SHA256 276fe5f3f54d8f0eb22fd29e69c9202d9825ca80e961db15760ed37494d0e929); unit-test report artifact ID 15019132838.
- Physical-device QA on itel P55: NOT RUN; required for Wave rendering, Basic Light contrast, ETA behavior, and dialog usability.
- PR #12 remains OPEN; no merge or auto-merge.

---

## 2026-11-25 — PR #12 follow-up: smoother button feedback and thinner gauge outline

### Changed
- Refined `powerButtonClickEffect`: press scale is 0.95 with `Spring.DampingRatioMediumBouncy` and `Spring.StiffnessMediumLow`; accent glow peaks at 0.20 using a 150ms press tween and 180ms release tween.
- Centralized the motion and close-delay tokens in `PressFeedback.kt`. Dialog-closing Save, Cancel, and close actions wait 90ms before invoking their callback so the press/glow feedback is not immediately removed with the dialog.
- Routed the named Settings actions through the shared action-button wrapper and applied the same interaction effect to Dashboard action buttons, Analytics actions, theme-package cards, and option pills. Existing action layouts are retained where they were already custom-sized.
- Reduced the Wave/Capsule percentage text outline from 3.5dp to 1.8dp with `StrokeJoin.Round`. The outline and white fill remain in the same centered Box and use the same label/font metrics; only the stroke width changes.

### Tests
- Added `InteractionVisualTokensTest` to lock the requested scale, glow peak, press/release durations, 90ms close delay, and 1.8dp gauge outline token.

### Protected boundaries
- No Room DB v3, sensor acquisition/sampling, calibration default 1x, foreground monitoring channel P0 or service lifecycle, RECOVERY_HOLD, or applicationId changes.
- No dependency, SDK, app version, build-number, or Gradle configuration changes.

### Verification
- Source diff review: PASS — 10 files changed (7 UI source files, 1 focused unit-test file, and 2 documentation files). No Room DB v3, sensor/sampling, calibration, foreground service/channel, RECOVERY_HOLD, manifest, Gradle, SDK, dependency, version, build-number, or applicationId paths changed.
- GitHub Actions Run #657 on implementation HEAD `d64893aaf6d269fd656775aef1b43e050376aaba`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload (Artifact ID `15014994471`), and unit-test report upload (Artifact ID `15014969066`). [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45601094481).
- Physical-device QA on itel P55: NOT RUN; verify animation feel and 1.8dp text contrast at 360dp / 720p before merge.
- PR #12 remains OPEN; no merge or auto-merge was performed.

---

## 2026-11-25 — PR #12 final polish: outlined gauge text, uniform controls, and dirty-state

### Changed
- Removed the opaque percentage badge surfaces from Wave and Capsule. Both now render a white ExtraBold percentage over a 3.5dp rounded black text outline using the centralized `GaugePercentageOutline` theme token.
- Separated the Capsule terminal cap from the body with a visible 2dp gap and removed the cramped positive (+) mark.
- Added the shared `powerButtonClickEffect`: 0.94 spring-bounce press scale and a 0.24 primary-accent outline flash, driven by the same `MutableInteractionSource` as the control.
- Standardized dialog action buttons and interactive choice pills to 36dp CircleShape. Save remains an OutlinedButton when unchanged (transparent fill, muted border/text) and becomes primary/onPrimary when the valid draft is dirty.
- Theme & Gauge dirty-state uses full `ThemePackConfig` equality, including gauge model, animation, accent/package mode, and Midnight dual-tint. Its footer is right-aligned with 10dp spacing and no equal-width button weights.
- Capacity Save is enabled only for a valid value different from the saved capacity; the automatic-mode action is enabled only for a manual override. The capacity dialog now has a scrollable body, sticky footer, and IME inset handling.
- Status Bar Indicator dirty-state compares the effective draft/saved mode, metric set, current scale, and interval. Its custom interval input has a 56dp minimum width and 32dp circular field; the dialog uses a scrollable body, sticky footer, and IME inset handling.
- Compactened the Info popup header/footer; its information icon continues to use the active theme primary. Standardized the Analysis tab's Hitung Sekarang action.
- History range formatting already uses U+2192; the existing regression test asserts one arrow and rejects the malformed `'n` sequence.

### Tests
- Added unit coverage for capacity dirty-state and automatic/manual action eligibility.
- Added unit coverage for Status Bar Indicator dirty-state across mode, metric set, scale, and interval.
- Expanded Theme & Gauge dirty-state coverage across model, animation, accent, and dual-tint settings.
- Retained the existing History range formatter regression test for the exact arrow output.

### Protected boundaries
- No Room DB v3, sensor acquisition/sampling, calibration default 1x, foreground monitoring channel P0 or service lifecycle, RECOVERY_HOLD, or applicationId changes intended.
- No dependency, SDK, app version, or build-number changes intended.

### Verification
- Source diff review: PASS — 15 files changed in this polish; only the requested UI/theme helpers, focused unit tests, and documentation changed. Room DB v3, sensor/sampling, calibration, foreground service/channel lifecycle, RECOVERY_HOLD, manifest, Gradle, SDK, dependency, version, and applicationId paths are untouched.
- GitHub Actions Run #655 on implementation HEAD `c5ec37d6474b47b28748b877ee4c04bc6af3ee6c`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload, and unit-test report upload. APK Artifact ID `15002199739`; unit-test report Artifact ID `15003148539`. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45573801106).
- Physical-device QA on itel P55: NOT RUN. The requested 36dp actions remain below the 48dp touch-target guidance in DESIGN.md and require thumb-use review.
- PR #12 remains OPEN; no merge or auto-merge was performed.

---

## 2026-11-25 — PR #12 final polish: Midnight, Wave, dialogs, and touch feedback

### Changed
- Renamed the Midnight package display label and summary prefix to Midnight while preserving existing internal configuration and preference keys.
- Added accent-specific Midnight canvas, surface, outline, and primary tokens for Ungu, Pink, Cyan, and Green using the exact approved hex values; all color literals remain in ui/theme/.
- Kept the four accent swatches in the theme package picker and bound them to the updated accent tokens.
- Wave now retains a subtle 2.5dp sinusoidal idle contour, including at 0% and 100%; charging continues to use its existing animated wave behavior. Wave and Capsule percentage badges use 8dp horizontal and 3dp vertical padding.
- Replaced the Info popup with a compact themed dialog and 36dp outlined close action. Unified the Dashboard and Settings design-capacity dialogs, with compact automatic-mode action, aligned 36dp Cancel/Save buttons, validation, and a Save gate that requires a valid changed value or mode.
- Status Bar Indicator Save now requires a valid unsaved draft. Added subtle animated press scaling to custom clickable cards and selectable pills; Material 3 buttons retain their built-in pressed/ripple feedback.
- History session range formatter already used Unicode U+2192; strengthened its regression test to assert exactly one arrow. The formatter is shared by every session row.

### Protected boundaries
- No Room DB v3, sensor acquisition/sampling, default calibration 1x, foreground monitoring channel P0 or service lifecycle, RECOVERY_HOLD, package/namespace, applicationId, SDK, dependencies, app version, or build number changes.

### Verification
- Source implementation and diff review: PASS — verified on baseline HEAD `e98c503d6eea46117a255be7734318d18055b1c1`.
- Baseline GitHub Actions Run #653 on `e98c503d6eea46117a255be7734318d18055b1c1`: SUCCESS — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK Artifact ID `15000545345`, and unit-test report Artifact ID `15000465571`.
- Physical-device visual/touch QA on itel P55: NOT RUN; 32dp choice controls and 36dp dialog actions remain below the project's 48dp touch-target guidance and require real-device review.

---

## 2026-11-25 — PR #12 polish: Midnight accent variants, gauge legibility, and Indonesian copy

### Changed
- Added Midnight Violet accent choices Ungu (default), Pink, Cyan, and Green. The selected accent is part of ThemePackConfig.MidnightViolet, safely parsed from its persisted key, restored through the existing theme SharedPreferences, applied to Material 3 primary colors, and included in the Settings summary.
- Added four uniform 32dp Warna tema choices to the Midnight configuration dialog. The selected draft accent is reflected by the existing live preview and is persisted only by Simpan.
- Improved Wave gauge legibility with a visible 1.5dp high-contrast circular outline, a readable surface track, two subtly offset charging-wave layers, amplitude damping near 0% and 100%, and a translucent percentage badge that keeps the value intact across the waterline.
- Improved Capsule gauge readability with a stronger body/terminal outline, a visible positive terminal, a filled track body, and a surface-backed percentage badge.
- Changed the Status Bar Indicator dialog's Batal action to a 36dp outlined circular button.
- History now states Menampilkan X dari Y sesi (terbaru); the DAO orders sessions by startTime DESC, and the filter chips remain visible separately.
- SOH copy now uses Taksiran SOH tersimpan · Kapasitas desain ... mAh (otomatis/manual) without mixed Snapshot or otomatis/default wording.

### Protected boundaries
- No Room DB v3, sensor acquisition/sampling, calibration default 1x, foreground monitoring channel/P0 lifecycle, RECOVERY_HOLD, or applicationId change.
- New accent color literals are centralized in ui/theme/; no new explicit color literal was added to screen/component files.
- Existing SharedPreferences file and keys remain backward-compatible; a missing/unknown Midnight accent falls back to Violet.

### Verification
- Added tests for all MidnightAccent keys, null/unknown fallback, Pink/Cyan/Green persistence round trips, and all four Midnight summary labels.
- Source diff review: PASS — 11 files changed; only theme, preference, gauge, Settings/History copy, focused tests, and documentation were touched. No Room schema/entity/migration, sensor/sampling, calibration, service/receiver, manifest, Gradle, SDK, dependency, version, or applicationId files changed.
- GitHub Actions Run #648 on initial polish commit `b223bd65fd462e8f718de6d4b89d8b3933185a39`: Anti-Slop UI Audit Gate SUCCESS; Kotlin compile failed because the new accent contrast branch lacked the `Color.luminance` import. Corrected in the next commit.
- Run #649 on `7651deac9af03a02282c74ec68270d0a619e1f50`: Kotlin compilation and `assembleDebug` succeeded; one summary unit test failed because the expected gauge label used `Arc` instead of the app's actual label `Busur`. Corrected in the next commit.
- Final implementation Run #650 on HEAD `0c2d87d0e473db59e1d66a6b9cd3ccb7d904e1b1`: SUCCESS — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload, and unit-test report upload. APK artifact `14989161654`; unit-test report artifact `14988424665`. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45540584616).
- Physical-device QA on itel P55: NOT RUN; verify all four accent previews and saved values, Wave/Capsule contrast at low/mid/full levels, and 36dp footer usability before merge.
- Compact 32dp accent pills and 36dp dialog buttons remain below the 48dp touch-target guidance in DESIGN.md; retain as explicit physical-usability review items.

---
## 2026-11-25 — PR #12: two-layer theme packs and four battery gauge models

### Added
- Added the extensible `ThemePackConfig` sealed interface with Basic (SYSTEM/LIGHT/DARK and TERRACOTTA/EMERALD/COBALT accents) and Midnight Violet packages. SYSTEM mode resolves through Compose `isSystemInDarkTheme()`.
- Added the Midnight Violet deep-indigo palette and optional cold/warm metric sub-card tints on the Ringkasan summary card.
- Centralized explicit `Color(0xFF...)` definitions in `ui/theme/Theme.kt`; the UI audit now rejects explicit color literals elsewhere under app source.
- Added `LocalBatterySemanticColors` with strict priority: critical thermal red, warm amber, charging green, then theme accent. Warm/high thresholds reuse the existing thermal analyzer constants (38°C / 40°C); no new sensor or diagnostic threshold was introduced.
- Added dedicated `spg_theme_preferences` SharedPreferences persistence for theme package, Basic mode/accent, dual-tint option, gauge model, and charging animation. Unknown string keys fall back safely; no `Enum.valueOf()` parsing is used.
- Exposed the persisted configuration through `PowerGuardViewModel.themePackConfig: StateFlow<ThemePackConfig>` and applied it at the application Compose root.
- Added ARC, CAPSULE, SEGMENTED (20 × 5% segments with 3.5° gaps), and WAVE gauge renderers. Charging animations are composed only while the gauge is charging, visible, and not in warm/critical thermal states; STATIC never creates an infinite transition.
- Added the two-layer Settings picker with package swatches, draft-only live gauge preview, model/animation choices, package-specific controls, and a sticky footer.
- Follow-up source review: enforce the thermal animation gate inside the reusable gauge (including previews), draw the capsule's positive terminal marker, honor the disabled Midnight Violet dual-tint option, and reuse shared thermal thresholds in the preview. Capsule text contrast now follows fill luminance; idle gauges render without charging-motion overlays. Animation phase state is consumed in draw scopes rather than recomposing the gauge each frame. Midnight Violet dual tint differentiates electrical metrics from the temperature sub-card.

### Protected boundaries
- No Room DB v3 entity, schema, or migration change.
- No sensor acquisition, sensor interpretation, sampling cadence, charging calculation, calibration default 1x, foreground monitoring channel/P0 lifecycle, or RECOVERY_HOLD change intended.
- `applicationId`, package namespace, SDK, dependencies, app version, and build number are unchanged.

### Verification status
- Added unit tests for safe preference parsing/fallback and round-trip persistence, semantic color priority, 20-segment level boundaries, and the two-line Settings summary formatter.
- Source diff review: PASS — 15 changed files; no Room schema/entity/migration, sensor acquisition/sampling, calibration, service lifecycle, manifest, Gradle, dependency, SDK, version, or applicationId files changed.
- GitHub Actions Run #646 on final implementation HEAD `14261dba3660d1ee24a8a61dee16a02b6748fa71`: SUCCESS — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload, and unit-test report upload. APK artifact `14955470895`; unit-test report artifact `14955331593`.
- Intermediate Run #644 failed Kotlin compilation during the draw-phase refactor (`State<Float>` phase handling); corrected before Run #645. Run #645 passed, and final implementation Run #646 passed.
- Physical-device QA on itel P55 (360dp / 720p): NOT RUN. Verify SYSTEM theme reactivity, light/dark contrast, all four gauge models, thermal color priority, charging animation gating, dialog scrolling/footer, and Midnight Violet metric tints.
- Compact 32dp choice pills and 36dp footer buttons are below DESIGN.md's 48dp minimum touch-target guidance; this explicit compact-design exception requires thumb-use review on itel P55.

---

## 2026-11-24 — PR #11 final polish: sticky Charge Guard popup and bounded alert repetition

### Changed
- Split the Charge Guard dialog into a fixed compact header, a scrollable middle body, and a sticky compact footer. The tone picker and draft preview button now share one row.
- Replaced the footer's plain text actions with 36dp outlined Batal and state-aware Simpan buttons. Simpan is disabled until the draft configuration or tone/vibration settings differ from persisted values.
- Removed the quick Tes action from the outer Settings card; the card now presents the two-line summary and chevron only.
- Added Sekali (default), Tiap 1 mnt, and Tiap 3 mnt repeat modes to the dedicated Charge Guard SharedPreferences configuration. The summary adds Ulang 1m / Ulang 3m when selected.
- Added per-incident repeat state for charge-limit and high-temperature alerts. The initial alert may be followed by at most five repeats at the selected interval. Repeats are emitted from the existing monitoring sample loop; no alarm scheduler or separate service was introduced.
- Charge repeats stop when the charge condition becomes inactive or the charger disconnect receiver clears the active repeat state. Temperature repeats stop when the charger disconnects, the temperature sensor is unavailable, or temperature falls strictly below threshold minus 2°C. A reading exactly at the hysteresis boundary does not re-arm.
- Added a lightweight non-exported ChargeGuardAlertDismissReceiver through each alert notification's deleteIntent. Swiping the notification marks only that incident as dismissed; it does not start, stop, or otherwise alter the foreground monitoring lifecycle.
- Kept the foreground monitoring channel, P0 service lifecycle, Room DB v3, sensor sampling/acquisition, calibration default 1x, RECOVERY_HOLD, and applicationId unchanged by intent.

### Verification and limitations
- Added unit tests for default/persisted repeat modes, one- and three-minute intervals, Sekali, the five-repeat cap, dismiss suppression, unplug reset, strict temperature hysteresis stop, and Settings summary text.
- GitHub Actions Run #634 on implementation HEAD ec3099afd53d8d2d9f465104de44e89e43852e54: SUCCESS — Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, debug APK upload, and unit-test report upload. Debug APK artifact 14940768566; unit-test report artifact 14940269848. The earlier Run #632 exposed an invalid Compose padding overload; it was corrected before Run #633, and Run #634 validates the final implementation including the temperature repeat-cap test.
- Physical-device QA on itel P55: NOT RUN; verify notification deleteIntent delivery, repeat timing, unplug stop, and temperature hysteresis before merge.
- Repeat timing is sample-driven by the existing monitoring loop (approximately one-second evaluation cadence); notification dispatch can be delayed slightly by scheduler/service load.
- The requested 36dp footer buttons and 32dp choice pills are smaller than the 48dp touch-target guidance in DESIGN.md; this deliberate compact-layout deviation requires thumb-use review on itel P55.
- PR #11 remains OPEN and UNMERGED. Merge remains on hold until physical itel P55 QA is confirmed.

---

## 2026-11-24 — PR #11 follow-up: external audio fallback, custom vibration, and compact switches

### Fixed and added
- Replaced the four Material 3 Switch controls in Settings and the Charge Guard popup with the reusable CompactSwitch: 42×24dp track, 18dp thumb, animated 3dp inset travel, and 36dp interaction height.
- Added READ_MEDIA_AUDIO for API 33+ and READ_EXTERNAL_STORAGE capped at API 32. When an external MediaStore audio URI is selected, Settings requests the relevant runtime permission if it is not already granted; denying it leaves the prior draft tone unchanged.
- Preview playback validates custom URI readability through ContentResolver.openAssetFileDescriptor and rejects zero-length descriptors. SecurityException, FileNotFoundException, unreadable URI, missing Ringtone, or play exception advance to the system notification URI and then Android's notification default URI.
- Preview playback retains the active Ringtone, stops a previous preview before starting another, and schedules a four-second stop. Selecting Hening stops the active preview; Off cancels any previous vibration.
- Added Off, Singkat, Ganda (default), SOS, and Kustom vibration modes. Kustom records tap intervals as pulse/gap segments, clamps each segment to 50–1500ms, and caps the stored waveform at eight pulses.
- Charge Guard notification channel identity now includes the full vibration waveform; channel vibration uses the selected waveform and notification-event audio attributes.
- Settings Charge Guard summary now uses two ellipsized one-line rows. Every History session range label, including the unavailable end marker, uses SessionLevelRangeFormatter.

### Verification and limitations
- Regression tests added for built-in waveforms, custom timing sanitization/persistence, channel hash changes, inaccessible/missing external audio fallback, missing system ringtone fallback, and two-line Settings summary.
- GitHub Actions Run #620 on implementation commit 87ebc3b4d6641ccda84e8fafb3b2b9ce4b53f150: SUCCESS for Anti-Slop UI Audit Gate, testDebugUnitTest, assembleDebug, debug APK upload, and unit-test report upload. APK artifact 14937035355; unit-test report artifact 14936547835.
- Physical-device QA on itel P55: NOT RUN. Verify permission grant and denial, external tone playback, fallback behavior, all five vibration modes, and notification-channel behavior before merge.
- Android cannot guarantee audible output if the system notification volume is muted, Do Not Disturb suppresses sound, the notification channel is muted, or both system default ringtone providers return no playable tone. The implementation guarantees fallback attempts for URI/provider/playback failures; it cannot override system audio policy.
- CompactSwitch uses the explicitly requested 36dp interaction height. DESIGN.md specifies a 48dp minimum touch target; this is a documented accessibility trade-off requiring physical usability review.
- PowerGuardViewModel's existing preview method already delegates to ChargeGuardAlertSoundPlayer on Dispatchers.IO, so no ViewModel change was necessary. The player implementation was corrected without duplicating preview orchestration.
- Protected invariants remain unchanged by intent: Room DB v3, sensor acquisition/cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, RECOVERY_HOLD, and applicationId.
- PR #11 remains OPEN and UNMERGED; merge remains on hold until physical itel P55 QA is confirmed.

---

## 2026-11-24 — PR #11 follow-up: compact Charge Guard popup and 50°C threshold

### Changed
- Replaced the expanded inline Charge Guard form with a compact one-row `Peringatan & Nada` Settings card. Its single-line summary reflects enabled charge/temperature rules and the selected alert tone; quick actions provide `Tes` and a chevron affordance.
- Added a scrollable, IME-aware `Peringatan & Nada` popup matching the Status Bar Indicator dialog container and footer pattern. All threshold, enable/disable, ringtone, and vibration edits are draft-only until `Simpan`; `Batal` discards them.
- Added uniform 32dp option pills: five charge targets (80–100%) and four temperature thresholds (40/42/45/50°C). Ringtone selection uses the Android notification picker with default/silent choices and the current draft URI. The draft `Tes` action previews the draft tone and vibration without persisting it.
- Added a pure domain formatter for the compact Settings summary.
- Added 50°C to the supported Charge Guard temperature thresholds. Temperature re-arming now requires a reading strictly below threshold minus 2°C; for a 50°C threshold, 48°C does not re-arm and any lower reading does.
- Verified History session level range remains formatted with the Kotlin Unicode escape `\\u2192` (for example, `44% → 54%`); no malformed apostrophe-`n` sequence exists in `AnalyticsScreen.kt`.

### Protected behavior
- Room DB v3, sensor acquisition and sampling cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, `RECOVERY_HOLD`, and `applicationId com.superdenot.superdguard` remain unchanged.
- `BatteryMonitorService` continues to evaluate Charge Guard through the existing `ChargeGuardSettings.evaluate` active-sample hook; no service lifecycle or sensor-loop changes were required. The 50°C option is consumed through the existing normalized domain policy.

### Verification
- Added unit tests for the 50°C preference round-trip, alert trigger and strict below-48°C hysteresis, compact summary output, and History arrow output / malformed-text exclusion.
- CI Run #608 on branch HEAD `ba7110bfe41f8e77ac05c4e424ad91fc766a265c`: `Anti-Slop UI Audit Gate`, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all **SUCCESS**. Debug APK artifact `14933876095`; unit-test report `14933741143`. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45418505229).
- Physical-device QA on itel P55: **NOT RUN** — required before PR #11 merge.
- PR #11 merge: **HOLD** until physical-device QA is confirmed.

---

## 2026-11-24 — PR #11 follow-up: Charge Guard alert tone controls

### Changed
- Replaced the remaining Settings alert-tone placeholder with a compact `Nada peringatan` row inside the existing Charge Guard card. The selected tone and vibration state share one summary line; the adjacent `Tes` action previews the configured sound and vibration.
- Added a concise settings dialog with the Android notification ringtone picker (`TYPE_NOTIFICATION`), default and silent choices, custom local audio selection, and a vibration toggle.
- Persisted the selection marker / URI, display title, and vibration preference in the isolated `powerguard_charge_guard` SharedPreferences file.
- Added a dedicated immutable Android 8+ Charge Guard notification channel whose ID is derived from the effective sound URI and vibration configuration. Replaced channel IDs are deleted when the configuration changes; the foreground monitoring channel remains separate.
- Invalid or inaccessible custom audio URIs fall back to `Settings.System.DEFAULT_NOTIFICATION_URI` for both channel creation and preview. Preview and notification dispatch run on IO/service coroutines, outside the sensor sampling loop.

### Protected behavior
- Room DB v3, sensor acquisition and sampling cadence, calibration default 1x, P0 foreground monitoring channel/lifecycle, and `RECOVERY_HOLD` remain unchanged.
- `applicationId` remains `com.superdenot.superdguard`; no dependency, SDK, app-version, or build-configuration change.

### Verification
- Added tests for sound/vibration preference defaults and persistence, invalid custom-URI fallback, and stable/configuration-sensitive alert channel IDs.
- Source implementation and focused tests were included in PR CI Run #601 on head `747f12e98511e7d4ca1a11e5a1e070d3ca06f501`: Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload: **SUCCESS**. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45399600005)
- Debug APK artifact ID: `14920844863` (`android-debug-apk`); unit-test report artifact ID: `14921524240`.
- Documentation follow-up CI Run #602 on head `31ed906448fe584c6a9baed608afcb1ab61cb3b0`: all required jobs SUCCESS; latest debug APK artifact ID `14925414575`; unit-test report artifact ID `14926982019`. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45407641307).
- Physical-device QA on itel P55: **NOT RUN** — required before PR #11 merge.
- PR #11 merge: **HOLD** until physical-device QA is confirmed.

---

## 2026-11-24 — PR #11 (pending): data consistency, History polish, and Charge Guard alarms

### Changed
- Session level range now uses the explicit Kotlin Unicode escape `\u2192`; History duration labels are compact single-line values (`3j 30m`, `48m 21d`). The generic `Anomali tercatat` badge is suppressed when a specific anomaly badge is available.
- Selected History sessions use a soft selection fill and chart icon instead of an orange anomaly-like border. The expanded curve panel is placed above the History filter chips.
- Current-curve axis scaling ignores readings outside ±25,000 mA for display-scale calculation only; raw samples and Room data are not changed.
- Overview and Insights now map the same persisted SOH snapshot for identical estimated-capacity and SOH values.
- Seven-day trend labels use distinct Indonesian three-letter weekdays; tapping a day shows its cycle, total incoming mAh with Indonesian thousands separators, peak temperature, and anomaly count.
- The existing green/yellow/red thermal-duration ribbon is retained on Charging and curve sessions. The Status Bar Indicator dialog now applies IME padding while retaining its scrollable content.
- Added Charge Guard settings in a dedicated SharedPreferences file. Charge-limit targets: 80/85/90/95/100%; temperature thresholds: 40/42/45°C. Both toggles default OFF. Notifications are one per charge session/temperature incident, with 2°C temperature hysteresis and a 15-minute cooldown. This is notification-only; it does not electrically stop charging.

### Protected behavior
- Room remains version 3; no Room entity, schema, migration, or historical data change.
- Sensor acquisition and sampling cadence, calibration default 1x, P0 service lifecycle and RECOVERY_HOLD transitions remain unchanged. The service only evaluates alerts after the existing active sample path; RECOVERY_HOLD returns before this hook.
- applicationId remains `com.superdenot.superdguard`. No package, dependency, SDK, version, or build configuration change.

### Verification
- Added unit tests for explicit session-level arrow output, compact duration, current-axis outlier filtering, shared SOH snapshot mapping, daily weekday/summary data, generic anomaly suppression, alarm thresholds/hysteresis/cooldown, and fresh-install preference defaults.
- Final source diff review: PASS — 21 changed files; no Room schema/entity/migration, Gradle, manifest, package/version, sensor acquisition, sampling cadence, or calibration files changed. The service changes are limited to alert evaluation and notification dispatch after the existing active-sample state is built.
- GitHub Actions PR CI Run #591: SUCCESS on head `507fe09fc85e8fa1075375ffeedc4ba6ba963f99`: Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all succeeded. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45390892432)
- Debug APK artifact ID: `14917414475` (`android-debug-apk`); unit-test report artifact ID: `14919250772`.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #11 merge.
- PR #11 merge: HOLD until CI SUCCESS and physical-device QA confirmation.

---

## 2026-11-24 — PR #10: visual presentation, diagnostic summaries, and live status-bar preview

### Changed
- Replaced the duplicated page label in the 38dp brand header with a compact real-time service-state pill: Mengisi, Penuh, Aktif, or Siaga. The bottom navigation remains the active-tab indicator.
- Consolidated the Overview battery level and live readings into one hero card with a circular arc gauge, status-dependent color, four measured metrics, and ETA only while the estimator reports valid tracking data. Kept SOH and system-readiness cards directly below it.
- Added a segmented session-sample thermal duration band (Aman <38°C, Hangat 38–<40°C, Tinggi ≥40°C) and CC/CV-like pattern indicators to Charging and the curve analyzer. CC/CV is explicitly a battery-side pattern heuristic, not protocol detection; insufficient/ambiguous samples and samples separated by gaps over two minutes remain unknown.
- Standardized displayed voltage as V with the corresponding mV value, and moved the battery-side power explanation into a tap-accessible information dialog.
- Added a seven-day daily-cycle mini chart with high-temperature markers and Indonesian date labels (Hari Ini, Kemarin, or dd MMM yyyy).
- Removed the History floating column header, displayed localized mAh values inline, widened the level range, highlighted the selected curve session, omitted unavailable average-power text, and removed the legacy current × fixed-voltage watt estimate.
- Added average peak-temperature aggregation per accessory label, normalized +%/jam comparison bars, and automatic Tercepat / Cenderung Panas badges. Cenderung Panas uses the established 40°C threshold.
- Added a 32dp dark-backed Settings preview using the production LiveStatusBarRenderer; the bitmap follows draft single/custom mode, selected rotation parameters and interval, and draft current-scale adjustment. Condensed the Settings summary line and localized visible labels to Indonesian.

### Protected behavior
- Room database remains v3; no entity, schema, migration, or historical-data changes.
- Battery sensor acquisition, sampling cadence, calibration default 1x, P0 BatteryMonitorService lifecycle, RECOVERY_HOLD, and startup policy are unchanged.
- applicationId remains com.superdenot.superdguard. No package, dependency, SDK, app-version, or build-number changes.
- No export/copy-report feature was added.

### Verification
- Full diff review: PASS — 21 changed files reviewed against PR #10 base `4140b41313959087a1b97c4a823ff4f176376ed7`; no Room entity/schema/migration, BatteryMonitorService, sensor acquisition, sampling cadence, calibration default, or build-configuration files changed.
- Added tests for thermal-zone durations and CC/CV-like pattern mapping (including long-gap Unknown behavior), seven-day trend aggregation/date labels, accessory ranking/average peak temperature, and compact Settings summary formatting.
- Final GitHub Actions PR CI Run #577: SUCCESS on commit `ecbe968b71a4efb402211634c8b673eb46c80e7f` — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all succeeded. [Workflow run](https://github.com/SUPPERDENOT/S.P.GUARDcheckpoint/actions/runs/45360617867)
- Debug APK artifact: `14904452445` (`android-debug-apk`); unit-test report artifact: `14903734041`.
- Physical-device QA on itel P55 at 360dp / 720p: PASS — user confirmed the CI #577 APK on the target device.
- PR #10 squash-merged into `main`; merge commit `18d771a9fca0d51703b01f3c81d8a00c350febc5`. Main CI Run #578 and Dynamic Run #14: SUCCESS.

---

## 2026-11-24 — PR #9: compact five-tab header, History anomaly filters, and crisp rotation

### Changed
- Added a 38dp brand/page header across all five primary tabs with status-bar inset handling, 16dp horizontal padding, and a 0.5dp divider. Removed redundant page subheaders from Charging, Insights, History, and Settings; normalized first content inset to 8dp.
- Replaced the History micro-session Switch row with five horizontal filter chips: < 1 mnt (default), Semua, Anomali, Berlabel, and Kenaikan ≥10%.
- Added pure session anomaly evaluation for mAh-per-percent spikes, zero-percent mAh spikes, available temperature at/above 40°C, sessions lasting at least 120 seconds without SoC gain, checkpoint recovery provenance, and previously recorded engine anomalies.
- Added date grouping, compact metadata, accessory-label pills, soft anomaly badges, and a left anomaly accent.
- Stored checkpoint-recovery provenance in existing anomalyReason metadata during Room checkpoint reconciliation; Room remains v3 and the isAnomaly flag / RECOVERY_HOLD state machine are unchanged.
- Kept session-average power explicitly unavailable (— W): no session-average voltage/power pair is persisted, and current alone cannot establish watts. A closed session with a missing end-level remains unknown; the evaluator does not infer a zero-percent gain.
- Retained the 24dp ALPHA_8 renderer and bounded cache; accepted five-character values and added a light 8% alpha-coverage boost.
- Added a separate notification-icon refresh policy: 1,000ms for enabled custom rotation while interactive, 3,000ms otherwise. The existing 1,000ms monitoring/sampling loop is unchanged.

### Protected behavior
- Room remains version 3; no schema migration or historical rewrite.
- Sensor acquisition, session calculations, P0 service lifecycle, and RECOVERY_HOLD state transitions remain unchanged.
- Current calibration remains 1x by default; applicationId remains com.superdenot.superdguard.
- No package, dependency, SDK, app-version, or build-number change.

### Verification
- Implementation and focused tests added on `feat/slim-header-history-and-crisp-rotation`.
- Diff review: completed for the 19 changed files against the PR #9 base.
- GitHub Actions CI: **SUCCESS** — Run #557 (`45339264157`) on head `9e123240245999c6a972b3e141a4af20a1177ff9`; Anti-Slop UI Audit Gate PASS, 258 unit tests PASS (0 failures), `assembleDebug` PASS, APK and test-report uploads PASS.
- Debug APK artifact ID: **14895517683** (`android-debug-apk`).
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #9 merge.

---

## 2026-11-24 — PR #8 follow-up: uniform 32dp Status Bar dialog pills

### Changed
- Replaced both Material 3 segmented-button rows (display mode and current-scale calibration) with equal-width 32dp bordered selection pills, 6dp spacing, centered labels, matching selected container/border styling, and no check glyphs.
- Reduced all six parameter choices to 32dp-high, 8dp-rounded bordered pills. Labels are centered horizontally and vertically with zero inner padding and font padding disabled.
- Combined the four rotation presets and the free numeric interval into one 32dp-high row: four equal-weight preset pills followed by a 52dp-wide BasicTextField. The field uses numeric input, IME Done, no placeholder, and remains synchronized with preset selection.
- A manually edited valid 1–60 second value highlights the input border; tapping a preset restores the preset selection and updates the input value.

### Protected behavior
- UI-only change in `AppScreens.kt`. Local draft ownership, 1–60 validation, Simpan/Batal behavior, renderer, sensor acquisition, current calibration default 1x, Room DB v3, service lifecycle / `RECOVERY_HOLD`, and `applicationId` remain unchanged.

### Verification
- Source review: PASS — both segmented rows are removed; all requested selection pills are 32dp high; interval presets and numeric input occupy one row.
- GitHub Actions Run #547 (`45323370940`) on commit `cb80e97c359fe74d02362ffd52d0dd997c93e0be`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all passed.
- Debug APK artifact ID: `14888163325` (`android-debug-apk`; expires 2026-12-08). Unit-test report artifact ID: `14888802071`.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- Accessibility note: the requested 32dp visual controls are below the 48dp touch-target minimum in `DESIGN.md`; this explicit compact-size requirement is retained and must be assessed during physical QA.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-24 — PR #8 follow-up: compact parameter cells and custom interval input

### Changed
- Reduced the six Status Bar parameter grid pills to 36dp high, with 8dp horizontal / 4dp vertical inner padding and 6dp horizontal / vertical gaps.
- Replaced the custom interval OutlinedTextField with a centered, single-line BasicTextField inside a 56dp × 34dp Surface using an 8dp rounded shape and a thin outline/error border. No placeholder is shown inside the field.
- Tightened the dialog title/content insets to 16dp and the content section spacing to 8dp.

### Protected behavior
- UI-only change in `AppScreens.kt`. Draft ownership, 1–60 second validation, preset selection, Simpan/Batal behavior, renderer, sensor acquisition, calibration default 1x, Room DB v3, service lifecycle / `RECOVERY_HOLD`, and `applicationId` remain unchanged.

### Verification
- Source diff review: PASS — limited to the Status Bar settings dialog layout and this documentation/checklist.
- GitHub Actions Run #542 on source commit `12182dc41536afcbaba758aa66f926f5eefdb50d`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload.
- Debug APK artifact ID: `14886369907` (`android-debug-apk`; expires 2026-12-08). Unit-test report artifact ID: `14886509930`.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-24 — PR #8 follow-up: compact Settings and Status Bar controls

### Changed
- Replaced the full-width `Atur indikator` TextButton with a full-width Material 3 `OutlinedButton`, matching the neighboring Settings actions and adding a compact Tune icon.
- Removed the check icon and its reserved trailing space from the 2 × 3 Status Bar parameter grid. Selected state continues to use the selected container tone, stronger border, and selected text weight/color, allowing labels to use the full cell width.
- Reworked the custom interval row to place `Interval kustom (1–60 dtk)` on the left and a centered, placeholder-free 72dp × 48dp numeric field on the right.

### Protected behavior
- UI-only change in `AppScreens.kt`; draft, Simpan/Batal behavior, renderer, sensor acquisition, current calibration (default 1x), Room DB v3, service lifecycle / `RECOVERY_HOLD`, and `applicationId` remain unchanged.

### Verification
- Source diff review: PASS — only the requested Settings and popup UI composition plus this documentation/checklist were changed.
- Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: NOT RUN — pending CI on the latest commit.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-24 — PR #8 follow-up: combine current calibration with Status Bar settings

### Changed
- Moved the three current-scale choices (1x, 1000x, 0.001x) into the Status Bar Indicator dialog. The selected scale is now included in the Settings card subtitle.
- Added a single ViewModel save action for the dialog draft. Metric, display mode, rotation parameters, and interval are persisted together in one DataStore edit; current scale is then persisted through the existing `BatteryCurrentScaleSettings` SharedPreferences path.
- Kept SharedPreferences as the synchronous source read by `BatteryMonitorEngine`; default and fallback remain 1x. The explanatory note states that calibration applies globally to `CURRENT_NOW` and derived battery-side power (`V × I`).
- Batal and dialog dismissal discard all local draft values because no persistence setter is invoked until Simpan.
- Removed the Settings-only “Diagnostik Auto Start” card and its log-export/share action. `AutoStartDiagnosticLog` and its internal receiver/coordinator/service call sites remain untouched.

### Protected behavior
- Room schema remains version 3; no database migration or historical data rewrite.
- `BatteryMonitorService`, `RECOVERY_HOLD`, receiver/coordinator recovery behavior, and monitoring lifecycle were not changed.
- `applicationId` remains `com.superdenot.superdguard`; package namespace, dependencies, SDK, app version, and build number are unchanged.

### Verification
- Source review: PASS — changes are limited to Settings UI, combined settings persistence entry points, and focused persistence regression coverage.
- `BatteryCurrentScaleSettingsTest`: existing SharedPreferences tests retained; execution pending CI.
- `StatusBarIndicatorSettingsTest`: added coverage for combined metric/mode/rotation/interval persistence; execution pending CI.
- Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: NOT RUN — pending CI on the latest branch commit.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-24 — PR #8 follow-up: compact Status Bar settings dialog

### Changed
- Replaced the six vertically wrapping parameter chips with a fixed 2-column × 3-row grid using concise labels: Daya (W), Arus (A/mA), Tegangan (V), Suhu (°C), Level (%), and Sumber (AC/USB).
- Added a clear selected state for both single-metric and custom-rotation modes: contrasting outline, selected container tone, and a check icon. The grid uses 48dp minimum touch targets and selection semantics.
- Replaced the mode chips with a full-width Material 3 single-choice segmented control.
- Kept all four rotation presets in one equal-width row and placed the custom 1–60 second numeric field beside its label without a placeholder.
- Replaced the floating Selesai action with a divider-separated footer containing Batal and Simpan. Dialog edits remain local until Simpan; Batal and outside dismissal discard the draft. Invalid custom intervals block saving while custom rotation is selected.

### Protected behavior
- UI-only change in Settings. Status-bar renderer, sensor acquisition, global current calibration (default 1x), rotation timing, BatteryMonitorService lifecycle / RECOVERY_HOLD, Room DB v3, and `applicationId` are unchanged.
- No dependency, SDK, package, or app-version change.

### Verification
- Source diff review: PASS — only `AppScreens.kt` UI composition and this documentation are in scope.
- Android CI (Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload): pending for commit `bac5c77d8f4d9b6c698b0a8ab6d0968df087e04c`.
- Physical-device layout verification at 360dp / 720p on itel P55: NOT RUN — required before PR #8 merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-24 — PR #8 follow-up: resource-source glyph, custom rotation, and accessory-label cleanup

### Changed
- Expanded the two-line numeric metric glyph allocation to 58% value / 42% unit, with a 0.75dp row gap and an 88% canvas-width ceiling for the unit glyph.
- Restored the sixth selectable parameter, `Status sumber daya`. It uses only the `ACTION_BATTERY_CHANGED` `EXTRA_PLUGGED` metadata already read by BatteryMonitorEngine: AC, USB, WL, or BAT. If the extra is not exposed or the plug type is unsupported, the renderer displays an em dash rather than guessing.
- Added a full-height, full-width one-line bitmap path for the resource-source code while retaining the bounded bitmap cache.
- Added persistent Single metric / Custom rotation settings, a normalized list of at least two rotation parameters, and a 1–60 second interval. The selected metric is derived deterministically from SystemClock.elapsedRealtime() inside the existing notification update path; no alarm, timer, or additional service is created.
- Added group-label clearing and reset-all label queries. Only charging_sessions.accessory_label is set to NULL; session rows and battery_samples remain intact. History now requires confirmation before either operation.

### Protected behavior
- Room remains schema version 3; no migration, entity, session/sample deletion, or historical-data rewrite was added.
- Current calibration remains 1x by default. The CURRENT_NOW normalization path is unchanged.
- BatteryMonitorService actor lifecycle, monitoring cadence, RECOVERY_HOLD persistence/recovery, and power-event startup policy are unchanged; rotation only selects the displayed metric within the existing notification refresh loop.
- applicationId remains com.superdenot.superdguard. No dependency, SDK, package namespace, or app-version change.

### Verification
- Source changes and focused regression tests: implemented on `feat/crisp-statusbar-indicator`; CI pending.
- GitHub Actions Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, and APK upload: pending for the latest test/documentation follow-up commit.
- GitHub Actions Run #527 (run ID `45246964970`) on commit `2fa4ca65333c4bf7cb8b33e118e17625d069a41b`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload all passed.
- Debug APK artifact: `android-debug-apk`, ID `14853533390` (expires 2026-12-07); unit-test report artifact ID `14853163872`.
- Follow-up test assertions cover full-line bitmap creation for AC / USB / WL / BAT and notification-text mapping for the selectable resource-source metric. GitHub Actions Run #528 (run ID `45247445044`) on commit `717bd714ff75e05b8551df8996c8326629daea71`: **SUCCESS** for Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, APK upload, and unit-test report upload.
- Debug APK artifact ID `14854875799` (`android-debug-apk`); unit-test report artifact ID `14855115433`.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — required before PR #8 merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-23 — PR #8 follow-up: larger, tighter Status Bar glyphs

### Changed
- Rebuilt the dedicated small-icon bitmap at the Android 24dp target size so System UI does not scale down a 48dp drawing and make both text rows appear undersized.
- Replaced FontMetrics-based baseline centering with Paint.getTextBounds tight-glyph bounds. The numeric row is top-anchored at a 0.5dp inset; the unit row is bottom-anchored at a 0.5dp inset, with a 1dp target gap.
- Switched to bold sans-serif-condensed glyphs and fitted each row against a 95.8% full-bleed width (0.5dp side insets) and its approximate 2:1 vertical allocation.
- Limited numeric text accepted by the renderer to four characters. Negative decimal rounding is covered (for example, -2.48 → -2.5); values rounding across 10 become integers.

### Verification
- Source and test changes: implemented on `feat/crisp-statusbar-indicator`; sensor acquisition, current calibration, Room v3, `applicationId`, notification/service lifecycle, and monitoring behavior are untouched.
- GitHub Actions Run #521 (run ID `45165118889`) on commit `01d8c8886d22211b9c25e5c457bf9358648babd8`: SUCCESS — `testDebugUnitTest`, `assembleDebug`, Anti-Slop UI Audit Gate, debug APK upload, and unit-test report upload all passed.
- Latest debug APK artifact: `android-debug-apk`, ID `14812579421`; unit-test report artifact ID `14813028690`.
- Documentation follow-up commit `e49986f63afe8d9e6c9bac0bcc0a803bcc42251f` was revalidated by GitHub Actions Run #522 (run ID `45165674846`): SUCCESS — both CI jobs and all steps passed. APK artifact ID `14813983055`; unit-test report artifact ID `14813488060`.
- Physical-device QA on itel P55 at 360dp / 720p: NOT RUN — must be repeated with the latest APK before merge.
- PR #8 remains OPEN / HOLD MERGE.

## 2026-11-23 — Stacked Status Bar Indicator and global current-scale calibration (PR #8)

### Changed
- Replaced the status-bar small-icon single-line glyph with a borderless two-line bitmap: bold numeric reading above its unit, using 62% / 35% vertical regions. Font size remains fixed; horizontal scaling handles wide readings.
- Added adaptive precision (one decimal below absolute 10, rounded integer at 10 and above) and current-unit switching between mA and A.
- Expanded the bounded bitmap cache key to include metric, value text, unit text, and display density; cache synchronization and bounded eviction remain.
- Restricted Settings metric choices to battery-side power (W, default), current (A/mA), voltage (V), temperature (°C), and battery level (%). Legacy stored `CHARGING_STATUS` and unknown values resolve safely to W.
- Added persistent 1x / 1000x / 0.001x current-scale calibration. The multiplier applies globally to raw `BatteryManager.CURRENT_NOW` before µA-to-mA normalization; battery-side power uses calibrated current with a Double intermediate. Voltage, temperature, level, source-capability metadata, and protocol Tier 2 evidence are not scaled.
- Calibration defaults to 1x. Existing Room v3 records remain unchanged; new live samples and sessions receive calibrated current/power through the existing engine pipeline.

### Verification
- Branch base: `1427664eccc69bb3c0f701b9e0b238ffef20d63e`; branch: `feat/crisp-statusbar-indicator`.
- GitHub Actions Run #515 (run ID `45160871480`) on implementation commit `baad432e0f990a56f281f51ffaacca0caafd8543`: SUCCESS — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload, and unit-test report upload all passed.
- Physical-device verification on itel P55 at 360dp / 720p: NOT RUN; required before merge.
- PR #8 must remain OPEN / HOLD MERGE until physical-device QA is confirmed.

## 2026-11-23 — PR #7 follow-up: responsive Charging Curve header

### Changed
- Separated Charging Curve Analyzer into its own title line; placed the three legends in a wrapping FlowRow with 12dp horizontal and 4dp vertical spacing.
- Renamed the battery legend to Baterai (%) and preserved the complete current and temperature labels.
- Set the chart content inset explicitly to 10dp at the top and tightened the bottom inset to 6dp. Source inspection found no 45–50dp spacer in ChargingCurveChart; physical screenshot re-check is still required to confirm the reported blank region is resolved.

### Verification
- Source change: implemented on polish/history-curve-compact-focus; chart drawing, sample selection, sensor acquisition, charging calculations, and Room persistence are untouched.
- Unit tests, assembleDebug, Anti-Slop UI Audit Gate, and APK artifact upload: pending CI for this commit.
- Physical-device verification at 360dp / 720p on itel P55: NOT RUN for this follow-up. PR #7 remains OPEN and MUST NOT be merged until the user confirms screenshot re-verification.

## 2026-11-23 — PR #6 squash merge

- Squash-merged `feat/accessory-benchmark-history` (PR #6) into `main`; merge commit: `d9e489f0e9d7c9a61d3948a58a2f782a12186086`.
- Run #510 on the exact synchronized PR head `a50dd540445379b95cd2d2a4b4a2ca833cdb77af`: SUCCESS — Anti-Slop UI Audit Gate, unit tests, debug APK build, APK upload, and unit-test report upload completed successfully.
- Physical QA on itel P55 was confirmed complete by the user before merge. Detailed observations were not included in the merge request.
- Baseline for subsequent PR #7: `d9e489f0e9d7c9a61d3948a58a2f782a12186086`.

## 2026-11-23 — History curve compact-focus polish (PR #7)

### Changed
- Added a single-row compact control bar while the charging curve is expanded, showing the visible/total session count, a compact `<1m` filter chip, and a `Tutup` action. The full History heading and filter switch return when the curve is closed.
- Kept the curve outside the independently scrolling session `LazyColumn`, so it remains pinned while users scroll and select other sessions. The accessory comparison card is temporarily omitted during curve-focus mode and returns when the curve closes.
- Compacted the chart heading/legend, canvas height, and vertical spacing. Replaced the two-row selected-point readout with a four-field horizontal strip (time, current, temperature, battery level); peak/min current, maximum temperature, and sample count remain present.
- Replaced the fixed two-column accessory quick-choice rows with a wrapping `FlowRow`; free-text input remains placeholder-free.
- Added unit tests for compact session-count formatting and measured-zero versus unavailable curve readings.

### Scope and verification
- UI-only scope: no sensor acquisition, charging calculations, Room schema v3, DAO/repository, `AccessoryBenchmarkCalculator`, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, dependencies, SDK, applicationId, package namespace, or app version changes.
- Source implementation and tests: committed on `polish/history-curve-compact-focus`; Android CI and 360dp / 720p physical-device QA pending.
- Physical-device QA on itel P55: NOT RUN for PR #7; do not merge until user completes and confirms device QA.

## 2026-11-23 — Charging source-capability labels and battery-side power clarity

### Changed
- Renamed AC Tier 2 classifications so each label explicitly describes source capability: `AC — Kemampuan Sumber Standar`, `AC — Kemampuan Sumber Tinggi (≥10 W)`, `AC — Kemampuan Tegangan 9V+`, and `AC — Kemampuan Sumber Tidak Tersedia`.
- Kept the classification inputs unchanged: BatteryManager maximum charging-current / maximum charging-voltage metadata and the existing thresholds. Battery-side measured power is not used to classify source capability.
- Expanded the Charging protocol information dialog to distinguish source capability metadata from battery-side power (battery voltage × battery current), explain transient adjustment after connection, and recommend comparing stable readings under similar battery level, temperature, and device-load conditions.
- Renamed the Charging metric to `Daya baterai` and added a concise note that it is calculated from battery voltage × current.
- Updated the Overview charging summary to label the shared `chargingProtocol` value as `Protokol / kemampuan sumber`, matching the Charging tab instead of the ambiguous `Deteksi` wording. The existing dashboard capsule and monitoring notification already render the same shared value with no conflicting label.
- Removed the unused `app/src/main/res/drawable/ic_status_turbo_ring.xml` resource after repository-wide reference searches returned no code, XML, or test references; the PR diff does not introduce any reference to it.
- Added regression assertions for the standardized AC labels and the explanatory guidance.

### Protected behavior
- No protocol is inferred from actual wattage, voltage, or current. Explicit protocol names still require explicit sysfs/HAL evidence.
- No sensor acquisition, battery-side power calculation, charging thresholds, Battery Monitoring lifecycle, Status Bar Indicator lifecycle, persistence, Room schema, dependency, SDK, package, or app-version changes.

### Verification
- Main CI Run #500 on base `77c8de31bf700e47b00717b85fb398070f2e7f5d`: SUCCESS — Anti-Slop UI Audit Gate and Build and Unit Tests.
- Feature branch implementation and regression tests: pending CI.
- Physical-device verification on itel P55: pending; do not treat CI as device validation.

## 2026-11-23 — Accessory Benchmark for History

### Added
- Added optional accessory_label to charging_sessions and an explicit additive Room MIGRATION_2_3 (ALTER TABLE charging_sessions ADD COLUMN accessory_label TEXT DEFAULT NULL). The app database is now schema version 3 and registers both existing MIGRATION_1_2 and new MIGRATION_2_3.
- Added DAO operations to update an individual session label, observe labeled sessions, and observe completed labeled sessions with their measured samples.
- Added pure Kotlin domain/benchmark models and calculator. Sessions are grouped by trimmed, case-insensitive user labels; metrics are averaged per completed session.
- Average current uses the existing persisted session average only when hasCurrentData confirms it is measured. Average battery-side power uses persisted samples with isPowerAvailable; samples explicitly marked as not charging are excluded. Temperature rise is the first available in-session temperature to the recorded session peak. Missing metrics remain unavailable rather than becoming zero.
- Charge rate (%/jam) is calculated only for completed sessions lasting at least 5 minutes. Comparability notes are emitted when group mean starting battery levels differ by more than 15 percentage points or group mean starting temperatures differ by more than 4°C.
- Added History session-detail label controls (Tambah/Ubah/Hapus), quick choices (Charger Bawaan, Adaptor Cadangan, Kabel Utama, Kabel Cadangan), and free text with a visible field heading and no placeholder.
- Added a comparison card only when at least two distinct labels have completed sessions. The card presents session count, battery-side W, mA, %/jam, temperature rise, and comparability notes in a vertically responsive layout.
- Added calculator, DAO persistence/relation, and Room migration regression tests.

### Data integrity and scope
- Wattage is treated only as measured battery-side power. It is not used to identify charger brand, charging protocol, or source capability.
- No charger brand/protocol inference, sensor acquisition change, Battery Monitoring lifecycle change, Status Bar Indicator lifecycle change, RECOVERY_HOLD change, sample persistence algorithm change, or atomic session-finalization change was made.
- applicationId (com.superdenot.superdguard), package namespace, dependencies, SDK, and app version are unchanged.
- Work is isolated on feat/accessory-benchmark-history, created directly from main commit 77c8de31bf700e47b00717b85fb398070f2e7f5d. PR #5 branch was not touched and remains OPEN/unmerged.

### Verification status
- Source and diff review: PASS — 15-file diff reviewed; no changes to recovery checkpoint or atomic session-finalization paths.
- GitHub Actions Run #506 on PR head 5dcd3a4f1bcfde1d4d5b8af2fcf98a6096ac9cbd: SUCCESS — testDebugUnitTest and assembleDebug passed.
- Anti-Slop UI Audit Gate: SUCCESS. APK upload: SUCCESS — artifact 14793940833 (SHA-256 1708e30cea5f71cf998b826c88efbf0656d6e2c6737dc8d810c09b9d0a96717c). Unit-test report upload: SUCCESS — artifact 14793138821.
- Physical-device QA on itel P55 (including 360dp / 720p layout and upgrade migration): NOT RUN; required before merge.

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

## 2026-11-24 — PR #11 follow-up: Charge Guard alert tone and preview

### Implemented
- Added persisted Charge Guard alert sound selection (system default, silent, or selected audio URI), display title, and independent vibration preference in the existing `powerguard_charge_guard` SharedPreferences file.
- Added a Settings row with a one-line tone/vibration summary, non-blocking preview, compact picker dialog, and Android notification ringtone picker with default/silent options.
- Added a dedicated versioned Charge Guard notification channel. Its ID is derived from effective sound URI and vibration setting; the previous Charge Guard channel is deleted when configuration changes.
- Invalid or revoked custom audio URIs fall back to `Settings.System.DEFAULT_NOTIFICATION_URI` for preview and alert channel creation.
- Charge Guard channel resolution and notification posting run on the service IO scope, outside the sensor sampling call stack.
- No storage permission is requested. Added only Android's normal vibration permission.

### Protected scope and verification
- Room v3, sensor acquisition/cadence, calibration default 1x, foreground monitoring channel, P0 lifecycle, `RECOVERY_HOLD`, and `applicationId` are not intentionally changed.
- Unit tests added for sound preference defaults/persistence, invalid URI fallback, and channel ID stability/configuration changes.
- CI Run #600 on commit `b152498c7b6a93b383c30cdd312fb484a8852ba4`: **SUCCESS** — Anti-Slop UI Audit Gate, `testDebugUnitTest`, `assembleDebug`, debug APK upload, and unit-test report upload.
- Debug APK artifact ID: `14921189893` (`android-debug-apk`); unit-test report artifact ID: `14922038659`.
- Physical-device QA on itel P55: NOT RUN; required before merge.

---

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
