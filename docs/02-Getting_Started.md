

# Getting started

Note: CGPipe is intended to run on a Unix/Linux workstation, server, or cluster (including macOS).

CGPipe is a Java program that is packaged as either a self-executing fat JAR 
file, or as an embeddable library. CGPipe is a Java program, but it has been 
developed to run on *nix-style hosts such as Mac OSX, FreeBSD or Linux. The 
only pre-requisite is a working installation of Java 1.7 or better. It is 
untested on Windows.


## Installation

### Step 0: Install Java

Java version 7 or higher can be used. Install Java from your Linux distribution (using apt-get or yum), or see the [Oracle site](http://www.oracle.com/technetwork/java/javase/downloads/index.html) for more details.

### Step 1: Download cgpipe

You can download the `cgpipe` program from the [CGPipe](http://compgen.io/cgpipe/downloads) website. You can save this file anywhere, but it is easiest to use if it is included on your `$PATH` somewhere, such as in `/usr/local/bin` or in a personal `$HOME/bin` directory.

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

CGPipe configuration or job variables can be set on a per-run, per-user, or 
per-server basis. At startup, CGPipe looks for configuration information in the following
locations (in order of preference):

1. From the CGPipe JAR itself at: `io/compgen/cgpipe/cgpiperc`,
1. `/etc/cgpiperc`, 
1. `$CGPIPE_HOME/.cgpiperc` (CGPIPE_HOME defaults to the directory containing the cgpipe binary), 
1. `~/.cgpiperc` (user-local config),
1. The environmental variable `CGPIPE_ENV` (semi-colon delimited)

These configuration files are CGPipe scripts that are 'included' with the primary CGPipe script 
(the running pipeline). This means that they can inherit options from each other.

Examples:

* Do you want to set `job.mail` for all of your personal scripts? Then set the value in `~/.cgpiperc`. 
* Do you want to configure the batch scheduler settings for everyone on a given system? Then set `job.runner` in
 `/etc/cgpiperc` or `$CGPIPE_HOME/.cgpiperc`. 
* Do you have a network shared folder with CGPipe installed that is mounted across different clusters? Then you 
can still set the CGPipe runner config from `$CGPIPE_HOME/.cgpiperc` by setting the `job.runner` config values within 
an if/then/endif block (cgpiperc files are full cgpipe scripts that are interpretted, so you can make them quite customizeable).
