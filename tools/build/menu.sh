# shellcheck shell=bash
#
# tools/build/menu.sh — the zero-arg interactive menu.
#
# Shown ONLY when build.sh is invoked with zero arguments from a real terminal
# (see maybe_interactive below). In CI (no TTY) it never runs — build.sh falls
# through to the safe defaults, so automation never hangs waiting on stdin.
#
# DESIGN: the menu is a *preset layer*, not a second code path. Every choice does
# nothing more than set the SAME flag variables the CLI parser sets (DO_BUILD,
# DO_TEST, BUILD_TYPE, DO_LINT, DO_COVERAGE). Whatever the menu can do, a flag can
# do too — so behavior is reproducible from the command line and the two never
# drift.
#
# SDK vs App: the App's menu also offers "install & launch on device" and Play/AAB
# presets and does adb device detection. A library has none of those, so this menu
# is the pure quality surface: build the AAR / PR check / tests / coverage / lint.
#
# Sourced by build.sh after args.sh (it calls init_defaults from there).

# Where the last run's plan is remembered (gitignored). One key=value per line.
LAST_RUN_FILE="$REPO_ROOT/.buildsh-last"

# ─── Last-run persistence ─────────────────────────────────────────────────────
# save_last_run writes the resolved plan; load_last_run reads it back into globals
# (prefixed LAST_) plus LAST_RUN_LABEL for display. Both are safe no-ops when the
# file is missing/unreadable, so a first-ever run just omits the "repeat" option.
save_last_run() {
  # Only meaningful for interactive humans; never litter CI checkouts.
  [[ -t 1 ]] || return 0
  # On a CLI-flag run there's no MENU_LABEL — synthesize one from the plan so the
  # next menu's "repeat last" is still descriptive.
  if [[ -z "$MENU_LABEL" ]]; then
    MENU_LABEL="${BUILD_TYPE}"
    [[ "$DO_LINT" == true ]]     && MENU_LABEL="${MENU_LABEL} +lint"
    [[ "$DO_TEST" == true ]]     && MENU_LABEL="${MENU_LABEL} +test"
    [[ "$DO_COVERAGE" == true ]] && MENU_LABEL="${MENU_LABEL} +coverage"
    [[ "$DO_BUILD" == true ]]    && MENU_LABEL="${MENU_LABEL} +build"
  fi
  {
    printf 'BUILD_TYPE=%s\n'  "$BUILD_TYPE"
    printf 'DO_BUILD=%s\n'    "$DO_BUILD"
    printf 'DO_TEST=%s\n'     "$DO_TEST"
    printf 'DO_LINT=%s\n'     "$DO_LINT"
    printf 'DO_COVERAGE=%s\n' "$DO_COVERAGE"
    printf 'LABEL=%s\n'       "$MENU_LABEL"
  } > "$LAST_RUN_FILE" 2>/dev/null || true
}

LAST_RUN_LABEL=""
HAVE_LAST_RUN=false
load_last_run() {
  HAVE_LAST_RUN=false
  [[ -r "$LAST_RUN_FILE" ]] || return 0
  local key val
  while IFS='=' read -r key val; do
    case "$key" in
      BUILD_TYPE)  LAST_BUILD_TYPE="$val" ;;
      DO_BUILD)    LAST_DO_BUILD="$val" ;;
      DO_TEST)     LAST_DO_TEST="$val" ;;
      DO_LINT)     LAST_DO_LINT="$val" ;;
      DO_COVERAGE) LAST_DO_COVERAGE="$val" ;;
      LABEL)       LAST_RUN_LABEL="$val" ;;
    esac
  done < "$LAST_RUN_FILE"
  HAVE_LAST_RUN=true
}

# Apply the loaded last-run values onto the live flag globals.
apply_last_run() {
  BUILD_TYPE="${LAST_BUILD_TYPE:-debug}"
  DO_BUILD="${LAST_DO_BUILD:-true}"
  DO_TEST="${LAST_DO_TEST:-false}"
  DO_LINT="${LAST_DO_LINT:-false}"
  DO_COVERAGE="${LAST_DO_COVERAGE:-false}"
}

# ─── The menu ─────────────────────────────────────────────────────────────────
# MENU_LABEL is a short human tag for the chosen action (stored for "repeat").
MENU_LABEL=""

# Entry point, called from parse_args. Shows the menu only for the zero-arg + TTY
# case; otherwise returns immediately and leaves defaults/flags untouched.
maybe_interactive() {
  [[ "$ORIG_ARGC" -eq 0 && -t 0 ]] || return 0

  load_last_run
  render_menu

  # Read the choice. Default is "PR check" (2) — the safe, CI-equivalent action.
  local choice
  printf '%s' "${C_CYAN}Enter choice [1-5, r, c, q] (default 2): ${C_RESET}" >&2
  read -r choice
  choice="${choice:-2}"
  apply_menu_choice "$choice"
}

# Draw the context header + grouped options.
render_menu() {
  local branch
  branch="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '<no-git>')"

  printf '\n%s\n' "${C_BOLD}build.sh${C_RESET} ${C_DIM}· Guardian.Android SDK${C_RESET}" >&2
  printf '%s\n'   "${C_DIM}branch:${C_RESET} ${C_CYAN}${branch}${C_RESET}" >&2
  if [[ "$HAVE_LAST_RUN" == true && -n "$LAST_RUN_LABEL" ]]; then
    printf '%s\n' "${C_DIM}last run:${C_RESET} ${LAST_RUN_LABEL}" >&2
  fi
  printf '\n' >&2

  printf '%s\n' " ${C_BOLD}Verify${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}1${C_RESET}) Build AAR       ${C_DIM}— assemble the release AAR, nothing else${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}2${C_RESET}) PR check        ${C_DIM}— lint + tests + coverage (mirrors pr.yml)${C_RESET}  ${C_DIM}← default${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}3${C_RESET}) Unit tests      ${C_DIM}— tests only, no build${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}4${C_RESET}) Coverage        ${C_DIM}— tests + JaCoCo report${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}5${C_RESET}) Lint            ${C_DIM}— Android Lint only${C_RESET}" >&2
  printf '\n' >&2

  # Only offer "repeat" when there's something to repeat.
  local repeat_line="  ${C_DIM}r) Repeat last   — (no previous run yet)${C_RESET}"
  if [[ "$HAVE_LAST_RUN" == true && -n "$LAST_RUN_LABEL" ]]; then
    repeat_line="  ${C_GREEN}r${C_RESET}) Repeat last   ${C_DIM}— ${LAST_RUN_LABEL}${C_RESET}"
  fi
  printf '%s\n' "$repeat_line" >&2
  printf '%s\n' "  ${C_GREEN}c${C_RESET}) Customize...   ${C_DIM}— choose buildType / steps${C_RESET}" >&2
  printf '%s\n' "  ${C_GREEN}q${C_RESET}) Quit" >&2
  printf '\n' >&2
}

# Map a menu selection onto flag globals. Each branch sets the SAME variables the
# CLI would; nothing here builds — build.sh's normal pipeline does that afterward.
apply_menu_choice() {
  local choice="$1"
  # init_defaults already ran in parse_args, so we start from the safe baseline
  # and only flip what each choice needs.
  case "$choice" in
    1) BUILD_TYPE="release"; DO_LINT=false; DO_TEST=false; DO_COVERAGE=false
       DO_BUILD=true
       MENU_LABEL="[1] Build AAR (release)" ;;
    2) # PR check: what pr.yml runs (lint + coverage, coverage runs the tests). No AAR.
       BUILD_TYPE="debug"; DO_LINT=true; DO_TEST=true; DO_COVERAGE=true
       DO_BUILD=false
       MENU_LABEL="[2] PR check (lint+test+coverage)" ;;
    3) BUILD_TYPE="debug"; DO_LINT=false; DO_TEST=true; DO_COVERAGE=false
       DO_BUILD=false
       MENU_LABEL="[3] Unit tests" ;;
    4) BUILD_TYPE="debug"; DO_LINT=false; DO_TEST=true; DO_COVERAGE=true
       DO_BUILD=false
       MENU_LABEL="[4] Coverage" ;;
    5) BUILD_TYPE="debug"; DO_LINT=true; DO_TEST=false; DO_COVERAGE=false
       DO_BUILD=false
       MENU_LABEL="[5] Lint" ;;
    r|R)
       if [[ "$HAVE_LAST_RUN" == true ]]; then
         apply_last_run
         MENU_LABEL="${LAST_RUN_LABEL:-repeat}"
       else
         die "No previous run to repeat yet — pick 1-5 or c."
       fi ;;
    c|C) customize_flow ;;
    q|Q) log "Nothing to do — bye."; exit 0 ;;
    *)   die "Invalid choice '$choice'. Expected 1-5, r, c, or q." ;;
  esac
}

# ─── Prompt helpers ───────────────────────────────────────────────────────────
# Two small readers that make the Customize flow forgiving and clear:
#   • the DEFAULT is rendered green/bold with an explicit "(Enter = …)" hint, so
#     it reads as pre-filled — just press Enter to accept it;
#   • input is case-insensitive and re-asks on anything unexpected instead of
#     aborting the whole run.
# macOS ships bash 3.2 (no `read -e -i`), so the default is *shown* prominently
# rather than literally editable — consistent behavior on every machine. All
# prompts go to stderr; ask_choice echoes the chosen index to stdout for capture.

# Yes/No prompt. Usage: `ask_yn "Run tests?" y` → returns 0 (yes) / 1 (no).
# Accepts y|yes|n|no in ANY case, or bare Enter for the default.
ask_yn() {
  local prompt="$1" default="$2" ans yes_lbl no_lbl def_word
  if [[ "$default" == "y" ]]; then
    yes_lbl="${C_GREEN}${C_BOLD}Y${C_RESET}"; no_lbl="${C_DIM}n${C_RESET}"; def_word="Yes"
  else
    yes_lbl="${C_DIM}y${C_RESET}"; no_lbl="${C_GREEN}${C_BOLD}N${C_RESET}"; def_word="No"
  fi
  while true; do
    printf '%s' "  ${C_CYAN}${prompt}${C_RESET} [${yes_lbl}/${no_lbl}] ${C_DIM}(Enter = ${def_word})${C_RESET} : " >&2
    read -r ans
    ans="${ans:-$default}"
    case "$ans" in
      y|Y|yes|YES|Yes) return 0 ;;
      n|N|no|NO|No)    return 1 ;;
      *) printf '%s\n' "  ${C_YELLOW}Please answer y or n (or press Enter for ${def_word}).${C_RESET}" >&2 ;;
    esac
  done
}

# Numbered single-choice prompt. Usage:
#   idx=$(ask_choice "Build type" 1 debug release)
# Renders the default option green/bold with an "(Enter = …)" hint, echoes the
# chosen 1-based index to stdout, and re-asks on invalid input (never aborts).
ask_choice() {
  local prompt="$1" default="$2"; shift 2
  local opts=("$@") n=${#opts[@]} i ans rendered
  while true; do
    rendered=""
    for ((i = 1; i <= n; i++)); do
      if [[ "$i" == "$default" ]]; then
        rendered="${rendered}  ${C_GREEN}${C_BOLD}${i}) ${opts[i-1]}${C_RESET}"
      else
        rendered="${rendered}  ${C_DIM}${i})${C_RESET} ${opts[i-1]}"
      fi
    done
    printf '%s' "  ${C_CYAN}${prompt}${C_RESET}${rendered}  ${C_DIM}(Enter = ${opts[default-1]})${C_RESET} : " >&2
    read -r ans
    ans="${ans:-$default}"
    if [[ "$ans" =~ ^[0-9]+$ ]] && (( ans >= 1 && ans <= n )); then
      printf '%s' "$ans"; return 0
    fi
    printf '%s\n' "  ${C_YELLOW}Enter a number 1-${n} (or press Enter for ${opts[default-1]}).${C_RESET}" >&2
  done
}

# ─── Customize sub-flow ───────────────────────────────────────────────────────
# A short guided prompt for the cases the presets don't cover: pick buildType and
# which steps to run. Still just sets flags. Bare Enter keeps the highlighted
# default at each step, so it's fast to tab through.
customize_flow() {
  printf '\n%s\n' "${C_BOLD}Customize${C_RESET} ${C_DIM}— the ${C_RESET}${C_GREEN}${C_BOLD}green${C_RESET}${C_DIM} choice is the default; just press Enter to accept it${C_RESET}" >&2

  case "$(ask_choice "Build type" 1 debug release)" in
    1) BUILD_TYPE=debug ;; 2) BUILD_TYPE=release ;;
  esac

  if ask_yn "Run lint?"     n; then DO_LINT=true; else DO_LINT=false; fi
  if ask_yn "Run tests?"    n; then DO_TEST=true; else DO_TEST=false; fi
  if ask_yn "Coverage report?" n; then DO_COVERAGE=true; else DO_COVERAGE=false; fi
  if ask_yn "Assemble the AAR?" y; then DO_BUILD=true; else DO_BUILD=false; fi

  MENU_LABEL="[c] custom (${BUILD_TYPE})"
}
