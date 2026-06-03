# Configuration reference

This chapter is the exhaustive list of everything CGPipe reads or writes by name: configuration variables, environment variables, command-line flags, and the order they're resolved in.

For per-job submission settings (`job.mem`, `job.walltime`, …) see [Running Jobs](07-Running_Jobs.md). This chapter focuses on the `cgpipe.*` namespace and the surrounding plumbing.

## Configuration sources, in resolution order

CGPipe assembles its run-time settings from several places. Later sources override earlier ones for the same variable:

1. **Built-in defaults** — bundled `cgpiperc` resource inside the JAR.
2. **System config** — `/etc/cgpiperc`.
3. **Install-local config** — `$CGPIPE_HOME/.cgpiperc` (where `CGPIPE_HOME` defaults to the directory containing the `cgpipe` binary).
4. **User config** — `~/.cgpiperc`.
5. **Environment variables** — `CGPIPE_ENV` is evaluated as a CGPipe expression; individual env vars (`CGPIPE_RUN_ID`, `CGPIPE_DRYRUN`) are read here.
6. **Command-line variables** — `-name value` pairs and `-t name=value` overrides.
7. **The pipeline script itself** — any `=`, `?=` or `+=` at the top of the script.

This means a setting in `~/.cgpiperc` beats one in `/etc/cgpiperc`, an env var beats both, and the command line beats everything. Inside the script, `?=` respects upstream overrides (only sets if unset); `=` always wins.

Each `.cgpiperc` file is itself a CGPipe script, so you can use `if`, variable substitution, and includes inside it:

    # ~/.cgpiperc
    if $(hostname) == "submit01.cluster"
        cgpipe.runner = "slurm"
        cgpipe.runner.slurm.account = "lab-allocation"
    else
        cgpipe.runner = "shell"
    endif

## Environment variables

| Variable | Effect |
|----------|--------|
| `CGPIPE_HOME` | Where install-local `.cgpiperc` is searched. Defaults to the directory containing the `cgpipe` binary. |
| `CGPIPE_ENV` | Evaluated as CGPipe code at startup (semicolon-separated). Use it to inject settings without touching a config file. |
| `CGPIPE_RUN_ID` | Sets `cgpipe.run_id` to the given string. Used by the BatchQ runner to tag every submitted job from this invocation. |
| `CGPIPE_DRYRUN` | When set (non-empty), enables dry-run mode. Equivalent to passing `-dr`. |

Example using `CGPIPE_ENV`:

    $ CGPIPE_ENV='cgpipe.loglevel = 3; job.mail = "me@example.com"' cgpipe pipeline.cgp

## Command-line flags

A subset (run `cgpipe -h` for the full list):

| Flag | Effect |
|------|--------|
| `-f file` | Run this pipeline file. (`cgpipe file` works too.) |
| `-h` | Show the pipeline's help text. |
| `-v`, `-vv`, `-vvv` | Increase verbosity. |
| `-l file` | Write CGPipe's log to `file` (also settable with `cgpipe.log`). |
| `-s` | Silent — suppress `print` output. |
| `-dr` | Dry run — render scripts to stdout, don't submit. |
| `-t name=value` | Set the named variable from the command line. |
| `-name value` | Set the named variable (same as `-t name=value`, slightly looser). |
| `-nolog` | Disable log-file writing for this run. |

Anything after `--` is passed through to the pipeline as arguments.

## `cgpipe.*` variables

Settings under the `cgpipe.*` namespace affect CGPipe itself (logging, runner choice, behavior flags). Some are runner-specific (`cgpipe.runner.<name>.*`); those are described in [Running Jobs](07-Running_Jobs.md).

### Logging and tracking

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.log` | string | Log file path. Also settable with `-l`. |
| `cgpipe.loglevel` | int | Verbosity level (0=quiet, 3=trace). Also settable with `-v` flags. |
| `cgpipe.joblog` | string | Path to the joblog file. Enables persistent job tracking across runs. |
| `cgpipe.run_id` | string | Identifier for the current pipeline run. Typically set via `CGPIPE_RUN_ID`. Used by the BatchQ runner. |

### Runner selection

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.runner` | string | Runner name: `shell`, `sge`, `slurm`, `pbs`, `batchq`, `graphviz`. |
| `cgpipe.runner.<name>.<setting>` | varies | Runner-specific. See [Running Jobs](07-Running_Jobs.md). |
| `cgpipe.runner.include_output_filenames` | bool | Print output filenames alongside job ids on submission. |

### Shell and execution

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.shell` | string | Default shell binary for rendered job bodies. Lookup falls back to `/bin/bash`, `/usr/bin/bash`, `/usr/local/bin/bash`, `/bin/sh`. |
| `cgpipe.dryrun` | bool | Set automatically when `-dr` or `CGPIPE_DRYRUN` is in effect. Readable from a pipeline. |

### Behavior flags

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.ignore_missing_inputs` | bool | When `true`, missing input dependencies don't error — useful for partial-rebuild scenarios where some inputs exist only in the joblog. |

### Remote pipelines

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.remote.<shortname>.baseurl` | string | Base URL for a named remote. See [Remote Pipelines](09-Remote_Pipelines.md). |

### Read-only system variables

These are exposed to your script but should not be set by it. They reflect the run-time environment.

| Variable | Type | Purpose |
|----------|------|---------|
| `cgpipe.sys.cwd` | string | Working directory when CGPipe started. |
| `cgpipe.sys.scriptname` | string | Name of the script that was invoked. |
| `cgpipe.sys.curfile` | string | Path of the file currently being parsed (changes inside `include`s). |
| `cgpipe.sys.curhash` | string | SHA-1 hash of `cgpipe.sys.curfile`. |
| `cgpipe.current.filename` | string | Alias for `cgpipe.sys.curfile`. |
| `cgpipe.current.hash` | string | Alias for `cgpipe.sys.curhash`. |
| `cgpipe.procs` | int | Number of CPUs available to the JVM. Useful as a default for `job.procs` when running on a single host. |
| `cgpipe.tmpfiles` | list | Temporary output files declared (via `^`) anywhere in the pipeline. Populated as targets are defined. |
| `cgpipe.outputfiles` | list | All declared output files. Same population semantics as `cgpipe.tmpfiles`. |

`cgpipe.sys.curhash` is mostly interesting for remote pipelines (where you may want to log or pin a hash) — see [Remote Pipelines](09-Remote_Pipelines.md).

## `job.*` variables

The full table is in [Running Jobs](07-Running_Jobs.md#job-settings). The short summary:

- Set globally to apply defaults to every target.
- Override inside a target's `<% %>` block to scope per-target.
- A handful (`job.shexec`, `job.nopre`, `job.nopost`) are flags rather than scheduler directives — they control how CGPipe assembles the job, not what it asks the scheduler for.

### Reserved internal names

These are populated by CGPipe when rendering a job template. Don't set them yourself — they exist so custom templates can read them.

| Variable | Purpose |
|----------|---------|
| `job._body` | The rendered target body — the shell text that should appear after the directive block. |
| `job._inputs` | List of input filenames for this target. |
| `job._outputs` | List of output filenames for this target. |

These are most useful when authoring a custom runner template (see [Running Jobs](07-Running_Jobs.md#custom-templates)).

## Defaults worth knowing

CGPipe's bundled `cgpiperc` sets a handful of sensible defaults. The ones worth being aware of:

    cgpipe.runner = "shell"
    cgpipe.runner.shell.filename = ""        # write to stdout
    cgpipe.runner.shell.autoexec = false
    cgpipe.runner.include_output_filenames = false
    cgpipe.loglevel = 1

Override any of these in `~/.cgpiperc` or per-pipeline. The bundled file is inside the JAR; you don't edit it directly.

## Putting it together: a typical setup

A common arrangement on a cluster:

    # /etc/cgpiperc — site-wide
    cgpipe.runner = "slurm"
    cgpipe.runner.slurm.account = "default-alloc"

    # ~/.cgpiperc — personal
    job.mail = "me@example.com"
    job.mailtype = "FAIL,END"
    cgpipe.joblog = "${HOME}/.cgpipe/joblog.txt"

…then per-pipeline:

    #!/usr/bin/env cgpipe
    # 
    # ...help...

    runid ?= "run.$(date +%Y%m%d-%H%M)"
    cgpipe.log = "logs/pipeline-${runid}.log"
    job.stdout = "logs/"
    job.stderr = "logs/"

…then per-invocation:

    $ CGPIPE_RUN_ID="batch-2026-06-03" cgpipe pipeline.cgp -ref hg38.fa -reads sample.fq

The result is fully composed defaults with cluster, user, pipeline, and invocation each layering cleanly.
