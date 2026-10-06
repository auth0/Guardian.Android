# shellcheck shell=bash
#
# tools/build/postbuild.sh — hook that runs just AFTER the lanes execute (success).
#
# Wired into build.sh's flow so post-build steps have an obvious, single home.
# For the SDK it does one real thing — generate the unified reports.html dashboard
# from whatever test/lint/coverage reports the run produced — plus room for future
# work. (The App's postbuild also does adb install/launch and a BrowserStack
# handoff; neither applies to a library, so both are omitted here.)
#
# Candidate future uses (SDK):
#   - upload the coverage/lint dashboard somewhere durable
#   - post a build summary/notification (Slack, PR comment)
#
# Contract: runs after ALL lanes complete successfully (a failing lane aborts the
# run before this via `set -e`). Has access to the resolved plan globals. See
# artifacts.sh for reporting the AAR specifically.

run_postbuild() {
  # Generate the unified test dashboard if any reports exist (test/lint/coverage).
  # Non-blocking: failures here don't abort the build.
  generate_reports_dashboard || true
}

# Generate unified test dashboard (reports.html) from test/lint/coverage results.
# Runs after all lanes complete, consolidates scattered reports into a single file.
# Non-blocking: always returns success so failures don't abort the build.
generate_reports_dashboard() {
  # Check if ANY report exists (at least one of test/coverage/lint ran)
  local has_reports=false

  # Check for test results (any buildType)
  [[ -d "$REPO_ROOT/guardian/build/test-results" ]] && has_reports=true

  # Check for coverage reports
  [[ -d "$REPO_ROOT/guardian/build/reports/jacoco" ]] && has_reports=true

  # Check for lint reports
  [[ -n "$(find "$REPO_ROOT/guardian/build/reports" -name "lint-results*.xml" -o -name "lint-results*.html" 2>/dev/null | head -1)" ]] && has_reports=true

  if [[ "$has_reports" != "true" ]]; then
    log "No test/lint/coverage reports found — skipping dashboard generation."
    return 0
  fi

  log "Generating unified test dashboard..."
  local script="$MODULES_DIR/reports.sh"
  if [[ ! -x "$script" ]]; then
    warn "Dashboard script not found or not executable: $script"
    warn "Run: chmod +x $script"
    return 0
  fi

  # Run script with error handling - never fail the build
  if "$script" 2>&1; then
    if [[ -f "$REPO_ROOT/guardian/build/reports/reports.html" ]]; then
      log "✅ Test dashboard generated: guardian/build/reports/reports.html"
    else
      warn "Dashboard script ran but guardian/build/reports/reports.html not found (non-fatal)."
    fi
  else
    warn "Failed to generate test dashboard (non-fatal, continuing)."
  fi

  return 0  # Always return success
}
