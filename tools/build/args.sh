# shellcheck shell=bash
#
# tools/build/args.sh — defaults, interactive menu, flag parsing, and resolution.
#
# This is the "brains" of the dispatcher: it turns the command line (or the
# interactive menu) into a fully-resolved plan — a validated set of variables plus
# the ordered LANES[] array that execution will run. Defines two entry functions
# that build.sh calls in order:
#     parse_args "$@"   — set defaults, run the menu (if applicable), parse flags
#     resolve_plan      — validate + map flags to GRADLE_BUILD_TYPE + VARIANT and
#                         the ordered LANES[] array
# Sourced by build.sh after common.sh + usage.sh.
#
# SDK vs App: the flag surface is a SUBSET of GuardianApp.Android's build.sh. A
# library never bundles an .aab, installs onto a device, publishes to a store, or
# runs a localisation gate (the :guardian module ships no string resources), so
# --aab/--install*/--publishTarget/--l10n and the `automation` buildType are all
# intentionally absent. --publish maps to the publish_maven Fastlane lane (Maven
# Central staging via com.vanniktech.maven.publish). That lane is guarded by
# RELEASE_CONTEXT=true, which is ONLY set inside the `publish` job of release.yml.

# ─── Defaults (safe for a local run: build + test + lint, never publishes) ────
init_defaults() {
  BUILD_TYPE="debug"
  DO_BUILD=true          # assemble the release AAR (part of the CI build gate)
  DO_TEST=true
  DO_LINT=true
  DO_COVERAGE=false
  DO_PUBLISH=false       # publish to Maven Central staging — only via release.yml
  BRANCH=""              # informational only; CI does the actual checkout
  SKIP_DEPS=false        # by default, ensure gems are installed before building
  DRY_RUN=false
}

# The zero-arg interactive menu (maybe_interactive) lives in menu.sh. It is a
# preset layer over these same flags — see that module's header. Sourced after
# this file so parse_args below can call it.

# ─── Argument parsing ─────────────────────────────────────────────────────────
# A hand-rolled loop (not getopts) because we accept GNU-style long flags. Each
# case is a single, obvious mapping to one variable — deliberately boring.
# `need_value` guards flags that take an argument: it rejects a missing value or
# one that looks like the next flag (e.g. `--buildType --test`), with full usage.
need_value() { [[ -n "${2:-}" && "${2:0:1}" != "-" ]] || die_usage "$1 requires a value."; }

parse_args() {
  init_defaults

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --buildType)      need_value "$1" "${2:-}"; BUILD_TYPE="$2";     shift 2 ;;
      --build)          DO_BUILD=true;     shift ;;
      --nobuild)        DO_BUILD=false;    shift ;;
      --test)           DO_TEST=true;      shift ;;
      --notest)         DO_TEST=false;     shift ;;
      --lint)           DO_LINT=true;      shift ;;
      --nolint)         DO_LINT=false;     shift ;;
      --coverage)       DO_COVERAGE=true;  shift ;;
      --nocoverage)     DO_COVERAGE=false; shift ;;
      --publish)        DO_PUBLISH=true;   shift ;;
      --nopublish)      DO_PUBLISH=false;  shift ;;
      --branch)         need_value "$1" "${2:-}"; BRANCH="$2";         shift 2 ;;
      --skip-deps)      SKIP_DEPS=true;    shift ;;
      --dryRun)         DRY_RUN=true;      shift ;;
      -h|--help)        usage 0 ;;
      --help-full)      usage_full 0 ;;
      *)                die_usage "Unknown argument: $1" ;;
    esac
  done

  # No args on a terminal? Offer the numbered menu (sets flags above). No-op in CI.
  maybe_interactive
}

# ─── Resolve the plan ─────────────────────────────────────────────────────────
# Validates the parsed flags and derives everything execution needs: the Gradle
# buildType (VARIANT) and the ordered LANES[] array. Leaves the result in globals
# for build.sh to log/run.
resolve_plan() {
  # Validate the buildType. A library has only debug + release (no automation).
  case "$BUILD_TYPE" in
    debug|release) ;;
    *) die_usage "--buildType must be one of: debug | release (got '$BUILD_TYPE')" ;;
  esac

  # Map --buildType -> Gradle buildType name (lowercase, matching build.gradle)
  case "$BUILD_TYPE" in
    debug)   GRADLE_BUILD_TYPE="debug" ;;
    release) GRADLE_BUILD_TYPE="release" ;;
  esac

  # VARIANT is kept only for the human-readable plan output
  VARIANT="${GRADLE_BUILD_TYPE}"   # e.g. debug, release (display only)

  # Select the Fastlane lane sequence. A single run can chain concerns (lint, then
  # test/coverage, then build). Each concern is its own lane so every step is
  # independently reproducible locally. We build an ordered list of
  # "lane arg:val …" strings; execution runs them in order, failing fast.
  LANES=()

  [[ "$DO_LINT" == true ]] && LANES+=("lint")

  # buildType args for lanes that accept them (coverage, build)
  local bt="build_type:${GRADLE_BUILD_TYPE}"

  if [[ "$DO_TEST" == true && "$DO_COVERAGE" == true ]]; then
    # The coverage lane runs the unit tests WITH coverage instrumentation and emits
    # the report, so we don't build/run the test suite twice. NOTE (SDK): coverage
    # runs only :guardian's debug unit tests (jacocoTestReport → testDebugUnitTest),
    # which is narrower than the top-level `test` lane (which also compiles :app).
    LANES+=("coverage ${bt}")
  elif [[ "$DO_TEST" == true ]]; then
    LANES+=("test")
  elif [[ "$DO_COVERAGE" == true ]]; then
    LANES+=("coverage ${bt}")
  fi

  # The build step assembles the library's release AAR. --nobuild (e.g. the PR
  # gate, which only tests+lints) drops it.
  if [[ "$DO_BUILD" == true ]]; then
    LANES+=("build ${bt}")
  fi

  if [[ "$DO_PUBLISH" == true ]]; then
    LANES+=("publish_maven")
  fi

  # Guard against an empty plan (e.g. --nobuild --notest --nolint --nocoverage).
  if [[ ${#LANES[@]} -eq 0 ]]; then
    die_usage "Nothing to do: no build, test, lint, or coverage was requested."
  fi
}