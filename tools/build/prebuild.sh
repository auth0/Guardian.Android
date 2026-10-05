# shellcheck shell=bash
#
# tools/build/prebuild.sh — hook that runs just BEFORE the lanes execute.
#
# PLACEHOLDER: intentionally a no-op today. Wired into build.sh's flow so future
# pre-build steps have an obvious, single home — no need to touch the orchestrator.
#
# Candidate future uses (SDK):
#   - decode the Maven Central GPG signing key from a base64 secret (publishing task)
#   - stamp the version from CI metadata
#   - warm the Gradle build cache
#
# Contract: runs after dependencies are ready and env is exported, before the
# first lane. Has access to all resolved plan globals (BUILD_TYPE, GRADLE_BUILD_TYPE,
# VARIANT, LANES, …). `die` on failure to abort the whole run.
run_prebuild() {
  : # no-op for now
}
