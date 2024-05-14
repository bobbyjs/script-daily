#!/usr/bin/env bash

function jc () {
    if [[ $JAVA_HOME == *"1.8"* ]]; then
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    else
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
    fi
    echo "JAVA_HOME=$JAVA_HOME"
    java -version
}
