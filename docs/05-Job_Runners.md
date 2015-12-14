
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
    job.mail       | Mail job status               |      |  X  |   X   |     |
    job.mailtype   | When to send mail             |      | [1] |  [2]  |     |
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
    
    1, 2 - job.mailtype has slightly different meanings for SGE and SLURM. The
           possible values are different for each scheduler.

    3 - the shell for the bash runner can be set using the global shell config

    4 - pre and post script are only included once for the bash runner, so if
        any job includes pre or post, then the final script will as well.

## Runner specific configuration

Runner configurations may be set using the form: `cgpipe.runner.{runner_name}.{option}`.

Each runner has different options, which are listed below.

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



## Bash script export

## Simple Job Queue (SJQ)

## SGE/OGE

## SLURM
