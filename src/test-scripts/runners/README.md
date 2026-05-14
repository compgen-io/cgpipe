# Runner Tests

Integration tests for the `TemplateRunner` family — `SGETemplateRunner`,
`SLURMTemplateRunner`, `PBSTemplateRunner`, `BatchQTemplateRunner`. The tests
live outside Java so they can be re-used as a parity-check when CGPipe is
ported to Go: set `CGPIPE_BIN=/path/to/cgpipe-go` and run the same suite.

## How a test runs

The harness (`runner-test.sh`) for each test:

1. Creates a fresh `test/runner/<scheduler>/<name>/` workdir.
2. Prepends `mocks/<scheduler>/` to `PATH` so cgpipe's `qsub` / `sbatch` /
   `batchq` / etc. resolves to a mock bash script.
3. Writes a `.cgpiperc` that pins:
   - `cgpipe.runner = "<scheduler>"`
   - `job.wd = "/cgpipe-test"` (avoids a non-deterministic `getCanonicalPath` call)
   - `job.shell = "/bin/bash"`
   - For batchq, also `cgpipe.runner.batchq.path = "<abs>/batchq"` (needed
     because BatchQ replaces — not augments — the subprocess env, dropping `PATH`).
4. Optionally seeds the joblog from `<name>.joblog` and per-test status
   responses from `<name>.responses/<tool>/<jobid>`.
5. Optionally sources `<name>.env.sh` so tests can set
   `CGPIPE_MOCK_FAIL_SUBMIT_AT=N` to force the Nth submit call to fail.
6. Runs cgpipe from the workdir, capturing stdout/stderr/exit-code.
7. Diffs every file under `<name>.expected/` against the captured equivalent
   (`stdout`, `stderr`, `rc` map to the per-run actuals; everything else maps
   to `capture/`). Flags any capture file not listed in `expected/`.

## Mock contract

Every mock binary sources `mocks/lib.sh`, which provides:

- `capture_call <kind> "$@"` — write argv (one element per line), stdin (only
  for `submit`), and a filtered env snapshot to
  `${CGPIPE_TEST_CAPTURE}/<kind>-<N>.{argv,stdin,env}`. Sets `LAST_SEQ` so the
  caller can pass it to `maybe_fail`.
- `next_jobid` — increment a monotonic counter and print the new id (default
  base 10001). `MOCK_JOBID_FORMAT` (env) lets PBS emit dotted ids like
  `10001.cluster1`.
- `serve_response <tool> <jobid>` — print the canned response from
  `${CGPIPE_TEST_RESPONSES}/<tool>/<jobid>` if present, otherwise exit 1. Used
  by status mocks to drive `isJobIdValid` outcomes.
- `maybe_fail <kind> <n>` — if `CGPIPE_MOCK_FAIL_<KIND>_AT=<n>` is set, exit
  non-zero. Used by the `06-abort-cancels` tests.

`MOCK_NAME` is set in each mock so the captured argv[0] is the plain command
name (`qsub`, `batchq`, …) even when cgpipe invoked it via absolute path.

## Adding a new test

1. Create `<scheduler>/<NN>-<short-name>.mvpt`. Wrap job-setting assignments
   in `<% %>` blocks; bare lines in a target body are interpreted as shell.
2. (Optional) Create `<NN>-<short-name>.env.sh` to set env vars consumed by
   the mocks.
3. (Optional) For resume tests, drop a `<NN>-<short-name>.joblog` (plain
   joblog format: `<jobid>\tOUTPUT\t<filename>` etc.) and matching
   `<NN>-<short-name>.responses/<tool>/<jobid>` files.
4. Run the test once with `-k` to capture artifacts:
   ```bash
   mkdir -p src/test-scripts/runners/<sched>/<NN>-<name>.expected
   ./src/test-scripts/runners/runner-test.sh -k src/test-scripts/runners/<sched>/<NN>-<name>.mvpt
   ```
5. Inspect `test/runner/<sched>/<NN>-<name>/capture/` to verify the captured
   submission script and argv look right.
6. Copy the captures into `<NN>-<name>.expected/`:
   ```bash
   cp test/runner/<sched>/<NN>-<name>/stdout.actual <NN>-<name>.expected/stdout
   cp test/runner/<sched>/<NN>-<name>/capture/*.argv \
      test/runner/<sched>/<NN>-<name>/capture/*.stdin \
      test/runner/<sched>/<NN>-<name>/capture/*.env \
      <NN>-<name>.expected/
   ```
   If the test expects a non-zero cgpipe exit code (e.g. an abort test),
   also copy `rc.actual` to `<NN>-<name>.expected/rc`.
7. Run without `-k` to confirm the test now passes:
   ```bash
   ./src/test-scripts/runners/runner-test.sh src/test-scripts/runners/<sched>/<NN>-<name>.mvpt
   ```

## Invocation

```bash
# Bulk: every *.mvpt under runners/<sched>/
./src/test-scripts/runners/runner-test.sh

# Single test
./src/test-scripts/runners/runner-test.sh src/test-scripts/runners/sge/04-dependencies.mvpt

# With unified diffs on failure
./src/test-scripts/runners/runner-test.sh -v ...

# Keep the workdir after a successful run (useful when adding new fixtures)
./src/test-scripts/runners/runner-test.sh -k ...

# Validate a future Go port against the same fixtures
CGPIPE_BIN=/path/to/cgpipe-go ./src/test-scripts/runners/runner-test.sh
```

`./test.sh` (the top-level harness) invokes `runner-test.sh` after the
language tests, so a single command exercises both layers.
