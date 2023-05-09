#!/usr/bin/env sh

# use internal jre
BASE_NAME=$(cd "$(dirname $0)" && pwd -P)
JAVACMD="$BASE_NAME/jre/bin/java"
if [ ! -f $JAVACMD ]; then
    JAVACMD=$(whereis java)
    if [ ! -f $JAVACMD ]; then
        JAVACMD=$JAVA_HOME/bin/java
        if [ ! -f $JAVACMD ]; then
            echo "java is not found in $BASE_NAME, \$PATH or \$JAVA_HOME"
            return 1
        fi
    fi
fi

# detect app
JAR_NAME=$(ls $BASE_NAME | grep *.jar | head -1)
if [ ! -f $JAR_NAME ]; then
    echo "the jar archive is not found, please put it in $BASE_NAME"
    return 1
fi

echo "exec $JAVACMD -jar $BASE_NAME/$JAR_NAME $@"

$JAVACMD -jar $BASE_NAME/$JAR_NAME $@
