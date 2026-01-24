# Repository Guidelines

## Project Structure & Module Organization
- `src/java` holds the Java implementation (packages under `io.compgen.cgpipe`).
- `src/conf` and `src/scripts` contain runtime configuration and launcher assets.
- `src/test-scripts` contains language-level test fixtures (`*.mvp`, `*.mvpt`).
- `docs` includes user and language reference documentation.
- `lib` and `blib` hold third-party jars; `build`, `dist` are build outputs.

## Build, Test, and Development Commands
- `ant jar` builds the CLI artifacts into `dist/` (includes `cgpipe` and `cgsub`).
- `ant clean` removes `build/` and `dist/` output.
- `./test.sh` runs the language tests in `src/test-scripts` and compares against `.good` files.
- `./build_docs.sh` regenerates documentation (when needed).

## Coding Style & Naming Conventions
- Java code uses tabs for indentation; match the existing style in nearby files.
- Package naming follows `io.compgen.cgpipe.*`; classes use `PascalCase`, methods/vars use `camelCase`.
- Keep changes compatible with Java 11 (see `build.xml` compiler settings).
- No automated formatter is configured; avoid large, unrelated reformatting.

## Testing Guidelines
- Tests are script-based; each `*.mvp`/`*.mvpt` should have a matching `.good` output file.
- Add or update tests for language/runtime changes and verify with `./test.sh`.
- Use `./test.sh -v` (or `-vv/-vvv`) to see expected vs. actual output diffs.

## Commit & Pull Request Guidelines
- Git history shows short, direct subjects (e.g., “debug”, “missing comment”); follow this concise style.
- Use imperative, present-tense summaries and include context in the PR description.
- Include test results (`./test.sh`) and note any config or scheduler assumptions.

## Configuration & Runtime Notes
- CGPipe reads configuration from `.cgpiperc` files and environment (`CGPIPE_HOME`, `CGPIPE_ENV`).
- Scheduler templates live under `src/java/io/compgen/cgpipe/runner`; update templates with care.
