
# Pipeline runners (backends)
Right now there are 5 available backends for running pipelines: a combined s
script (default), SGE/Open Grid Engine, PBS, SLURM, and single-user SBS 
(also from compgen.io, see below).

Job runners are chosen by setting the configuration value `cgpipe.runner` in
`$HOME/.cgpiperc` to either: 'pbs', 'sge', 'slurm', 'sbs', or 'shell' (default).

Example:

    cgpipe.runner="sbs"
    cgpipe.runner.sbs.sbshome="/home/users/me/.sbs"


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


    setting name      | description                           | shell | sge | slurm | pbs | sbs |
    ------------------+---------------------------------------+-------+-----+-------+-----+-----|
    job.name          | Name of the job                       |       |  X  |   X   |  X  |  X  |
    job.procs         | Number of CPUs (per node)             |       |  X  |   X   |  X  |  X  |
    job.walltime      | Max wall time for the job             |       |  X  |   X   |  X  |     |
    job.mem           | Req'd RAM (ex: 2M, 4G) [*]            |       |  X  |   X   |  X  |  X  |
    job.stack         | Req'd stack space (ex: 10M)           |       |  X  |       |     |     |
    job.hold          | Place a user-hold on the job          |       |  X  |   X   |  X  |  X  |
    job.env   (T/F)   | Capture the current ENV vars          |       |  X  |   X   |  X  |     |
    job.qos           | QoS setting                           |       |  X  |   X   |  X  |     |
    job.nice          | Job "nice" setting                    |       |     |       |  X  |     |
    job.queue         | Specific queue to submit job to       |       |     |       |  X  |     |
    job.wd            | Working directory                     |       |  X  |   X   |  X  |  X  |
    job.account       | Billing account                       |       |  X  |   X   |  X  |     |
    job.mail          | Mail job status                       |       |  X  |   X   |  X  |  X  |
    job.mailtype      | When to send mail                     |       | [1] |  [2]  |  X  |     |
    job.stdout        | Capture stdout to file                |       |  X  |   X   |  X  |  X  |
    job.stderr        | Capture stderr to file                |       |  X  |   X   |  X  |  X  |
    job.shell         | Job-specific shell binary             |  [3]  |  X  |   X   |  X  |     |
    job.node.property | Property requirement for an exec node |       |     |       |  X  |     |
    job.node.hostname | Exact host to run job on              |       |     |       |  X  |     |

     
    global setting    | description                           | shell | sge | slurm | pbs | sbs |
    ------------------+---------------------------------------+-------+-----+-------+-----+-----|
    job.shexec(T/F)   | Exec job; don't submit job            |   X   |  X  |   X   |  x  |  X  |
    job.nopre (T/F)   | Don't include global pre              |  [4]  |  X  |   X   |  x  |  X  |
    job.nopost(T/F)   | Don't include global post             |  [4]  |  X  |   X   |  x  |  X  |

    * - Memory should be specified as the total amount required for the job, if
        required, CGPipe will re-calculate the per-processor required memory.
    
    1, 2 - job.mailtype has slightly different meanings for SGE and SLURM. The
           possible values are different for each scheduler.

    3 - the shell for the shell runner can be set using the global shell config,
        but the defaults are "/bin/bash", "/usr/bin/bash", "/usr/local/bin/bash", 
        "/bin/sh" (in that order of priority).

    4 - pre and post script are only included once for the shell runner, so if
        any job includes pre or post, then the final script will as well.

## Runner specific configuration

Runner configurations may be set using the form: `cgpipe.runner.{runner_name}.{option}`.

Each runner has different options, which are listed below.

For PBS, SGE, SLURM, and SBS, you have the option: `cgpipe.runner.{runner_name}.global_hold`. If 
`global_hold` is `true`, then each job will have a user-hold placed on it until
the entire pipeline has been submitted. Once the entire pipeline has been 
submitted (successfully), the user-hold will be released and the pipeline can 
start. This is useful to make sure that any step of the pipeline will run if 
and only if the entire pipeline was able to be submitted. This also makes sure 
that quick running jobs don't finish before their child jobs have been submitted. 
If there is an issue with submitting any of the jobs, then all jobs will be aborted
before they are released for execution.

For PBS, SGE, and SLURM, you can also set a global default account by using the
`cgpipe.runner.{runner_name}.account` option.


### Template script

PBS, SGE, SLURM, and SBS job runners all operate by processing a job template,
setting the appropriate variables, and then calling the appropriate executable
to submit the job (`qsub`, `sbatch`, or `sbs`). A basic job template is included
in CGPipe for each of these schedulers; however, if you'd like to use your own
template, this can be specified by setting the variable `cgpipe.runner.{runner_name}.template`.
As with all other options, this can be done on an adhoc, per-user or per-host basis. 
If you'd like to write your own templates, the templates are themselves written 
as CGPipe scripts and can include logic and flow-control.

    * https://github.com/compgen-io/cgpipe/blob/master/src/java/io/compgen/cgpipe/runner/PBSTemplateRunner.template.cgp
    * https://github.com/compgen-io/cgpipe/blob/master/src/java/io/compgen/cgpipe/runner/SBSTemplateRunner.template.cgp
    * https://github.com/compgen-io/cgpipe/blob/master/src/java/io/compgen/cgpipe/runner/SGETemplateRunner.template.cgp
    * https://github.com/compgen-io/cgpipe/blob/master/src/java/io/compgen/cgpipe/runner/SLURMTemplateRunner.template.cgp

## Shell script export

Shell scripts will write a single script that contains all of the tasks for a given 
pipeline as functions in the script. When the script is executed, each of the  functions
will be executed in order. In case multiple pipelines need to be run, the variable `cgpipe.runner.shell.filename` 
can be set. If this is set, then each successive pipeline will be added to this single
script file.

By default the script is written to stdout. 

The shell runner has one specific option that can be set: `cgpipe.runner.shell.autoexec`. If this
is set, then instead of writing the assembled shell script to stdout, the 
script will be immediately executed.


## Simple Batch Scheduler (SBS)

https://github.com/compgen-io/sbs

SBS has two other options (`cgpipe.runner.sbs.`): `sbshome` and `path`. `sbshome` sets where
the SBS job scripts will be tracked. By default this is in the current directory under `.sbs`. However,
this can be set by the `$SBSHOME` environmental variable or overridden by this property. `path` is 
the path to the `sbs` program, if it isn't part of your `$PATH`. 


## PBS (Torque/PBS)

PBS has three unique options (`cgpipe.runner.pbs.`): `trim_jobid`, `use_vmem` and `ignore_mem`.
`trim_jobid` will trim away the cluster name from the jobid returned from `qsub`. If your cluster
returns something like "1234.cluster.hostname.org" from qsub, but requires "1234" for `qstat`, etc...
then this option will trim away the "cluster.hostname.org" from the captured jobid. `use_vmem` will 
set all memory restrictions with `-l vmem=XX` as opposed to the default `-l mem=XX`. And `ignore_mem`
will ignore any memory restrictions whatsoever in the job submission script (this may cause
problems with your cluster or job scheduler, so use at your own risk).


## SGE/OGE

For SGE, there are two additional options (`cgpipe.runner.sge.`): the name of the parallel
environment needed to request more than one slot per node (`parallelenv`;
`-pe` in qsub), and if the memory required should be specified per job or per
slot (`hvmem_total`; `-l h_vmem` in qsub). The default parallelenv is named
'shm' and by default `h_vmem` is specified on a per-slot basis
(`hvmem_total=F`).



## SLURM

SLURM has no additional options that haven't already been mentioned above.