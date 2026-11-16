#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESIGN="$ROOT/DESIGN.md"
MAIN="$ROOT/app/src/main/java/com/superdenot/powerguard/MainActivity.kt"
THEME="$ROOT/app/src/main/java/com/superdenot/powerguard/ui/theme/Theme.kt"
SCREENS="$ROOT/app/src/main/java/com/superdenot/powerguard/ui/AppScreens.kt"
INFO="$ROOT/app/src/main/java/com/superdenot/powerguard/ui/dashboard/DashboardScreen.kt"

fail() { echo "UI AUDIT FAIL: $1" >&2; exit 1; }
pass() { echo "UI AUDIT PASS: $1"; }

[[ -s "$DESIGN" ]] || fail "DESIGN.md missing or empty"
grep -q "Industrial Instrument" "$DESIGN" || fail "Design direction not declared"
grep -q "Anti-Slop review gate" "$DESIGN" || fail "Anti-Slop checklist missing"
pass "Design contract exists"

for file in "$MAIN" "$THEME" "$SCREENS"; do
  [[ -s "$file" ]] || fail "Required UI source missing: $file"
done
pass "Primary UI source files exist"

if grep -Eiq 'FAST[ _-]?CHARGING|Label Fast Charging adalah estimasi' "$SCREENS" "$INFO"; then
  fail "Unsupported fast-charging claim remains in user-facing UI"
fi
pass "No stale fast-charging claim in primary screens"

if grep -Eq 'fontSize[[:space:]]*=[[:space:]]*9\.sp' "$MAIN"; then
  fail "Navigation label is below the minimum readable type target"
fi
pass "Navigation label minimum-size guard"

if grep -q '0xFF12161F\|0xFF1B2230' "$THEME"; then
  fail "Legacy dark palette remains in the active theme"
fi
pass "Legacy dark palette removed from active theme"

echo "UI AUDIT: all automated checks passed."
