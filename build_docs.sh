#!/bin/bash
echo "Building PDF"
pandoc -V geometry:margin=1.5in -N --toc -V documentclass=report -o dist/CGPipe-User-Guide.pdf <(cat docs/*md)
echo "Building single-page HTML"
pandoc -N --chapters -o dist/CGPipe-User-Guide.html <(cat docs/*md)
