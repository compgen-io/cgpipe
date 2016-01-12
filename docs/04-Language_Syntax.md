
# Language syntax

The CGPipe language is a very simple syntax that is similar to most other languages.

The source repository contains a set of test scripts that have examples of all statements and operations available. These test scripts are the definitive source for the language syntax. These are the test scripts that are run to verify each build of CGPipe. In any case where this documentation conflicts with the test scripts, the test scripts are correct.

Test scripts are available in the `src/test-scripts` directory and are named `*.cgpt or *.cgpipe`.

## Contexts
There are two contexts in an CGPipe file: global and target. In the global
context, all uncommented lines are evaluated. Within the target context, 
any lines wrapped with `<% %>` is evaluated as an CGPipe expression. The
target context is a "template" mode, where the areas not wrapped in `<% %>`
are treated as the body of the job-script to execute. Any whitespace present
in the target body is kept and not stripped. In a target, any print
statements will be added to the target body, not written to the console.

When a target is defined, it captures the existing global context *at
definition*. Target contexts are therefore detached from the global context.
This means that a target can read a global variable (if it has been set prior
to the build-target definition), however, a target can not update a global variable
and have the new value be visible outside or it's own context.

A target is defined using the format:

    output_file1 {output_file2 ... } : {input_file1 input_file2 ...}
        script body snippet
        <% cgpipe-expression %>
        script body snippet
        script body snippet
        <% 
            cgpipe-expression
            cgpipe-expression
        %>
        ...
    # ends with outdent.


Notes: In both global and target contexts, for-loops will dynamically add and
remove the iterating variable from the context.


## Data types

There are 6 primary data types in CGPipe: boolean, float, integer, list, range
and string. Booleans are either `true` or `false` (case-sensitive). Strings must be enclosed in
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

`foo = "val"` Set a variable

`foo ?= "val"` Set a variable if it hasn't already been set

`foo += "val"` Append a value to a list (if the variable has already been set,
then this will convert that variable to a list)

`unset foo` Unsets a variable. Note: if the variable was used by a target,
it will still be set within the context of the target.

Variables may also be set at the command-line like this: `cgpipe -foo bar -baz 1 -baz 2`.
This is the same as saying:

    foo = "bar"
    bar = 2.59

## Lists

You can also create and access elements in a list using the [] splice operator. List items don't have to be of the same data type, but 
it is recommended that they are. List indexing starts at zero. Negative indexes are treated as relative to the end of the list.

    foo = []
    foo = [1, 2, "three"]

    print foo[2]
    >>> "three"
    print foo[-1]
    >>> "three"

You can also append to lists:

    foo = ["foo"]
    foo += "bar"
    foo += "baz"

    print foo
    >>> "foo bar baz"

List elements can be sliced using the same [start:end] syntax as in Python. If start or end is omitted, it is assumed to be the 0 or len(list), respectively.

    foo = ["one", "two", "three"]
    print foo[1:]
    >>> two three
    print foo[:2]
    >>> one two
    print foo[:-1]
    >>> one two


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

You can perform basic logic operations as well. This will most commonly be used in the context of an if-else condition.

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

## Shell escaping
You may also include the results from shell commands as well using the syntax
`$(command)`. Anything surrounded by `$()` will be executed in the current shell.
Anything written to stdout can be captured as a variable. The shell command will
be evaluated as a CGPipe string and any variables substituted.

Example:

    submit_host = $(hostname)
    submit_date = $(date)

Shell escaping can also be used within strings, such as:

	print "The current time is: $(date)"

## Printing

You can output arbitrary messages using the "print" statement. The default output is stdout, but this can be silenced using the `-s` command-line argument.

Example:

    print "Hello world"

    foo = "bar"
    print "foo${bar}"



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
Other Pipeline files can be imported into the currently running Pipeline by
using the `include filename` statement. In this case, the directory of the
current Pipeline file will be searched for 'filename'. If it isn't found, 
then the current working directory will be searched. If it still isn't found,
then an ParseError will be thrown.

## Logging
You can define a log file to use within the Pipeline file. You can do this
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
them, such as `\$`. If they will be evaluated twice, you will need to escape them twice
(as is the case with shell evaluated strings).

## Help text
The user can request to disply help/usage text for any given pipeline. Any comment
lines at the start of the file will be used as the help/usage text. The first non-comment
line (including blank lines) will terminate the help text. If the script starts with a
shebang (#!), then that line will not be included in the help text.

## Job execution options

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
        <% job.shexec = true %>
        mkdir -p output


Another common use-case for this is having a `clean` target to remove all
output files to perform a fresh set of calculations. For example:

    clean:
        <% job.shexec = true %>
        rm *.bam

# Experimental cgpipe features

The following features are experimental. Syntax for the below may change in 
future versions of cgpipe.

## Target snippets imports
Sometimes you might have more than one target definition that has the same (or
similar) recipe body. In this case, you might want to have only one copy of
the recipe, and import that copy into each separate build-target script.

You can do this with an "importable" target definition. This is simply a way
to include a common snippet into a target script that isn't `__pre__` or
`__post__`. Importable target definitions are targets that have only one
output (the name), followed by two colons. That snippet can then be imported
into the body of a target definition using the `import` statement. 

(Note: the `import` statement only works within the context of a build-target.
If you need something like import in a Pipeline, try the `include` statement.)

Here's an example:

    common::
        echo "this is the common snippet"
        
    out.txt: input.txt
        <% import common %>

## Multi-line strings
You can have strings that span more than one line if you quote them with three
double quotes. Here is an example:

    str = """this
    is a very
    long
    string""" 
