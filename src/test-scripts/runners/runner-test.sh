#!/bin/bash
# runner-test.sh — driver for TemplateRunner integration tests.
#
# A test consists of:
#   src/test-scripts/runners/<scheduler>/<name>.mvpt
#   src/test-scripts/runners/<scheduler>/<name>.expected/
#       stdout              # what cgpipe prints (jobid list)
#       rc                  # expected cgpipe exit code (defaults to 0 if missing)
#       submit-1.argv       # qsub/sbatch/... argv (one element per line)
#       submit-1.stdin      # full rendered submission script
#       submit-1.env        # filtered env (BATCHQ_HOME, CGPIPE_*)
#       submit-2.{argv,stdin,env}    (one trio per submitted job)
#       release-1.argv      # if global_hold caused releases
#       cancel-1.argv       # if abort() ran
#       ...
#   src/test-scripts/runners/<scheduler>/<name>.responses/   (optional)
#       qstat/<jobid>       # canned status responses
#       scontrol/<jobid>
#       batchq/<jobid>
#   src/test-scripts/runners/<scheduler>/<name>.joblog       (optional)
#       pre-seeded joblog file for resume tests
#   src/test-scripts/runners/<scheduler>/<name>.env.sh       (optional)
#       sourced before cgpipe runs; can set CGPIPE_MOCK_FAIL_SUBMIT_AT etc.
#
# Usage:
#   ./runner-test.sh                         # run every *.mvpt under runners/<sched>/
#   ./runner-test.sh path/to/test.mvpt       # run one
#   ./runner-test.sh -v ...                  # show diffs on failure
#   ./runner-test.sh -k ...                  # keep workdir after run (for debugging)
#
# Env overrides:
#   CGPIPE_BIN     path to cgpipe (default: dist/cgpipe).
#                  Set to the future Go binary to validate parity.

set -u

VERBOSE=""
KEEP_WORKDIR=""
while [ $# -gt 0 ]; do
    case "$1" in
        -v|-vv|-vvv) VERBOSE="$1"; shift ;;
        -k) KEEP_WORKDIR=1; shift ;;
        --) shift; break ;;
        -*) echo "runner-test.sh: unknown flag: $1" >&2; exit 2 ;;
        *) break ;;
    esac
done

REPO_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
RUNNERS_DIR="$REPO_ROOT/src/test-scripts/runners"
MOCKS_ROOT="$RUNNERS_DIR/mocks"
CGPIPE_BIN="${CGPIPE_BIN:-$REPO_ROOT/dist/cgpipe}"
WORKDIR_ROOT="$REPO_ROOT/test/runner"

if [ ! -x "$CGPIPE_BIN" ]; then
    echo "runner-test.sh: $CGPIPE_BIN not found or not executable" >&2
    echo "Run 'ant jar' (or set CGPIPE_BIN=...)" >&2
    exit 2
fi

# Bulk mode: walk every .mvpt under runners/<scheduler>/
if [ $# -eq 0 ]; then
    rm -f "$WORKDIR_ROOT/.err"
    pass=0
    fail=0
    while IFS= read -r -d '' t; do
        if "$0" ${VERBOSE:+$VERBOSE} ${KEEP_WORKDIR:+-k} "$t"; then
            pass=$((pass + 1))
        else
            fail=$((fail + 1))
            mkdir -p "$WORKDIR_ROOT"
            touch "$WORKDIR_ROOT/.err"
        fi
    done < <(find "$RUNNERS_DIR" -mindepth 2 -name '*.mvpt' -print0 | sort -z)

    echo
    echo "runner-tests: $pass passed, $fail failed"
    if [ -f "$WORKDIR_ROOT/.err" ]; then
        rm -f "$WORKDIR_ROOT/.err"
        exit 1
    fi
    exit 0
fi

# Single-test mode.
test_file="$1"
if [ ! -f "$test_file" ]; then
    echo "runner-test.sh: not found: $test_file" >&2
    exit 2
fi
test_file="$(cd "$(dirname "$test_file")" && pwd)/$(basename "$test_file")"

# Derive scheduler from parent directory name; expected dir lives alongside.
scheduler="$(basename "$(dirname "$test_file")")"
base="$(basename "${test_file%.mvpt}")"
expected_dir="${test_file%.mvpt}.expected"
responses_dir="${test_file%.mvpt}.responses"
joblog_seed="${test_file%.mvpt}.joblog"
env_script="${test_file%.mvpt}.env.sh"

if [ ! -d "$MOCKS_ROOT/$scheduler" ]; then
    echo "runner-test.sh: no mock bin dir for scheduler '$scheduler' at $MOCKS_ROOT/$scheduler" >&2
    exit 2
fi
if [ ! -d "$expected_dir" ]; then
    echo "runner-test.sh: missing fixture dir: $expected_dir" >&2
    exit 2
fi

workdir="$WORKDIR_ROOT/$scheduler/$base"
rm -rf "$workdir"
mkdir -p "$workdir/capture"

# Per-test .cgpiperc pinning the scheduler choice and deterministic defaults.
{
    echo "cgpipe.runner = \"$scheduler\""
    echo 'job.wd = "/cgpipe-test"'
    echo 'job.shell = "/bin/bash"'
    # BatchQ needs an absolute path to the mock when batchqhome is in play:
    # Java's Runtime.exec(cmd, env) replaces env entirely (no PATH inherited)
    # when getSubCommandEnv() returns non-null, so the binary lookup must not
    # rely on PATH. Mocks set MOCK_NAME so the captured argv[0] stays as
    # plain "batchq", keeping fixtures portable.
    if [ "$scheduler" = "batchq" ]; then
        echo "cgpipe.runner.batchq.path = \"$MOCKS_ROOT/batchq/batchq\""
    fi
} > "$workdir/.cgpiperc"

# Seed joblog if the test provides one.
if [ -f "$joblog_seed" ]; then
    cp "$joblog_seed" "$workdir/joblog.txt"
    echo "cgpipe.joblog = \"joblog.txt\"" >> "$workdir/.cgpiperc"
fi

export PATH="$MOCKS_ROOT/$scheduler:$PATH"
export CGPIPE_TEST_CAPTURE="$workdir/capture"
if [ -d "$responses_dir" ]; then
    export CGPIPE_TEST_RESPONSES="$responses_dir"
else
    unset CGPIPE_TEST_RESPONSES 2>/dev/null || true
fi
# Stable jobid base so deps are deterministic across test runs.
export CGPIPE_TEST_JOBID_BASE="${CGPIPE_TEST_JOBID_BASE:-10001}"

# Optional per-test env (e.g. CGPIPE_MOCK_FAIL_SUBMIT_AT=2).
if [ -f "$env_script" ]; then
    # shellcheck disable=SC1090
    . "$env_script"
fi

# Run cgpipe from the workdir so its CWD is stable. Use CGPIPE_HOME so the
# user's ~/.cgpiperc never bleeds in.
(
    cd "$workdir"
    CGPIPE_HOME="$workdir" "$CGPIPE_BIN" -nolog -f "$test_file" \
        > stdout.actual 2> stderr.actual
)
rc=$?
echo "$rc" > "$workdir/rc.actual"

# Compare fixtures. expected/<f> maps to:
#   stdout, stderr, rc -> $workdir/{stdout,stderr,rc}.actual
#   *                  -> $workdir/capture/<f>
fail=0
diffs=""
while IFS= read -r -d '' f; do
    rel="${f#"$expected_dir"/}"
    case "$rel" in
        stdout|stderr|rc)
            actual="$workdir/${rel}.actual"
            ;;
        *)
            actual="$workdir/capture/$rel"
            ;;
    esac

    if [ ! -f "$actual" ]; then
        fail=1
        diffs+=$'\n'"[MISSING] expected file not produced: capture/$rel"
        continue
    fi
    if ! diff_out="$(diff -u "$f" "$actual")"; then
        fail=1
        diffs+=$'\n'"[DIFF] $rel"$'\n'"$diff_out"
    fi
done < <(find "$expected_dir" -type f -print0 | sort -z)

# If no expected/rc fixture is provided, default-assert rc==0. Otherwise the
# fixture comparison above has already handled it.
if [ ! -f "$expected_dir/rc" ] && [ "$rc" -ne 0 ]; then
    fail=1
    diffs+=$'\n'"[RC] cgpipe exited $rc (expected 0; pin with expected/rc to allow)"
fi

# Surface unexpected captures so silent regressions don't sneak by.
while IFS= read -r -d '' f; do
    rel="${f#"$workdir/capture/"}"
    case "$rel" in
        .seq.*|.jobid.seq) continue ;;
    esac
    if [ ! -f "$expected_dir/$rel" ]; then
        fail=1
        diffs+=$'\n'"[UNEXPECTED] capture/$rel not in expected/"
    fi
done < <(find "$workdir/capture" -type f -print0 | sort -z)

label="$(realpath --relative-to="$REPO_ROOT" "$test_file" 2>/dev/null || echo "$test_file")"
if [ $fail -eq 0 ]; then
    echo "$label OK"
    [ -z "$KEEP_WORKDIR" ] && rm -rf "$workdir"
    exit 0
else
    echo "$label ERROR (cgpipe rc=$rc)"
    if [ -n "$VERBOSE" ]; then
        echo "$diffs"
        echo "[stderr]"
        sed 's/^/  | /' "$workdir/stderr.actual"
        echo "[workdir] $workdir"
    fi
    exit 1
fi
