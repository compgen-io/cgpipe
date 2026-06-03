# Troubleshooting

A guided tour of CGPipe's debugging tools and the most common failure modes.

## Tools at your disposal

### Dry run (`-dr`)

The single most useful command. `cgpipe -dr pipeline.cgp ...` walks the build graph and prints every rendered job script to stdout instead of submitting anything. Use this:

- Before any real submission, to verify resources, names, and shell quoting render correctly.
- To debug `<% if %>` fragments and `${var}` substitution — the rendered output is exactly what the scheduler will see.
- To compare what changes between two invocations.

### Verbose (`-v`, `-vv`, `-vvv`)

Adds increasingly detailed CGPipe logging on top of normal output. `-v` shows which targets were considered and which were skipped. `-vv` adds parser-level detail. `-vvv` is mostly for CGPipe-internals debugging.

Combine with `-dr` for a complete trace:

    $ cgpipe -dr -v pipeline.cgp -reads sample.fq

### Shell runner

`cgpipe.runner = "shell"` (the default) writes a single self-contained bash script to stdout. Read it. Run it locally. It's the closest thing to "show me what you're going to do" CGPipe offers, and it makes problems with substitution and dependency ordering obvious.

    $ cgpipe -t cgpipe.runner=shell pipeline.cgp ... > pipeline.sh
    $ less pipeline.sh
    $ bash pipeline.sh   # actually run it locally

### `job.src`

Set `job.src = "logs/%JOBID.cmd"` (or any path with a `%JOBID` placeholder) and CGPipe writes each rendered job script to disk as it submits. When a job fails three days later, the exact script is sitting in `logs/12345.cmd` for you to inspect.

### Joblog inspection

If `cgpipe.joblog` is set, the joblog is a plain text file. Cat it, grep it, look at the last entry that claimed a particular output:

    $ grep "aligned.bam" logs/joblog.txt
    SUBMIT 12340 align.bam     SLURM  align-step1     2026-06-03T10:30:00
    SUBMIT 12345 aligned.bam   SLURM  align-step2     2026-06-03T11:00:00
    DONE   12340 align.bam     SLURM  align-step1     2026-06-03T11:30:00
    DONE   12345 aligned.bam   SLURM  align-step2     2026-06-03T12:15:00

The last DONE for a given output is the job that actually produced what's on disk now.

### Graphviz output

`cgpipe.runner = "graphviz"` doesn't submit anything — it prints the dependency graph as a `.dot` file:

    $ cgpipe -t cgpipe.runner=graphviz pipeline.cgp ... > pipeline.dot
    $ dot -Tpng pipeline.dot -o pipeline.png

Useful when a pipeline is doing something you didn't expect — does the graph have the edges you intended? Are there orphan targets? Cycles?

## Common errors

### "No way to build *target*"

CGPipe walked the graph from your requested output and couldn't find a sequence of targets whose inputs are all satisfiable. Causes:

- An input file is missing on disk and no rule produces it.
- A target's input depends on a typo'd filename.
- A wildcard rule that should match doesn't — wildcards only match `%` to the *output* name; check that the input pattern matches.
- A required CLI argument was forgotten and a `${var}` substituted to the empty string.

Diagnostic: run with `-v` to see which targets were considered, and `-dr` to see what the inputs/outputs look like after substitution.

### `Operator ... has no left operand`

You wrote a stray operator with nothing on its left. Most commonly `+1.5` (no unary plus in CGPipe) or `if = "value"` (missing variable name). The error includes a line number — start there.

### `Method not found: <name>`

You called a method that doesn't exist on the receiver's type. Check [Methods Reference](04-Methods_Reference.md). Common slips:

- `samples.split(",")` — only strings have `split`, not lists.
- `"abc".count()` — there's no `count`; use `.length()`.
- `path.exists` — methods need parentheses: `path.exists()`.

### Substitution that "should work" but doesn't

`${var}` errors if `var` is unset. Use `${var?}` to allow empty fallback, or guard with `if !var`.

`@{list}` only expands inside string literals and in target output/input lines. Inside the body, use `${list}` (joins with spaces) or write a `<% for x in list %>...<% done %>` loop.

`$<` and `$>` are CGPipe substitutions and only mean something *inside* a target body. They are not shell variables. To use them in `$()` shell substitution, escape: `\$<`. (Rare.)

### Job submitted but nothing happened

If you're using a scheduler runner:

- Check `qstat` / `squeue` / `qstat -t` / `batchq status` to see if the job is actually in the queue.
- Check the rendered script if `job.src` is set.
- Check `job.stderr` for runtime errors from the body.
- Check the joblog — was the job recorded? What state did the runner verify?

If you're using `shell` and the script went to stdout, nothing is supposed to run automatically. Pipe to `bash`, or set `cgpipe.runner.shell.autoexec = true`.

### Resources insufficient (OOM, walltime exceeded)

Per-target overrides win over globals. Inside the target's `<% %>` block:

    <%
        job.mem = "64G"
        job.walltime = "72:00:00"
        job.procs = 32
    %>

If the same target is run many times (e.g., per-chromosome), give the heaviest chromosome's worth of resources and let the smaller ones over-request — schedulers handle this fine.

### "Why did it re-submit a job that was already done?"

Causes, in order of likelihood:

1. **No joblog.** Without `cgpipe.joblog`, CGPipe only knows about files on disk. If the output exists but its mtime is older than any input's mtime, CGPipe rebuilds.
2. **Inputs really changed.** Check `ls -lt input.txt output.txt` — if the input is newer, the rule fires. To force a rebuild without changing inputs, delete the output.
3. **A different file with the same name was claimed by an earlier job and then deleted.** Without an mtime to compare, CGPipe re-submits.
4. **The joblog says the job is `FAILED`.** A failed entry doesn't satisfy a dependency.

### "Why didn't it re-submit a job whose inputs changed?"

Causes:

1. **Joblog has a pending entry.** CGPipe sees a submitted job and waits for it; if that job is actually dead but the runner hasn't marked it failed, you can be stuck. Check the runner's status (`squeue`, `qstat`, etc.) and manually mark the joblog entry as failed if needed (the joblog is plain text).
2. **The input is itself a temp output that was skipped.** Temp outputs aren't checked on disk and aren't rebuilt unless their downstream is missing.
3. **A wildcard rule with a different pattern matched first.** Multiple-definition resolution tries rules in source order. Reorder or remove the unintended rule.

## Shell-quoting in target bodies

CGPipe substitutes `${var}`, `$<`, `$>`, `$%`, and `${{var}}` before the shell sees the body. To get a literal `$` past CGPipe, escape with `\`:

    \$HOME        # the shell's $HOME, not CGPipe's HOME
    \\$HOME       # escaped twice for double evaluation contexts

`@` follows the same pattern. Single-`\` escapes get you through one CGPipe pass; double-`\\` gets you through two (for double-evaluated strings or shell-captured commands).

## Debug-print pattern

A useful pattern when a pipeline's behavior is mysterious:

    print "DEBUG: out = ${out}"
    print "DEBUG: per_chrom = ${per_chrom}"
    print "DEBUG: by_chrom = ${by_chrom?}"

    # ... target definitions

These run at parse time (global context), so they fire before any submissions happen. With `-dr`, you see them and the rendered scripts and can verify both at once.

For target-internal debugging, put `echo` statements in the body — they're shell, so they only appear at job-execution time. For the parse-time view of what a *target* sees, define a quick `printvars` target:

    printvars:
        <% job.shexec = true %>
        echo "out      = ${out}"
        echo "ref      = ${ref}"
        echo "samples  = ${samples}"

…and `cgpipe pipeline.cgp printvars` runs it locally.

## When all else fails

- The test suite under `src/test-scripts/` is exhaustive and well-organized. Search for the feature you're stuck on — there's almost certainly a worked example.
- Compare a working pipeline (one of the tutorials, or any of the example scripts in your collection) line-by-line against the one that's misbehaving.
- File an issue at the [GitHub repository](https://github.com/compgen-io/cgpipe) with the smallest pipeline that reproduces the problem and the output of `cgpipe -dr -v ...`.
