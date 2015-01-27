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
        #$ mvpipe-expression
        ...
        continues
    # ends with outdent.

Any text that is indented in the target is assumed to be part of the script
that will be used to build the target file(s). When the indent is lost, then
the target context is closed.

Notes: In both global and target contexts, a for-loop (iteration) will create
a new child-context. If/then sections do *not* create a separate context.

## Data types

There are 6 primary data types in mvpipe: boolean, float, integer, list, range
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
`foo = "bar"` Set a variable.

`foo ?= "bar"` Set a variable if it hasn't already been set.

`foo += "bar"` Adds "bar" to the value of "foo". For a numerical type, this 
will add the numbers together. For a string type, this will concatenate the
strings together. If the first argument (foo) is a list, then this will append
"bar" to the list.

`unset foo` Unsets a variable. Note: if the variable was used by a target,
it will still be set within the context of the target.

Variables may also be set at the command-line like this: 

    mvpipe --foo bar --baz 1 --baz 2

This is the same as saying:

    foo = "bar"
    baz = []
    baz += 1
    baz += 2

## Math

You can perform basic math on integer and float variables. Available
operations are:
* + add
* - subtract
* * multiplication
* / divide (integer division if on an integer)
* % remainder
* ** power (2**3 = 8)

Operations are in standard order; however, you can also add also parentheses
around clauses to process things in a different order. For example:

    8 + 2 * 10 = 28
    (8 + 2) * 10 = 100
    8 + (2 * 10) = 28


## Variable substitution
Anywhere there is a processed string, you can embed variables that will be 

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

    user = $(echo $USER)

                  
## If/Else/Endif
Basic syntax:

    if [condition]
       do something...
    else
       do something else...
    endif

If clauses can be nested, and you can combine conditions using 

### Conditions
`if foo`          - if the variable `foo` is true (all non-explicitly false values are true)
`if !foo`         - if the variable `foo` false or missing

`if foo == "bar"` - if the variable `foo` equals the string "bar"
`if foo != "bar"` - if the variable `foo` doesn't equal the string "bar"

`if foo < 1`    
`if foo <= 1`    
`if foo > 1`    
`if foo >= 1`    
          
## For loops
Basic syntax:

    for i in start..end
       echo i
       do something...
    done

    for i in 1..10
        do something...
    done

    for i in list
       do something...
    done

    