#!/usr/bin/env bash

function add_gradle_submodule() {
    if [ -z $1 ]; then
        echo "please specified a sub module name!"
        return 1
    fi

    # replace : with /
    name=$(echo $1 | sed 's/:/\//g')
    if [ -n $2 ]; then
        package=$(echo $2 | sed 's/./\//g')
    fi

    mkdir -p $name/src/main/java/$package
    touch $name/build.gradle
    echo "done."
}

# don't call it via source the script
if [ $# != 0 ]; then
    add_gradle_submodule $@
fi
