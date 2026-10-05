# shellcheck shell=bash
#
# tools/build/usage.sh — help text + bad-input handling.
#
# Defines usage() (concise colorized help), usage_full() (the full narrative), and
# die_usage() (error + concise usage). Sourced by build.sh after common.sh.
#
# NOTE: usage_full() prints the header comment block of build.sh itself (the file
# named by $ENTRYPOINT), so the detailed docs and the code never drift apart.

# Concise, colorized help: usage line + flags + examples. This is what users see
# on an error or plain --help. For the full narrative (deps, architecture) they
# opt in with --help-full.
usage() {
  cat >&2 <<EOF
${C_BOLD}build.sh${C_RESET} — one command to build/test the Guardian Android SDK, locally and in CI.

${C_BOLD}USAGE${C_RESET}
  ${C_CYAN}./build.sh${C_RESET} [options]        ${C_DIM}# no args on a terminal = interactive menu${C_RESET}
                              ${C_DIM}# no args in CI       = debug build + test + lint${C_RESET}

${C_BOLD}OPTIONS${C_RESET} ${C_DIM}(defaults in brackets)${C_RESET}
  ${C_GREEN}--buildType${C_RESET} debug|release               ${C_DIM}[debug]${C_RESET}
  ${C_GREEN}--build${C_RESET} | ${C_GREEN}--nobuild${C_RESET}                     assemble the release AAR ${C_DIM}[--build]${C_RESET}
  ${C_GREEN}--test${C_RESET} | ${C_GREEN}--notest${C_RESET}                       run unit tests ${C_DIM}[--test]${C_RESET}
  ${C_GREEN}--lint${C_RESET} | ${C_GREEN}--nolint${C_RESET}                       run Android Lint (:guardian) ${C_DIM}[--lint]${C_RESET}
  ${C_GREEN}--coverage${C_RESET} | ${C_GREEN}--nocoverage${C_RESET}               emit JaCoCo report (:guardian) ${C_DIM}[--nocoverage]${C_RESET}
  ${C_GREEN}--branch${C_RESET} <name>                        label for logs/artifacts
  ${C_GREEN}--skip-deps${C_RESET}                            skip 'bundle install' ${C_DIM}[install]${C_RESET}
  ${C_GREEN}--dryRun${C_RESET}                               print the plan, run nothing
  ${C_GREEN}-h${C_RESET}, ${C_GREEN}--help${C_RESET}                             this help
  ${C_GREEN}--help-full${C_RESET}                            full docs: architecture, deps

${C_BOLD}EXAMPLES${C_RESET}
  ${C_CYAN}./build.sh${C_RESET}                                       ${C_DIM}# PR-style: debug + test + lint${C_RESET}
  ${C_CYAN}./build.sh${C_RESET} --buildType debug --coverage --lint --nobuild   ${C_DIM}# what pr.yml runs${C_RESET}
  ${C_CYAN}./build.sh${C_RESET} --buildType release --notest --nolint --nocoverage   ${C_DIM}# just the AAR${C_RESET}
  ${C_CYAN}./build.sh${C_RESET} --buildType release --dryRun          ${C_DIM}# show what would run${C_RESET}

${C_DIM}Run '${C_RESET}${C_CYAN}./build.sh --help-full${C_RESET}${C_DIM}' for the architecture and dependency behavior.${C_RESET}
EOF
  exit "${1:-0}"
}

# Full docs, on demand: print the header comment block of build.sh — every line
# from the title down to just before 'set -e' — so the detailed narrative and the
# code never drift. $ENTRYPOINT is the absolute path to build.sh (set there).
# We derive the end line dynamically (the line before 'set -euo pipefail') instead
# of hardcoding it, so the header can grow without silently truncating this output.
usage_full() {
  local end
  end="$(( $(grep -n '^set -euo pipefail' "$ENTRYPOINT" | head -1 | cut -d: -f1) - 1 ))"
  sed -n "3,${end}p" "$ENTRYPOINT" | sed 's/^# \{0,1\}//' >&2
  exit "${1:-0}"
}

# Bad-input death: show WHAT was wrong, then the CONCISE usage, then exit non-zero.
# Use this for any invalid flag/value so the user always sees how to run it right.
die_usage() {
  printf '%s[build.sh] ERROR:%s %s\n\n' "$C_RED" "$C_RESET" "$*" >&2
  usage 1
}