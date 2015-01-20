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
if [ "$1" == "-v" ]; then
    VERBOSE="-v"
    shift
fi

if [ "$1" == "" ]; then
    find src/test-scripts -name '*.mvp' -exec $0 $VERBOSE \{\} \;
else
    mkdir -p test
    echo "global_foo = \"bar\"" > test/.mvpiperc
    echo "bar = \"baz\"" > test/global.incl
    MVPIPE_HOME=test dist/mvpipe $VERBOSE -f $1 &> .testout
    TEST=$(cat .testout | grep -v '^#' | grep -v '^$' | sed -e 's/MVPIPE ERROR.*/MVPIPE ERROR/g' | $MD)
    GOOD=$(cat $1.good | grep -v '^#' | grep -v '^$' | $MD)
    if [ "$TEST" != "$GOOD" ]; then
        echo "$1 ERROR"
    else
        echo "$1 OK"
    fi
    if [ "$VERBOSE" != "" ]; then
        cat .testout
    fi
    rm .testout
    rm test/global.incl
    rm test/.mvpiperc
    rmdir test
fi
