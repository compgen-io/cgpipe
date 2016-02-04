cgpipe - minimum viable pipeline
====

Make for HPC analysis pipelines
----

CGPipe is a replacement for the venerable "make" that aims to automate
the process of running complex analysis pipelines on high-throughput clusters.
Make is a standard build tool for compiling software. The power of make is
that it takes a set of instructions for building various files, and given a
target output, make will determine what commands need to be run in order to
build those outputs. It basically answers the questions: what inputs are
needed? and how do I build those inputs? CGPipe aims to perform a similar
function for pipelines that run on high-throughput/performance clusters.
These pipelines can be quite elaborate, but by focusing on specific
transformations, you can make the pipeline easier to create and run.

Make is a powerful tool, but it is sometimes difficult to integrate into 
bioinformatics pipelines. Bioinformatics pipelines are typically run on an
HPC/HTC cluster using a batch scheduler. Each job could have different IO, 
memory, or CPU requirements. Finally, each pipeline could have to run on 
different clusters. Currently, only SGE/OGE and Grid Engine derivatives
include a grid-aware make replacement. However, it is primarily aimed at
directly building Makefiles, and that is somewhat limiting for more
complicated pipelines.

CGPipe is designed to run jobs that don't span multiple nodes. Most
bioinformatics pipelines require simple programs that execute on one node with
one or more threads. More complicated, multi-node jobs (MPI), are likely better
suited with custom execution scripts. We are willing to accept patches to
better support multi-node jobs, but don't have a lot of internal experience
with them.

See here for more documentation, including language specifics: https://github.com/compgen-io/cgpipe/tree/master/docs

