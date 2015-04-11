#!/bin/bash
echo "Building PDF"
pandoc -N --chapters -o dist/CGPipe-User-Guide.pdf <(cat docs/*md)
echo "Building single-page HTML"
pandoc -N --chapters -o dist/CGPipe-User-Guide.html <(cat docs/*md)
