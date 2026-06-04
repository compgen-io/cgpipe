# Tutorial 9: containerized jobs (Docker and Singularity)

Most production bioinformatics tools ship as Docker or Singularity containers. CGPipe has first-class support: set the engine once in your config, set the image per pipeline (or per target), and CGPipe wraps every job in `docker run` or `singularity exec` automatically. The pipeline source stays clean — it doesn't have to know about volume mounts, working directories, or user mapping.

## Setting it up

Pick the engine in `~/.cgpiperc` (do this once):

    cgpipe.container.engine = "docker"        # or "singularity" / "apptainer"

Then in a pipeline:

    #!/usr/bin/env cgpipe
    #
    # Tutorial 9: BAM stats inside a container.
    #
    # Options:
    #     --bam FILE        input BAM
    #     --out FILE        output stats

    if !bam
        print "ERROR: --bam is required"
        exit 1
    endif
    if !out
        print "ERROR: --out is required"
        exit 1
    endif

    job.container = "biocontainers/samtools:1.18"

    ${out}: ${bam}
        samtools flagstat ${bam} > $>

Run it:

    $ ./tutorial9.cgp -bam aligned.bam -out stats.txt

CGPipe renders the job script with the body wrapped in a `docker run` (or `singularity exec`) invocation. The body itself is unchanged — `samtools flagstat ${bam} > $>` is what runs inside the container. The only thing the pipeline author sees is "I asked for samtools, I got samtools."

## What gets bind-mounted

Container engines only see files explicitly bind-mounted from the host. CGPipe figures out the minimum cover automatically:

- The working directory (`${job.wd}`).
- The temp body-file directory (`/tmp` by default, overridable via `cgpipe.container.body_dir`).
- Every declared input's parent directory.
- Every declared output's parent directory.
- Any absolute path that appears in the body's shell text (after CGPipe substitution).

Discovered paths are collapsed by common parent — if `/data/in/sample.bam` and `/data/out/result.bam` both appear, the wrap mounts `/data` once. System paths (`/`, `/bin`, `/usr`, `/etc`, `/proc`, `/sys`, `/lib`, `/var`, `/dev`, `/boot`, `/root`) are excluded — those should come from the image, not the host.

For paths the scanner can't see (shell variables, paths buried in tool config files), add them explicitly:

    cgpipe.container.bind = ["/shared", "/scratch"]      # global, every container job
    
    ${out}: ${bam}
        <% job.container.bind = ["/refs"] %>             # per-target
        ...

## Per-target images

Like other `job.*` settings, `job.container` can be set inside a target's `<% %>` block to use a different image per step:

    aligned.bam: reads.fq ref.fa
        <%
            job.container = "biocontainers/bwa:0.7.17"
            job.mem = "8G"
        %>
        bwa mem ${ref} ${reads} > $>.tmp && mv $>.tmp $>

    variants.vcf: aligned.bam ref.fa
        <%
            job.container = "biocontainers/bcftools:1.18"
        %>
        bcftools call ${aligned.bam} > $>

Each step pulls (or reuses) the right image; the pipeline doesn't need any extra glue.

## Singularity instead of Docker

The same pipeline runs on Singularity with a one-line config change:

    cgpipe.container.engine = "singularity"

No pipeline edits. CGPipe emits `singularity exec` instead of `docker run`, with the same auto-derived bind mounts, working directory, and image. Bare Docker Hub references get a `docker://` prefix prepended automatically so they pull the same images:

    job.container = "biocontainers/samtools:1.18"
    # Docker:      docker run … biocontainers/samtools:1.18 sh "$body"
    # Singularity: singularity exec … docker://biocontainers/samtools:1.18 sh "$body"

This separation — image-as-property-of-script, engine-as-property-of-run — is the same model Snakemake and Nextflow use, and it's what lets a pipeline you developed locally with Docker run unchanged on a Singularity-only HPC cluster.

## Picking the shell

CGPipe invokes the body inside the container with `sh "$bodyfile"`. `sh` works in alpine, distroless-with-shell, and almost every other image. Modern `sh` implementations (busybox `ash`, `dash`) support `set -o pipefail` and the substitutions, pipes, and conditionals most cgpipe bodies use.

If your body needs bash-specific syntax (arrays, `[[ ... ]]`, `${var^^}`, process substitution), opt up:

    cgpipe.container.shell = "bash"                # global

…or per-target:

    aligned.bam: reads.fq ref.fa
        <%
            job.container = "biocontainers/bwa:0.7.17"
            job.container.shell = "bash"
        %>
        ...

`bash` has to be installed in the image. Most bioinformatics containers have it; minimal images like `alpine:latest` don't (use `bash:latest` or `apk add bash` if you need it).

## Running on GPUs

For a single GPU job, set `job.gpu`:

    aligned.bam: reads.fq ref.fa
        <%
            job.container = "nvidia/cuda:12.0-base"
            job.gpu = 1
        %>
        cuda-aligner ${ref} ${reads} > $>

`job.gpu` is one setting that drives both layers:

- **Container side.** Docker emits `--gpus all` (for `job.gpu = 1`) or `--gpus N` (for larger counts). Singularity emits `--nv`. The container can see and use the host's GPUs.
- **Scheduler side.** SLURM emits `#SBATCH --gres=gpu:N`. PBS appends `:gpus=N` to the resource spec. SGE emits `#$ -l gpu=N`. The scheduler allocates GPUs for the job.

A typical GPU pipeline on a SLURM cluster running Singularity therefore needs exactly one setting per target — `job.gpu = 2` produces both `#SBATCH --gres=gpu:2` in the scheduler block AND `--nv` in the singularity invocation, no extra plumbing.

Requirements:

- **Docker:** `nvidia-container-toolkit` on the host.
- **Singularity:** NVIDIA drivers on the host; `--nv` does the rest.
- **NVIDIA-only in v1.** ROCm (AMD) and other accelerators are future work.

Clusters with non-standard syntax (SGE sites where the GPU complex isn't called `gpu`, Torque sites that want a different resource form) override the rendered directive via a custom job-submission template — see [Tutorial 10](10-custom-templates.md).

## Inspecting what CGPipe will run

When a container invocation misbehaves, the fastest path to understanding is the shell runner. It renders the entire pipeline into one bash script without submitting anything, so you can read exactly what docker (or singularity) will be asked to do:

    $ cgpipe -t cgpipe.runner=shell pipeline.cgp > pipeline.sh
    $ less pipeline.sh
    $ bash pipeline.sh    # run it locally

Each per-target function in `pipeline.sh` contains the full wrap — the `mktemp` for the body file, the `docker run -v … -u $(id -u):$(id -g) … sh "$body"`, everything. Same trick works with `cgpipe -dr` for any scheduler runner.

This is also useful when porting an existing pipeline to containers — generate the script, eyeball whether the auto-discovered binds cover everything the body touches, add explicit binds via `job.container.bind` if anything is missing, iterate.

## Platform notes

- **Symlinked `/tmp` on macOS.** `/tmp` on macOS is a symlink to `/private/tmp`. CGPipe resolves the canonical path before generating the bind mount so docker (which mounts the canonical target) sees the temp body file. Automatic; no setting needed.
- **Docker Desktop file sharing on macOS.** Default-shared paths are `/Users`, `/Volumes`, `/private/var/folders`. **`/tmp` is not in the default set.** A pipeline whose working directory or `cgpipe.container.body_dir` lives outside the shared list will fail with "No such file or directory" inside the container. Either add `/tmp` to Docker Desktop → Settings → Resources → File Sharing, or keep work under `/Users`.
- **Image must have a shell.** CGPipe invokes the body with `sh` (or `bash` if you override). Minimal images like distroless without a shell won't work.
- **User mapping is docker-only.** Docker gets `-u $(id -u):$(id -g)` so output files land with host ownership. Singularity already runs as the submitting user and doesn't need the mapping.

## Legacy: manual wrapping with `__pre__` / `__post__`

Before first-class support, the way to wrap jobs in containers was to put a `docker run … bash <<HEREDOC` in `__pre__` and the closing `HEREDOC` marker in `__post__`. That pattern still works (and is occasionally useful when you want full control over the invocation — for engines CGPipe doesn't natively support, or when you need to thread a complex setup):

    image ?= "biocontainers/samtools:1.18"
    wd ?= $(pwd)

    __pre__:
        docker run --rm \
            -v ${wd}:${wd} \
            -w ${wd} \
            -u $(id -u):$(id -g) \
            ${image} \
            bash <<'CGPIPE_BODY'

    __post__:
        CGPIPE_BODY

    ${out}: ${bam}
        samtools flagstat ${bam} > $>

But for the common case, just use `job.container = "..."` — it's shorter, the bind mounts are auto-derived, and switching engines doesn't require pipeline edits.

---

[← Tutorial 8](08-include.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 10 — custom job-submission templates →](10-custom-templates.md)
