
# What is CGPipe?

CGPipe is a language for building data analysis pipelines. It is a declarative programming language similar to Makefiles, however instead of directly executing target scripts, jobs are submitted to a dedicated job scheduler, such as PBS, SGE, or SLURM.

You define output files, which input files they need, and the script required to get from input > output. Each output file (or job) is defined separately. So what you end up with is a directed acyclic graph (DAG).

    input > file1 --> file2 > file3 > ... > output1
                 \
                  \-> file1b > output2


or map-reduce style...

	# split input
    input > file1 + file2 + file3

    # map
    file1 > fileA
    file2 > fileB
    file3 > fileC

    #reduce
    fileA + fileB + fileC > output

You give CGPipe these definitions and the name of the file you ultimately want. Knowing these task definitions and the desired output, CGPipe will figure out how to "walk the graph" to figure out what files/jobs are needed. It also backtracks the walk in case there are multiple ways to get to your output. CGPipe will look first *on-disk* for required input files. It will only submit a job to build a missing or out of date file. If an input is newer than an output, that output file is considered out of date. So far, this is the same as how `make` works, except for the job submission part... where `make` executes the tasks, CGPipe submits them as jobs to a scheduler.

## Similarities and differences to Makefiles

CGPipe pipelines have a very simliar syntax to Makefiles, particularly in the way that build-targets are defined.

* Jobs are defined based on the files they will produce as output. The pipeline author can define which output files will be created, and which input files are required for a particular task. After that, a shell script snippet is included that defines the commands that need to be executed to produce the defined output files.

* Jobs dependencies are automatically calculated and a build-graph produced. Jobs are then executed in the proper order along that build-graph to produce the required outputs.

* CGPipe pipelines can be included in other pipelines.

* If a given output file exists, it will not be rebuilt unless a defined input file will be rebuilt. CGPipe extends this to also track outputs/jobs that have been submitted to a job scheduler. If an built-target requires an input that has already been submitted to the scheduler, that input will not be resubmitted, rather the existing job will be listed as a job dependency.

In this way, CGPipe is very similar to the `qmake` program that is available for SGE clusters, which executes unmodified Makefiles using the SGE scheduler. This allows for some parallelization. But there are some key differences between CGPipe pipelines and using `qmake` to execute an unaltered Makefile.

* CGPipe allows you to specify job requirements, such as execution time, CPUs, and memory. These requirements can be set on a job or pipeline basis, allowing the pipeline author to set requirements globally or for only individual tasks. For example, the `account` setting can be set for all tasks, but `walltime` could be set on a per-task basis.

* `qmake` runs interactively, submitting jobs in order and waiting for the results before submitting the next job. Because of this, `qmake` needs to keep running on the job submission host. With CGPipe, the entire pipeline is submitted to the job scheduler at once. There is no need to have a watchdog script running, as the job scheduler will automatically cancel any dependent jobs in the event of an error.

* Command line arguments can be easily used to set variables within the script.

* Makefiles don't include any type of flow control (if/else, for-loops), but CGPipe is a full language that includes if/else conditions and for-loops for iterating over a list of values or a range. Everything in a CGPipe line can be scripted, including target definitions. This means that an author could define a pipeline that executed in a Map-Reduce pattern where an input file is split into `N` number of chunks, each chunk could be processed in parallel, and then the results could be merged back together after each chunk was processed. This type of pipeline could be written in a traditional Makefile (verbosely), but by allowing build targets to be included in for-loops, the number of chunks can now be a run-time option and written in an easily readable syntax.

* Build targets are scriptable with CGPipe templates

* `qmake` is only available for SGE clusters and isn't available for other job schedulers. CGPipe pipelines can execute on SGE or SLURM systems as well as on single hosts with SBS or bash script exports (see below).

* Multiple targets can be defined for the same output files, enabling multiple execution paths to build the same output files. This means that there can be multiple set of jobs defined to yield the same output file(s), based upon what input files are available. For example, you could have paired-end next-generation sequencing reads stored in two separate FASTQ files or one interleaved FASTQ file. Either of these inputs could be used in a read alignment step to produce a BAM file, but the arguments for the alignment program may be slightly different, depending on which type of input is used. With CGPipe, you can specify two different targets for the same output file, with the targets prioritized in the order they are defined in the pipeline. If the first target (or any of its inputs) can't be used, then the next target is attempted until all possible build-graphs are exhausted.

* Pipelines can be stored remotely, such as on a GitHub repository or web server.

* Pipelines can be used as executable scripts using the #! (shebang) first line syntax (like bash, perl, ruby, or python scripts).

* Pipelines can display help text. If the first lines are comments, then they will be displayed when help text is requested (excluding #! first lines). The first blank or non-commented line marks the end of the help text.

## Executing pipelines

It is expected that jobs will be executed on an HPC cluster with a job scheduler (SGE, SLURM, or PBS supported). This way individual tasks can be efficiently executed in a parallel manner. CGPipe will take care of setting inter-task dependencies to make sure that jobs execute in the proper order. Pipelines can also be run on a single host by using either the simple [SBS scheduler](http://compgen.io/sbs) or by exporting the pipeline as a bash script. SBS is well suited for single-host systems where there is no existing job scheduler. SBS requires having the `sbs` program installed somewhere in your `$PATH`.
