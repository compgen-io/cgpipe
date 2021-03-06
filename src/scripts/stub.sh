#!/bin/sh
MYSELF=$(which "$0" 2>/dev/null)
if [ "$?" -gt 0 ]; then
	MYSELF="./$0"
fi

JAVA_OPTS="${JAVA_OPTS} -Dio.compgen.cgpipe.scriptname=\"$MYSELF\""

# limit MALLOC_ARENA to keep vmem under control on bigmem systems
if [ "$MALLOC_ARENA_MAX" = "" ]; then
    export MALLOC_ARENA_MAX=4
fi

# limit parallel gc threads (systems with lots of processors)
FOUNDGC=0
case "${JAVA_OPTS}" in
  *"XX:ParallelGCThreads"*)
  FOUNDGC=1
  ;;
esac

if [ $FOUNDGC -eq 0 ]; then
    JAVA_OPTS="${JAVA_OPTS} -XX:ParallelGCThreads=2"
fi

JAVABIN=$(which java)
if [ "${JAVA_HOME}" != "" ]; then
    JAVABIN="$JAVA_HOME/bin/java"
fi
exec "${JAVABIN}" ${JAVA_OPTS} -jar $0 "$@"
exit 1