# How CGPipe compares to other pipeline tools

CGPipe sits in a crowded field of bioinformatics pipeline runners. This chapter contrasts CGPipe with the three most likely alternatives — Snakemake, Nextflow, and WDL — to help you decide which is the right fit for a given project and to translate concepts between the tools.

The short version:

| | CGPipe | Snakemake | Nextflow | WDL |
|---|---|---|---|---|
| **Model** | Make-like (output-first) targets | Make-like (output-first) rules | Channel-based (data-flow) | Typed, call-based |
| **Host language** | Custom DSL (`.cgp`) | Python + custom DSL | Groovy DSL | Custom DSL |
| **Scheduler integration** | SGE / SLURM / PBS / BatchQ / SBS / bash | SLURM / SGE / PBS / Kubernetes / cloud (DRMAA-like) | SLURM / SGE / PBS / Kubernetes / AWS Batch / Google Batch / Azure | Cromwell / miniwdl / Terra back-ends |
| **Containers** | Manual via `__pre__`/`__post__` HEREDOC | First-class (`container:` directive) | First-class (`container` directive, profiles) | First-class (`runtime { docker: }`) |
| **Cross-pipeline composition** | `include` (source-level) plus a persistent joblog (file-level coordination across unrelated pipelines) | Workflow imports (Python) | Pipeline subworkflows (Groovy) | Imported `.wdl` files |
| **Typical scale** | Small to large pipelines, focus on HPC clusters | Same | Cloud-native and HPC | Typed, often used in large consortia (GATK, broadinstitute) |
| **Best at** | Lightweight, shell-script-feeling pipelines that integrate with existing scheduler/joblog | Reproducible scientific pipelines with strong defaults | Cloud-portable workflows with rich data-flow operators | Strict typing, sharing across institutions |

The rest of this chapter walks through each tool in more depth and shows how a common pattern (per-chromosome variant calling) looks in each.

## A distinguishing feature: pipelines are executable scripts

The single largest day-to-day difference between CGPipe and the alternatives is that a CGPipe pipeline file *is* an executable script — same shebang line, same `chmod +x`, same argument handling as a bash or Python program:

    #!/usr/bin/env cgpipe
    #
    # Options:
    #     --bam FILE   input BAM
    #     --out FILE   output VCF

    if !bam
        print "ERROR: --bam is required"
        exit 1
    endif
    ...

    $ ./call-variants -bam sample.bam -out sample.vcf

That short detail has consequences:

- **Arguments are first-class.** `-name value` pairs on the command line become CGPipe variables. No JSON inputs file, no YAML config, no `--config key=value` indirection. A user typing `./pipeline.cgp -reads sample.fq -ref hg38.fa` is using a normal Unix CLI.
- **Pipelines compose like other Unix tools.** A CGPipe pipeline can be invoked from a shell loop, a Makefile, another CGPipe pipeline (via shell escape or `cgsub`), a CI job, or a wrapper script. There's nothing magical about it — it's just a program that happens to submit jobs.
- **Conditional structure resolves at invocation time.** The same pipeline file can produce different build graphs depending on its arguments. `--by_chrom true` swaps the whole topology between a single job and a 24-way fan-out (see [Tutorial 4](tutorials/04-map-reduce.md)). The script *is* the pipeline; you're not editing a config to change behavior.
- **Help text is just comments.** Leading `#` lines become `--help` output. No separate manifest. Authors get free documentation just by writing comments at the top.
- **`?=` lets pipelines configure themselves.** Defaults fall back to environment variables, config files, or hard-coded values — but the invocation always wins. Composing pipelines into larger workflows means *passing arguments down*, not editing config files at each layer.

This is fundamentally different from the alternatives:

- **Snakemake** runs as `snakemake --config sample=foo` against a `Snakefile`, or via `--configfile config.yaml`. Workable, but invocation has the shape "tell snakemake about a workflow file" rather than "run this script."
- **Nextflow** runs as `nextflow run pipeline.nf --reads sample.fq`. Better than Snakemake here, but the `nextflow` runtime is a layer in front of the pipeline.
- **WDL** is the heaviest: you write a JSON inputs file with typed values and submit it to an engine (`cromwell run pipeline.wdl --inputs inputs.json`). Excellent for reproducibility at the cost of fluidity for ad-hoc work.

When you're iterating on a new pipeline, writing a wrapper that submits N variations of one pipeline with different arguments, or composing several small pipelines into a project-level workflow, the executable-script model is dramatically lighter weight. When you're publishing a fixed pipeline for many users to run with rigorous reproducibility, the heavier alternatives offer guarantees CGPipe doesn't.

## vs. GNU Make and `qmake`

CGPipe started from Make's mental model: targets, prerequisites, recipes. The differences are summarized in [Introduction](01-Introduction.md). The short version: CGPipe doesn't run recipes itself — it submits each target as a job. It also has flow control (`if`/`for`), variables that persist across rules, and a persistent joblog. `qmake` (the SGE-aware Make) is closer to CGPipe in intent than `make` is, but it's SGE-only and lacks the language affordances.

## vs. Snakemake

[Snakemake](https://snakemake.readthedocs.io/) is the closest neighbor — both tools use Make's output-first target model, both expand into job submissions, and both think in terms of dependency graphs.

### Common ground

- Rule/target is `output: input` and a body.
- Wildcards expand patterns across many concrete outputs.
- Re-runs only build what's missing or stale.
- Both support major HPC schedulers.

### Where they differ

- **Host language.** Snakemake rules live inside a Python file (`Snakefile`); top-level statements are real Python. CGPipe pipelines are written in a small DSL that doesn't embed a host language. Python is more powerful; the CGPipe DSL is smaller and easier to keep in your head.
- **Wildcard semantics.** Snakemake's `{wildcards}` are namespaced per-rule and matched by regex; CGPipe's `%` is a single stem captured into `$%`. Snakemake's approach is more flexible at the cost of more cognitive overhead.
- **Configuration.** Snakemake reads YAML/JSON configs by convention. CGPipe uses `.cgpiperc` files and command-line `-name value` pairs. CGPipe doesn't have a built-in config-schema concept.
- **Containers.** Snakemake has first-class container support (`container: "docker://..."` per rule, plus Singularity profiles). In CGPipe you wire containers through `__pre__`/`__post__` HEREDOCs — flexible but more verbose. See [Tutorial 9](tutorials/09-containers.md).
- **Cross-pipeline composition: two mechanisms.** Snakemake composes via Python `include:` and subworkflows. CGPipe has both `include` (source-level inlining — same idea as Snakemake's `include:`; see [Tutorial 8](tutorials/08-include.md)) *and* a persistent joblog. The joblog is the part Snakemake doesn't have: pipelines that don't share source — written separately, maybe by different people, run at different times — still coordinate as long as they point at the same joblog file. See [Running Jobs §Joblogs](07-Running_Jobs.md#joblogs).
- **Reports and DAG visualization.** Snakemake ships rich HTML reports and DAG renderers. CGPipe has a `graphviz` runner that emits a `.dot` file; for richer reporting you compose external tools.

### Same pattern, different tools

A per-chromosome variant-calling fan-out, in CGPipe:

    chroms = "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y".split(" ")
    per_chrom = []

    for c in chroms
        per_chrom += "${out}.${c}.vcf"
        ^${out}.${c}.vcf: ${bam} ${ref}
            bcftools mpileup -r chr${c} -f ${ref} ${bam} \
                | bcftools call -mv - > $>.tmp && mv $>.tmp $>
    done

    ${out}: @{per_chrom}
        bcftools concat -O z -o $>.tmp $< && mv $>.tmp $>

The Snakemake equivalent:

    CHROMS = [str(i) for i in range(1, 23)] + ["X", "Y"]

    rule all:
        input: config["out"]

    rule call_chrom:
        input:
            bam = config["bam"],
            ref = config["ref"]
        output:
            temp("{out}.{chrom}.vcf")
        shell:
            "bcftools mpileup -r chr{wildcards.chrom} -f {input.ref} {input.bam} "
            "| bcftools call -mv - > {output}.tmp && mv {output}.tmp {output}"

    rule merge:
        input:
            expand("{out}.{chrom}.vcf", out=config["out"], chrom=CHROMS)
        output:
            config["out"]
        shell:
            "bcftools concat -O z -o {output}.tmp {input} && mv {output}.tmp {output}"

### When to pick which

- **Pick CGPipe** when your environment already has a working scheduler, you want a small DSL that reads almost like shell, you value being able to compose pipelines both ways (`include` for source-level reuse *and* a shared joblog for coordinating pipelines you don't want to merge into one source file).
- **Pick Snakemake** when you want Python in the pipeline (data structures, helper functions, complex parameter logic), when you need first-class container support, or when you're going to share the pipeline with a wider community where Snakemake is the lingua franca.

## vs. Nextflow

[Nextflow](https://www.nextflow.io/) takes a different model entirely — instead of declaring outputs and inferring the graph, you build the graph explicitly with *processes* connected by *channels* in a data-flow style. The result is more portable across compute environments (especially clouds), at the cost of a steeper learning curve.

### Mental model

- **Processes** are typed units of work (inputs/outputs declared as channels).
- **Channels** are queues that move data between processes; you compose them with operators like `map`, `flatten`, `groupTuple`, `mix`.
- DSL2 makes processes reusable as modules.

### Where it differs from CGPipe

- **Graph construction.** Nextflow is data-flow-first — you describe how data moves between processes. CGPipe is output-first — you describe what outputs exist and what they need. For static fan-out/merge patterns (per-chromosome work) both work fine; for complex branching channel logic Nextflow has the better abstractions.
- **Containers and cloud.** Nextflow has the broadest cloud back-end support (AWS Batch, Google Batch, Azure, Kubernetes) of any of the four tools here. If your work runs on managed cloud queues, Nextflow's ecosystem will save you time.
- **State and resumes.** Nextflow's `-resume` uses an internal session cache. CGPipe's joblog plays a similar role but is plain-text and easy to grep — different trade-offs around debuggability and shareability.
- **Language.** Nextflow scripts are Groovy with a process DSL; CGPipe is its own small DSL. Groovy is more powerful and more complex.

### The same per-chromosome fan-out in Nextflow

    nextflow.enable.dsl=2

    params.bam = null
    params.ref = null
    params.out = null

    chroms = Channel.of(*((1..22)*.toString() + ['X', 'Y']))

    process call_chrom {
        input:  tuple val(chrom), path(bam), path(ref)
        output: path("${params.out}.${chrom}.vcf")
        script:
        """
        bcftools mpileup -r chr${chrom} -f ${ref} ${bam} \\
          | bcftools call -mv - > ${params.out}.${chrom}.vcf
        """
    }

    process merge {
        input:  path(vcfs)
        output: path("${params.out}")
        script:
        """
        bcftools concat -O z -o ${params.out} ${vcfs}
        """
    }

    workflow {
        call_chrom(chroms.map { c -> tuple(c, file(params.bam), file(params.ref)) })
            | collect
            | merge
    }

### When to pick which

- **Pick CGPipe** when you're targeting a single cluster or a small number of hosts, you want to be able to read a rendered job script line-by-line, and you don't need the channel abstractions.
- **Pick Nextflow** when you need cloud portability, the channel operators are a natural fit for your data flow (e.g., grouping samples by lane, demultiplexing then re-merging), or when you're sharing with a community that already uses nf-core.

## vs. WDL / Cromwell

[WDL](https://openwdl.org/) (Workflow Description Language) is a typed, declarative DSL designed for portability across execution engines (Cromwell, miniwdl, Terra, dxWDL). It's the lingua franca for many large genomics consortia (Broad Institute's GATK pipelines, many TCGA-era tools).

### Mental model

- **Tasks** are units of execution with typed inputs/outputs and a command block.
- **Workflows** call tasks (and other workflows) and connect them via outputs.
- Strong typing: every input and output declares its type (`File`, `Int`, `Array[File]`, …).
- Execution is delegated to an *engine* (Cromwell is the reference); the engine handles HPC, cloud, containers.

### Where it differs from CGPipe

- **Types.** WDL is statically typed; CGPipe is dynamically typed. WDL's type discipline is what makes it readable and shareable at scale; it also makes quick iteration heavier (more boilerplate per task).
- **Execution engine.** WDL workflows don't run themselves — you submit them to Cromwell (or another WDL engine). CGPipe is the engine *and* the language.
- **Containers.** WDL tasks declare `runtime { docker: "..." }` and the engine handles invocation. In CGPipe you wrap with `__pre__`/`__post__`.
- **Where it shines.** Cross-institutional collaboration. WDL files are intended to be shared and re-run on someone else's infrastructure with minimal modification. CGPipe is more lightweight but assumes you're running on infrastructure you control.

### Same fan-out in WDL

    version 1.0

    workflow variants_by_chrom {
        input {
            File bam
            File ref
            String out_prefix
        }
        Array[String] chroms = ["1","2","3","4","5","6","7","8","9","10",
                                "11","12","13","14","15","16","17","18",
                                "19","20","21","22","X","Y"]

        scatter (c in chroms) {
            call call_chrom {
                input: bam = bam, ref = ref, chrom = c, prefix = out_prefix
            }
        }

        call merge {
            input: vcfs = call_chrom.vcf, out_prefix = out_prefix
        }

        output {
            File merged_vcf = merge.merged
        }
    }

    task call_chrom {
        input {
            File bam
            File ref
            String chrom
            String prefix
        }
        command <<<
            bcftools mpileup -r chr~{chrom} -f ~{ref} ~{bam} \
              | bcftools call -mv - > ~{prefix}.~{chrom}.vcf
        >>>
        output {
            File vcf = "${prefix}.${chrom}.vcf"
        }
        runtime { docker: "biocontainers/bcftools:1.18" }
    }

    task merge {
        input {
            Array[File] vcfs
            String out_prefix
        }
        command <<<
            bcftools concat -O z -o ~{out_prefix} ~{sep=' ' vcfs}
        >>>
        output { File merged = "${out_prefix}" }
        runtime { docker: "biocontainers/bcftools:1.18" }
    }

### When to pick which

- **Pick CGPipe** for fast iteration, small-to-medium pipelines, when you own the infrastructure, or when you want shell-script-grade access to what's actually being submitted.
- **Pick WDL** for pipelines that will be shared across organizations, where strict typing pays off, or when you're already invested in the Cromwell/Terra ecosystem.

## Interoperability

CGPipe can interoperate with the others in three useful ways:

- **The shell runner exports a bash script.** That script can be invoked by anything (including another workflow engine). It's how you take a CGPipe pipeline and hand it to a system that doesn't speak CGPipe.
- **The joblog is plain text.** Other tools can inspect or update it; it's not a black-box database.
- **(Planned) WDL output runner.** A future `cgpipe.runner = "wdl"` would emit a WDL workflow file representing the pipeline's job graph, letting you author in CGPipe and submit to Cromwell or another WDL engine. See the project plan for status.

## Summary

CGPipe's distinguishing features compared to the alternatives:

- **Pipelines are executable scripts.** Shebang line, command-line arguments, help text from comments — the same shape as bash or Python. Composable into wrappers, shell loops, and other pipelines without any runtime indirection.
- **Small DSL, low ceremony.** No host language to learn, no type system to satisfy.
- **Two composition models.** `include` (source-level) is familiar from Make, Snakemake, Nextflow. The persistent joblog is the part you don't get elsewhere — pipelines that don't share source can coordinate through a shared joblog file, without an external workflow daemon.
- **Job scripts are first-class.** The rendered script is what the scheduler sees; `-dr` shows it before submission and `job.src` saves it after.
- **Targeted at HPC clusters today.** Cloud and K8s back-ends are on the roadmap (see the project plan for the WDL and K8s runner sketches) but not first-class yet.

When that's the right shape for your work — small-to-medium pipelines, fast iteration, HPC infrastructure you control — CGPipe is the smallest tool that does the job. When you need typed contracts, cloud portability, rich data-flow abstractions, or rigorous reproducibility guarantees for many external users, one of the alternatives will fit better.
