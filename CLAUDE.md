# Claude Instructions — Superd Power Guard

Read and follow the repository-wide instructions in `AGENTS.md` first. If any instruction here appears inconsistent, `AGENTS.md`, the approved task scope, and the project's existing architecture/design contracts take precedence.

Before implementation:
- Read `docs/engineering/DEVELOPMENT_STANDARDS.md` and `docs/engineering/QUALITY_CHECKLIST.md`.
- For UI work, read `DESIGN.md` and `docs/engineering/ANTI_SLOP_POLICY.md`.
- For Android implementation, read `docs/engineering/ANDROID_ENGINEERING_GUIDE.md` and the relevant upstream Android Skill reference.
- Inspect the existing code path before proposing or adding another component, state holder, repository, service, or setting.

During implementation:
- Explain the verified root cause and planned scope before broad changes.
- Keep diffs minimal and task-focused.
- Preserve established monitoring/service behavior unless the task explicitly authorizes changing it.
- Do not claim a protocol, fast-charge mode, sensor value, or diagnostic cause without reliable device evidence.
- Do not silently add dependencies, architecture patterns, toggles, or features.

Before delivery:
- Review the diff.
- Run relevant unit tests and Android build/CI checks available in the environment.
- Report any check that could not be run; never invent a passing result.
- Complete the quality checklist in the final report.
- Update `CHANGELOG.md`, preserving history and separating implementation status from CI/device testing.

Git safety:
- Do not commit, push, merge, or target `main` without explicit user approval.
- Do not alter version/build numbers without explicit approval.
