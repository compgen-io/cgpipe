# Tutorial 6: shared `__pre__` and `__post__`

Real pipelines almost always start each job with a small preamble (record start time, inputs, outputs, working dir) and end with a postamble (record end time, duration). The `__pre__` and `__post__` special targets get prepended/appended to every other target automatically.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 6: per-job timing and log preamble.

    if !input
        print "ERROR: --input is required"
        exit 1
    endif

    runid ?= "run.$(date +%Y%m%d-%H%M)"
    cgpipe.log = "logs/tutorial6-${runid}.log"
    job.stdout = "logs/"
    job.stderr = "logs/"

    __setup__:
        <% job.shexec = true %>
        mkdir -p logs results

    __pre__:
        echo "Inputs:  $<"
        echo "Outputs: $>"
        echo "Start:   $(date)"
        START=\$(date +%s)

    __post__:
        END=\$(date +%s)
        echo "End:     $(date)"
        echo "Elapsed: \$((END - START))s"

    results/copy.txt: ${input}
        <% job.name = "copy" %>
        cp $< $>

What's running:

- `__setup__` runs once at the start of the pipeline. With `job.shexec = true`, it runs directly on the submission host rather than going through the scheduler — that's normally what you want for `mkdir`.
- `__pre__` is prepended to every other target's body. The shell sees `Start:` lines from every job.
- `__post__` is appended. Combined with `__pre__`, you get a uniform job-log structure for free.

The `\$(date)` and `\$((...))` escape the `$` so it survives CGPipe's substitution pass and is interpreted by the shell at job-run time.

---

[← Tutorial 5](05-opportunistic-cleanup.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 7 — importable snippets →](07-importable-snippets.md)
