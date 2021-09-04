#!/usr/bin/env bash

# sh some.sh: fork a new shell proc
# exec: will discard rest of current scriptt
# source: inline mode

function init_gradle_java_project() {
    project_name=$1
    package_name=$2

    if [ -z $1 ]; then
        echo "please specified a project name!"
        return 1
    fi
    if [ -z $2 ]; then package_name=$project_name; else package_name=$2; fi

    gradle init --type java-library \
    --dsl groovy --test-framework junit \
    --project-name ${project_name} --package ${package_name}
    echo "done."
    # return what gradle return, 0-255 number
    return $?
}

# don't call it via source the script
if [ $# != 0 ]; then
    init_gradle_java_project $@
fi
