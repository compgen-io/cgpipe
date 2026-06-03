# Getting started

CGPipe runs on Linux, macOS, and any other Unix-like system. Windows is untested. The only runtime dependency is Java 11 or newer.

## Installation

### Step 0: Install Java 11+

Install from your package manager (`apt-get install default-jre`, `brew install openjdk@21`, etc.) or download from [Adoptium](https://adoptium.net/). Confirm with:

    $ java -version
    openjdk version "11.0.20" 2023-07-18
    ...

### Step 1: Install cgpipe

Two options:

**Pre-built binary.** Download the latest `cgpipe` from the [releases page](https://github.com/compgen-io/cgpipe/releases) and put it on your `$PATH` — `/usr/local/bin/cgpipe` or `~/bin/cgpipe` are both fine. The same artifact handles all schedulers; runner selection happens at configuration time.

**Build from source.**

    $ git clone https://github.com/compgen-io/cgpipe.git
    $ cd cgpipe
    $ ant jar
    # produces dist/cgpipe (a self-executing fat JAR)

Copy `dist/cgpipe` to a directory on your `$PATH`.

### Step 2: Run a test pipeline

Most analysis pipelines you run will be custom written, however you can verify that your CGPipe installation is working with the following script:


	[hello.cgp]
	#!/usr/bin/env cgpipe

	print "Hello from cgpipe!"

This will load `cgpipe` from your path and execute the above script. Right now the script doesn't do anything other than
print a message to the console. If you save this as `hello.cgp` (and make it executable with `chmod +x hello.cgp`), you 
should see the following:

	$ ./hello.cgp
	Hello from cgpipe!

or...

	$ cgpipe hello.cgp
	Hello from cgpipe!

We will build from here to demonstate how to make your own pipelines in the next sections.

### Step 3: Configure CGPipe for your computing environment

CGPipe can run on a single user workstation, server, or HPC cluster. If you want to run more complex 
workflows by submitting jobs to a scheduler, it's necessary to configure CGPipe to use your scheduler. 
In CGPipe job submission is handled by "job runners".

The currently supported job schedulers are: SBS, SGE, SLURM, or PBS. For more information about available 
runners, or the possible configuration settings, see "Running jobs".

If no scheduler is configured, jobs will be written as a bash script to stdout.

For information about how to configure CGPipe, see the "Configuring CGPipe" section below.

## Running CGPipe

As shown above, CGPipe can be run either from the command-line (`cgpipe mypipeline.cgp`) or a pipeline script can be made executable
and cgpipe loaded with the shebang (`!#`) first line definition like any other scripting language. It is recommended that
the script method be used and you install CGPipe to a location in your `$PATH`. This way, you can use the following format to 
start your script:

    #!/usr/bin/env cgpipe


## Configuring CGPipe

CGPipe layers configuration from several sources, with later sources overriding earlier ones:

1. Bundled defaults inside the JAR.
2. `/etc/cgpiperc` — system-wide.
3. `$CGPIPE_HOME/.cgpiperc` — install-local (defaults to the directory containing the `cgpipe` binary).
4. `~/.cgpiperc` — your user-level config.
5. The `CGPIPE_ENV` environment variable, evaluated as CGPipe code (semicolon-separated statements).
6. Command-line variables (`-name value`).
7. The pipeline script itself.

Each `.cgpiperc` file is a full CGPipe script. You can use `if`, `include`, and any other language feature inside them.

Typical setup:

    # ~/.cgpiperc — your defaults
    cgpipe.runner = "slurm"
    job.mail = "me@example.com"
    job.mailtype = "FAIL,END"
    cgpipe.joblog = "${HOME}/.cgpipe/joblog.txt"

…then per-pipeline:

    # at the top of pipeline.cgp
    runid ?= "run.$(date +%Y%m%d-%H%M)"
    cgpipe.log = "logs/pipeline-${runid}.log"
    job.stdout = "logs/"
    job.stderr = "logs/"

For exhaustive coverage of every variable and the precedence rules, see [Configuration Reference](08-Configuration_Reference.md).

## Where to next

- [Pipeline Tutorials](06-Pipeline_Tutorials.md) — work through a handful of synthetic examples that build from "copy one file" to "map-reduce across chromosomes."
- [Language Syntax](03-Language_Syntax.md), [Build Targets](05-Build_Targets.md), [Methods Reference](04-Methods_Reference.md) — the language reference.
- [Running Jobs](07-Running_Jobs.md) — picking and configuring a runner.
