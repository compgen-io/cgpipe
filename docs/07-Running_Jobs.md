# Running jobs

CGPipe doesn't execute target bodies itself. Instead, each rendered job script is handed to a *runner* — a backend that knows how to submit a job to a particular scheduler. CGPipe ships with runners for the most common HPC schedulers and a couple of local options:

| Runner | Used for | `cgpipe.runner` value |
|--------|----------|-----------------------|
| Shell | One-host execution; export to a single shell script | `shell` (default) |
| SGE / Open Grid Engine | Cluster | `sge` |
| SLURM | Cluster | `slurm` |
| PBS / Torque | Cluster | `pbs` |
| BatchQ | Lightweight queue (https://github.com/compgen-io/batchq) | `batchq` |
| Graphviz | Render the dependency graph; doesn't submit | `graphviz` |

Pick one with `cgpipe.runner = "..."`, usually in `~/.cgpiperc` or `/etc/cgpiperc`. Most other settings inherit from there — see [Configuration Reference](08-Configuration_Reference.md) for the loading order.

    # ~/.cgpiperc
    cgpipe.runner = "slurm"
    cgpipe.runner.slurm.account = "my-allocation"
    job.mail = "me@example.com"

## Job settings

The following `job.*` variables are honored by the scheduler runners. Set them globally in the script, in `.cgpiperc`, or per-target inside a `<% %>` block.

    job_setting       | description                            | sge | slurm | pbs | batchq | shell
    ------------------+----------------------------------------+-----+-------+-----+--------+------
    job.name          | Display name for the job               |  X  |   X   |  X  |   X    |
    job.procs         | CPUs per node                          |  X  |   X   |  X  |   X    |
    job.walltime      | Wall-clock limit (HH:MM:SS or similar) |  X  |   X   |  X  |   X    |
    job.mem           | Total RAM (e.g. "8G", "2048M")*        |  X  |   X   |  X  |   X    |
    job.gpu           | GPU count or spec (see "GPUs" below)   |  X  |   X   |  X  |        |
    job.stack         | Stack size                             |  X  |       |     |        |
    job.hold          | Submit with a user-hold                |  X  |   X   |  X  |   X    |
    job.env           | Capture current env into the job       |  X  |   X   |  X  |   X    |
    job.qos           | QoS request                            |     |   X   |  X  |        |
    job.queue         | Queue name                             |     |       |  X  |        |
    job.project       | Project tag                            |  X  |       |     |        |
    job.priority      | Priority                               |  X  |       |     |        |
    job.nice          | Niceness                               |     |       |  X  |        |
    job.wd            | Working directory                      |  X  |   X   |  X  |   X    |
    job.account       | Billing account                        |  X  |   X   |  X  |        |
    job.mail          | Email for status                       |  X  |   X   |  X  |   X    |
    job.mailtype      | When to mail                           |  X  |   X   |  X  |        |
    job.stdout        | Capture stdout                         |  X  |   X   |  X  |   X    |
    job.stderr        | Capture stderr                         |  X  |   X   |  X  |   X    |
    job.shell         | Shell binary for the body              |  X  |   X   |  X  |   X    |
    job.src           | Write the rendered script to this path |  X  |   X   |  X  |   X    |
    job.setup         | Lines prepended to the job body        |  X  |   X   |  X  |   X    |
    job.custom        | Extra lines added to the directive     |  X  |   X   |  X  |   X    |
                      | block (#SBATCH, #PBS, etc.)            |     |       |     |        |
    job.node.property | Node-property requirement              |     |       |  X  |        |
    job.node.hostname | Specific host                          |     |       |  X  |        |
    job.shexec        | Run directly on the submit host        |  X  |   X   |  X  |   X    |
    job.nopre         | Skip global __pre__ for this target    |  X  |   X   |  X  |   X    |
    job.nopost        | Skip global __post__ for this target   |  X  |   X   |  X  |   X    |

\* Memory takes the total amount; CGPipe converts to per-slot units for runners that need them.

`job.src` is useful for debugging: setting `job.src = "logs/%JOBID.cmd"` makes CGPipe write each rendered job script to disk under that path (with `%JOBID` replaced).

## Resources, per-target overrides

Set defaults at the top of the script:

    job.walltime = "24:00:00"
    job.mem = "4G"
    job.procs = 1

Override per-target inside its `<% %>` block:

    big_align.bam: reads.fq ref.fa
        <%
            job.procs = 16
            job.mem = "32G"
            job.walltime = "48:00:00"
        %>
        bwa mem -t ${job.procs} ${ref} ${reads} > $>

The target-local values are scoped to that target only and don't leak out.

## Direct execution

A target can be run *immediately* — bypassing the scheduler — by setting `job.shexec = true` inside it. Only targets without scheduler-bound dependencies are eligible; `__setup__`, `__teardown__`, and `__postsubmit__` are the typical use cases.

    __setup__:
        <% job.shexec = true %>
        mkdir -p logs results

    clean:
        <% job.shexec = true %>
        rm -rf results/*.tmp

For ad-hoc utility targets like `clean`, shexec keeps the operation synchronous and local.

## Shell on the job side

Each scheduler runner picks a shell for the rendered job body. The default lookup is `/bin/bash`, `/usr/bin/bash`, `/usr/local/bin/bash`, `/bin/sh` (in that order). Override globally:

    cgpipe.shell = "/usr/local/bin/bash"

…or per-target with `job.shell`.

## Per-runner configuration

Runners take settings under `cgpipe.runner.<name>.*`. All template-based runners (SGE/SLURM/PBS/BatchQ) share two:

- `cgpipe.runner.<name>.template` — path to a custom job template. The runner's built-in template covers most needs; override for cluster-specific quirks. Templates are themselves CGPipe code.
- `cgpipe.runner.<name>.global_hold` — when `true`, every submitted job gets a user-hold. After the whole pipeline submits successfully, holds are released in order. This guarantees the pipeline only starts if all of it submitted cleanly, and protects against fast jobs finishing before their dependants are submitted.

There's also `cgpipe.runner.include_output_filenames = true`, which makes CGPipe print each submitted jobid paired with its outputs instead of just the jobid.

### Shell runner

`cgpipe.runner = "shell"` (default) writes a single bash script with one function per target and dependency edges enforced by ordering. By default the script is written to stdout.

- `cgpipe.runner.shell.filename` — write to this file instead. Repeated pipelines can append to the same file; CGPipe rewrites job names to avoid collisions.
- `cgpipe.runner.shell.autoexec` — execute the generated script immediately instead of writing it. This makes `shell` behave like a fully local runner; only one job runs at a time.

### SGE / OGE

- `cgpipe.runner.sge.account` — global account.
- `cgpipe.runner.sge.parallelenv` — name of the parallel environment for multi-slot jobs. Defaults to `smp`.
- `cgpipe.runner.sge.hvmem_total` — when `true`, memory is requested as `-l h_vmem=TOTAL`. Default is per-slot.

### SLURM

No SLURM-only settings beyond the common ones. The standard `job.*` covers everything (cores, account, qos, mem, mail, walltime). Custom directives go in `job.custom`.

### PBS / Torque

- `cgpipe.runner.pbs.account` — global account.
- `cgpipe.runner.pbs.trim_jobid` — when `true`, strip the cluster suffix from job ids (`12345.cluster` → `12345`).
- `cgpipe.runner.pbs.use_vmem` — request memory as `vmem=…` instead of `mem=…`.
- `cgpipe.runner.pbs.ignore_mem` — drop memory requests entirely.

### BatchQ

[BatchQ](https://github.com/compgen-io/batchq) is a lightweight queue/scheduler used in some compgen.io deployments.

- `cgpipe.runner.batchq.path` — path to the `batchq` binary if it isn't on `$PATH`.
- `cgpipe.runner.batchq.batchqhome` — value for the `BATCHQ_HOME` environment variable, passed to subcommands.

The BatchQ template emits a couple of directives that the other schedulers don't:

- `#BATCHQ -input <file>` and `#BATCHQ -output <file>` — one line per declared input and output. BatchQ uses these for fileset tracking.
- `#BATCHQ -run-id <id>` — emitted when the variable `cgpipe.run_id` is set. The easiest way to set it is via the `CGPIPE_RUN_ID` environment variable:

      $ CGPIPE_RUN_ID="2026-06-03-run-7" cgpipe pipeline.cgp

  Every BatchQ job submitted by that invocation carries the same `-run-id`, even when the jobs aren't dependent on each other. This is the canonical way to group jobs from one pipeline run.

### Graphviz (no submission)

`cgpipe.runner = "graphviz"` doesn't submit anything. It emits a Graphviz `.dot` representation of the dependency graph to stdout. Use it for diagrams:

    $ cgpipe -t cgpipe.runner=graphviz pipeline.cgp > pipeline.dot
    $ dot -Tpng pipeline.dot -o pipeline.png

Cross-pipeline edges from the joblog aren't yet represented.

## Containers

Set the engine in `.cgpiperc` and the image per pipeline (or per target). CGPipe wraps every job in `docker run` or `singularity exec` automatically; no pipeline edits required to switch engines.

    # ~/.cgpiperc
    cgpipe.container.engine = "singularity"     # or "docker" / "apptainer"

    # in your pipeline
    job.container = "biocontainers/samtools:1.18"

    ${out}: ${bam}
        samtools flagstat ${bam} > $>

The full container settings:

| Setting | Type | Purpose |
|---|---|---|
| `cgpipe.container.engine` | string | `docker`, `singularity`, `apptainer` (alias for singularity). Unset disables wrapping. |
| `cgpipe.container.body_dir` | string | Where the temp body file is written and mounted from. Default `/tmp`. |
| `cgpipe.container.shell` | string | Shell used inside the container. Default `sh`. Set to `bash` for bash-only syntax. |
| `cgpipe.container.bind` | list | Extra bind mounts applied to every container job. |
| `cgpipe.container.env` | list | Names of host env vars passed through. |
| `cgpipe.container.docker_opts` | list | Raw flags appended to `docker run` (e.g. `--shm-size=4g`). |
| `cgpipe.container.singularity_opts` | list | Same, for `singularity exec`. |
| `cgpipe.container.user_map` | bool | When `true` (default) and engine is docker, add `-u $(id -u):$(id -g)`. |
| `job.container` | string | Image reference. Unset means "don't wrap this target." |
| `job.container.bind` | list | Extra binds beyond auto-discovered, per-target. |
| `job.container.env` | list | Extra env vars to pass through, per-target. |
| `job.container.opts` | list | Engine-specific raw flags, per-target. |
| `job.container.shell` | string | Per-target shell override. |

CGPipe auto-derives the bind-mount set from the working directory, declared inputs/outputs, body-discovered absolute paths, and the body-file directory. The denylist (`/`, `/bin`, `/sbin`, `/usr`, `/etc`, `/lib`, `/var`, `/proc`, `/sys`, `/dev`, `/boot`, `/root`) keeps system paths off the mount list — those should come from the image.

For the full story including per-target image overrides, the shell choice, GPU support, the macOS Docker Desktop file-sharing caveat, and the inspect-via-shell-runner workflow, see [Tutorial 9](tutorials/09-containers.md).

## GPUs

One setting, `job.gpu`, drives both the scheduler request and (when containers are in use) the container engine's GPU flag:

    aligned.bam: reads.fq ref.fa
        <%
            job.container = "nvidia/cuda:12.0-base"
            job.gpu = 2
        %>
        cuda-aligner ${ref} ${reads} > $>

Renders into both:

- A scheduler directive (`#SBATCH --gres=gpu:2`, PBS appends `:gpus=2` to the resource spec, `#$ -l gpu=2` for SGE).
- A container flag (`--gpus 2` for docker, `--nv` for singularity).

| Setting | Type | Purpose |
|---|---|---|
| `cgpipe.gpu` | bool / int / string | Global default applied to every job. |
| `job.gpu` | bool / int / string | Per-target spec. Overrides the global. |

Values:

| Value | What it means |
|---|---|
| unset / `false` / `0` | No GPU. |
| `true` | Equivalent to `1`. |
| Integer N (`2`, `4`, …) | Request N GPUs. |
| String (`"v100:2"`, `"device=0,1"`) | Passed through to whichever engines accept the syntax (SLURM uses GPU type:count; docker uses `--gpus device=…`). |

NVIDIA only in v1. Docker needs `nvidia-container-toolkit`; Singularity needs the NVIDIA driver on the host and uses `--nv` to bind in the libraries. ROCm and other accelerators are future work.

Cluster syntax that doesn't match the bundled rendering — SGE complex named `nvgpu` instead of `gpu`, PBS Pro using `select=…:ngpus=N` instead of Torque's `nodes=…:gpus=N` — overrides via a custom template (next section).

## Custom templates

The bundled templates handle common cases. When your cluster does something unusual — non-standard directive names, site-mandated billing, required module-loads — you point CGPipe at your own template instead:

    cgpipe.runner.slurm.template = "${HOME}/cgpipe/templates/site-slurm.template.cgp"

A template is a CGPipe script that emits the scheduler directives at the top, optional setup lines next, and `${job._body}` at the bottom. The bundled templates are a good starting point:

* [SLURMTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/SLURMTemplateRunner.template.cgp)
* [SGETemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/SGETemplateRunner.template.cgp)
* [PBSTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/PBSTemplateRunner.template.cgp)
* [BatchQTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/BatchQTemplateRunner.template.cgp)

Templates see every `job.*` value plus internal helpers (`job._body`, `job._inputs`, `job._outputs`). For a full worked example — taking the SLURM template and modifying it for a hypothetical site with a custom GPU complex, billing requirements, and a site-wide module-load — see [Tutorial 10](tutorials/10-custom-templates.md).

## Dry runs

`-dr` (dry run) prints the rendered job scripts to stdout instead of submitting them. Combine with `-v` for verbose details about which targets are scheduled and why:

    $ cgpipe -dr -v pipeline.cgp -reads sample.fq -ref ref.fa

This is the fastest way to verify that variable substitution, conditional flags, and resource settings render correctly before tying up cluster quota.

The `shell` runner (which writes a stand-alone bash script) is also a useful debugging tool — read the generated script before running it.

## Joblogs

Setting `cgpipe.joblog = "path/to/joblog.txt"` enables persistent job tracking. The joblog is one of the most important features in CGPipe — it's what lets multiple pipelines coordinate without needing a central daemon, a workflow engine, or any out-of-band state.

With a joblog in place CGPipe:

- Records every submitted job id, its outputs, the target name, and the runner state.
- Consults the joblog before building a target — won't resubmit a job that's still queued or running.
- Verifies stale entries with the runner so dead jobs don't permanently mask a missing output.

### Composing pipelines through the joblog

The intended workflow is many short, focused pipelines rather than one giant one. A typical project might have:

- A DNA alignment pipeline that produces `aligned.bam` and `aligned.bam.bai`.
- A variant-calling pipeline that consumes `aligned.bam` and produces `variants.vcf`.
- A QC pipeline that consumes both and produces summary reports.

These can be written by different people, live in different files, and be run at different times. As long as they all point at the same `cgpipe.joblog`, they coordinate:

1. You run the alignment pipeline. It submits a job for `aligned.bam` and records the job id and target in the joblog.
2. Before the alignment finishes, you run the variant-calling pipeline. It sees `aligned.bam` isn't on disk yet, but the joblog says a pending job will produce it, so the variant-caller submits with a dependency on that job rather than treating the input as missing or resubmitting an alignment job.
3. The QC pipeline runs later and sees that both inputs are already accounted for — pending or done — and wires up its dependencies accordingly.

Each pipeline stays small and reviewable, but the joblog lets them act like one connected DAG.

### Provenance: which job produced this file?

The joblog is also the source of truth for *which job created which output*. Pipelines fail mid-run, a parent job dies and orphans its children, files get re-created on a re-run — across all of that, the joblog records every claim. The last successful job that claimed a given output is the one that actually produced the file currently on disk.

This matters when you're debugging a stale or corrupt output: knowing the job id that wrote a particular file lets you find the script (saved via `job.src`), the logs (`job.stdout`/`job.stderr`), and the surrounding context. Without a joblog, you only have the file modification time and a guess.

### `cgsub`

The same joblog is what `cgsub` (CGPipe's standalone submit tool) reads and writes. Use `cgsub` to add a one-off job that should still participate in the joblog's bookkeeping — e.g., a manually-triggered re-alignment after fixing a bad parameter, without rewriting the pipeline file.
