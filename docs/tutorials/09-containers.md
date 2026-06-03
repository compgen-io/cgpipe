# Tutorial 9: containerized jobs (Docker and Singularity)

Most production bioinformatics tools ship as Docker or Singularity containers. The natural place to wire container invocation into a CGPipe pipeline is `__pre__`/`__post__`: have the *preamble* open a HEREDOC piped into the container's shell, and the *postamble* close it. The body of every target then runs inside the container with the right tools and the right paths visible.

## The HEREDOC trick

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

## What the volume mounts need to cover

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

## Notes on the HEREDOC

- The quotes around `'CGPIPE_BODY'` are deliberate. Without them, the shell on the host (where `docker run` is invoked) would expand `$VAR` references inside the HEREDOC before the container ever sees them. Quoted, the HEREDOC is passed through verbatim and shell variables in the body (`$1`, `$RANDOM`, etc.) evaluate inside the container at job run time.
- CGPipe's substitutions (`${var}`, `$<`, `$>`, `${out.basename()}`) run at submission time and have already been resolved before the script is sent to the runner. The quoted HEREDOC has no effect on them.
- `CGPIPE_BODY` in `__post__` is written *indented* in the source, just like any other line in a target body. CGPipe strips the common leading-whitespace prefix when it assembles the rendered script, so the marker ends up at column zero — which is what bash HEREDOC parsing requires. You don't have to do anything special; the indent in your source is normal CGPipe body convention.
- `-u $(id -u):$(id -g)` runs the container as your UID/GID so output files land with your ownership rather than `root:root`. The `$(...)` is evaluated on the submission host (where CGPipe runs), which is normally what you want.

## Singularity equivalent

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

## Picking up where the pattern ends

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

---

[← Tutorial 8](08-include.md) · [Tutorials index](../06-Pipeline_Tutorials.md)
