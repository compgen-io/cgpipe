# Tutorial 10: custom job-submission templates

CGPipe ships with built-in templates for every supported scheduler (SLURM, SGE, PBS, BatchQ). They cover the common cases. When your cluster does something unusual — a non-standard GPU complex, a site-mandated billing directive, a required module-load before every job — you don't have to live with the default rendering. You hand CGPipe your own template, and it uses that one instead.

This is one of CGPipe's most useful escape hatches and it's the right answer whenever the rendered job script needs to look different from what the bundled template produces.

## When to reach for a custom template

Three motivating cases:

1. **Cluster uses non-standard directive names.** The bundled SGE template emits `#$ -l gpu=N` for `job.gpu`, but your site's SGE complex is called `nvgpu`. The PBS template uses the Torque-style `nodes=1:ppn=N:gpus=M`, but your cluster runs PBS Pro and wants `select=1:ncpus=N:ngpus=M`. A custom template renders the directive your scheduler actually accepts.
2. **Site mandates additional directives.** Project accounting, billing tags, custom queue selection logic that depends on `job.mem`, a `#SBATCH --constraint=…` for hardware features. None of these are universally portable, so they don't belong in the bundled template — but they're trivial to add to a site-specific override.
3. **You want site-wide setup injected into every job.** Loading a module, sourcing a profile, exporting a license-server hostname. A custom template puts those lines in once and every submitted job picks them up.

## Starting point

Templates are themselves CGPipe scripts, so the bundled template for your runner is a working starting point — copy it and edit. The current bundled templates live at:

- [SLURMTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/SLURMTemplateRunner.template.cgp)
- [SGETemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/SGETemplateRunner.template.cgp)
- [PBSTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/PBSTemplateRunner.template.cgp)
- [BatchQTemplateRunner.template.cgp](https://github.com/compgen-io/cgpipe/blob/main/src/java/io/compgen/cgpipe/runner/BatchQTemplateRunner.template.cgp)

Save your copy somewhere stable — your home directory, a per-project `templates/` folder, a `/etc/cgpipe/` if you're configuring the cluster for everyone.

## What the template sees

When CGPipe renders a target into a job script, the template runs with every `job.*` setting available as a CGPipe variable, plus a handful of internal helpers:

| Variable | What it holds |
|---|---|
| `job.name`, `job.mem`, `job.walltime`, `job.procs`, `job.queue`, `job.account`, `job.wd`, etc. | The standard per-job settings the target (or the pipeline globally) configured. |
| `job.gpu` | The unified GPU count/spec. Read it however your scheduler expects (`#SBATCH --gres=gpu:${job.gpu}`, `#PBS -l ngpus=${job.gpu}`, etc.). |
| `job.stdout`, `job.stderr` | Stdout / stderr capture paths. The bundled SLURM template detects when these are directories and appends `slurm-%j.out`; copy that pattern if you want it. |
| `job.container`, `job.container.bind`, `job.container.shell`, etc. | Container settings, if you're using them. The template doesn't render container directives — `${job._body}` already contains the wrapped form (see Tutorial 9). |
| `job.custom` | A list of arbitrary additional directives the pipeline appended. The bundled templates emit each item on its own `#SBATCH` / `#PBS` line. |
| `job.setup` | A list of shell lines to inject between the directive block and the body — useful for `module load`, `source /opt/site/env.sh`, etc. |
| `job._body` | The rendered target body (possibly container-wrapped). Put `${job._body}` at the bottom of your template after all the setup. |
| `job._inputs`, `job._outputs` | Lists of the target's declared input and output filenames. Rarely needed in the template itself; the BatchQ template uses them for its `#BATCHQ -input` / `-output` directives. |
| Anything from the global context | If your pipeline (or `.cgpiperc`) set a global variable like `site_account`, the template can read `${site_account}` directly. |

## Template syntax refresher

Templates use the same CGPipe template engine as `<% %>` blocks inside target bodies. The shapes you'll use most:

    <% if job.foo %>
    #SCHED --foo ${job.foo}
    <% endif %>

    <% if job.bar > 100 %>
    #SCHED --big-job
    <% endif %>

    <% for item in job.custom %>
    #SCHED ${item}
    <% done %>

    <%
        # multi-line CGPipe code block
        if job.mem
            mem_gb = job.mem.sub("G","")
            if mem_gb > 64
                queue = "bigmem"
            endif
        endif
    %>

Everything outside `<% ... %>` is emitted verbatim into the rendered job script.

## A worked example: SLURM with site customizations

Suppose our SLURM cluster:

- Uses `nvgpu` as the GPU complex instead of `gpu`.
- Mandates a billing account for every job via `#SBATCH --account=${project_code}`, where `project_code` is a variable every pipeline sets.
- Wants `/opt/site/modules.sh` sourced at the top of every job script.

Copy the bundled SLURM template to `~/cgpipe/templates/site-slurm.template.cgp` and edit it. The relevant diff:

    -<% if job.gpu %>
    -#SBATCH --gres=gpu:${job.gpu}
    -<% endif %>
    +<% if job.gpu %>
    +#SBATCH --gres=nvgpu:${job.gpu}
    +<% endif %>
    +
    +<% if project_code %>
    +#SBATCH --account=${project_code}
    +<% endif %>

…and at the top of the body section (before `${job._body}`):

    +# Site-wide environment
    +source /opt/site/modules.sh
    +
     <% for line in job.setup %>
     <% if line %>
     ${line}
     <% endif %>
     <% done %>
     
     ${job._body}

Wire it in via `~/.cgpiperc`:

    cgpipe.runner = "slurm"
    cgpipe.runner.slurm.template = "${HOME}/cgpipe/templates/site-slurm.template.cgp"

Every pipeline that runs through `cgpipe.runner = "slurm"` now uses your template. Pipelines themselves don't change — they keep saying `job.gpu = 2`, `project_code = "P-12345"`, etc. The rendering details live in the template.

## Where the template lives

A few patterns that work well:

- **Per-user.** Put the template in `~/cgpipe/templates/` and point `~/.cgpiperc` at it. Survives reboots, easy to version-control alongside your pipelines.
- **Per-project.** Keep the template alongside the pipeline files (e.g. `pipelines/templates/site-slurm.template.cgp`), and point a project-level `.cgpiperc` at it. Useful when different projects need different templates.
- **Site-wide.** Put the template in `/etc/cgpipe/templates/` and the `cgpipe.runner.<runner>.template` setting in `/etc/cgpiperc`. Every user on the cluster picks it up without configuration.

The precedence is: command-line `-t` overrides the pipeline script, which overrides `~/.cgpiperc`, which overrides `/etc/cgpiperc`. So a user can override the site default for one run if they need to.

## Verification

Render a real pipeline with `-dr` to see what the custom template produces without actually submitting anything:

    $ cgpipe -dr pipeline.cgp -bam sample.bam ...

The dry-run output shows the exact job script that *would* go to `sbatch`. Confirm the new directives are present, the deletions are gone, and the body is intact. When the rendering looks right, drop the `-dr` to submit for real.

If something looks wrong, the template is just a CGPipe script — variables not in scope show up as parse errors, conditionals that should fire but don't usually mean the setting wasn't actually set. `dumpvars` inside the template prints every variable in scope at that point, which is handy for debugging.

---

[← Tutorial 9](09-containers.md) · [Tutorials index](../06-Pipeline_Tutorials.md)
