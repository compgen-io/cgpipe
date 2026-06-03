# Tutorial 3: per-target resources and conditional flags

A more realistic single-step pipeline: align reads to a reference.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 3: BWA alignment with optional read group.
    #
    # Options:
    #     --reads FILE    input FASTQ
    #     --ref FILE      reference FASTA (BWA-indexed)
    #     --out FILE      output BAM
    #     --rg STRING     optional read group, e.g. "@RG\tID:sample\tSM:sample"

    if !reads
        print "ERROR: --reads is required"
        exit 1
    endif
    if !ref
        print "ERROR: --ref is required"
        exit 1
    endif
    if !out
        print "ERROR: --out is required"
        exit 1
    endif

    threads ?= 8

    ${out}: ${reads} ${ref}
        <%
            job.name = "bwa-${out.basename()}"
            job.procs = threads
            job.mem = "16G"
            job.walltime = "12:00:00"
        %>
        bwa mem -t ${job.procs} \
            <% if rg %>-R "${rg}" \<% endif %>
            ${ref} ${reads} \
        | samtools view -b -o $>.tmp - && mv $>.tmp $>

Things to notice:

- `threads ?= 8` sets a default the user can override with `-threads 16`.
- The `<% %>` block at the top of the body sets per-job resources. Inside it, `${out.basename()}` strips any directory from `--out` for the display name.
- The `<% if rg %>...<% endif %>` fragment splices the optional `-R` flag into the command line only when `--rg` was provided.
- `$>.tmp && mv $>.tmp $>` is a common atomicity trick — the final output only appears on disk if every command in the pipeline succeeded. Without it, an interrupted alignment leaves a half-written BAM that confuses the next run.

---

[← Tutorial 2](02-gzip-wildcard.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 4 — map-reduce across chromosomes →](04-map-reduce.md)
