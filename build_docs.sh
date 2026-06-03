#!/bin/bash
# Build the PDF and single-page HTML user guide from docs/*.md.
# docs/README.md is the GitHub navigation page and is excluded from the bundle.
set -eu

CHAPTERS=$(ls docs/[0-9]*.md)

echo "Building PDF"
pandoc -V geometry:margin=1.5in -N --toc -V documentclass=report -o dist/CGPipe-User-Guide.pdf $CHAPTERS

echo "Building single-page HTML"
pandoc -N --toc -o dist/CGPipe-User-Guide.html $CHAPTERS
