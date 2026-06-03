# Build targets

A *target* describes one or more output files, the input files those outputs depend on, and the script that turns the inputs into the outputs. Targets are the unit of work CGPipe submits to a scheduler.

The basic shape:

    output1 [output2 ...] : [input1 input2 ...]
        shell line 1
        shell line 2
        ...

The target definition ends at the first non-blank line that returns to the original indentation level.

## A complete example

    sorted.bam: input.bam
        samtools sort -o sorted.bam input.bam

When CGPipe is asked for `sorted.bam`, it sees the rule above, checks whether `input.bam` exists (or can itself be built), and — if `sorted.bam` is missing or older than `input.bam` — submits the body to the configured runner.

## Multiple outputs and inputs

A single target can produce more than one file, and depend on more than one:

    paired_1.fq paired_2.fq: raw.fq adapters.fa
        cutadapt -a file:adapters.fa --paired-output paired_2.fq -o paired_1.fq raw.fq

If any of the named outputs is requested, the whole rule runs once and produces all of them.

## Special substitutions inside the body

Inside the target body these tokens expand at job-submission time:

| Token | Expands to |
|-------|------------|
| `$>` | All outputs, space-separated |
| `$>N` | The N'th output (1-based: `$>1`, `$>2`, …) |
| `$<` | All inputs, space-separated |
| `$<N` | The N'th input (1-based) |
| `$%` | The wildcard stem (when used with `%` wildcards) |
| `${var}` | Any CGPipe variable in scope at target-definition time |
| `${{var}}` | Double-evaluated: substitute `var`, then re-evaluate the result |

These are CGPipe substitutions and happen *before* the shell sees the body. The shell still sees its own `$VAR` and `$1` and so on.

    out.txt: input.txt
        wc -l $< > $>
        echo "first input was $<1, primary output is $>1"

## Target-local CGPipe blocks

Inside a target body, code wrapped in `<% %>` runs as CGPipe code at job-submission time and can set per-job variables (job name, resources, conditional flags). Everything outside `<% %>` is treated as shell text.

    sorted.bam: input.bam
        <%
            job.name = "sort-${input.basename()}"
            job.mem = "8G"
            job.walltime = "4:00:00"
        %>
        samtools sort -@ ${job.procs} -o $>.tmp $< && mv $>.tmp $>

Single-line variants and conditional fragments work too:

    aligned.bam: reads.fq ref.fa
        bwa mem -t ${job.procs} \
            <% if rg %>-R "${rg}" \<% endif %>
            ${ref} ${reads} | samtools view -b - > $>

The `<% if rg %>...<% endif %>` only inserts the `-R "${rg}"` flag if `rg` is set. There's no syntax error if it's not.

`print` inside a target body appends to the script being built, **not** to stdout. This is occasionally useful for debug output, but more often a sign that you wanted the line outside the target.

### Variable scoping in targets

A target captures the surrounding (global) context at definition time, like a closure. The target body can *read* any variable that was in scope when the target was defined, but it can't *set* a global variable in a way that leaks out:

    foo = "global"
    sorted.bam: input.bam
        <% foo = "local" %>
        echo "this target sees ${foo}"
    # back in global scope: foo is still "global"

Per-job settings like `job.mem`, `job.name`, `job.procs` are also target-local — set them inside the target's `<% %>` block.

## Multiple definitions for the same output

You can define the same output more than once with different inputs. CGPipe tries each definition in source order; the first one whose inputs are all satisfiable wins:

    alignment.bam: reads_R1.fq reads_R2.fq
        bwa mem ${ref} reads_R1.fq reads_R2.fq | samtools view -b - > $>

    alignment.bam: interleaved.fq
        bwa mem -p ${ref} interleaved.fq | samtools view -b - > $>

If neither set of inputs can be produced or found on disk, CGPipe errors with a "no build path" message.

## Wildcards

`%` matches one or more characters in an output name and the matching stem is reused on the input side. Inside the body, the stem is available as `$%`.

    %.gz: %
        gzip -c $< > $>

This rule says: to produce any file ending in `.gz`, gzip the same name with `.gz` stripped. So `report.txt.gz` would be built from `report.txt`.

`%` is only valid in the target definition line (outputs and inputs). Use `$%` to reference the captured stem inside the body.

## Temporary outputs

Prefix an output with `^` to mark it as *temporary*: intermediate state that's only needed to satisfy downstream rules.

    ^intermediate.bam: raw.bam
        samtools sort raw.bam > intermediate.bam

    final.vcf: intermediate.bam
        bcftools call intermediate.bam > final.vcf

Temporary outputs differ from regular outputs in three ways:

1. **Not required to exist on disk.** If the downstream output (`final.vcf` above) is already current, the temp job is skipped entirely — even if the temp file has been deleted.
2. **Not checked against the filesystem.** Only non-temporary outputs are compared by modification time when CGPipe decides whether to rebuild.
3. **Tracked separately.** Temporary outputs show as `TEMP` in pending-job status and the joblog.

The `^` is a marker only — it's stripped before the filename is passed to the shell. The actual file on disk is `intermediate.bam`, not `^intermediate.bam`.

Use temp outputs for large intermediates (sorted BAMs, pre-merge per-chromosome shards) whose only purpose is to feed the next step.

## Opportunistic jobs

A target with no outputs — just a leading colon and a list of inputs — is an *opportunistic* job:

    : qc_summary.html alignment.bam
        zip qc_bundle.zip qc_summary.html alignment.bam

Opportunistic jobs run **after** the rest of the pipeline has been submitted. They never force their inputs to be built. They only run if all of their inputs are already available — either as existing files on disk, as jobs submitted earlier in the same run, or as recorded successes in the joblog.

If any input is missing and no upstream rule will produce it, the opportunistic job is silently skipped.

Use opportunistic jobs for cleanup, post-hoc reporting, or optional bundling that should only happen if the main pipeline produced what it needs:

    : ${out}
        <% for o in tmpfiles %>
            rm -f ${o}
        <% done %>

Opportunistic jobs compose with temp outputs: if a temp file was skipped because the downstream target was already current, an opportunistic job that depends on that temp file is also skipped.

## Special targets

CGPipe recognizes five reserved target names:

| Name | When it runs |
|------|--------------|
| `__pre__` | Prepended to the body of every other target (unless disabled) |
| `__post__` | Appended to the body of every other target (unless disabled) |
| `__setup__` | Once, as the first job in the pipeline |
| `__teardown__` | Once, as the last job in the pipeline |
| `__postsubmit__` | Once per submitted job, immediately after the job is submitted to the scheduler |

`__pre__` and `__post__` are the right place for per-job preamble/postamble — start time, environment dump, error trap, end time:

    __pre__:
        echo "Inputs: $<"
        echo "Outputs: $>"
        echo "Start: $(date)"

    __post__:
        echo "End: $(date)"

`__setup__` is typically used to create output directories before any other job runs:

    __setup__:
        <% job.shexec = true %>
        mkdir -p output logs

The `job.shexec = true` makes the setup run *directly* (on the submission host) instead of being submitted as a scheduler job — usually what you want for `mkdir`-style setup. `__setup__`/`__teardown__` are the only targets that can be shexec.

`__postsubmit__` is always shexec. It runs once for each submitted job and has access to that job's id via the runner's variable surface. Useful for adding submitted jobs to an external tracking system.

To skip the global `__pre__`/`__post__` for a specific target, set `job.nopre = true` or `job.nopost = true` in that target's `<% %>` block.

## Dynamic target generation

Targets can be defined inside `for` loops, `if` branches, or anywhere else in the global context — they're a statement like any other. This lets you generate one target per element of a list (one per chromosome, sample, lane, …) and then a separate merge target that depends on all of them.

The pattern has three pieces:

1. A list of values to iterate over.
2. An empty *accumulator list*.
3. A `for` loop that defines a per-value target **and** appends each target's output to the accumulator.
4. A downstream merge target whose inputs are the accumulator, expanded with `@{accumulator}`.

### Worked example: per-chromosome variant calling

    chroms = "chr1 chr2 chr3 chr22 chrX chrY".split(" ")
    per_chrom_vcfs = []

    for chrom in chroms
        per_chrom_vcfs += "calls.${chrom}.vcf"

        ^calls.${chrom}.vcf: aligned.bam aligned.bam.bai ref.fa
            <%
                job.name = "call-${chrom}"
                job.mem = "8G"
                job.walltime = "12:00:00"
            %>
            bcftools call -r ${chrom} -f ${ref} aligned.bam > $>.tmp && mv $>.tmp $>
    done

    final.vcf: @{per_chrom_vcfs}
        <%
            job.name = "merge-vcfs"
            job.mem = "4G"
        %>
        bcftools concat $< > $>.tmp && mv $>.tmp $>

What's happening:

- `chroms` is a list (5 entries in the example).
- `per_chrom_vcfs` starts empty and grows by one inside the loop.
- Each iteration defines a new build target with a chromosome-specific output. `^` marks it temporary so the per-chromosome files don't need to stick around once `final.vcf` is built.
- After the loop, `per_chrom_vcfs` contains the five output names. The merge target's input list `@{per_chrom_vcfs}` expands to all five.
- `${chrom}` inside the target captures the loop variable's value at the point the target is defined — each target ends up with its own `chrom`.

When you ask CGPipe to build `final.vcf`, it discovers that `final.vcf` needs all five per-chromosome VCFs, plans them, and submits them with proper dependency edges so the merge job waits for all of them.

### `@{list}` expansion in detail

`@{var}` expands a list into multiple items at parse time. It works in three places:

- Target output and input lines: `@{outs}: @{ins}` expands to a target with each output and each input listed separately.
- Inside a string literal: `"prefix_@{list}_suffix"` produces one string per list element, with prefix and suffix preserved. For a list `["a","b"]` this yields `"prefix_a_suffix prefix_b_suffix"`.
- Range form: `@{1..N}` works just like a list with elements `1, 2, …, N`. `N` can be a variable.

Compare with `${var}`, which substitutes a single value (joining lists with spaces) — useful inside a body, where you usually want one shell argument that lists everything:

    ${final.vcf}: @{per_chrom_vcfs}
        # body sees $< as "calls.chr1.vcf calls.chr2.vcf ..." via ${var} semantics
        bcftools concat ${per_chrom_vcfs} > $>

### Conditional structure inside the loop

A common pattern is to choose what gets built based on a flag, with the merge target reading from an accumulator that the loop populated:

    if by_chrom
        per_chrom_bams = []
        for chrom in chroms
            per_chrom_bams += "aligned.${chrom}.bam"

            ^aligned.${chrom}.bam: raw.fq ref.fa
                bwa mem -R "@RG\tID:${chrom}" ${ref} ${reads} | samtools view -b -o $> - 
        done

        aligned.bam: @{per_chrom_bams}
            samtools merge $> $<
    else
        aligned.bam: raw.fq ref.fa
            bwa mem ${ref} ${reads} | samtools view -b -o $> -
    endif

A single flag `by_chrom` swaps the whole pipeline between a one-job alignment and a parallel per-chromosome split-then-merge.

## Importable target snippets

If two or more targets share a chunk of body, you can factor it out as an importable snippet — a target with a single name followed by two colons:

    common::
        echo "Job started at $(date)"
        set -euo pipefail

    out1.txt: input1.txt
        <% import common %>
        process input1.txt > out1.txt

    out2.txt: input2.txt
        <% import common %>
        process input2.txt > out2.txt

`import` only works inside a target body. For sharing variables and target definitions across files, use `include` instead (see [Language Syntax](03-Language_Syntax.md#including-other-files)).

Importable snippets are also how `__pre__` and `__post__` are implemented internally — you can think of them as targets that are imported automatically into every job.
