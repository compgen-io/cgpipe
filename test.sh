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
    rm -f test/err
    find src/test-scripts -name '*.mvp' -exec $0 $VERBOSE \{\} \;
    find src/test-scripts -name '*.mvpt' -exec $0 $VERBOSE \{\} \;

    if [ -e test/err ]; then
        rm test/err
        exit 1
    fi

else
    RET=0
    mkdir -p test/run
    echo "global_foo = \"bar\"" > test/run/.cgpiperc
    echo "bar = \"baz\"" > test/run/global.incl

    touch test/file1 test/file2

    if [ "$(echo $1 | grep ".mvpt$")" != "" ]; then
        CGPIPE_HOME=test/run dist/cgpipe $VERBOSE -nolog -f $1 > test/.testout 2> test/.testerr
    else
        CGPIPE_HOME=test/run $1 > test/.testout 2> test/.testerr
    fi

    if [ "$VERBOSE" = "-vvv" ]; then
        cat test/.testerr
    fi

    TEST=$(cat test/.testout | grep -v '^#' | grep -v '^$' | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' | $MD)
    GOOD=$(cat $1.good | grep -v '^#' | grep -v '^$' |sed -e 's/^[[:blank:]]*//g' | $MD)
    if [ "$TEST" != "$GOOD" ]; then
        echo "$1 ERROR"
        touch test/err
        RET=1
    else
        echo "$1 OK"
    fi
    if [ "$VERBOSE" != "" ]; then
        cat test/.testout | grep -v '^#' | grep -v '^$'  | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' > test/.testout1
        cat $1.good | grep -v '^#' | grep -v '^$'  | sed -e 's/^[[:blank:]]*//g' | sed -e 's/CGPIPE ERROR.*/CGPIPE ERROR/g' > test/.testout2
        echo "[EXPECTED]"
        cat test/.testout2
        echo ""
        echo "[GOT]"
        cat test/.testout1
        if [ "$TEST" != "$GOOD" ]; then
            echo ""
            echo "[DIFF]"
            diff -y test/.testout1 test/.testout2
        fi
        rm test/.testout1 test/.testout2
    fi
    rm test/file1 test/file2
    rm test/.testout
    rm test/.testerr
    rm test/run/*

    exit $RET
fi
