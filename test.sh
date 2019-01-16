#!/bin/bash
MD=$(which md5sum)
if [ "$MD" == "" ]; then
    MD=$(which md5)
fi

if [ "$MD" == "" ]; then
    echo "Missing MD5!"
    exit 1
fi
VERBOSE=""
if [[ "$1" == "-v" || "$1" == "-vv" || "$1" == "-vvv" ]]; then
    VERBOSE="$1"
    shift
fi

if [ "$1" == "" ]; then
    find src/test-scripts -name '*.mvp' -exec $0 $VERBOSE \{\} \;
    find src/test-scripts -name '*.mvpt' -exec $0 $VERBOSE \{\} \;
else
    mkdir -p test/run
    echo "global_foo = \"bar\"" > test/run/.cgpiperc
    echo "bar = \"baz\"" > test/run/global.incl

    if [ "$(echo $1 | grep ".mvpt$")" != "" ]; then
        CGPIPE_HOME=test/run dist/cgpipe $VERBOSE -nolog -f $1 > .testout 2> .testerr
    else
        CGPIPE_HOME=test/run $1 > .testout 2> .testerr
    fi

    TEST=$(cat .testout | grep -v '^#' | grep -v '^$' | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' | $MD)
    GOOD=$(cat $1.good | grep -v '^#' | grep -v '^$' |sed -e 's/^[[:blank:]]*//g' | $MD)
    if [ "$TEST" != "$GOOD" ]; then
        echo "$1 ERROR"
    else
        echo "$1 OK"
    fi
    if [ "$VERBOSE" != "" ]; then
        cat .testout | grep -v '^#' | grep -v '^$'  | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' > .testout1
        cat $1.good | grep -v '^#' | grep -v '^$'  | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' > .testout2
        echo "[EXPECTED]"
        cat .testout2
        echo ""
        echo "[GOT]"
        cat .testout1
        if [ "$TEST" != "$GOOD" ]; then
            echo ""
            echo "[DIFF]"
            diff -y .testout1 .testout2
        fi
        rm .testout1 .testout2
    fi
    rm .testout
    rm .testerr
    rm test/run/*
fi
