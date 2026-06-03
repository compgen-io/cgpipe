# Pipeline tutorials

A handful of worked examples that build on each other. They're synthetic — none of these is a "real" pipeline you should run as-is — but the shapes are taken straight from production usage. Pair each tutorial with the relevant reference chapter for full detail.

The tools referenced (`bwa`, `samtools`, `bcftools`, `gzip`) appear here as stand-ins; the same patterns work for whatever your real workflow uses.

## Tutorial 1: Hello, target

The smallest interesting pipeline.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 1: copy one file to another, slowly.
    #
    # Options:
    #     --input FILE
    #     --output FILE

    if !input
        print "ERROR: --input is required"
        exit 1
    endif
    if !output
        print "ERROR: --output is required"
        exit 1
    endif

    ${output}: ${input}
        cp ${input} ${output}

Run it:

    $ ./tutorial1.cgp -input data.txt -output backup.txt

What's happening:

- The first comment block is the help text. `cgpipe tutorial1.cgp -h` prints it.
- `!input` and `!output` are the standard CLI-argument guard. Without them you'd see a confusing error from the shell when `${input}` substitutes to the empty string.
- The single target says: to produce `${output}`, you need `${input}`, and the body is `cp ${input} ${output}`.

CGPipe's default runner (`shell`) prints the rendered script to stdout. To actually execute it, either pipe to `bash` or set `cgpipe.runner.shell.autoexec = true`.

## Tutorial 2: gzip with a wildcard

Same shape, but using a wildcard to handle any file rather than one named input.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 2: gzip a file using a wildcard rule.
    #
    # Options:
    #     --file FILE     file to compress (produces FILE.gz)

    if !file
        print "ERROR: --file is required"
        exit 1
    endif

    all: ${file}.gz

    %.gz: %
        gzip -c $< > $>

Run it:

    $ ./tutorial2.cgp -file data.txt

The `all` target is a convention — a top-level alias for the things you want CGPipe to build. Its dependency `${file}.gz` triggers the wildcard rule, which matches any `*.gz` target and depends on the same stem without the `.gz`.

`$<` and `$>` are the all-inputs and all-outputs substitutions. Inside this body they're `data.txt` and `data.txt.gz`.

## Tutorial 3: per-target resources and conditional flags

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

## Tutorial 4: map-reduce across chromosomes

The pattern from real pipelines: split the work by chromosome, run independently, then merge. This is where dynamic target generation pays off.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 4: per-chromosome variant calling and merge.
    #
    # Options:
    #     --bam FILE         input BAM
    #     --ref FILE         reference FASTA
    #     --out FILE         output VCF

    if !bam
        print "ERROR: --bam is required"
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

    chroms = "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y".split(" ")
    per_chrom = []

    for c in chroms
        per_chrom += "${out}.${c}.vcf"

        ^${out}.${c}.vcf: ${bam} ${ref}
            <%
                job.name = "call-chr${c}"
                job.mem = "8G"
                job.walltime = "12:00:00"
            %>
            bcftools mpileup -r chr${c} -f ${ref} ${bam} \
                | bcftools call -mv - > $>.tmp && mv $>.tmp $>
    done

    ${out}: @{per_chrom}
        <%
            job.name = "merge-${out.basename()}"
            job.mem = "4G"
            job.walltime = "2:00:00"
        %>
        bcftools concat -O z -o $>.tmp $< && mv $>.tmp $>

What this pipeline does:

1. Defines 24 chromosomes as a list of strings.
2. Empties an accumulator (`per_chrom = []`) that will collect the per-chromosome output filenames.
3. The `for` loop runs 24 times. Each iteration:
   - Appends one filename to the accumulator.
   - Defines a new build target whose output is chromosome-specific. The `^` marks it temporary — these per-chromosome VCFs are only needed to produce the final merged output.
4. After the loop the accumulator has 24 entries.
5. The merge target depends on `@{per_chrom}`, which expands to all 24 filenames. CGPipe wires up the dependency edges so the merge waits for every chromosome.

Run it:

    $ ./tutorial4.cgp -bam aligned.bam -ref hg38.fa -out variants.vcf.gz

On a SLURM cluster this submits 25 jobs (24 calls + 1 merge) with the merge depending on all 24 callers. If you re-run with the same arguments and the merged output exists and is current, nothing is resubmitted. If you delete one per-chromosome VCF, it doesn't matter — the temporary marker tells CGPipe not to check it on disk.

### Toggling between chunked and single-shot

A small extension: let the user choose between per-chromosome and single-shot via a flag.

    by_chrom ?= false

    if by_chrom
        chroms = "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y".split(" ")
        per_chrom = []

        for c in chroms
            per_chrom += "${out}.${c}.vcf"

            ^${out}.${c}.vcf: ${bam} ${ref}
                <%
                    job.name = "call-chr${c}"
                    job.mem = "8G"
                %>
                bcftools mpileup -r chr${c} -f ${ref} ${bam} \
                    | bcftools call -mv - > $>.tmp && mv $>.tmp $>
        done

        ${out}: @{per_chrom}
            <%
                job.name = "merge-${out.basename()}"
                job.mem = "4G"
            %>
            bcftools concat -O z -o $>.tmp $< && mv $>.tmp $>
    else
        ${out}: ${bam} ${ref}
            <%
                job.name = "call-${out.basename()}"
                job.mem = "16G"
                job.walltime = "48:00:00"
            %>
            bcftools mpileup -f ${ref} ${bam} \
                | bcftools call -mv -O z -o $>.tmp - && mv $>.tmp $>
    endif

    $ ./tutorial4b.cgp -bam aligned.bam -ref hg38.fa -out variants.vcf.gz -by_chrom true

A single flag flips the whole shape of the pipeline between one long-running job and a parallel-then-merge structure. The user doesn't have to pick which file to write.

## Tutorial 5: opportunistic cleanup for storage efficiency

The map-reduce pattern in Tutorial 4 produces 24 per-chromosome VCFs that exist only to feed the merge. Marking them `^` (temp) means CGPipe won't waste time *checking* them on disk — but the files still *exist* on disk after each per-chromosome job runs. On a real dataset (per-chromosome BAMs, intermediate alignments, sorted shards) that can easily be hundreds of gigabytes of intermediate state.

Opportunistic jobs are how you reclaim that space without compromising restartability. The idea:

- An opportunistic job (target with no outputs, only inputs — `: input1 input2 ...`) runs only when **all** of its inputs are available.
- If you make the inputs the final merged output **plus** every temp file, the cleanup only fires after the merge has succeeded.
- And because opportunistic jobs don't force their inputs to be built, re-runs where the merge is already up-to-date don't fire the cleanup either — the disk is already clean.

### Basic cleanup

Extending Tutorial 4 with one extra rule:

    chroms = "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y".split(" ")
    per_chrom = []

    for c in chroms
        per_chrom += "${out}.${c}.vcf"

        ^${out}.${c}.vcf: ${bam} ${ref}
            <%
                job.name = "call-chr${c}"
                job.mem = "8G"
            %>
            bcftools mpileup -r chr${c} -f ${ref} ${bam} \
                | bcftools call -mv - > $>.tmp && mv $>.tmp $>
    done

    ${out}: @{per_chrom}
        <%
            job.name = "merge-${out.basename()}"
            job.mem = "4G"
        %>
        bcftools concat -O z -o $>.tmp $< && mv $>.tmp $>

    # NEW: cleanup
    : ${out} @{per_chrom}
        <%
            job.name = "cleanup-${out.basename()}"
            job.mem = "1G"
            job.walltime = "10:00"
        %>
        rm -v ${per_chrom}

Submission order on a scheduler:

1. 24 per-chrom call jobs (dependents of nothing).
2. The merge job, dependent on all 24.
3. The cleanup job, dependent on the merge and all 24.

The cleanup only runs after the merge is genuinely complete. If the pipeline aborts before the merge, the per-chrom VCFs survive and the next run picks up from where it left off. If you re-run later and the merged VCF is already current, none of the 24 callers run, the merge doesn't run, and (because opportunistic jobs don't force their inputs) the cleanup doesn't run either.

### Defensive cleanup

The basic form will print errors from `rm` if any of the per-chrom files don't exist when the cleanup fires — for example, if a partial earlier cleanup deleted some of them. A defensive variant from production pipelines checks each input first:

    : ${out} @{per_chrom}
        <%
            job.name = "cleanup-${out.basename()}"
            job.mem = "1G"
        %>
        if [ -e ${out} ]; then
            <% for f in per_chrom %>
            if [ -e ${f} ]; then
            <% done %>
                rm -v ${per_chrom}
            <% for f in per_chrom %>
            fi
            <% done %>
        fi

For `per_chrom = ["a.vcf","b.vcf"]` this renders to:

    if [ -e variants.vcf.gz ]; then
        if [ -e a.vcf ]; then
        if [ -e b.vcf ]; then
            rm -v a.vcf b.vcf
        fi
        fi
    fi

The nested `if [ -e ... ]` blocks wrap the `rm`, so the delete only fires if every intermediate is still there. Any missing file short-circuits the entire block silently. This idiom is common enough that it's worth recognizing on sight — the `<% for %>...<% done %>` template loops are generating matching opening/closing lines from one accumulator.

### Why this matters

On a multi-sample alignment + variant-calling project with, say, 50 samples and 24 chromosomes per sample, the per-chrom intermediate BAMs are ~3 TB. Without opportunistic cleanup, that 3 TB stays on disk indefinitely. With it, the disk usage tracks roughly the size of the merged outputs.

The same pattern works for any intermediate-heavy fan-out: per-lane FASTQ shards, per-region pileups, per-window depth files. Wherever Tutorial 4's pattern produces fan-out outputs, a Tutorial 5 cleanup rule belongs alongside it.

## Tutorial 6: shared `__pre__` and `__post__`

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

## Tutorial 7: importable snippets for shared body fragments

When two targets share a long chunk of body, factor it out as an importable snippet.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 7: importable snippets.

    if !input
        print "ERROR: --input is required"
        exit 1
    endif

    safe::
        set -euo pipefail
        umask 077
        echo "Running as $(whoami) on $(hostname)"

    out1.txt: ${input}
        <% import safe %>
        wc -l ${input} > $>

    out2.txt: ${input}
        <% import safe %>
        wc -c ${input} > $>

`safe::` (note the double colon) defines an importable snippet. `<% import safe %>` inlines its body into the importing target's body at that point. This is how `__pre__` and `__post__` work internally; you can use the same mechanism for your own shared fragments.

The difference from `include` (which is a file-level statement) is scope: `import` only works inside a target body, and it inlines a snippet rather than a whole file.

## Tutorial 8: composing pipelines via include

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

## Tutorial 9: containerized jobs (Docker and Singularity)

Most production bioinformatics tools ship as Docker or Singularity containers. The natural place to wire container invocation into a CGPipe pipeline is `__pre__`/`__post__`: have the *preamble* open a HEREDOC piped into the container's shell, and the *postamble* close it. The body of every target then runs inside the container with the right tools and the right paths visible.

### The HEREDOC trick

`__pre__` is prepended to every target body; `__post__` is appended. Whatever appears between them is treated as one shell script. That means you can use `__pre__` to *start* a command that reads from stdin, and `__post__` to feed the closing HEREDOC marker. The whole target body becomes the HEREDOC's content.

A minimal Docker-wrapped pipeline:

    #!/usr/bin/env cgpipe
    #
    # Tutorial 9: run every job inside a Docker container.
    #
    # Options:
    #     --bam FILE        input BAM
    #     --out FILE        output stats
    #     --image IMAGE     docker image (default: biocontainers/samtools:1.18)

    if !bam
        print "ERROR: --bam is required"
        exit 1
    endif
    if !out
        print "ERROR: --out is required"
        exit 1
    endif

    image ?= "biocontainers/samtools:1.18"
    wd ?= $(pwd)

    __pre__:
        docker run --rm \
            -v ${wd}:${wd} \
            -v ${bam.dirname()}:${bam.dirname()} \
            -v ${out.dirname()}:${out.dirname()} \
            -w ${wd} \
            -u $(id -u):$(id -g) \
            ${image} \
            bash <<'CGPIPE_BODY'

    __post__:
    CGPIPE_BODY

    ${out}: ${bam}
        <% job.name = "stats-${bam.basename()}" %>
        samtools flagstat ${bam} > $>.tmp && mv $>.tmp $>

What the rendered job script looks like:

    docker run --rm \
        -v /data/proj:/data/proj \
        -v /data/proj/in:/data/proj/in \
        -v /data/proj/out:/data/proj/out \
        -w /data/proj \
        -u 1001:1001 \
        biocontainers/samtools:1.18 \
        bash <<'CGPIPE_BODY'
    samtools flagstat /data/proj/in/sample.bam > /data/proj/out/sample.stats.txt.tmp && mv /data/proj/out/sample.stats.txt.tmp /data/proj/out/sample.stats.txt
    CGPIPE_BODY

CGPipe's substitution has already turned `${bam}` and `$>` into absolute paths. The HEREDOC is delivered to `bash` *inside* the container, and the container sees those paths because of the `-v` bind mounts.

### What the volume mounts need to cover

The container can only see paths you bind-mount. Three are usually required:

- **Working directory** (`-v ${wd}:${wd} -w ${wd}`) — what the scheduler considers "where the job runs."
- **Input file's directory** (`-v ${bam.dirname()}:${bam.dirname()}`) — so the container can read the input.
- **Output file's directory** (`-v ${out.dirname()}:${out.dirname()}`) — so the container can write the output (and the atomic `.tmp` rename works).

For pipelines with many input/output files, mount the whole project root once instead of listing each file's directory:

    proj_root ?= "/data/project-xyz"

    __pre__:
        docker run --rm \
            -v ${proj_root}:${proj_root} \
            -w ${wd} \
            -u $(id -u):$(id -g) \
            ${image} \
            bash <<'CGPIPE_BODY'

    __post__:
    CGPIPE_BODY

### Notes on the HEREDOC

- The quotes around `'CGPIPE_BODY'` are deliberate. Without them, the shell on the host (where `docker run` is invoked) would expand `$VAR` references inside the HEREDOC before the container ever sees them. Quoted, the HEREDOC is passed through verbatim and shell variables in the body (`$1`, `$RANDOM`, etc.) evaluate inside the container at job run time.
- CGPipe's substitutions (`${var}`, `$<`, `$>`, `${out.basename()}`) run at submission time and have already been resolved before the script is sent to the runner. The quoted HEREDOC has no effect on them.
- The closing `CGPIPE_BODY` marker in `__post__` **must** be in column zero of the rendered body, with no leading whitespace. Bash HEREDOC parsing is sensitive to that. Don't indent the `CGPIPE_BODY` line inside `__post__`.
- `-u $(id -u):$(id -g)` runs the container as your UID/GID so output files land with your ownership rather than `root:root`. The `$(...)` is evaluated on the submission host (where CGPipe runs), which is normally what you want.

### Singularity equivalent

Singularity (or Apptainer) is what most HPC sites use — it doesn't need root, mounts the user's home directory automatically, and works with Docker images via `docker://` URLs. The structural pattern is identical; only the command changes:

    image ?= "docker://biocontainers/samtools:1.18"
    wd ?= $(pwd)
    proj_root ?= "/data/project-xyz"

    __pre__:
        singularity exec \
            -B ${proj_root}:${proj_root} \
            --pwd ${wd} \
            ${image} \
            bash <<'CGPIPE_BODY'

    __post__:
    CGPIPE_BODY

Differences worth knowing:

- `singularity exec ... bash <<HEREDOC` doesn't go through a Docker daemon — it runs as the submitting user directly. No `-u` mapping needed; files are owned by the right account by default.
- Bind mounts use `-B` instead of `-v`. The syntax is identical.
- `--pwd` sets the working directory inside the container (vs. Docker's `-w`).
- `$HOME` and `/tmp` are auto-bound on most Singularity installations. You usually don't need to mount them explicitly.
- For Docker Hub images, prefix with `docker://`. Singularity caches the image as a SIF on first use, so cold-cache jobs may pay a one-time download cost.

### Picking up where the pattern ends

This tutorial gives you one container per job, which is what most schedulers prefer (each scheduler-allocated slot starts a fresh container). For pipelines where a single workflow uses several different images (one for alignment, one for variant calling, etc.), set `image` per-target inside its `<% %>` block:

    aligned.bam: reads.fq ref.fa
        <%
            image = "biocontainers/bwa:0.7.17"
            job.mem = "8G"
        %>
        bwa mem ${ref} ${reads} > $>.tmp && mv $>.tmp $>

    variants.vcf: aligned.bam ref.fa
        <%
            image = "biocontainers/bcftools:1.18"
            job.mem = "4G"
        %>
        bcftools call ${aligned.bam} > $>.tmp && mv $>.tmp $>

Each target carries its own image and the `__pre__`/`__post__` wrapping picks it up automatically — exactly the same `${image}` substitution, different value per target.

## Where next

- [Build Targets](05-Build_Targets.md) — full coverage of target syntax: wildcards, temp outputs, opportunistic jobs, special targets.
- [Methods Reference](04-Methods_Reference.md) — every method on every value type.
- [Running Jobs](07-Running_Jobs.md) — every `job.*` setting and runner-specific quirks.
- [Troubleshooting](11-Troubleshooting.md) — dry runs, joblog inspection, common pitfalls.
