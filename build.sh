#!/usr/bin/env bash
#
# build.sh — the single, stable entrypoint for building/testing the Guardian
#            Android SDK, usable IDENTICALLY on a developer laptop and in CI.
#
# ─────────────────────────────────────────────────────────────────────────────
# WHAT THIS IS (and just as importantly, what it is NOT)
# ─────────────────────────────────────────────────────────────────────────────
# This script is a *dispatcher*, not a build system. Its ONLY job is:
#     parse flags  ->  validate them  ->  map them to a Fastlane lane + env  ->  exec it
# No gradle logic lives here. All real build logic lives one layer down, in
# fastlane/Fastfile. That keeps this file tiny, readable, and stable.
#
# It is intentionally the SAME contract (same flags, same defaults, same skeleton)
# as GuardianApp.Android/build.sh, trimmed to what a LIBRARY needs. The App builds
# and distributes an app (APK/AAB → Firebase/BrowserStack/Play Store); the SDK
# only ever tests, lints, measures coverage, and assembles a release AAR. So the
# App's --aab/--install*/--publish*/--l10n flags are intentionally absent here.
#
#     Layer 3  GHA / CI           ── calls ─▶  ./build.sh <flags>
#     Layer 2  build.sh (here)   ── calls ─▶  bundle exec fastlane android <lane>
#     Layer 1  Fastlane lanes    ── call  ─▶  gradlew (test / lint / jacoco / assemble)
#
# This file is deliberately THIN: it just sources the phase modules under
# tools/build/ and runs them in order. The real logic lives in those modules:
#     tools/build/common.sh        colors + log/die helpers
#     tools/build/usage.sh         --help / --help-full / bad-input handling
#     tools/build/args.sh          defaults, flag parsing, plan resolution
#     tools/build/menu.sh          the zero-arg interactive menu (TTY only)
#     tools/build/dependencies.sh  bundle install (the toolchain-ready step)
#     tools/build/prepare.sh       plan display, --dryRun, env export
#     tools/build/prebuild.sh      hook before the lanes run        (placeholder)
#     tools/build/test.sh          test-orchestration hook          (placeholder)
#     tools/build/postbuild.sh     hook after the lanes run (reports dashboard)
#     tools/build/artifacts.sh     report/collect build outputs (the AAR)
#
# ─────────────────────────────────────────────────────────────────────────────
# USAGE
# ─────────────────────────────────────────────────────────────────────────────
#   ./build.sh --buildType debug|release \
#              [--build | --nobuild]          # assemble the release AAR (default: --build)
#              [--test | --notest]            # run unit tests (default: --test)
#              [--lint | --nolint]            # run Android Lint (default: --lint)
#              [--coverage | --nocoverage]    # emit JaCoCo report (default: --nocoverage)
#              [--branch <name>]              # informational label for logs/artifacts
#              [--skip-deps]                  # don't run 'bundle install' first (default: install)
#              [--dryRun]                     # print the resolved plan, run nothing
#
# DEPENDENCIES
#   By default the script makes the Ruby/Fastlane toolchain ready before building:
#   it installs bundler if missing, then runs 'bundle install' (skipped quickly if
#   the bundle is already satisfied). The SDK's Gemfile lives under fastlane/, so
#   BUNDLE_GEMFILE is exported to point there. This is what makes "clone then
#   ./build.sh" work on a fresh machine or CI runner. Pass --skip-deps to bypass it
#   (e.g. in CI where a cached, dedicated install step already ran). A JDK (see
#   .java-version) must already be on PATH — that's provided by CI/the local SDK.
#
# EXAMPLES
#   ./build.sh --buildType debug                       # PR-style: build + test + lint
#   ./build.sh --buildType debug --coverage --lint --nobuild   # what pr.yml runs
#   ./build.sh --buildType release --notest --nolint --nocoverage   # just the AAR
#   ./build.sh --buildType release --dryRun            # show what would run
#
set -euo pipefail

# ─── Locate ourselves + the module dir (works regardless of caller's cwd) ─────
ENTRYPOINT="${BASH_SOURCE[0]}"
REPO_ROOT="$(cd "$(dirname "$ENTRYPOINT")" && pwd)"
MODULES_DIR="$REPO_ROOT/tools/build"
# Gradle + fastlane resolve relative paths from the repo root — run from there so
# `gradlew`, guardian/, and fastlane/ resolve the same as before.
cd "$REPO_ROOT"

# ─── Constants specific to this repo (the only Android-specific knobs) ────────
readonly PLATFORM="android"

# Remember whether the user passed ANY arguments, before we parse them. With zero
# args on an interactive terminal we show a numbered menu; with zero args in CI
# (no TTY) we silently use the safe defaults. (Consumed by args.sh.)
ORIG_ARGC=$#

# ─── Load the phase modules (order matters: common -> usage -> the rest) ──────
# shellcheck source=tools/build/common.sh
source "$MODULES_DIR/common.sh"
# shellcheck source=tools/build/usage.sh
source "$MODULES_DIR/usage.sh"
# shellcheck source=tools/build/args.sh
source "$MODULES_DIR/args.sh"
# shellcheck source=tools/build/menu.sh
source "$MODULES_DIR/menu.sh"
# shellcheck source=tools/build/dependencies.sh
source "$MODULES_DIR/dependencies.sh"
# shellcheck source=tools/build/prepare.sh
source "$MODULES_DIR/prepare.sh"
# shellcheck source=tools/build/prebuild.sh
source "$MODULES_DIR/prebuild.sh"
# shellcheck source=tools/build/test.sh
source "$MODULES_DIR/test.sh"
# shellcheck source=tools/build/postbuild.sh
source "$MODULES_DIR/postbuild.sh"
# shellcheck source=tools/build/artifacts.sh
source "$MODULES_DIR/artifacts.sh"

# ─── Run one lane through Fastlane ────────────────────────────────────────────
run_lane() {
  local lane="$1"
  log "▶ bundle exec fastlane ${PLATFORM} ${lane}"
  # shellcheck disable=SC2086  # intentional: split "lane arg:val arg2:val2" into words
  bundle exec fastlane "$PLATFORM" $lane
}

# ─── Orchestrate the phases ───────────────────────────────────────────────────
main() {
  parse_args "$@"        # defaults + interactive menu + flag parsing
  resolve_plan           # validate + derive GRADLE_BUILD_TYPE, VARIANT, LANES[]
  save_last_run          # remember this plan so the menu can offer "repeat last" (TTY only)

  print_plan             # always show what will run

  if is_dry_run; then
    print_dry_run
    exit 0
  fi

  export_env             # GUARDIAN_* + BUNDLE_GEMFILE for the lanes/bundler
  ensure_dependencies    # bundle install (honors --skip-deps; uses BUNDLE_GEMFILE)
  run_prebuild           # placeholder hook

  local lane
  for lane in "${LANES[@]}"; do
    run_lane "$lane"
  done

  run_test_hooks         # placeholder hook
  run_postbuild          # reports dashboard hook
  report_artifacts       # tell the user where outputs landed

  log "✅ Done: ${LANES[*]}"
}

main "$@"