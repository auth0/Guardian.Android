# shellcheck shell=bash
#
# tools/build/artifacts.sh — collect / report build outputs.
#
# The Guardian SDK is a library: `./gradlew :guardian:assembleRelease` writes the
# AAR under guardian/build/outputs/aar/ by convention (guardian-release.aar). This
# module's job is what happens to that output AFTER a run: today it reports where
# it landed; in future it can gather/rename/upload it (or the Maven artifact).
#
# Defines report_artifacts(), called by build.sh after a successful run (skipped
# on --dryRun). Sourced by build.sh after common.sh.

# Where Gradle drops the library AAR.
AAR_DIR="$REPO_ROOT/guardian/build/outputs/aar"

# Tell the user where the outputs are, if any were produced.
report_artifacts() {
  local found=false
  if [[ -d "$AAR_DIR" ]] && [[ -n "$(find "$AAR_DIR" -type f -name '*.aar' 2>/dev/null)" ]]; then
    found=true
    while IFS= read -r f; do
      log "  - ${f#"$REPO_ROOT"/}"
    done < <(find "$AAR_DIR" -type f -name '*.aar' 2>/dev/null | sort)
  fi
  # Producing no AAR is a legitimate outcome, not an error: the test, lint and
  # coverage lanes emit reports rather than artifacts (and `test` runs `clean`
  # first, which wipes any earlier build's outputs). This must stay an `if` rather
  # than `[[ ... ]] && log ...`, because that form returns 1 when $found is false
  # — and as the last command in the function that becomes its exit status, which
  # build.sh's `set -e` treats as a failure, aborting the run just before the final
  # "✅ Done" and turning a fully green test/lint run into a non-zero exit for CI.
  if [[ "$found" == true ]]; then
    log "Build outputs are under guardian/build/outputs/aar/."
  fi
}