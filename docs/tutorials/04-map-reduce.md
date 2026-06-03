# Tutorial 4: map-reduce across chromosomes

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

## Toggling between chunked and single-shot

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

---

[← Tutorial 3](03-resources-and-flags.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 5 — opportunistic cleanup →](05-opportunistic-cleanup.md)
