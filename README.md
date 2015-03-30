CGPipe - minimum viable pipeline
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

# Pipeline file syntax

## Contexts
There are two contexts in an CGPipe file: global and target. In the global
context, all uncommented lines are evaluated. Within the target context, 
any line prefixed with `#$` is evaluated as an CGPipe expression. 

When a target is defined, it captures the existing global context *at
definition*.

A target is defined using the format:

    output_file1 {output_file2 ... } : {input_file1 input_file2 ...}
        script snippet
        #$ cgpipe-expression
        ...
        continues
    # ends with outdent.

Any text that is indented in the target is assumed to be part of the script
that will be used to build the target file(s). When the indent is lost, then
the target context is closed.

Notes: In both global and target contexts, for-loops will dynamically add and
remove the iterating variable from the context.


## Data types

There are 6 primary data types in CGPipe: boolean, float, integer, list, range
and string. Booleans are either "true" or "false". Strings must be enclosed in
double quotes. Lists are initialized using the syntax "[]". Ranges can be used
to iterate over a list of numbers using the syntax "from..to".

Here are some examples:

    foo = "Hello world"
    foo = 1
    foo = 1.0

    isvalid = true

    list = []
    list += "one"
    list += "two"

    range = 1..10

## Variables

`foo = bar` Set a variable

`foo ?= bar` Set a variable if it hasn't already been set

`foo += bar` Append a value to a list (if the variable has already been set,
then this will convert that variable to a list)

`unset foo` Unsets a variable. Note: if the variable was used by a target,
it will still be set within the context of the target.

Variables may also be set at the command-line like this: `cgpipe -foo bar -baz 1 -baz 2`.
This is the same as saying:

    foo = "bar"
    bar = 2.59

## Lists

You can also create and access elements in a list:

    foo = []
    foo = [1, 2, "three"]

    print foo[2]
    >>> "three"

You can also append to lists:

    foo = ["foo"]
    foo += "bar"
    foo += "baz"

    print foo
    >>> "foo bar baz"


## Math

You can perform basic arithmetic on integer and float variables. Available
operations are:

* `+` add
* `-` subtract
* `*` multiplication
* `/` divide (integer division if on an integer)
* `%` remainder
* `**` power (2**3 = 8)

Operations are performed in standard order; however, you can also add also parentheses
around clauses to process things in a different order. For example:

    8 + 2 * 10 = 28
    (8 + 2) * 10 = 100
    8 + (2 * 10) = 28


## Logic

You can perform basic logic operations as well:

* `&&` and
* `||` or
* `!` not (or is unset)
* `==` equals
* `!=` not equals
* `<` less than
* `<=` less than or equals
* `>` greater than
* `>=` greater than or equals

You can chain these together to form more complex conditions. For example:

    foo = "bar"
    baz = 12

    if foo == "bar" && baz < 20
        print "test"
    endif


## Variable substitution
Inside of strings, variables can be substituted. Each string (including build script snippets)
will be evaluated for variable substitutions.

    ${var}          - Variable named "var". If "var" is a list, ${var} will
                      be replaced with a space-separated string with all
                      members of the list. **If "var" hasn't been set, then this
                      will throw a ParseError exception.**

    ${var?}         - Optional variable substitution. This is the same as
                      above, except that if "var" hasn't been set, then it
                      will be replaced with an empty string: ''.

    foo_@{var}_bar  - A replacement list, capturing the surrounding context.
                      For each member of list, the following will be returned:
                      foo_one_bar, foo_two_bar, foo_three_bar, etc...

    foo_@{n..m}_bar - A replacement range, capturing the surrounding context.
                      For each member of range ({n} to {m}, the following will
                      be returned: foo_1_bar, foo_2_bar, foo_3_bar, etc...

                      {n} and {m} may be variables or integers

### Printing

You can output arbitrary messages to stderr using the "echo" command.

Example:

    echo "Hello world"

    foo = "bar"
    echo "foo${bar}"


### Shell escaping
You may also include the results from shell commands as well using the syntax
`$(command)`. Anything surrounded by `$()` will be executed in the current shell.
Anything written to stdout can be captured as a variable. 

Example:

    submit_host = $(hostname)
    submit_date = $(date)


## If/Else/Endif
Basic syntax:

    if [condition]
       do something...
    elif [condition]
       do something...
    else
       do something else...
    endif


### Conditions
`if foo` - if the variable ${foo} was set
`if !foo` - if the variable ${foo} was not set or is false

`if foo == "bar"` - if the variable `foo` equals the string "bar"
`if foo != "bar"` - if the variable `foo` doesn't equal the string "bar"

`if foo < 1`    
`if foo <= 1`    
`if foo > 1`    
`if foo >= 1`    


## For loops
Basic syntax:

    for i in {start}..{end}
       do something...
    done

    for i in 1..10
        do something...
    done

    for i in list
       do something...
    done


## Build target definitions
Targets are the files that you want to create. They are defined on a single
line listing the outputs of the target, a colon (:), and any inputs that
are needed to build the outputs.

Any text (indented) after the target definition will be included in the
script used to build the outputs. The indentation for the first line will be
removed from all subsequent lines, in case there is a need for indentation to
be maintained. The indentation can be any number of tabs or spaces. The first
(non-blank) line that is at the *same* indentation level as the target
definition line marks the end of the target definition.

CGPipe expressions can also be evaluated within the target definition. These
will only be evaluated if the target needs to be built and can be used to 
dynamically alter the build script. Any variables that are defined within the
target can only be used within the target. Any global variables are captured
at the point *when the target is defined*. Global variables may not altered
within a target, but they can be reset within the context of the target
itself.

Example:

    output1.txt.gz output2.txt.gz : input1.txt input2.txt
        gzip -c input1.txt > output1.txt.gz
        gzip -c input2.txt > output2.txt.gz

You may also have more than one target definition for any given output
file(s). In the event that there is more than one way to build an ouput,
the first listed build definition will be tried first. If the needed inputs
(or dependencies) aren't available for the first definition, then the next
will be tried until all methods are exhausted.

In the event that a complete build tree can't be found, a ParseError will be
thrown.

### Wildcards in targets
Using wildcards, the above could also be rewritten like this:

    %.gz: %
        gzip -c $< > $>

Note: The '%' is only valid as a wildcard placeholder for inputs / outputs
of build targets. 

### Target substitutions
In addition to global variable substitutions, within a target these
additional substitutions are available. Targets may also have their own
local variables.

Note: For global variables, their values are captured when a target is
defined.

    $>              - The list of all outputs
    $>num           - The {num}'th output (starts at 1)

    $<              - The list of all inputs
    $<num           - The {num}'th input (starts at 1)


### Special targets
There are four special target names that can be added for any pipeline: 
`__pre__`, `__post__`, `__setup__`, and `__teardown__`. These are target
definitions that accept no input dependencies. `__pre__` is automatically
added to the start of the body for all targets.  `__post__` is automatically
added to the end of the body for all targets. `__setup__` and `__teardown__`
will always run as the first and last job in the pipeline.

You can selectively disable `__pre__` and `__post__` for any job by setting
the variable `job.nopre` and `job.nopost`.


## Including other files
Other Pileline files can be imported into the currently running Pipeline by
using the `include filename` statement. In this case, the directory of the
current Pileline file will be searched for 'filename'. If it isn't found, 
then the current working directory will be searched. If it still isn't found,
then an ParseError will be thrown.

## Logging
You can define a log file to use within the Pileline file. You can do this
with the `log filename` directive. If an existing log file is active, then
it will be closed and the new log file used. By default all output from the
Pipeline will be written to the last log file specified.

You may also specify a log file from the command-line with the `-l logfile`
command-line argument.

## Output logs
You can keep track of which files are scheduled to be created using an output log.
Do use this, you'll need to set the `cgpipe.joblog` variable. If you set a joblog,
then in addition to checking the local filesystem to see if a target already exists,
the joblog will also be consulted. This file keeps track of outputs that have already
been submitted to the job scheduler. CGPipe will also check with the job runner,
to verify that the job is still valid (running or queued).

This way you can avoid re-submitting the same jobs over and over again if you re-run
the pipeline.

## Comments
Comments are started with a `#` character. You may also include the '$' and '@'
characters in strings or evaluated lines by escaping them with a '\' character before 
them, such as `\$`.


# Pipeline runners (backends)
Right now there are 4 available backends for running pipelines: a combined bash
script (default), SGE/Open Grid Engine, SLURM, and a embedded job-runner SJQ
(see below).

Job runners are chosen by setting the configuration value `cgpipe.runner` in
`$HOME/.cgpiperc` to either: 'sge', 'slurm', 'sjq', or 'bash' (default).

Note: Slurm and SJQ support is still in development 


## HPC server backends
The more common use-case for CGPipe, however, is running jobs within an HPC
context. Currently, the only HPC job schedulers that are supported are SGE/Open
Grid Engine and SLURM. CGPipe integrates with these schedulers by dynamically
generating job scripts and submitting them to the scheduler by running
scheduler-specific programs (qsub/sbatch).

## Specifying requirements
Resource requirements for each job (output-target) can be set on a per-job
basis by setting CGPipe variables. Because of the way that variable scoping
works, you can set any of the variables below at the script or job level.


    setting name   | description                   | bash | sge | slurm | sjq |
    ---------------+-------------------------------+------+-----+-------+-----|
    job.name       | Name of the job               |      |  X  |   X   |  X  |
    job.procs      | Number of CPUs (per node)     |      |  X  |   X   |  X  |
    job.walltime   | Max wall time for the job     |      |  X  |   X   |     |
    job.nodes      | Number of nodes to request    |      |  X  |   X   |     |
    job.tasks      | Number of tasks               |      |     |   X   |     |
    job.mem        | Req'd RAM (ex: 2M, 4G) [*]    |      |  X  |   X   |  X  |
    job.stack      | Req'd stack space (ex: 10M)   |      |  X  |       |     |
    job.hold       | Place a user-hold on the job  |      |  X  |   X   |     |
    job.env   (T/F)| Capture the current ENV vars  |      |  X  |   X   |     |
    job.qos        | QoS setting                   |      |  X  |   X   |     |
    job.wd         | Working directory             |      |  X  |   X   |  X  |
    job.account    | Billing account               |      |  X  |   X   |     |
    job.mail       | Mail job status               |      | [1] |  [2]  |     |
    job.stdout     | Capture stdout to file        |      |  X  |   X   |  X  |
    job.stderr     | Capture stderr to file        |      |  X  |   X   |  X  |
    job.keepfailed | Keep outputs from failed jobs |  X   |  X  |   X   |     |
    job.shell      | Job-specific shell binary     | [3]  |  X  |   X   |     |


    global setting | description                   | bash | sge | slurm | sjq |
    ---------------+-------------------------------+------+-----+-------+-----|
    job.shexec(T/F)| Exec job; don't submit job    |  X   |  X  |   X   |  X  |
    job.nopre (T/F)| Don't include global pre      | [4]  |  X  |   X   |  X  |
    job.nopost(T/F)| Don't include global post     | [4]  |  X  |   X   |  X  |

    * - Memory should be specified as the total amount required for the job, if
        required, CGPipe will re-calculate the per-processor required memory.
    
    1, 2 - job.mail has slightly different meanings for SGE and SLURM. For
           each, it corresponds to the `-m` setting.

    3 - the shell for the bash runner can be set using the global shell config

    4 - pre and post script are only included once for the bash runner, so if
        any job includes pre or post, then the final script will as well.

### Runner specific settings
You can set runner specific settings by setting config values in
`$HOME/.cgpiperc`. These settings should be in the form:
`cgpipe.runner.{runner_name}.{option}`.

For SGE, SLURM, and SJQ, you have the option: `global_hold`. If 
`global_hold` is set to 'T', then a preliminary job will be submitted with a
user-hold set. All of the rest of the jobs will include this as a dependency.
Once the entire pipeline has been submitted (successfully), the user-hold will
be released and the pipeline can start. This is useful to make sure that any 
step of the pipeline will run if and only if the entire pipeline was able to 
be submitted. This also makes sure that quick running jobs don't finish before
their child jobs have been submitted. 

For SGE and SLURM, you can also set a global default account by using the
`default_account` option.

For SGE, there are two additional options: the name of the parallel
environment needed to request more than one slot per node (`parallelenv`;
`-pe` in qsub), and if the memory required should be specified per job or per
slot (`hvmem_total`; `-l h_vmem` in qsub). The default parallelenv is named
'shm' and by default `h_vmem` is specified on a per-slot basis
(`hvmem_total=F`).

The bash runner has one specific option that can be set: `autoexec`. If this
is set, then instead of writing the assembled bash script to stdout, the 
script will also be executed.

### Specifying the shell to use
CGPipe will attempt to find the correct shell interpreter to use for executing
scripts. By default it will look for `/bin/bash`, `/usr/bin/bash`, 
`/usr/local/bin/bash`, or `/bin/sh` (in order of preference). Alternatively,
you may set the config value `cgpipe.shell` in the `$HOME/.cgpiperc` file to
set a specific shell binary.

The shell may also be chosen on a per-job basis by setting the `job.shell`
variable for each job.

### Direct execution of jobs
Jobs can also be directly executed as part of the pipeline building process.
Instead of submitting the jobs to a scheduler, the jobs can be put into a
temporary shell script and executed directly. The global shell will be used
to run the script. If you would like a job to just run directly without being
scheduled, set the variable `job.shexec=true`. `__setup__` and `__teardown__` can
also be directly executed instead of scheduled.

One use for this is to setup any output folders that may be required. For example:

    __setup__:
        #$ job.shexec = true
        mkdir -p output


Another common use-case for this is having a `clean` target to remove all
output files to perform a fresh set of calculations. For example:

    clean:
        #$ job.shexec = true
        rm *.bam

