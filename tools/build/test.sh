# shellcheck shell=bash
#
# tools/build/test.sh — hook for test-related orchestration.
#
# PLACEHOLDER: intentionally a no-op today. Unit testing currently runs as a
# Fastlane lane ("test"/"coverage"), selected in args.sh and executed by the lane
# loop — so there's nothing for this module to do yet. It exists so that future
# test orchestration that DOESN'T belong in a Fastlane lane has a home.
#
# Candidate future uses (SDK):
#   - gate on a coverage threshold once one is agreed
#   - run instrumentation tests separate from the JUnit4/Robolectric unit tests
#
# Contract: if used, call from the appropriate point in build.sh. Has access to
# the resolved plan globals. `die` on failure to abort the run.
run_test_hooks() {
  : # no-op for now — unit tests run via the Fastlane test/coverage lanes
}