# Pipeline tutorials

Nine worked examples that build on each other, each in its own page. They're synthetic — none of these is a "real" pipeline you should run as-is — but the shapes are taken straight from production usage. The tools referenced (`bwa`, `samtools`, `bcftools`, `gzip`) are stand-ins; the same patterns work for whatever your real workflow uses.

| # | Tutorial | Covers |
|---|----------|--------|
| 1 | [Hello, target](tutorials/01-hello.md) | Help text, CLI-argument guard (`if !var`), single-target pipeline |
| 2 | [gzip with a wildcard](tutorials/02-gzip-wildcard.md) | `%` wildcards, the `all:` convention, `$<` and `$>` |
| 3 | [Per-target resources and conditional flags](tutorials/03-resources-and-flags.md) | `<% %>` blocks, `?=` defaults, `<% if rg %>` fragments, `$>.tmp && mv` atomicity |
| 4 | [Map-reduce across chromosomes](tutorials/04-map-reduce.md) | Dynamic target generation: `for c in chroms`, `+=` accumulator, `@{}` expansion, temp `^` outputs, a single-flag toggle between chunked and single-shot |
| 5 | [Opportunistic cleanup for storage efficiency](tutorials/05-opportunistic-cleanup.md) | Opportunistic jobs (`: input1 input2 ...`) that delete intermediates only after a merge succeeds; basic and defensive variants |
| 6 | [Shared `__pre__` and `__post__`](tutorials/06-pre-post.md) | Per-job timing and log preamble, `__setup__` with `job.shexec`, escaping `\$` to defer evaluation |
| 7 | [Importable snippets](tutorials/07-importable-snippets.md) | `safe::` snippet with `<% import safe %>`; difference vs. `include` |
| 8 | [Composing pipelines via include](tutorials/08-include.md) | Shared `defaults.cgp` for cluster/project defaults; pipeline-level `include` |
| 9 | [Containerized jobs (Docker and Singularity)](tutorials/09-containers.md) | The HEREDOC trick in `__pre__`/`__post__`, volume mounts, the Singularity equivalent |

## Where next

- [Build Targets](05-Build_Targets.md) — full coverage of target syntax: wildcards, temp outputs, opportunistic jobs, special targets.
- [Methods Reference](04-Methods_Reference.md) — every method on every value type.
- [Running Jobs](07-Running_Jobs.md) — every `job.*` setting and runner-specific quirks.
- [Troubleshooting](11-Troubleshooting.md) — dry runs, joblog inspection, common pitfalls.
