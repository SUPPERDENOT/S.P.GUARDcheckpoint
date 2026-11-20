# ChatGPT Development Workflow — Superd Power Guard

Status: Project workflow for ChatGPT-assisted implementation  
Applies to: Work performed through the connected GitHub repository tools and available project files.

This document describes how ChatGPT must work on SPG when the user asks it to inspect, implement, debug, or document a change. It supplements `AGENTS.md`; it does not replace the project standards or authorize a broader scope.

## 1. Before touching files

1. Confirm the repository, target branch, current branch tip, and relevant commit history.
2. Read `AGENTS.md`, `docs/engineering/DEVELOPMENT_STANDARDS.md`, and `docs/engineering/QUALITY_CHECKLIST.md`.
3. Read `DESIGN.md` for any UI, UX, copy, theme, accessibility, or visual change.
4. Read `docs/engineering/ANTI_SLOP_POLICY.md` for UI/UX or quality-review work.
5. Read `docs/engineering/ANDROID_ENGINEERING_GUIDE.md` and the task-matched source skills listed in `ANDROID_SKILLS_INDEX.md` for Android work.
6. Inspect the actual implementation, tests, relevant workflow files, and current changelog. Never rely on filenames or an earlier summary when source inspection is available.
7. For reported bugs, establish expected versus observed behavior and trace the data/control path to the first verified divergence. Do not implement a guess.

## 2. Scope and plan

Before editing, establish:
- verified root cause or documentation gap;
- requested and explicitly approved behavior;
- files expected to change;
- regression-sensitive areas;
- tests and CI checks that can verify the change.

Keep the change narrow. Ask one focused clarification only when an essential product decision is missing. Do not silently add features, dependencies, settings, abstractions, or version/build changes.

## 3. Implementation through GitHub

- Work only on the user-approved development branch.
- Fetch the current file and its blob SHA immediately before updating it.
- Use create/update/delete operations only for the requested paths.
- For multiple sequential edits to the same file, use the latest returned blob SHA.
- Do not overwrite unrelated user work or force-update a branch.
- Do not commit, push, merge, or open a pull request targeting `main` without explicit user approval.
- Never change app version or build number without explicit approval.
- Keep documentation-only changes isolated from app source, build configuration, and runtime behavior.

## 4. Review and verification

After implementation:

1. Compare the branch against its pre-change base/commit and inspect every changed path.
2. Check for scope creep, contradictory documentation, broken links/paths, duplicated policy, and stale instructions.
3. Run available focused checks. For source changes, run relevant tests and Android build/CI.
4. Inspect GitHub Actions results for the exact commit. Report the run URL/ID and actual conclusion when available.
5. If a check cannot be run, mark it NOT RUN. Do not infer a pass from a successful file write or commit.
6. Do not claim physical-device validation unless the user explicitly reports or supplies evidence of that test.

### Existing CI and Anti-Slop gate

The repository already defines an `Anti-Slop UI Audit Gate` job in `.github/workflows/android-ci.yml`, which runs `bash scripts/ui-audit.sh`. It is paired with the Android build and unit-test job. Inspect and reuse this existing gate for applicable UI changes; do not add a second overlapping Anti-Slop workflow or duplicate script. A documentation-only change does not need to trigger UI or Android build checks unless the changed documentation itself alters CI behavior.

## 5. Changelog and quality checklist

- Update `CHANGELOG.md` for every meaningful SPG change, including documentation-policy changes.
- Preserve prior entries and dates; do not rewrite history to make an unverified change appear complete.
- Separate implementation status, automated CI/test status, and physical-device QA status.
- Complete `docs/engineering/QUALITY_CHECKLIST.md`. Use PASS, FAIL, N/A (with reason), or NOT RUN; never mark an unexecuted check PASS.

## 6. Final report

The delivery report must state:
- repository and branch;
- base and resulting commit SHA;
- files changed and purpose;
- concise summary of implementation;
- diff review result;
- tests/build/CI results with exact status;
- physical-device QA status;
- quality checklist PASS / FAIL / N/A / NOT RUN;
- remaining risks and follow-up items;
- explicit confirmation that no unapproved main-branch operation occurred.

A branch write is not proof of correctness. A green CI run is not proof of physical-device behavior.