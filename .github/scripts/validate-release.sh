#!/usr/bin/env bash
# validate-release.sh: check that every part of a Guardian.Android release happened,
# and report each check as a row of a pass or fail table. Read-only: it never
# changes a release, it only reports. Exits 1 if any check fails.
#
# Usage: validate-release.sh X.Y.Z
#
# Environment:
#   REPO              owner/name (default auth0/Guardian.Android)
#   GH_TOKEN          token for gh (read access is enough)
#   STAGE             "staged": right after the Release run, before anyone clicks
#                     Publish in the Maven Central Portal (the default);
#                     "published": after the Portal click, also checks that Maven
#                     Central serves the version and that it resolves
#   WAIT_MINUTES      how long to wait for Maven Central to serve the version
#                     (default 0: check once)
#   CURRENT_RUN_ID    id of the Release run calling this, if any; that run counts
#                     as the newest Release run for the version
#   GITHUB_STEP_SUMMARY  where the table goes (default stdout)
#
# Needs gh, jq, curl, git and mvn (all on the GitHub ubuntu runners).
#
# The staging deployment in the Portal is not checked: reading it needs the Maven
# Central credentials, which only the approved publish job may use.
set -uo pipefail

VERSION="${1:?usage: validate-release.sh X.Y.Z}"
REPO="${REPO:-auth0/Guardian.Android}"
STAGE="${STAGE:-staged}"
WAIT_MINUTES="${WAIT_MINUTES:-0}"
CURRENT_RUN_ID="${CURRENT_RUN_ID:-}"
SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "::error::'$VERSION' is not X.Y.Z."
  exit 1
fi
if [[ "$STAGE" != "staged" && "$STAGE" != "published" ]]; then
  echo "::error::STAGE must be staged or published, not '$STAGE'."
  exit 1
fi

rows=()
failed=0
# check RESULT NAME EXPECTED FOUND
check() {
  local mark="PASS"
  if [[ "$1" != "pass" ]]; then
    mark="FAIL"
    failed=$((failed + 1))
    echo "::error::$2: expected $3, found $4"
  fi
  rows+=("| $mark | $2 | $3 | $4 |")
}
# Runs a condition until it passes or WAIT_MINUTES is up. Returns its last status.
wait_for() {
  local deadline=$((SECONDS + WAIT_MINUTES * 60))
  until "$@"; do
    if (( SECONDS >= deadline )); then
      return 1
    fi
    sleep 15
  done
}
# file_at REF PATH: a file's content at a ref, or nothing.
file_at() {
  local content
  content="$(gh api "repos/$REPO/contents/$2?ref=$1" --jq .content 2>/dev/null)" || return 0
  base64 --decode <<<"$content" 2>/dev/null
}
# The X.Y.Z entry of a CHANGELOG on stdin, as release.yml extracts it.
changelog_entry() {
  tr -d '\r' | awk -v h="## [${VERSION}](" '
    index($0, h) == 1 { on = 1; next }
    on && /^## \[/    { exit }
    on                { print }
  '
}
# Text without CRs, trailing spaces or leading and trailing blank lines, to compare.
normalise() {
  python3 -c 'import sys; print("\n".join(l.rstrip() for l in sys.stdin.read().replace("\r", "").strip().split("\n")))'
}
at_least() {  # at_least FOUND WANTED: FOUND is WANTED or a later version
  [[ -n "$1" && "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -1)" == "$2" ]]
}

# ── GitHub Release ──────────────────────────────────────────────────────────────
release="$(gh api "repos/$REPO/releases?per_page=100" --jq \
  "[.[] | select(.tag_name == \"$VERSION\" or .name == \"$VERSION\")]" 2>/dev/null)" || release='[]'
published="$(jq -c '[.[] | select(.draft == false and .tag_name == $v)][0] // empty' --arg v "$VERSION" <<<"$release")"
drafts="$(jq '[.[] | select(.draft)] | length' <<<"$release")"
if [[ -n "$published" ]]; then
  check pass "GitHub Release" "published Release $VERSION" "published"
else
  check fail "GitHub Release" "published Release $VERSION" "$( [[ "$drafts" != "0" ]] && echo "only a draft" || echo "none")"
fi
if [[ "$drafts" == "0" ]]; then
  check pass "No leftover draft" "no draft Release for $VERSION" "none"
else
  check fail "No leftover draft" "no draft Release for $VERSION" "$drafts draft(s)"
fi

highest="$(git ls-remote --tags --refs "https://github.com/$REPO.git" 2>/dev/null \
  | sed -nE 's|.*refs/tags/([0-9]+\.[0-9]+\.[0-9]+)$|\1|p' | sort -V | tail -1)"
latest="$(gh api "repos/$REPO/releases/latest" --jq .tag_name 2>/dev/null)" || latest=""
if [[ "$highest" != "$VERSION" ]]; then
  check pass "Marked Latest" "Latest only if $VERSION is the highest version" "not the highest ($highest), Latest is $latest"
elif [[ "$latest" == "$VERSION" ]]; then
  check pass "Marked Latest" "Latest is $VERSION" "$latest"
else
  check fail "Marked Latest" "Latest is $VERSION" "${latest:-none}"
fi

if [[ -n "$published" ]]; then
  body="$(jq -r .body <<<"$published" | normalise)"
  entry="$(file_at "$VERSION" CHANGELOG.md | changelog_entry | normalise)"
  if [[ -z "$entry" ]]; then
    check fail "Release notes" "the CHANGELOG.md entry for $VERSION at the tag" "no entry at the tag"
  elif [[ "$body" == "$entry" ]]; then
    check pass "Release notes" "the CHANGELOG.md entry for $VERSION at the tag" "identical"
  else
    check fail "Release notes" "the CHANGELOG.md entry for $VERSION at the tag" "a different body"
  fi
fi

# ── Tag and commit ──────────────────────────────────────────────────────────────
# The commit the tag points at: the peeled entry for an annotated tag, else the tag.
tag_sha="$(git ls-remote --tags "https://github.com/$REPO.git" "refs/tags/$VERSION" "refs/tags/$VERSION^{}" 2>/dev/null \
  | sort -k2 | tail -1 | cut -f1)"
merge_sha="$(gh pr list --repo "$REPO" --head "release/$VERSION" --base master --state merged \
  --json mergeCommit,mergedAt --jq 'sort_by(.mergedAt) | last | .mergeCommit.oid // ""' 2>/dev/null)" || merge_sha=""
if [[ -z "$tag_sha" ]]; then
  check fail "Tag" "tag $VERSION on the release PR's merge commit" "no tag"
elif [[ -z "$merge_sha" ]]; then
  check fail "Tag" "tag $VERSION on the release PR's merge commit" "no merged release/$VERSION PR"
elif [[ "$tag_sha" == "$merge_sha" ]]; then
  check pass "Tag" "tag $VERSION on the release PR's merge commit" "${tag_sha:0:7}"
else
  check fail "Tag" "tag $VERSION on the release PR's merge commit ${merge_sha:0:7}" "${tag_sha:0:7}"
fi
if [[ -n "$tag_sha" ]]; then
  status="$(gh api "repos/$REPO/compare/$tag_sha...master" --jq .status 2>/dev/null)" || status=""
  if [[ "$status" == "ahead" || "$status" == "identical" ]]; then
    check pass "On master" "the tagged commit is in master's history" "yes ($status)"
  else
    check fail "On master" "the tagged commit is in master's history" "${status:-unknown}"
  fi
  verified="$(gh api "repos/$REPO/commits/$tag_sha" --jq .commit.verification.verified 2>/dev/null)" || verified=""
  if [[ "$verified" == "true" ]]; then
    check pass "Verified" "the tagged commit is Verified" "Verified"
  else
    check fail "Verified" "the tagged commit is Verified" "${verified:-unknown}"
  fi
fi

# ── master's version files and README ─────────────────────────────────────────
master_version="$(file_at master .version | tr -d '[:space:]')"
bad=""
at_least "$master_version" "$VERSION" || bad=".version is ${master_version:-missing}"
readme_v="$(file_at master README.md | sed -nE 's/.*com\.auth0\.android:guardian:([0-9]+\.[0-9]+\.[0-9]+).*/\1/p' | head -1)"
at_least "$readme_v" "$VERSION" || bad="${bad:+$bad, }README install line is ${readme_v:-missing}"
if [[ -z "$bad" ]]; then
  check pass "master versions" "$VERSION or later in .version and the README" "$master_version"
else
  check fail "master versions" "$VERSION or later in .version and the README" "$bad"
fi

# ── Release workflow run ──────────────────────────────────────────────────────
# The version a Release run released: from a manual run's title, or the release PR
# its pushed commit belongs to.
run_version() {  # event head_sha title
  if [[ "$1" == "workflow_dispatch" ]]; then
    sed -nE 's/^Release ([0-9]+\.[0-9]+\.[0-9]+) \(manual\)$/\1/p' <<<"$3"
  else
    gh api "repos/$REPO/commits/$2/pulls" \
      --jq '[.[] | select(.merged_at != null and (.head.ref | startswith("release/")))][0].head.ref // "" | ltrimstr("release/")' 2>/dev/null
  fi
}
newest=""
while IFS=$'\t' read -r id event head_sha title conclusion; do
  [[ -z "$id" ]] && continue
  if [[ "$(run_version "$event" "$head_sha" "$title")" == "$VERSION" ]]; then
    newest="$id"$'\t'"$conclusion"
    break
  fi
done < <(gh api "repos/$REPO/actions/workflows/release.yml/runs?per_page=50" \
  --jq '.workflow_runs[] | [.id, .event, .head_sha, .display_title, (.conclusion // "running")] | @tsv' 2>/dev/null)
IFS=$'\t' read -r run_id run_conclusion <<<"$newest"
if [[ -z "${run_id:-}" ]]; then
  check fail "Release run" "the newest Release run for $VERSION succeeded" "no Release run found"
elif [[ "$run_id" == "$CURRENT_RUN_ID" ]]; then
  check pass "Release run" "the newest Release run for $VERSION succeeded" "this run ($run_id), publish succeeded"
elif [[ "$run_conclusion" == "success" ]]; then
  check pass "Release run" "the newest Release run for $VERSION succeeded" "run $run_id: success"
else
  check fail "Release run" "the newest Release run for $VERSION succeeded" "run $run_id: $run_conclusion"
fi

# ── Maven Central, once promoted ────────────────────────────────────────────────
base="https://repo1.maven.org/maven2/com/auth0/android/guardian/$VERSION/guardian-$VERSION"
if [[ "$STAGE" == "published" ]]; then
  served() {
    local f
    for f in pom pom.asc aar aar.asc; do
      [[ "$(curl -s -o /dev/null -w '%{http_code}' "$base.$f")" == "200" ]] || return 1
    done
  }
  if wait_for served; then
    check pass "Maven Central" "repo1 serves the POM, the AAR and their .asc signatures" "all served"
    pom_v="$(curl -sf "$base.pom" | grep -m1 -oE '<version>[^<]+' | sed 's/<version>//')"
    if [[ "$pom_v" == "$VERSION" ]]; then
      check pass "POM version" "the published POM is version $VERSION" "$pom_v"
    else
      check fail "POM version" "the published POM is version $VERSION" "${pom_v:-unreadable}"
    fi
    # As a consumer would: resolve the exact artifact from Maven Central. Not its
    # dependencies, which come from Google's Maven repository.
    if mvn -B -q dependency:get -Dartifact="com.auth0.android:guardian:$VERSION:aar" -Dtransitive=false \
        >/tmp/mvn-resolve.log 2>&1; then
      check pass "Resolves" "Maven resolves com.auth0.android:guardian:$VERSION" "resolved"
    else
      check fail "Resolves" "Maven resolves com.auth0.android:guardian:$VERSION" "failed: $(tail -1 /tmp/mvn-resolve.log)"
    fi
  else
    check fail "Maven Central" "repo1 serves the POM, the AAR and their .asc signatures" "not served after $WAIT_MINUTES min"
  fi
fi

# ── Report ──────────────────────────────────────────────────────────────────────
{
  if (( failed == 0 )); then
    echo "### Guardian.Android $VERSION ($STAGE): all release checks passed"
  else
    echo "### Guardian.Android $VERSION ($STAGE): $failed release check(s) failed"
  fi
  echo
  echo "| | Check | Expected | Found |"
  echo "|---|---|---|---|"
  printf '%s\n' "${rows[@]}"
  echo
  if [[ "$STAGE" == "staged" ]]; then
    echo "Before promotion: check the com.auth0.android:guardian $VERSION deployment in the Maven Central Portal (Deployments) and click Publish, then run Validate release by hand for $VERSION with stage published."
  else
    echo "The staging deployment in the Portal is not checked here (it needs the release credentials)."
  fi
} >> "$SUMMARY"

(( failed == 0 ))
