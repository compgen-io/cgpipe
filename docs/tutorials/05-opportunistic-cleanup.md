# Tutorial 5: opportunistic cleanup for storage efficiency

The map-reduce pattern in [Tutorial 4](04-map-reduce.md) produces 24 per-chromosome VCFs that exist only to feed the merge. Marking them `^` (temp) means CGPipe won't waste time *checking* them on disk — but the files still *exist* on disk after each per-chromosome job runs. On a real dataset (per-chromosome BAMs, intermediate alignments, sorted shards) that can easily be hundreds of gigabytes of intermediate state.

Opportunistic jobs are how you reclaim that space without compromising restartability. The idea:

- An opportunistic job (target with no outputs, only inputs — `: input1 input2 ...`) runs only when **all** of its inputs are available.
- If you make the inputs the final merged output **plus** every temp file, the cleanup only fires after the merge has succeeded.
- And because opportunistic jobs don't force their inputs to be built, re-runs where the merge is already up-to-date don't fire the cleanup either — the disk is already clean.

## Basic cleanup

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

## Defensive cleanup

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

## Why this matters

On a multi-sample alignment + variant-calling project with, say, 50 samples and 24 chromosomes per sample, the per-chrom intermediate BAMs are ~3 TB. Without opportunistic cleanup, that 3 TB stays on disk indefinitely. With it, the disk usage tracks roughly the size of the merged outputs.

The same pattern works for any intermediate-heavy fan-out: per-lane FASTQ shards, per-region pileups, per-window depth files. Wherever Tutorial 4's pattern produces fan-out outputs, a Tutorial 5 cleanup rule belongs alongside it.

---

[← Tutorial 4](04-map-reduce.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 6 — shared __pre__ and __post__ →](06-pre-post.md)
