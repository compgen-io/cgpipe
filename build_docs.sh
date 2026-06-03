#!/bin/bash
# Build the PDF and single-page HTML user guide from the docs/ chapters.
# docs/README.md is the GitHub navigation page and is excluded from the bundle.
# Tutorials live one-per-file under docs/tutorials/ and are inserted right
# after chapter 06 (the tutorials index) so the reading order is
#   01..05 chapters, 06 index, tutorials/01..09, 07..11 chapters.
#
# Requires: pandoc. PDF build also requires a LaTeX engine (pdflatex).
set -eu

mkdir -p dist

FILES=(
    docs/01-Introduction.md
    docs/02-Getting_Started.md
    docs/03-Language_Syntax.md
    docs/04-Methods_Reference.md
    docs/05-Build_Targets.md
    docs/06-Pipeline_Tutorials.md
)
FILES+=( docs/tutorials/[0-9]*.md )
FILES+=(
    docs/07-Running_Jobs.md
    docs/08-Configuration_Reference.md
    docs/09-Remote_Pipelines.md
    docs/10-Glossary.md
    docs/11-Troubleshooting.md
    docs/12-Comparisons.md
)

if command -v pdflatex >/dev/null 2>&1; then
    echo "Building PDF"
    pandoc -V geometry:margin=1.5in -N --toc -V documentclass=report \
        -o dist/CGPipe-User-Guide.pdf "${FILES[@]}"
else
    echo "Skipping PDF (pdflatex not found)"
fi

echo "Building single-page HTML"
pandoc -N --toc -o dist/CGPipe-User-Guide.html "${FILES[@]}"
