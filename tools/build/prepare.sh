# shellcheck shell=bash
#
# tools/build/prepare.sh — surface the resolved plan and export the run's env.
#
# Runs after args are resolved but before anything is built. Defines:
#     print_plan   — human-readable summary of what will run (always)
#     is_dry_run / print_dry_run — handle --dryRun (print commands, don't execute)
#     export_env   — export the GUARDIAN_*/BUNDLE_GEMFILE vars the lanes/bundler read
# Sourced by build.sh after args.sh.

# Human-readable summary of the fully-resolved plan (always printed).
print_plan() {
  log "Resolved build plan:"
  log "  platform      = ${PLATFORM}"
  log "  buildType     = ${BUILD_TYPE}  (gradle: ${GRADLE_BUILD_TYPE})"
  log "  variant       = ${VARIANT}"
  log "  build         = ${DO_BUILD}"
  log "  test          = ${DO_TEST}"
  log "  lint          = ${DO_LINT}"
  log "  coverage      = ${DO_COVERAGE}"
  log "  branch        = ${BRANCH:-<current>}"
  log "  install deps  = $([[ "$SKIP_DEPS" == true ]] && echo false || echo true)"
  log "  lanes         = ${LANES[*]}"
}

# True when --dryRun was passed.
is_dry_run() { [[ "$DRY_RUN" == true ]]; }

# Print the fastlane commands that WOULD run, then the caller exits.
print_dry_run() {
  log "--dryRun set: not executing. The commands that WOULD run:"
  local lane
  for lane in "${LANES[@]}"; do
    # shellcheck disable=SC2086  # word-splitting the "lane arg:val" string is intended
    printf '  bundle exec fastlane %s %s\n' "$PLATFORM" "$lane"
  done
}

# Export flags the Fastlane lanes may read (as env, the same way CI passes them).
# Keeping them in the environment — not baked into arguments — mirrors CI, and
# lets lanes (and reports.sh) use them for filtering.
export_env() {
  export GUARDIAN_BUILD_TYPE="$BUILD_TYPE"
  export GUARDIAN_BRANCH="$BRANCH"
  # Export what was actually requested (for dashboard filtering)
  export GUARDIAN_DO_TEST="$DO_TEST"
  export GUARDIAN_DO_COVERAGE="$DO_COVERAGE"
  export GUARDIAN_DO_LINT="$DO_LINT"
  # The SDK's Gemfile lives under fastlane/, not the repo root. Point bundler at it
  # so `bundle` (in dependencies.sh) and `bundle exec fastlane` (in build.sh)
  # resolve the right Gemfile without a cd. Only set if not already provided.
  export BUNDLE_GEMFILE="${BUNDLE_GEMFILE:-$REPO_ROOT/fastlane/Gemfile}"
}