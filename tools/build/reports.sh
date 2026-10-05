#!/usr/bin/env bash
# reports.sh — parse test/coverage/lint reports and generate
# a single self-contained reports.html with all data embedded.
#
# Called by tools/build/postbuild.sh after tests/coverage/lint complete.
# Parses XML reports, generates unified HTML, and cleans up detailed HTML files.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

# Report paths - auto-detect test results directory
TEST_RESULTS_DIR=""
for dir in guardian/build/test-results/test*UnitTest; do
  if [[ -d "$dir" ]] && [[ -n "$(ls -A "$dir" 2>/dev/null)" ]]; then
    TEST_RESULTS_DIR="$dir"
    break
  fi
done

COVERAGE_XML=""
# Use find so the path works regardless of the exact JaCoCo output filename. The
# SDK's guardian/build.gradle pins the report to a flat jacocoTestReport.xml, but
# match the jacocoTestReport*.xml pattern anyway for robustness.
while IFS= read -r xml; do
  COVERAGE_XML="$xml"
  break
done < <(find guardian/build/reports/jacoco -name "jacocoTestReport*.xml" 2>/dev/null | sort)

LINT_XML_FILES=()
LINT_HTML=""
# The SDK has a single library module (:guardian) whose `lint` lane writes one XML.
# Look for the merged lint results first (from the 'lint' task), otherwise any variant.
for module_dir in guardian; do
  for file in "$module_dir"/build/reports/lint-results.xml "$module_dir"/build/reports/lint-results-*.xml; do
    if [[ -f "$file" ]]; then
      LINT_XML_FILES+=("$file")
      break
    fi
  done
done
for file in guardian/build/reports/lint-results.html guardian/build/reports/lint-results-*.html; do
  if [[ -f "$file" ]]; then
    LINT_HTML="$file"
    break
  fi
done

OUTPUT_HTML="guardian/build/reports/reports.html"

# Temporary data storage (ensure numeric types)
TESTS_PASSED=0
TESTS_FAILED=0
FAILED_TESTS=""
PASSED_TESTS=""
COVERAGE_PERCENT=0
COVERAGE_DETAILS=""
LINT_WARNINGS=0
LINT_ERRORS=0
LINT_INFO=0
LINT_DETAILS=""

# Helper: ensure numeric value (default to 0 if empty/invalid)
ensure_number() {
  local val="$1"
  [[ "$val" =~ ^[0-9]+$ ]] && echo "$val" || echo "0"
}

# Parse test results from XML
parse_test_results() {
  if [[ -z "$TEST_RESULTS_DIR" || ! -d "$TEST_RESULTS_DIR" ]]; then
    echo "  No test results directory found" >&2
    return 0
  fi

  echo "  Found test results in: $TEST_RESULTS_DIR" >&2

  local xml_files=("$TEST_RESULTS_DIR"/*.xml)
  if [[ ! -e "${xml_files[0]}" ]]; then
    echo "  No XML files found in $TEST_RESULTS_DIR" >&2
    return 0
  fi

  echo "  Parsing ${#xml_files[@]} test XML files..." >&2

  for xml in "${xml_files[@]}"; do
    [[ ! -f "$xml" ]] && continue

    # Extract test counts from testsuite tag
    local tests failures errors
    tests=$(grep '<testsuite' "$xml" | head -1 | sed -n 's/.*tests="\([0-9]*\)".*/\1/p')
    failures=$(grep '<testsuite' "$xml" | head -1 | sed -n 's/.*failures="\([0-9]*\)".*/\1/p')
    errors=$(grep '<testsuite' "$xml" | head -1 | sed -n 's/.*errors="\([0-9]*\)".*/\1/p')

    # Ensure numeric values
    tests=$(ensure_number "$tests")
    failures=$(ensure_number "$failures")
    errors=$(ensure_number "$errors")

    local failed=$((failures + errors))
    local passed=$((tests - failed))

    TESTS_PASSED=$((TESTS_PASSED + passed))
    TESTS_FAILED=$((TESTS_FAILED + failed))

    # Extract test case names (simplified - just count for now)
    local suite_name
    suite_name=$(grep '<testsuite' "$xml" | head -1 | sed -n 's/.*name="\([^"]*\)".*/\1/p')

    # Count actual testcase tags
    while read -r testcase_line; do
      # Extract name - it comes first
      local test_name
      test_name=$(echo "$testcase_line" | grep -oE 'name="[^"]*"' | head -1 | sed 's/name="//; s/"$//')

      # Extract classname
      local class_name
      class_name=$(echo "$testcase_line" | grep -oE 'classname="[^"]*"' | head -1 | sed 's/classname="//; s/"$//')

      # Check if this is a self-closing tag (passed) or has failure/error children
      if echo "$testcase_line" | grep -q "/>"; then
        # Self-closing = passed test
        PASSED_TESTS="${PASSED_TESTS}${class_name}.${test_name}|${class_name}
"
      else
        # Has children - might be failure/error
        FAILED_TESTS="${FAILED_TESTS}${class_name}.${test_name}|Test failed|${class_name}
"
      fi
    done < <(grep '<testcase' "$xml")
  done
}

# Parse coverage from JaCoCo XML or HTML
parse_coverage() {
  # Try XML first
  if [[ -f "$COVERAGE_XML" ]]; then
    echo "  Found coverage XML: $COVERAGE_XML" >&2

    # Use Python for all JaCoCo parsing — grep -P is BSD-incompatible on macOS.
    local tmpscript
    tmpscript=$(mktemp)
    cat > "$tmpscript" << 'PYEOF'
import sys
import xml.etree.ElementTree as ET

tree = ET.parse(sys.argv[1])
root = tree.getroot()

# Overall LINE coverage from root-level counters
total_covered = 0
total_missed = 0
for c in root.findall('counter'):
    if c.get('type') == 'LINE':
        total_covered += int(c.get('covered', 0))
        total_missed += int(c.get('missed', 0))

total = total_covered + total_missed
pct = int(total_covered * 100 / total) if total > 0 else 0
print('OVERALL|{}'.format(pct))

# Per-sourcefile breakdown
entries = []
for pkg in root.findall('.//package'):
    pkg_name = pkg.get('name', '').replace('/', '.')
    for sf in pkg.findall('sourcefile'):
        name = sf.get('name', '')
        covered = 0
        missed = 0
        for c in sf.findall('counter'):
            if c.get('type') == 'LINE':
                covered += int(c.get('covered', 0))
                missed += int(c.get('missed', 0))
        total = covered + missed
        if total > 0:
            pct = int(covered * 100 / total)
            display = pkg_name + '.' + name if pkg_name else name
            entries.append((pct, display))
entries.sort(key=lambda x: x[0])
for pct, filename in entries:
    print('{}|{}'.format(filename, pct))
PYEOF
    local file_output
    file_output=$(python3 "$tmpscript" "$COVERAGE_XML" 2>/dev/null || echo "")
    rm -f "$tmpscript"

    # Extract overall percentage from first line
    local overall_line
    overall_line=$(echo "$file_output" | head -1)
    if [[ "$overall_line" == OVERALL\|* ]]; then
      local pct="${overall_line#OVERALL|}"
      pct=$(ensure_number "$pct")
      [[ $pct -gt 0 ]] && COVERAGE_PERCENT=$pct
      file_output=$(echo "$file_output" | tail -n +2)
    fi

    while IFS='|' read -r filename pct; do
      [[ -z "$filename" ]] && continue
      COVERAGE_DETAILS="${COVERAGE_DETAILS}${filename}|${pct}
"
    done <<< "$file_output"

    return
  fi

  # Fallback to HTML if XML doesn't exist — search dynamically so the path
  # works regardless of task name or build-type subdirectory.
  #
  # JaCoCo's HTML report has one index.html per package PLUS one root-level
  # index.html with the true overall total. A plain `find | sort` can pick a
  # package's index.html instead of the root one purely on alphabetical luck
  # (e.g. "com.example/index.html" sorts before "index.html"), silently
  # reporting one package's coverage as if it were the whole project's — and
  # inconsistently across branches/builds depending on which packages exist.
  # The root summary page always sits directly inside a directory literally
  # named "html" (package folders are named after java packages, which never
  # collide with that), so filter on that instead of trusting sort order.
  local html_index=""
  while IFS= read -r f; do
    if [[ "$(basename "$(dirname "$f")")" == "html" ]]; then
      html_index="$f"
      break
    fi
  done < <(find guardian/build/reports/jacoco -name "index.html" 2>/dev/null | sort)
  if [[ -n "$html_index" && -f "$html_index" ]]; then
    echo "  Found coverage HTML: $html_index" >&2

    # Extract coverage percentage from Total row in HTML
    local percent
    # grep -m 1 closes stdin early → upstream grep gets SIGPIPE (exit 141).
    # `|| true` suppresses that under set -o pipefail.
    # Pattern: JaCoCo renders `class="ctr2" id="i0">72%` — the id attribute sits
    # between ctr2" and >, so matching ctr2">[0-9]*% never fires. Match any line
    # containing "ctr2", extract the first XX% value, then strip the %.
    percent=$(grep -A 50 '<tfoot>' "$html_index" | grep -m 1 'ctr2' | grep -oE '[0-9]+%' | head -1 | tr -d '%' || true)
    COVERAGE_PERCENT=$(ensure_number "$percent")

    echo "  Extracted coverage: ${COVERAGE_PERCENT}%" >&2
  fi
}

# Parse lint results (filtered to changed files only)
parse_lint() {
  if [[ ${#LINT_XML_FILES[@]} -eq 0 ]]; then
    return 0
  fi

  # Get list of changed files AND changed line ranges (compared to master).
  local base_branch="master"

  local changed_files
  changed_files=$(git diff --name-only origin/${base_branch}...HEAD 2>/dev/null || git diff --name-only HEAD~10 2>/dev/null || echo "")

  echo "  Changed files: $(echo "$changed_files" | wc -l | tr -d ' ')" >&2

  # Get changed line ranges per file: file.txt:10,15:20,25 means lines 10-15 and 20-25 changed
  # Using git diff -U0 to show only changed lines
  local changed_lines
  changed_lines=$(git diff -U0 origin/${base_branch}...HEAD 2>/dev/null | grep -E '^\+\+\+ |^@@ ' || echo "")

  # Parse lint issues and filter to changed files only. Accumulate across every
  # module's lint XML (see LINT_XML_FILES discovery above) rather than resetting
  # per file, so counts/details reflect all modules combined.
  LINT_DETAILS=""
  LINT_WARNINGS=0
  LINT_ERRORS=0
  LINT_INFO=0

  local lint_file
  for lint_file in "${LINT_XML_FILES[@]}"; do
    echo "  Found lint XML: $lint_file" >&2
    parse_lint_file "$lint_file" "$changed_files"
  done

  echo "  Lint issues in changed files: ${LINT_WARNINGS} warnings, ${LINT_ERRORS} errors" >&2
}

# Parse a single module's lint XML, accumulating into LINT_WARNINGS/LINT_ERRORS/
# LINT_INFO/LINT_DETAILS. Split out of parse_lint() so multiple modules can share
# the same per-issue extraction logic without resetting counters between files.
parse_lint_file() {
  local lint_file="$1" changed_files="$2"
  local in_issue=false
  local message="" severity="" file_path="" line_num="" full_file_path=""

  while IFS= read -r line; do
    if [[ "$line" =~ \<issue ]]; then
      in_issue=true
      message=""
      severity=""
      file_path=""
      full_file_path=""
      line_num=""
    elif [[ "$in_issue" == true ]]; then
      # Extract attributes from multi-line issue tag
      if [[ "$line" =~ message=\"([^\"]+)\" ]]; then
        message="${BASH_REMATCH[1]}"
      fi
      if [[ "$line" =~ severity=\"([^\"]+)\" ]]; then
        severity="${BASH_REMATCH[1]}"
      fi
      if [[ "$line" =~ file=\"([^\"]+)\" ]]; then
        full_file_path="${BASH_REMATCH[1]}"
        # Extract just the filename from full path
        file_path=$(basename "$full_file_path")
      fi
      if [[ "$line" =~ line=\"([^\"]+)\" ]]; then
        line_num="${BASH_REMATCH[1]}"
      fi

      # When we hit the closing > of the issue tag, check if file was changed
      if [[ "$line" =~ \> ]] && [[ -n "$message" ]] && [[ -n "$file_path" ]]; then
        # Simple check: is this file in the changed files list?
        local is_changed=false

        if [[ -z "$changed_files" ]]; then
          # No git history - show all issues
          is_changed=true
        else
          # Check if the filename appears in changed files
          # Use the full path to match properly
          if [[ -n "$full_file_path" ]] && echo "$changed_files" | grep -qF "$(basename "$full_file_path")"; then
            is_changed=true
          elif echo "$changed_files" | grep -qF "$file_path"; then
            is_changed=true
          fi
        fi

        if [[ "$is_changed" == true ]]; then
          # Use relative path from repo root for clarity
          local display_path="$file_path"
          if [[ -n "$full_file_path" ]]; then
            # Extract relative path: remove repo root prefix
            display_path="${full_file_path#$REPO_ROOT/}"
          fi

          local location="${display_path}"
          [[ -n "$line_num" ]] && location="${display_path}:${line_num}"
          LINT_DETAILS="${LINT_DETAILS}${message}|${severity,,}|${location}
"

          # Count by severity
          case "${severity,,}" in
            warning) LINT_WARNINGS=$((LINT_WARNINGS + 1)) ;;
            error) LINT_ERRORS=$((LINT_ERRORS + 1)) ;;
            information) LINT_INFO=$((LINT_INFO + 1)) ;;
          esac
        fi

        in_issue=false
      fi
    fi
  done < "$lint_file"
}

# Generate the HTML report
generate_html() {
  # Ensure all variables are numeric
  TESTS_PASSED=$(ensure_number "$TESTS_PASSED")
  TESTS_FAILED=$(ensure_number "$TESTS_FAILED")
  COVERAGE_PERCENT=$(ensure_number "$COVERAGE_PERCENT")
  LINT_WARNINGS=$(ensure_number "$LINT_WARNINGS")
  LINT_ERRORS=$(ensure_number "$LINT_ERRORS")
  LINT_INFO=$(ensure_number "$LINT_INFO")

  local total_tests=$((TESTS_PASSED + TESTS_FAILED))
  local pass_rate=0
  [[ $total_tests -gt 0 ]] && pass_rate=$((TESTS_PASSED * 100 / total_tests))

  local total_lint=$((LINT_WARNINGS + LINT_ERRORS + LINT_INFO))

  local coverage_color="error"
  [[ $COVERAGE_PERCENT -ge 80 ]] && coverage_color="success"
  [[ $COVERAGE_PERCENT -ge 60 && $COVERAGE_PERCENT -lt 80 ]] && coverage_color="warning"

  local current_date
  current_date=$(date '+%B %d, %Y at %l:%M %p' | sed 's/  / /g')

  local branch
  branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

  mkdir -p "$(dirname "$OUTPUT_HTML")"
  cat > "$OUTPUT_HTML" << 'EOF_TEMPLATE'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Guardian SDK - Test Report Dashboard</title>
    <link rel="icon" href="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA1MTIgNTEyIj48bGluZSB4MT0iMTg4IiB5MT0iMTU4IiB4Mj0iMTQ4IiB5Mj0iODIiIHN0cm9rZT0iIzNEREM4NCIgc3Ryb2tlLXdpZHRoPSIyNCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+PGxpbmUgeDE9IjMyNCIgeTE9IjE1OCIgeDI9IjM2NCIgeTI9IjgyIiBzdHJva2U9IiMzRERDODQiIHN0cm9rZS13aWR0aD0iMjQiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPjxwYXRoIGQ9Ik0xMjggMjMyIEExMjggMTI4IDAgMCAxIDM4NCAyMzIgTDM4NCAzNjQgTDEyOCAzNjQgWiIgZmlsbD0iIzNEREM4NCIvPjxjaXJjbGUgY3g9IjIwMiIgY3k9IjIxOCIgcj0iMjIiIGZpbGw9IndoaXRlIi8+PGNpcmNsZSBjeD0iMzEwIiBjeT0iMjE4IiByPSIyMiIgZmlsbD0id2hpdGUiLz48L3N2Zz4=">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
            background: #f5f5f5;
            padding: 20px;
            line-height: 1.6;
            color: #333;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 { font-size: 28px; margin-bottom: 10px; font-weight: 600; }
        .header p { opacity: 0.9; font-size: 14px; }

        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
            border-bottom: 1px solid #e0e0e0;
        }
        .summary-card {
            background: white;
            padding: 20px;
            border-radius: 6px;
            border-left: 4px solid #ccc;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .summary-card.success { border-left-color: #28a745; }
        .summary-card.warning { border-left-color: #ffc107; }
        .summary-card.error { border-left-color: #dc3545; }
        .summary-card.info { border-left-color: #17a2b8; }

        .summary-card h3 {
            font-size: 13px;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 10px;
            font-weight: 600;
        }
        .summary-card .value {
            font-size: 36px;
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }
        .summary-card .label { color: #666; font-size: 14px; }

        .section {
            padding: 30px;
            border-bottom: 1px solid #e0e0e0;
        }
        .section:last-child { border-bottom: none; }

        .section-title {
            font-size: 20px;
            color: #333;
            margin-bottom: 20px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        details {
            background: #f8f9fa;
            border: 1px solid #e0e0e0;
            border-radius: 6px;
            margin-bottom: 15px;
            overflow: hidden;
        }
        details[open] {
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }

        summary {
            padding: 20px;
            cursor: pointer;
            font-weight: 600;
            font-size: 16px;
            color: #333;
            background: #fff;
            border-left: 4px solid #667eea;
            transition: background 0.2s;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        summary:hover {
            background: #f8f9fa;
        }
        details[open] summary {
            border-bottom: 1px solid #e0e0e0;
            background: #f8f9fa;
        }
        summary::marker {
            color: #667eea;
        }

        .details-content {
            padding: 20px;
            background: white;
        }

        .test-item, .coverage-item, .lint-item {
            padding: 15px;
            margin-bottom: 10px;
            background: #f8f9fa;
            border-left: 3px solid #ccc;
            border-radius: 4px;
        }
        .test-item.failed { border-left-color: #dc3545; background: #fff5f5; }
        .test-item.passed { border-left-color: #28a745; }
        .coverage-item.low { border-left-color: #dc3545; background: #fff5f5; }
        .coverage-item.medium { border-left-color: #ffc107; background: #fffef5; }
        .coverage-item.high { border-left-color: #28a745; }
        .lint-item.warning { border-left-color: #ffc107; background: #fffef5; }
        .lint-item.error { border-left-color: #dc3545; background: #fff5f5; }

        .item-title {
            font-weight: 600;
            font-size: 14px;
            margin-bottom: 8px;
            color: #333;
        }
        .item-detail {
            font-size: 13px;
            color: #666;
            margin-bottom: 5px;
        }
        .item-location {
            font-size: 12px;
            color: #999;
            font-family: 'Monaco', 'Courier New', monospace;
        }
        .error-message {
            background: white;
            padding: 10px;
            margin-top: 10px;
            border-radius: 4px;
            font-family: 'Monaco', 'Courier New', monospace;
            font-size: 12px;
            color: #d63031;
            overflow-x: auto;
        }

        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
        }
        .badge.success { background: #d4edda; color: #155724; }
        .badge.error { background: #f8d7da; color: #721c24; }
        .badge.warning { background: #fff3cd; color: #856404; }
        .badge.info { background: #d1ecf1; color: #0c5460; }

        .footer {
            text-align: center;
            padding: 20px;
            color: #666;
            font-size: 13px;
            background: #f8f9fa;
        }

        .icon { font-size: 20px; }

        .progress-bar {
            width: 100%;
            height: 8px;
            background: #e0e0e0;
            border-radius: 4px;
            overflow: hidden;
            margin-top: 8px;
        }
        .progress-fill {
            height: 100%;
            transition: width 0.3s;
        }
        .progress-fill.success { background: #28a745; }
        .progress-fill.warning { background: #ffc107; }
        .progress-fill.error { background: #dc3545; }

        .empty-state {
            text-align: center;
            padding: 40px;
            color: #999;
        }
        .empty-state .icon { font-size: 48px; margin-bottom: 10px; opacity: 0.3; }

        /* Pagination styles */
        .pagination {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 10px;
            margin-top: 15px;
            padding: 15px;
        }
        .pagination button {
            padding: 6px 12px;
            border: 1px solid #ddd;
            background: white;
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
            transition: all 0.2s;
        }
        .pagination button:hover:not(:disabled) {
            background: #667eea;
            color: white;
            border-color: #667eea;
        }
        .pagination button:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }
        .pagination .page-info {
            color: #666;
            font-size: 13px;
        }
        .paginated-item { display: none; }
        .paginated-item.visible { display: block; }
    </style>
    <script>
        function setupPagination(containerId, itemsPerPage = 10) {
            const container = document.getElementById(containerId);
            if (!container) return;

            const items = Array.from(container.querySelectorAll('.paginated-item'));
            if (items.length <= itemsPerPage) {
                items.forEach(item => item.classList.add('visible'));
                return; // No pagination needed
            }

            let currentPage = 1;
            const totalPages = Math.ceil(items.length / itemsPerPage);

            const paginationDiv = document.createElement('div');
            paginationDiv.className = 'pagination';
            paginationDiv.innerHTML = `
                <button class="prev-btn">← Previous</button>
                <span class="page-info"></span>
                <button class="next-btn">Next →</button>
            `;
            container.appendChild(paginationDiv);

            const prevBtn = paginationDiv.querySelector('.prev-btn');
            const nextBtn = paginationDiv.querySelector('.next-btn');
            const pageInfo = paginationDiv.querySelector('.page-info');

            function showPage(page) {
                currentPage = page;
                const start = (page - 1) * itemsPerPage;
                const end = start + itemsPerPage;

                items.forEach((item, index) => {
                    item.classList.toggle('visible', index >= start && index < end);
                });

                pageInfo.textContent = `Page ${page} of ${totalPages} (${items.length} total)`;
                prevBtn.disabled = page === 1;
                nextBtn.disabled = page === totalPages;
            }

            prevBtn.addEventListener('click', () => showPage(currentPage - 1));
            nextBtn.addEventListener('click', () => showPage(currentPage + 1));

            showPage(1);
        }

        document.addEventListener('DOMContentLoaded', function() {
            setupPagination('failed-tests-list', 10);
            setupPagination('passed-tests-list', 20);
            setupPagination('lint-errors-list', 10);
            setupPagination('lint-warnings-list', 20);
            setupPagination('lint-info-list', 20);
            setupPagination('coverage-low-list', 20);
            setupPagination('coverage-med-list', 20);
            setupPagination('coverage-good-list', 20);
        });
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Guardian SDK - Test Report Dashboard</h1>
            <p>BUILD_DATE_PLACEHOLDER | Branch: BRANCH_PLACEHOLDER</p>
        </div>

        <div class="summary">
            <div class="summary-card PASS_CARD_CLASS">
                <h3>Tests Passed</h3>
                <div class="value">TESTS_PASSED_PLACEHOLDER/TOTAL_TESTS_PLACEHOLDER</div>
                <div class="label">PASS_RATE_PLACEHOLDER% pass rate</div>
                <div class="progress-bar">
                    <div class="progress-fill success" style="width: PASS_RATE_PLACEHOLDER%;"></div>
                </div>
            </div>

            <div class="summary-card FAIL_CARD_CLASS">
                <h3>Tests Failed</h3>
                <div class="value">TESTS_FAILED_PLACEHOLDER</div>
                <div class="label">FAIL_RATE_PLACEHOLDER% failure rate</div>
            </div>

            <div class="summary-card COVERAGE_CARD_CLASS">
                <h3>Code Coverage</h3>
                <div class="value">COVERAGE_PERCENT_PLACEHOLDER%</div>
                <div class="label">Target: 80%</div>
                <div class="progress-bar">
                    <div class="progress-fill COVERAGE_COLOR_CLASS" style="width: COVERAGE_PERCENT_PLACEHOLDER%;"></div>
                </div>
            </div>

            <div class="summary-card LINT_CARD_CLASS">
                <h3>Lint Issues</h3>
                <div class="value">TOTAL_LINT_PLACEHOLDER</div>
                <div class="label">LINT_ERRORS_PLACEHOLDER errors, LINT_WARNINGS_PLACEHOLDER warnings</div>
            </div>
        </div>

        TEST_SECTION_PLACEHOLDER

        COVERAGE_SECTION_PLACEHOLDER

        LINT_SECTION_PLACEHOLDER

        <div class="section">
            <h2 class="section-title"><span class="icon">📖</span> Understanding This Report</h2>

            <div style="background: #f8f9fa; padding: 20px; border-radius: 6px; border-left: 4px solid #667eea;">
                <h3 style="margin-bottom: 15px; color: #333;">What These Metrics Mean:</h3>
                <ul style="margin-left: 20px; color: #666; line-height: 1.8;">
                    <li style="margin-bottom: 12px;">
                        <strong>Tests Passed/Failed:</strong> Automated checks that verify the app's features work correctly.
                        A high pass rate (90%+) means the code is reliable. Failed tests indicate issues that need fixing before release.
                    </li>
                    <li style="margin-bottom: 12px;">
                        <strong>Code Coverage:</strong> The percentage of code that has automated tests.
                        Higher coverage (80%+) means more confidence that the code works as expected and reduces the risk of bugs reaching users.
                    </li>
                    <li style="margin-bottom: 12px;">
                        <strong>Lint Issues:</strong> Automated code quality checks that find potential problems, inefficiencies, or style inconsistencies.
                        Think of it like spell-check for code. These are suggestions for improvement, not critical errors.
                    </li>
                </ul>
                <div style="margin-top: 20px; padding-top: 15px; border-top: 1px solid #e0e0e0;">
                    <strong style="color: #333;">How to Use This Report:</strong>
                    <p style="margin-top: 8px; color: #666;">
                        Click on any section above to expand and see detailed information. All test failures, coverage gaps,
                        and lint issues are embedded in this single file - no need to open multiple reports.
                    </p>
                </div>
            </div>
        </div>

        <div class="footer">
            Generated by Guardian Android SDK CI/CD Pipeline<br>
            Questions? Contact the Guardian SDK team
        </div>
    </div>
</body>
</html>
EOF_TEMPLATE

  # Replace placeholders (use | as delimiter to avoid conflicts with / in branch names)
  sed -i.bak "s|BUILD_DATE_PLACEHOLDER|${current_date}|g" "$OUTPUT_HTML"
  sed -i.bak "s|BRANCH_PLACEHOLDER|${branch}|g" "$OUTPUT_HTML"
  sed -i.bak "s|TESTS_PASSED_PLACEHOLDER|${TESTS_PASSED}|g" "$OUTPUT_HTML"
  sed -i.bak "s|TESTS_FAILED_PLACEHOLDER|${TESTS_FAILED}|g" "$OUTPUT_HTML"
  sed -i.bak "s|TOTAL_TESTS_PLACEHOLDER|${total_tests}|g" "$OUTPUT_HTML"
  sed -i.bak "s|PASS_RATE_PLACEHOLDER|${pass_rate}|g" "$OUTPUT_HTML"
  sed -i.bak "s|FAIL_RATE_PLACEHOLDER|$((100 - pass_rate))|g" "$OUTPUT_HTML"
  sed -i.bak "s|COVERAGE_PERCENT_PLACEHOLDER|${COVERAGE_PERCENT}|g" "$OUTPUT_HTML"
  sed -i.bak "s|LINT_ERRORS_PLACEHOLDER|${LINT_ERRORS}|g" "$OUTPUT_HTML"
  sed -i.bak "s|LINT_WARNINGS_PLACEHOLDER|${LINT_WARNINGS}|g" "$OUTPUT_HTML"
  sed -i.bak "s|LINT_INFO_PLACEHOLDER|${LINT_INFO}|g" "$OUTPUT_HTML"

  # Set lint card color based on severity
  local lint_card="info"
  [[ $LINT_WARNINGS -gt 0 ]] && lint_card="warning"
  [[ $LINT_ERRORS -gt 0 ]] && lint_card="error"
  sed -i.bak "s|LINT_CARD_CLASS|${lint_card}|g" "$OUTPUT_HTML"
  sed -i.bak "s|TOTAL_LINT_PLACEHOLDER|${total_lint}|g" "$OUTPUT_HTML"
  sed -i.bak "s|COVERAGE_COLOR_CLASS|${coverage_color}|g" "$OUTPUT_HTML"

  # Set card classes
  local pass_card="success"
  [[ $TESTS_FAILED -gt 0 ]] && pass_card="warning"
  local fail_card="success"
  [[ $TESTS_FAILED -gt 0 ]] && fail_card="error"
  local cov_card="error"
  [[ $COVERAGE_PERCENT -ge 60 ]] && cov_card="warning"
  [[ $COVERAGE_PERCENT -ge 80 ]] && cov_card="success"

  sed -i.bak "s|PASS_CARD_CLASS|${pass_card}|g" "$OUTPUT_HTML"
  sed -i.bak "s|FAIL_CARD_CLASS|${fail_card}|g" "$OUTPUT_HTML"
  sed -i.bak "s|COVERAGE_CARD_CLASS|${cov_card}|g" "$OUTPUT_HTML"

  # Generate test section
  generate_test_section

  # Generate coverage section
  generate_coverage_section

  # Generate lint section
  generate_lint_section

  # Clean up backup files
  rm -f "${OUTPUT_HTML}.bak"
}

# Generate test results section
generate_test_section() {
  if [[ $((TESTS_PASSED + TESTS_FAILED)) -eq 0 ]]; then
    local empty='<div class="section"><h2 class="section-title"><span class="icon">🧪</span> Test Results</h2><div class="empty-state"><div class="icon">📝</div><p>No test results available</p></div></div>'
    sed -i.bak "s|TEST_SECTION_PLACEHOLDER|${empty}|g" "$OUTPUT_HTML"
    return
  fi

  local section='<div class="section"><h2 class="section-title"><span class="icon">🧪</span> Test Results</h2>'

  # Failed tests
  if [[ $TESTS_FAILED -gt 0 ]]; then
    section+="<details open><summary><span>Failed Tests (${TESTS_FAILED})</span><span class=\"badge error\">View Details</span></summary><div class=\"details-content\" id=\"failed-tests-list\">"

    while IFS='|' read -r name message location; do
      [[ -z "$name" ]] && continue
      section+="<div class=\"test-item failed paginated-item\"><div class=\"item-title\">❌ ${name}</div><div class=\"item-detail\">${message}</div><div class=\"item-location\">at ${location}</div></div>"
    done <<< "$FAILED_TESTS"

    section+='</div></details>'
  fi

  # Passed tests
  if [[ $TESTS_PASSED -gt 0 ]]; then
    section+="<details><summary><span>Passed Tests (${TESTS_PASSED})</span><span class=\"badge success\">View Details</span></summary><div class=\"details-content\" id=\"passed-tests-list\">"

    while IFS='|' read -r name location; do
      [[ -z "$name" ]] && continue
      section+="<div class=\"test-item passed paginated-item\"><div class=\"item-title\">✅ ${name}</div><div class=\"item-location\">at ${location}</div></div>"
    done <<< "$PASSED_TESTS"

    section+='</div></details>'
  fi

  section+='</div>'

  # Escape special characters for sed
  section=$(echo "$section" | sed 's/[&/\]/\\&/g')
  sed -i.bak "s|TEST_SECTION_PLACEHOLDER|${section}|g" "$OUTPUT_HTML"
}

# Generate coverage section
generate_coverage_section() {
  local cov_card="error"
  [[ $COVERAGE_PERCENT -ge 60 ]] && cov_card="warning"
  [[ $COVERAGE_PERCENT -ge 80 ]] && cov_card="success"

  local coverage_color="error"
  [[ $COVERAGE_PERCENT -ge 80 ]] && coverage_color="success"
  [[ $COVERAGE_PERCENT -ge 60 && $COVERAGE_PERCENT -lt 80 ]] && coverage_color="warning"

  if [[ $COVERAGE_PERCENT -eq 0 ]]; then
    local empty='<div class="section"><h2 class="section-title"><span class="icon">📊</span> Code Coverage</h2><div class="empty-state"><div class="icon">📈</div><p>No coverage data available</p></div></div>'
    sed -i.bak "s|COVERAGE_SECTION_PLACEHOLDER|${empty}|g" "$OUTPUT_HTML"
    return
  fi

  local section='<div class="section"><h2 class="section-title"><span class="icon">📊</span> Code Coverage</h2>'

  # Overall summary row — static, not expandable
  section+="<div class=\"details-content\" style=\"padding: 12px 0;\">"
  section+="<div class=\"coverage-item\"><div class=\"item-title\" style=\"display:flex;justify-content:space-between;\"><span>Overall Coverage</span><span class=\"badge ${cov_card}\">${COVERAGE_PERCENT}%</span></div><div class=\"progress-bar\" style=\"margin-top:8px;\"><div class=\"progress-fill ${coverage_color}\" style=\"width: ${COVERAGE_PERCENT}%;\"></div></div></div>"
  section+='</div>'

  # Per-file breakdown (files needing coverage, sorted lowest first)
  if [[ -n "$COVERAGE_DETAILS" ]]; then
    local low_count=0 med_count=0
    while IFS='|' read -r filename pct; do
      [[ -z "$filename" ]] && continue
      [[ $pct -lt 60 ]] && low_count=$((low_count + 1))
      [[ $pct -ge 60 && $pct -lt 80 ]] && med_count=$((med_count + 1))
    done <<< "$COVERAGE_DETAILS"

    if [[ $low_count -gt 0 ]]; then
      section+="<details open><summary><span>Needs Coverage (${low_count})</span><span class=\"badge error\">Below 60%</span></summary><div class=\"details-content\" id=\"coverage-low-list\">"
      while IFS='|' read -r filename pct; do
        [[ -z "$filename" ]] && continue
        [[ $pct -ge 60 ]] && continue
        section+="<div class=\"coverage-item paginated-item\"><div class=\"item-title\">❌ ${filename}</div><div class=\"item-detail\">${pct}% coverage</div><div class=\"progress-bar\"><div class=\"progress-fill error\" style=\"width: ${pct}%;\"></div></div></div>"
      done <<< "$COVERAGE_DETAILS"
      section+='</div></details>'
    fi

    if [[ $med_count -gt 0 ]]; then
      section+="<details open><summary><span>Improve Coverage (${med_count})</span><span class=\"badge warning\">60–79%</span></summary><div class=\"details-content\" id=\"coverage-med-list\">"
      while IFS='|' read -r filename pct; do
        [[ -z "$filename" ]] && continue
        [[ $pct -lt 60 || $pct -ge 80 ]] && continue
        section+="<div class=\"coverage-item paginated-item\"><div class=\"item-title\">⚠️ ${filename}</div><div class=\"item-detail\">${pct}% coverage</div><div class=\"progress-bar\"><div class=\"progress-fill warning\" style=\"width: ${pct}%;\"></div></div></div>"
      done <<< "$COVERAGE_DETAILS"
      section+='</div></details>'
    fi

    local good_count=0
    while IFS='|' read -r filename pct; do
      [[ -z "$filename" ]] && continue
      [[ $pct -ge 80 ]] && good_count=$((good_count + 1))
    done <<< "$COVERAGE_DETAILS"

    if [[ $good_count -gt 0 ]]; then
      section+="<details><summary><span>Good Coverage (${good_count})</span><span class=\"badge success\">80%+</span></summary><div class=\"details-content\" id=\"coverage-good-list\">"
      while IFS='|' read -r filename pct; do
        [[ -z "$filename" ]] && continue
        [[ $pct -lt 80 ]] && continue
        section+="<div class=\"coverage-item paginated-item\"><div class=\"item-title\">✅ ${filename}</div><div class=\"item-detail\">${pct}% coverage</div><div class=\"progress-bar\"><div class=\"progress-fill success\" style=\"width: ${pct}%;\"></div></div></div>"
      done <<< "$COVERAGE_DETAILS"
      section+='</div></details>'
    fi
  fi

  section+='</div>'

  section=$(echo "$section" | sed 's/[&/\]/\\&/g')
  sed -i.bak "s|COVERAGE_SECTION_PLACEHOLDER|${section}|g" "$OUTPUT_HTML"
}

# Generate lint section
generate_lint_section() {
  local total_lint=$((LINT_WARNINGS + LINT_ERRORS + LINT_INFO))

  if [[ $total_lint -eq 0 ]]; then
    local empty='<div class="section"><h2 class="section-title"><span class="icon">🔍</span> Lint Issues</h2><div class="empty-state"><div class="icon">✨</div><p>No lint issues found</p></div></div>'
    sed -i.bak "s|LINT_SECTION_PLACEHOLDER|${empty}|g" "$OUTPUT_HTML"
    return
  fi

  local section='<div class="section"><h2 class="section-title"><span class="icon">🔍</span> Lint Issues</h2>'

  # Show errors first
  if [[ $LINT_ERRORS -gt 0 ]]; then
    section+="<details open><summary><span>Errors (${LINT_ERRORS})</span><span class=\"badge error\">Must Fix</span></summary><div class=\"details-content\" id=\"lint-errors-list\">"

    while IFS='|' read -r message severity location; do
      [[ -z "$message" ]] && continue
      [[ "${severity,,}" != "error" ]] && continue
      section+="<div class=\"lint-item error paginated-item\"><div class=\"item-title\">❌ ${message}</div><div class=\"item-location\">${location}</div></div>"
    done <<< "$LINT_DETAILS"

    section+='</div></details>'
  fi

  # Show warnings
  if [[ $LINT_WARNINGS -gt 0 ]]; then
    section+="<details><summary><span>Warnings (${LINT_WARNINGS})</span><span class=\"badge warning\">Review Suggested</span></summary><div class=\"details-content\" id=\"lint-warnings-list\">"

    while IFS='|' read -r message severity location; do
      [[ -z "$message" ]] && continue
      [[ "${severity,,}" != "warning" ]] && continue
      section+="<div class=\"lint-item warning paginated-item\"><div class=\"item-title\">⚠️ ${message}</div><div class=\"item-location\">${location}</div></div>"
    done <<< "$LINT_DETAILS"

    section+='</div></details>'
  fi

  # Show info/hints
  if [[ $LINT_INFO -gt 0 ]]; then
    section+="<details><summary><span>Info (${LINT_INFO})</span><span class=\"badge info\">Optional</span></summary><div class=\"details-content\" id=\"lint-info-list\">"

    while IFS='|' read -r message severity location; do
      [[ -z "$message" ]] && continue
      [[ "${severity,,}" != "information" && "${severity,,}" != "hint" ]] && continue
      section+="<div class=\"lint-item paginated-item\"><div class=\"item-title\">ℹ️ ${message}</div><div class=\"item-location\">${location}</div></div>"
    done <<< "$LINT_DETAILS"

    section+='</div></details>'
  fi

  section+='</div>'

  section=$(echo "$section" | sed 's/[&/\]/\\&/g')
  sed -i.bak "s|LINT_SECTION_PLACEHOLDER|${section}|g" "$OUTPUT_HTML"
}

# Clean up detailed HTML reports
cleanup_detailed_reports() {
  echo "Cleaning up detailed reports (keeping only reports.html)..."
  # Remove all detailed HTML reports
  rm -rf guardian/build/reports/tests/ 2>/dev/null || true
  rm -rf guardian/build/reports/jacoco/ 2>/dev/null || true
  # Remove all lint XML/TXT/HTML files (data is now in reports.html)
  rm -f guardian/build/reports/lint-results-*.* 2>/dev/null || true
}

# Main execution
main() {
  # Check what was actually requested (from environment variables)
  local do_test="${GUARDIAN_DO_TEST:-true}"
  local do_coverage="${GUARDIAN_DO_COVERAGE:-true}"
  local do_lint="${GUARDIAN_DO_LINT:-true}"

  if [[ "$do_test" == "true" ]]; then
    echo "Parsing test results..."
    parse_test_results
  else
    echo "Skipping test results (--notest specified)"
    TESTS_PASSED=0
    TESTS_FAILED=0
  fi

  if [[ "$do_coverage" == "true" ]]; then
    echo "Parsing coverage data..."
    parse_coverage
  else
    echo "Skipping coverage data (--nocoverage specified)"
    COVERAGE_PERCENT=0
  fi

  if [[ "$do_lint" == "true" ]]; then
    echo "Parsing lint results..."
    parse_lint
  else
    echo "Skipping lint results (--nolint specified)"
    LINT_WARNINGS=0
    LINT_ERRORS=0
    LINT_INFO=0
  fi

  echo "Generating unified dashboard..."
  generate_html

  cleanup_detailed_reports

  echo "✅ Dashboard generated: $OUTPUT_HTML"
  echo "   Tests: $TESTS_PASSED passed, $TESTS_FAILED failed"
  echo "   Coverage: ${COVERAGE_PERCENT}%"
  echo "   Lint: ${LINT_WARNINGS} warnings, ${LINT_ERRORS} errors"
}

main "$@"