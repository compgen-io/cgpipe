

# Quickstart

The quickest way to get started with CGPipe is to use one of the pre-made pipelines that are available from the compgen.io pipeline repository. The list of these can be found from the [compgen.io](http://compgen.io) website or from the [compgen-io/cgpipe-pipelines GitHub repository](https://github.com/compgen-io/cgpipe-pipelines).

## Step 0: Install Java

## Step 1: Download cgpipe

You can download the `cgpipe` program from the [CGPipe](http://compgen.io/cgpipe/downloads) website. You can save this file anywhere, but it is easiest to use if it is included on your `$PATH` somewhere, such as in `/usr/local/bin`.

## Step 2: Run an example pipeline

Each pipeline is going to be fairly custom, however there are a few supported pipelines that are available on [compgen-io/cgpipe-pipelines GitHub repository](https://github.com/compgen-io/cgpipe-pipelines). The supported pipelines are for bioinformatics based workflows, but CGPipe could be useful in a variety of domains. The supported pipelines include an RNA-seq pipeline and a basic DNA-seq mapping pipeline. The RNA-seq pipeline uses the [ngsutilsj](http://compgen.io/ngsutilsj) suite for data pre-processing and the [STAR](http://github.com/adobbin/STAR) aligner and requires [bwa](http://bwa), [samtools](http://htssdk.org), [ngsutilsj](http://compgen.io/ngsutilsj), and [STAR](http://github.com/adobbin/STAR) to be installed. The DNA-seq pipeline requires [bwa](http://bwa), [samtools](http://htssdk.org), and [ngsutilsj](http://compgen.io/ngsutilsj) to be installed. Both pipelines can use split-FASTQ files, a single interleaved FASTQ file, or unmapped BAM files as input.

For more information on available remote pipelines, run `cgpipe -h -f compgen_io:pipelines`.

