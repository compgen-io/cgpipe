# Tutorial 7: importable snippets for shared body fragments

When two targets share a long chunk of body, factor it out as an importable snippet.

    #!/usr/bin/env cgpipe
    #
    # Tutorial 7: importable snippets.

    if !input
        print "ERROR: --input is required"
        exit 1
    endif

    safe::
        set -euo pipefail
        umask 077
        echo "Running as $(whoami) on $(hostname)"

    out1.txt: ${input}
        <% import safe %>
        wc -l ${input} > $>

    out2.txt: ${input}
        <% import safe %>
        wc -c ${input} > $>

`safe::` (note the double colon) defines an importable snippet. `<% import safe %>` inlines its body into the importing target's body at that point. This is how `__pre__` and `__post__` work internally; you can use the same mechanism for your own shared fragments.

The difference from `include` (which is a file-level statement) is scope: `import` only works inside a target body, and it inlines a snippet rather than a whole file.

---

[← Tutorial 6](06-pre-post.md) · [Tutorials index](../06-Pipeline_Tutorials.md) · [Next: Tutorial 8 — composing pipelines via include →](08-include.md)
