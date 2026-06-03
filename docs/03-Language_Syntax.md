# Language syntax

CGPipe is a small interpreted language built around the goal of generating job scripts. Files have a `.cgp`, `.cgpipe`, or `.mvp` extension. A pipeline file is read top-to-bottom in *global* context: every uncommented line is CGPipe code. Target definitions inside that file open a separate *target* context for the body — see [Build Targets](#build-targets).

The test suite under `src/test-scripts/` is the definitive reference. When this document conflicts with a test, the test is correct.

## Comments and help text

`#` starts a comment that runs to the end of the line.

The leading run of comment lines at the top of a script (excluding the shebang) is treated as *help text* and displayed when the user passes `-h`:

    #!/usr/bin/env cgpipe
    #
    # Align reads to a reference and call variants.
    #
    # Options:
    #     --reads FILE      input FASTQ
    #     --ref FILE        reference FASTA
    #     --out FILE        output VCF
    #

    # rest of the pipeline

The first blank or non-comment line ends the help block.

## Data types

There are six data types: `bool`, `int`, `float`, `string`, `list`, `range`.

    flag = true            # bool — true or false (case-sensitive)
    count = 10             # int
    rate = 0.5             # float
    name = "sample-1"      # string — always double-quoted
    samples = []           # list
    samples = [1, 2, "x"]  # lists can mix types but usually shouldn't
    chunks = 1..100        # range — produces 1, 2, ..., 100 when iterated

The type a value carries is mostly invisible — arithmetic, comparisons, and string substitution work the way you'd expect. Use `.type()` to ask explicitly (see [Methods Reference](04-Methods_Reference.md)).

## Variables

A variable is set with `=`:

    sample = "patient_42"
    threads = 8

There are no declarations and no scopes other than the global/target split. A variable exists from the line that first sets it.

| Form | Meaning |
|------|---------|
| `foo = expr` | Set `foo` to `expr` |
| `foo ?= expr` | Set `foo` only if it isn't already set (defaults) |
| `foo += expr` | Append to `foo` (converts to a list if `foo` was scalar) |
| `unset foo` | Remove the variable from scope |

`?=` is the workhorse for defaults — let the user override on the command line, otherwise fall back:

    threads ?= 4
    method ?= "fast"

### Command-line variables

Any `-name value` pair on the command line is the same as `name = "value"` at the top of the script:

    $ cgpipe pipeline.cgp -sample patient_42 -threads 16

is equivalent to:

    sample = "patient_42"
    threads = 16

CGPipe parses numbers and booleans from the strings when possible. CLI values arrive *before* the script runs, so `?=` defaults set in the script don't override them.

## Operators

### Arithmetic

`+`, `-`, `*`, `/`, `%`, `**` (power). Standard precedence; parenthesize for clarity.

    print 8 + 2 * 10       # 28
    print (8 + 2) * 10     # 100
    print 2 ** 10          # 1024

`+` also concatenates strings, and `*` repeats strings and lists:

    print "ab" + "cd"      # abcd
    print "x" * 3          # xxx
    print [0] * 5          # 0 0 0 0 0

### Comparison and logic

`==`, `!=`, `<`, `<=`, `>`, `>=`, `&&` (and), `||` (or), `!` (not).

    if count > 0 && method == "fast"
        ...
    endif

`!foo` is also used as "is the variable unset or false," which is the idiom for argument-validation guards:

    if !sample
        print "ERROR: --sample is required"
        exit 1
    endif

### Conditional assignment chain

    threads ?= 4

Equivalent to `if !threads; threads = 4; endif`, but written inline.

## Strings and substitution

String literals use double quotes. Inside a string, two forms substitute CGPipe values:

| Form | Behavior |
|------|----------|
| `${var}` | Substitute `var`. Throws an error if `var` is unset. If `var` is a list, joins with spaces. |
| `${var?}` | Like `${var}` but yields `""` when `var` is unset. |
| `@{list}` | List expansion — produces one copy per element (see below). |
| `@{N..M}` | Range expansion — produces one copy per integer in the range. |
| `${{var}}` | *Double evaluation* — substitute `var`, then evaluate the result again. |
| `$(cmd)` | Run `cmd` in the shell at parse time; substitute its stdout. |

Examples:

    sample = "patient_42"
    out = "results/${sample}/variants.vcf"           # "results/patient_42/variants.vcf"
    out_opt = "results/${dir?}/variants.vcf"         # "results//variants.vcf" if dir unset
    host = $(hostname)                                # shell-captured

    # list expansion in a string
    samples = ["a","b","c"]
    print "out_@{samples}.txt"                        # "out_a.txt out_b.txt out_c.txt"

### Double-evaluation

`${{var}}` reads `var`, then evaluates the result as if it were source. Use it when the *content* of a variable is itself a template:

    cmd_template = "echo ${greeting}"
    greeting = "hello"

    target_a:
        ${{cmd_template}}    # body becomes:  echo hello

Pure `${cmd_template}` would substitute the literal string `"echo ${greeting}"`. The extra evaluation re-runs CGPipe over it, so the inner `${greeting}` gets resolved too.

### Shell command substitution

`$(cmd)` runs in the current shell at parse time and captures stdout:

    submit_time = $(date)
    revision = $(git rev-parse HEAD)

The `cmd` itself is a CGPipe string and is variable-substituted first.

### Escaping

To get a literal `$` or `@` into the output, prefix it with `\`. If the same string will be evaluated again (template body, shell command), you'll need to escape twice: `\\$`.

## Lists

Lists are zero-indexed; negative indices count from the end. Slicing uses Python-style `[start:end]`:

    foo = ["one", "two", "three"]
    print foo[0]      # one
    print foo[-1]     # three
    print foo[1:]     # two three
    print foo[:2]     # one two

Append with `+=`:

    samples = []
    samples += "A"
    samples += "B"
    samples += "C"

Common list operations are methods (`length`, `contains`, `join`) — see [Methods Reference](04-Methods_Reference.md).

Lists print with elements space-separated.

## Ranges

`from..to` defines an inclusive range:

    for i in 1..10
        print i
    done

Endpoints can be variables. Ranges are iterable and have a `.length()` method.

## Control flow

### if / elif / else / endif

    if count > 100
        print "many"
    elif count > 0
        print "some"
    else
        print "none"
    endif

`!` works as expected for negation, including `if !foo` for "unset or false".

### for / done

Three forms:

    for i in 1..10           # range
        print i
    done

    for sample in samples    # list
        print sample
    done

    for cond                 # while-style (runs while `cond` is true)
        ...
    done

Loop variables are scoped like any other — they remain set after the loop.

### exit

Stop the pipeline with an optional status code:

    if !ref
        print "ERROR: --ref is required"
        exit 1
    endif

`exit` (no argument) is `exit 0`.

## Statements

Beyond control flow, the language has a small set of statement keywords.

| Statement | Purpose |
|-----------|---------|
| `print expr [, expr ...]` | Write to stdout. Inside a target body, appends to the script instead. |
| `log filename` | Open a log file. Subsequent CGPipe output is mirrored there. |
| `include "path"` | Inline another `.cgp` file at this point. Searched relative to the current file, then the working directory. |
| `import name` | (Inside a target body only) inline an importable target snippet. |
| `eval expr` | Evaluate the string-valued `expr` as CGPipe source at run time. |
| `unset name` | Remove a variable from scope. |
| `dumpvars` | Print every variable currently in scope. Debugging aid. |
| `showhelp` | Print the script's help-text block. Same as the `-h` flag. |
| `sleep seconds` | Pause for the given number of seconds. Rarely needed. |

### include vs. import

- **`include`** runs in the global context. The included file's top-level statements and target definitions become part of the current pipeline. Use it for shared configuration and shared target libraries.
- **`import`** runs only inside a target body. The named importable snippet (a target written with `name::`) is inlined into the current body. Use it for shared script fragments — see [Build Targets](05-Build_Targets.md#importable-target-snippets).

### eval

`eval s` parses and runs the string `s` as if it were a line of source:

    i = 1
    code = "i = i + 1"
    eval code
    print i      # 2

Most pipelines don't need `eval`; it's useful for building per-environment variable assignments from a configuration string, or for tests of the language itself.

## Logging

Inside a script, `log filename` opens a new log file. If a log was already active it's closed first. From the command line, `-l filename` does the same thing.

Independently of `log`, the joblog tracks *submitted jobs* (their ids, outputs, and runner state). Set it with `cgpipe.joblog = "path/to/joblog.txt"`. With a joblog in place, CGPipe consults it before deciding to build a target — it won't resubmit a job whose output is already pending in the scheduler, even across separate `cgpipe` invocations. See [Running Jobs](07-Running_Jobs.md) for joblog mechanics.

## Including other files

    include "shared/defaults.cgp"
    include "shared/targets.cgp"

The path is resolved relative to the file that contains the `include` line, then relative to the working directory. A failing lookup is a parse error.

`include` is commonly used to share a set of default `job.*` settings across many pipelines:

    # shared/defaults.cgp
    job.stdout = "logs/"
    job.stderr = "logs/"
    job.env = true

    # pipeline.cgp
    include "shared/defaults.cgp"
    ...

`include` is the way you compose pipelines — the included file runs as if its contents were pasted in.

## Build targets

Targets are the heart of a pipeline and have their own chapter — see [Build Targets](05-Build_Targets.md).

## Where to look next

- [Methods Reference](04-Methods_Reference.md) — the methods available on each type.
- [Build Targets](05-Build_Targets.md) — defining what gets built and how.
- [Pipeline Tutorials](06-Pipeline_Tutorials.md) — worked examples.
- [Configuration Reference](08-Configuration_Reference.md) — every `cgpipe.*` and `job.*` setting.
