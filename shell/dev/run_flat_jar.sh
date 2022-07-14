#!/usr/bin/env sh

# use internal jre
BASE_NAME=$(cd "$(dirname $0)" && pwd -P)
JAVA_HOME=$BASE_NAME/jre

JAVACMD="$JAVA_HOME/bin/java"

# detect app
JAR_NAME=`ls $BASE_NAME | grep *.jar | head -1`
if [ ! -f $JAR_NAME ]; then
    echo "the jar archive is not found, please specify it!"
    exit 1
fi

echo "exec $JAVACMD -jar $BASE_NAME/$JAR_NAME $@"

$JAVACMD -jar $BASE_NAME/$JAR_NAME \
  $@
