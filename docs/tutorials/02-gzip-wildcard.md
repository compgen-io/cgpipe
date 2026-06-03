# Tutorial 2: gzip with a wildcard

Same shape as [Tutorial 1](01-hello.md), but using a wildcard to handle any file rather than one named input.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 2: gzip a file using a wildcard rule.
    #
    # Options:
    #     --file FILE     file to compress (produces FILE.gz)

    if !file
        print "ERROR: --file is required"
        exit 1
    endif

    all: ${file}.gz

    %.gz: %
        gzip -c $< > $>

Run it:

    $ ./tutorial2.cgp -file data.txt

The `all` target is a convention — a top-level alias for the things you want CGPipe to build. Its dependency `${file}.gz` triggers the wildcard rule, which matches any `*.gz` target and depends on the same stem without the `.gz`.

`$<` and `$>` are the all-inputs and all-outputs substitutions. Inside this body they're `data.txt` and `data.txt.gz`.

---

[← Tutorial 1](01-hello.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 3 — resources and conditional flags →](03-resources-and-flags.md)
