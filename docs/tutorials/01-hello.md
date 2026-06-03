# Tutorial 1: Hello, target

The smallest interesting pipeline.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 1: copy one file to another, slowly.
    #
    # Options:
    #     --input FILE
    #     --output FILE

    if !input
        print "ERROR: --input is required"
        exit 1
    endif
    if !output
        print "ERROR: --output is required"
        exit 1
    endif

    ${output}: ${input}
        cp ${input} ${output}

Run it:

    $ ./tutorial1.cgp -input data.txt -output backup.txt

What's happening:

- The first comment block is the help text. `cgpipe tutorial1.cgp -h` prints it.
- `!input` and `!output` are the standard CLI-argument guard. Without them you'd see a confusing error from the shell when `${input}` substitutes to the empty string.
- The single target says: to produce `${output}`, you need `${input}`, and the body is `cp ${input} ${output}`.

CGPipe's default runner (`shell`) prints the rendered script to stdout. To actually execute it, either pipe to `bash` or set `cgpipe.runner.shell.autoexec = true`.

---

[← Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 2 — gzip with a wildcard →](02-gzip-wildcard.md)
