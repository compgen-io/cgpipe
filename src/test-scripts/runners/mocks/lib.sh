# Shared helpers for runner-test mock scheduler binaries.
#
# Every mock binary sources this file and uses capture_call to record what
# CGPipe sent it (argv, stdin, selected env vars). The harness then diffs
# the captured artifacts against committed expected/ fixtures.
#
# Mocks rely on three env vars set by runner-test.sh:
#   CGPIPE_TEST_CAPTURE     directory where capture-* files are written
#   CGPIPE_TEST_RESPONSES   directory of canned responses for status mocks
#                           (optional; per-test fixture override)
#   CGPIPE_TEST_JOBID_BASE  starting jobid (default 10001)

set -u

# Java's Runtime.exec(cmd, env) replaces — not augments — the subprocess
# environment when env is non-null (BatchQ does this whenever batchqhome is
# set). That strips CGPIPE_TEST_CAPTURE, so we fall back to ./capture/ which
# is reliable because the harness chdirs to the workdir before running cgpipe.
if [ -z "${CGPIPE_TEST_CAPTURE:-}" ]; then
    if [ -d "./capture" ]; then
        CGPIPE_TEST_CAPTURE="$(pwd)/capture"
    else
        echo "lib.sh: CGPIPE_TEST_CAPTURE is unset and no ./capture/ found" >&2
        exit 2
    fi
fi
mkdir -p "$CGPIPE_TEST_CAPTURE"

# Capture a call to a mock binary.
#
#   capture_call <kind> "$@"
#
# kind is one of: submit, status, release, cancel.
# Writes three files into $CGPIPE_TEST_CAPTURE keyed by an ordinal counter
# per kind:
#   <kind>-<N>.argv   one arg per line, argv[0] first
#   <kind>-<N>.stdin  raw stdin
#   <kind>-<N>.env    sorted, filtered env (BATCHQ_HOME, CGPIPE_*, etc.)
capture_call() {
    local kind="$1"; shift
    local seq_file="${CGPIPE_TEST_CAPTURE}/.seq.${kind}"
    local n
    n=$(( $(cat "$seq_file" 2>/dev/null || echo 0) + 1 ))
    echo "$n" > "$seq_file"
    LAST_SEQ="$n"
    local stem="${CGPIPE_TEST_CAPTURE}/${kind}-${n}"

    # argv: one element per line so diffs are readable
    {
        printf '%s\n' "${MOCK_NAME:-$(basename "$0")}"
        local a
        for a in "$@"; do
            printf '%s\n' "$a"
        done
    } > "${stem}.argv"

    # Only "submit" mocks actually receive stdin from cgpipe (the rendered
    # script piped to qsub/sbatch/etc.). For other kinds we deliberately skip
    # writing a .stdin file — reading from stdin would block, and the absence
    # of the file is meaningful (no stdin was passed).
    if [ "$kind" = "submit" ]; then
        cat > "${stem}.stdin"
    fi

    # Curated env: only the vars CGPipe might set on a subprocess.
    {
        env | grep -E '^(BATCHQ_HOME|CGPIPE_)=' | sort || true
    } > "${stem}.env"
}

# Emit the next deterministic jobid. Counter persists in CGPIPE_TEST_CAPTURE
# so it survives across multiple mock invocations in a single test run.
#
#   MOCK_JOBID_FORMAT (env, optional): printf-style format string applied to
#       the numeric id. Default "%d". PBS tests set "%d.cluster1" for dotted.
next_jobid() {
    local f="${CGPIPE_TEST_CAPTURE}/.jobid.seq"
    local base="${CGPIPE_TEST_JOBID_BASE:-10001}"
    local n
    if [ -s "$f" ]; then
        n=$(( $(cat "$f") + 1 ))
    else
        n=$base
    fi
    echo "$n" > "$f"
    # shellcheck disable=SC2059
    printf "${MOCK_JOBID_FORMAT:-%d}" "$n"
}

# If env requests it, exit non-zero from the current mock call. Used to drive
# the abort/cancel code path. Call with the kind and the LAST_SEQ ordinal,
# e.g.:
#
#   capture_call submit "$@"
#   maybe_fail submit "$LAST_SEQ"
#   next_jobid; echo
#
# Env vars: CGPIPE_MOCK_FAIL_<KIND>_AT=<N> — fail on the N-th call of <kind>.
maybe_fail() {
    local kind="$1"
    local n="$2"
    local var="CGPIPE_MOCK_FAIL_$(echo "$kind" | tr '[:lower:]' '[:upper:]')_AT"
    local fail_at="${!var:-}"
    if [ -n "$fail_at" ] && [ "$fail_at" = "$n" ]; then
        echo "mock: forced failure on $kind #$n" >&2
        exit 1
    fi
}

# For status mocks: print a canned response from
# $CGPIPE_TEST_RESPONSES/<tool>/<jobid>. Exits non-zero if no canned
# response exists (mirrors a scheduler that doesn't know the job).
#
#   serve_response <tool> <jobid>
serve_response() {
    local tool="$1"
    local jobid="$2"
    local path="${CGPIPE_TEST_RESPONSES:-}/${tool}/${jobid}"
    if [ -n "${CGPIPE_TEST_RESPONSES:-}" ] && [ -f "$path" ]; then
        cat "$path"
        return 0
    fi
    return 1
}
