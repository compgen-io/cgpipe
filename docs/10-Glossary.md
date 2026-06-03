# Glossary

A short reference for terms used throughout this guide.

**Accumulator list.** A list (usually named `*_acc`, `out*`, `per_chrom`, etc.) populated inside a `for` loop with `+=`, then consumed by a downstream target via `@{accumulator}` expansion. The core idiom for dynamic target generation. See [Build Targets](05-Build_Targets.md#dynamic-target-generation).

**Body.** The shell script lines that appear after a target's `:` and indentation. Submitted to the runner as the actual job script (after CGPipe substitutes `${var}`, `$<`, `$>`, etc.).

**Build graph / DAG.** The directed acyclic graph of file dependencies CGPipe computes from your target definitions. A target's inputs are its parents in the graph; a target's outputs are its children's parents. CGPipe walks the graph backwards from the requested output to figure out what needs to be submitted.

**Build target.** See *Target*.

**`cgpipe`.** The command-line tool. Also the language's namespace prefix for configuration variables (`cgpipe.runner`, `cgpipe.log`, …).

**`cgpiperc`.** A configuration file (`.cgpiperc`) that's loaded at startup. Itself a CGPipe script — you can use `if`, `include`, and variable substitution inside it. Loaded from `/etc/cgpiperc`, `$CGPIPE_HOME/.cgpiperc`, and `~/.cgpiperc` in order.

**`cgsub`.** The standalone job-submission tool that ships with CGPipe. Uses the same runner and joblog as `cgpipe` but submits one job at a time without parsing a pipeline file. Useful for ad-hoc submissions that should still appear in the joblog.

**Closure / capture.** When a target is defined, it captures the surrounding global context — variable values, settings, the current `__pre__`/`__post__` — at that point. The target body sees those captured values at job-render time, not the values current at execution time. This is what makes per-iteration targets work cleanly inside a `for` loop.

**Dry run.** Submission mode where rendered job scripts are printed to stdout instead of sent to the scheduler. Enabled with `-dr` or `CGPIPE_DRYRUN=1`. The fastest way to verify substitution and resource settings before tying up cluster quota.

**Importable snippet.** A target written with `name::` (double colon). Has no inputs or outputs; can only be inlined into another target body via `<% import name %>`. The mechanism behind `__pre__` and `__post__`.

**Include.** Inlining one CGPipe file into another at the global context level. Used for shared defaults, target libraries, and per-cluster configuration. Different from *import*, which inlines a snippet inside a target body.

**Joblog.** The file (configured by `cgpipe.joblog`) where CGPipe records every submitted job: its id, outputs, target name, runner state. The joblog is what lets independent pipelines compose — a short alignment pipeline and a later variant-calling pipeline can be written separately, even by different people, and still coordinate correctly as long as they share a joblog. When pipeline B asks for `aligned.bam` and pipeline A already submitted the job that produces it, the joblog tells pipeline B to wait for that job rather than treat the file as missing. The joblog is also the source of truth for *which job produced which file*: jobs fail, parent jobs fail, files get re-created — but the last job that claimed an output is the one that actually generated it. Joblog uses:

- Avoid resubmitting jobs that are still queued or running.
- Let independent pipeline runs share work and chain together without an external scheduler.
- Track provenance: which job produced which output file (including across re-runs after failures).
- Drive cleanup and bookkeeping in `cgsub` and post-run scripts.

**Opportunistic job.** A target with no outputs — just a leading colon and a list of inputs. Runs only if all inputs happen to be available (on disk, in the joblog, or submitted earlier in the same run). Never forces an input to be built. Used for cleanup, post-hoc reporting, optional bundling. See [Build Targets](05-Build_Targets.md#opportunistic-jobs).

**Runner.** The backend that takes a rendered job script and submits it to a particular scheduler. CGPipe ships with runners for shell, SGE, SLURM, PBS, BatchQ, and Graphviz (graph rendering, not submission). Select with `cgpipe.runner`.

**Run ID.** A pipeline-wide identifier for all jobs from a single `cgpipe` invocation. Set via the `CGPIPE_RUN_ID` environment variable; exposed to scripts as `cgpipe.run_id`; passed through to BatchQ as `#BATCHQ -run-id`. Lets you group jobs from one pipeline even when they aren't dependent on each other.

**Special target.** One of the five reserved names: `__pre__`, `__post__`, `__setup__`, `__teardown__`, `__postsubmit__`. Handled specially by CGPipe — not built like regular targets. See [Build Targets](05-Build_Targets.md#special-targets).

**Target.** A definition that tells CGPipe how to produce one or more output files from zero or more input files. The fundamental unit of work; everything else is bookkeeping around target definitions.

**Temp output.** An output file marked with `^` to indicate it's an intermediate not needed for its own sake. Not required to exist on disk; not checked for mtime against downstream rules. Useful for big intermediates whose only consumer is the next target. See [Build Targets](05-Build_Targets.md#temporary-outputs).

**Template (job script template).** A CGPipe script that renders the directive block and body for a runner. Each scheduler runner has a built-in template that's a good starting point. Override with `cgpipe.runner.<name>.template = "/path/to/template.cgp"` to customize.

**Wildcard target.** A target whose output (and matching input) uses `%` as a placeholder. Matches any output name; the captured stem is available as `$%` in the body. Pattern: `%.gz: %`. See [Build Targets](05-Build_Targets.md#wildcards).

**Working directory.** The directory the runner uses when the job actually runs. Set via `job.wd`. CGPipe itself runs in whatever directory you launch it from; that becomes `cgpipe.sys.cwd`.
