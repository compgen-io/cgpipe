mvpipe - minimum viable pipeline
===

Make for HPC analysis pipelines
---

# Overview

mvpipe is a tool designed for submitting jobs to HPC job schedulers. The syntax
is loosely based on the Makefile syntax. It is a declarative format that
defines target files and a script snippet to build those targets. You then
specify which files you want to build, and then the script figures out which 
jobs need to be submitted to the scheduler, and sets the proper dependencies. 
In this way, mvpipe is similar to using Makefiles. However, mvpipe also allows
for an extra level of scripting both globally and within target definitions.
This extra level of scripting includes for-loops and if/then blocks. Also, the
make tool runs all jobs on a single host, whereas mvpipe will submit the jobs
to an existing job scheduler (SGE, Slurm, PBS, etc...) for execution.


# Syntax

## Contexts
There are two contexts in an mvpipe file: global and target. In the global
context, all uncommented lines are evaluated. Within the target context, 
any line prefixed with `#$` is evaluated as an mvpipe expression. 

When a target is defined, it captures the existing global context *at
definition*.

A target is defined using the format:

    output_file1 {output_file2 ... } : {input_file1 input_file2 ...}
        script snippet
        continues
        continues
    # ends with outdent.

Any text that is indented in the target is assumed to be part of the script
that will be used to build the target file(s). When the indent is lost, then
the target context is closed.

Notes: In both global and target contexts, a for-loop (iteration) will create
a new child-context. If/then sections do *not* create a separate context.
