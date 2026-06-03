# Tutorial 8: composing pipelines via include

A real-world pattern: keep cluster-wide settings, per-project defaults, and pipeline logic in different files.

    # shared/defaults.cgp
    job.stdout = "logs/"
    job.stderr = "logs/"
    job.env = true
    cgpipe.joblog = "logs/joblog.txt"

    __setup__:
        <% job.shexec = true %>
        mkdir -p logs results

    __pre__:
        echo "Start: $(date)"

    __post__:
        echo "End:   $(date)"

    # tutorial8.cgp
    #!/usr/bin/env cgpipe
    #
    # Tutorial 8: pipeline that includes shared defaults.
    #
    # Options:
    #     --bam FILE     input BAM
    #     --out FILE     output stats

    if !bam
        print "ERROR: --bam is required"
        exit 1
    endif
    if !out
        print "ERROR: --out is required"
        exit 1
    endif

    include "shared/defaults.cgp"

    ${out}: ${bam}
        <% job.name = "stats-${bam.basename()}" %>
        samtools flagstat $< > $>.tmp && mv $>.tmp $>

`include` runs the file inline at that point in the global context. The included file can set variables, define `__pre__`/`__post__`, define targets, or all three. The pipeline file ends up with everything from `shared/defaults.cgp` plus its own targets.

A single project might keep one shared-defaults file and a dozen pipeline files that all `include` it.

---

[← Tutorial 7](07-importable-snippets.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 9 — containerized jobs →](09-containers.md)
