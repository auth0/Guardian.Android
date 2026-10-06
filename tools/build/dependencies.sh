# shellcheck shell=bash
#
# tools/build/dependencies.sh — make the toolchain ready before building.
#
# Defines ensure_dependencies(), called by build.sh's prepare phase. This is what
# makes "clone then ./build.sh" work on a fresh machine or CI runner. Honors the
# SKIP_DEPS flag (set by --skip-deps). Sourced by build.sh after common.sh.
#
# NOTE (Android specifics): the Gemfile lives under fastlane/ (not repo root), so
# build.sh exports BUNDLE_GEMFILE=fastlane/Gemfile before this runs — bundle then
# resolves against the right Gemfile. Java itself is NOT installed here (CI's
# setup-java / a local SDK provides it); we only verify it's present.

# Install Ruby gem dependencies so the Fastlane lanes can run.
# Idempotent: `bundle check` is fast and skips the (slow) install when the bundle
# is already satisfied, so repeat runs pay almost nothing.
ensure_dependencies() {
  if [[ "$SKIP_DEPS" == true ]]; then
    log "--skip-deps set: not installing gems. Assuming the bundle is already present."
    command -v bundle >/dev/null 2>&1 || die "bundler not found and --skip-deps was passed. Drop --skip-deps or install it."
    return 0
  fi

  command -v ruby >/dev/null 2>&1 || die "Ruby not found. Install Ruby (see .ruby-version) then re-run."
  command -v java >/dev/null 2>&1 || warn "java not found on PATH (see .java-version) — Gradle lanes will fail without a JDK."

  # bundler itself may be absent on a truly fresh machine — install it first.
  if ! command -v bundle >/dev/null 2>&1; then
    log "bundler not found — installing it (gem install bundler)…"
    gem install bundler || die "Failed to install bundler. Check your Ruby/gem setup."
  fi

  # `bundle check` exits 0 when every gem in Gemfile.lock is already installed;
  # only then do we skip the install. Otherwise install.
  if bundle check >/dev/null 2>&1; then
    log "Dependencies already satisfied — skipping 'bundle install'."
  else
    log "Installing Ruby dependencies (bundle install)…"
    bundle install || die "'bundle install' failed. Fix the errors above and re-run."
  fi
}
