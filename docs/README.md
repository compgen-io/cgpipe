# CGPipe documentation

CGPipe is a Make-like pipeline language that compiles target definitions into job-scheduler submissions (SLURM, SGE, PBS, BatchQ, SBS, or plain bash).

| | Chapter | What's in it |
|--|---|---|
| 1 | [Introduction](01-Introduction.md) | What CGPipe is, where it fits, how it differs from `make` and `qmake` |
| 2 | [Getting Started](02-Getting_Started.md) | Install, first script, configuring CGPipe |
| 3 | [Language Syntax](03-Language_Syntax.md) | Types, variables, operators, control flow, statements |
| 4 | [Methods Reference](04-Methods_Reference.md) | Per-type methods (string, list, range, etc.) |
| 5 | [Build Targets](05-Build_Targets.md) | Target syntax, wildcards, temp outputs, opportunistic jobs, special targets, dynamic generation |
| 6 | [Pipeline Tutorials](06-Pipeline_Tutorials.md) | Nine worked examples: hello-world, map-reduce, storage-efficient cleanup, containerized jobs, more |
| 7 | [Running Jobs](07-Running_Jobs.md) | Runners, `job.*` settings, joblogs, dry runs |
| 8 | [Configuration Reference](08-Configuration_Reference.md) | Every `cgpipe.*` and `job.*` variable; env vars; precedence |
| 9 | [Remote Pipelines](09-Remote_Pipelines.md) | Loading scripts from URLs, named remotes, hash pinning |
| 10 | [Glossary](10-Glossary.md) | Terminology used throughout |
| 11 | [Troubleshooting](11-Troubleshooting.md) | Debugging tools, common errors, recovery |

## I want to…

- **…write my first pipeline.** [Getting Started](02-Getting_Started.md) → [Tutorial 1](06-Pipeline_Tutorials.md#tutorial-1-hello-target).
- **…look up a syntax detail.** [Language Syntax](03-Language_Syntax.md), [Build Targets](05-Build_Targets.md), [Methods Reference](04-Methods_Reference.md).
- **…fan out work over chromosomes / samples / lanes.** [Tutorial 4](06-Pipeline_Tutorials.md#tutorial-4-map-reduce-across-chromosomes) and [Dynamic target generation](05-Build_Targets.md#dynamic-target-generation).
- **…clean up intermediates without breaking restarts.** [Tutorial 5](06-Pipeline_Tutorials.md#tutorial-5-opportunistic-cleanup-for-storage-efficiency).
- **…run jobs inside Docker or Singularity containers.** [Tutorial 9](06-Pipeline_Tutorials.md#tutorial-9-containerized-jobs-docker-and-singularity).
- **…set up a cluster.** [Running Jobs](07-Running_Jobs.md), [Configuration Reference](08-Configuration_Reference.md).
- **…coordinate multiple pipelines.** [Joblogs](07-Running_Jobs.md#joblogs).
- **…debug a misbehaving pipeline.** [Troubleshooting](11-Troubleshooting.md).

## Building the PDF / single-page HTML

The full guide can be built into a PDF or single-page HTML with `./build_docs.sh` from the project root (requires `pandoc` and a LaTeX toolchain for PDF output). The chapters concatenate in numerical order.

## Test scripts are the source of truth

When a doc page conflicts with the behavior of an actual test under `src/test-scripts/`, the test is correct. Each language feature has at least one dedicated test there.
