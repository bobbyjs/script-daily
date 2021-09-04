#!/usr/bin/env bash

function wc_code_line() {
    dir_path=$1
    file_path=$2

    if [ -z $file_path ]; then
        lines=$(find $dir_path -type f)
    else
        lines=$(find $dir_path -type f -name $file_path)
    fi

    count=0
    for line in $lines; do
        line_count=$(cat $line | wc -l)

        count=$(expr $count + $line_count)
    done
    echo $count
}


if [ $# != 0 ]; then
    wc_code_line $@
fi
