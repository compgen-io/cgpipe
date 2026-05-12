# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CGPipe is a make-like pipeline orchestrator for HPC clusters. It uses a custom DSL (`.mvp`/`.mvpt` files) to define pipeline targets and dependencies, then submits jobs to batch schedulers (SGE, SLURM, PBS, SBS, BatchQ). Written in Java 11, built with Apache Ant.

## Build & Test Commands

- `ant jar` — build CLI artifacts (`dist/cgpipe`, `dist/cgsub`, JARs)
- `ant clean` — remove `build/` and `dist/`
- `./test.sh` — run all language tests (compares output against `.good` files)
- `./test.sh -v` — verbose with diffs (`-vv`, `-vvv` for more)
- `./test.sh <test-file.mvp>` — run a single test
- `./build_docs.sh` — regenerate documentation (requires pandoc)

## Architecture

**Entry points:** `CGPipe.java` (main pipeline tool), `CGSub.java` (dynamic job submission)

**Parser pipeline:** `Tokenizer` → `Parser`/`TemplateParser` → AST nodes → `Eval` (expression evaluation) → `ExecContext`/`RootContext` (variable state)

**Key packages** (under `io.compgen.cgpipe`):
- `parser/` — tokenizer, parser, evaluator, AST nodes, statements, operators
- `parser/context/` — execution contexts and variable scope (`RootContext` is the global state)
- `parser/target/` — `BuildTarget`, `BuildTargetTemplate` (pipeline targets with dependencies)
- `parser/variable/` — typed values: `VarString`, `VarInt`, `VarFloat`, `VarBool`, `VarList`
- `runner/` — `JobRunner` orchestrates job submission; `*TemplateRunner` classes adapt to specific schedulers
- `cmd/` — CLI subcommands (job status, cancel, vacuum)
- `loader/` — source file loading
- `exceptions/` — `ASTParseException`, `ASTExecException`, `RunnerException`, `VarTypeException`

**Runtime config:** `.cgpiperc` files, `CGPIPE_HOME` and `CGPIPE_ENV` environment variables.

## Code Style

- Java tabs for indentation; match surrounding code style
- `PascalCase` classes, `camelCase` methods/variables
- Package namespace: `io.compgen.cgpipe.*`
- Target Java 11 compatibility
- No automated formatter — avoid unrelated reformatting

## Testing

Tests are script-based, not JUnit. Each `.mvp`/`.mvpt` in `src/test-scripts/` has a matching `.good` file with expected output. The test harness uses MD5 to compare actual vs expected output. Always run `./test.sh` after language/runtime changes.

## CI

GitHub Actions (`.github/workflows/ant.yml`): builds on Ubuntu with JDK 11 (Temurin), runs on push to main, tags, and PRs. Tagged commits auto-create GitHub releases with built artifacts.
