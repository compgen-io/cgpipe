#!/bin/bash
# Build the PDF and single-page HTML user guide from docs/*.md.
# docs/README.md is the GitHub navigation page and is excluded from the bundle.
#
# Requires: pandoc. PDF build also requires a LaTeX engine (pdflatex).
set -eu

mkdir -p dist

if command -v pdflatex >/dev/null 2>&1; then
    echo "Building PDF"
    pandoc -V geometry:margin=1.5in -N --toc -V documentclass=report \
        -o dist/CGPipe-User-Guide.pdf docs/[0-9]*.md
else
    echo "Skipping PDF (pdflatex not found)"
fi

echo "Building single-page HTML"
pandoc -N --toc -o dist/CGPipe-User-Guide.html docs/[0-9]*.md
