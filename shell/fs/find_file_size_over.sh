#!/usr/bin/env bash


function find_file_size_over() {
    file=$1
    min_size=$2

    # default 1M
    if [ -z "$min_size" ]; then min_size$((1024*1024)); fi

    size=`ls -l $file | awk '{print $5}'`
    if [[ $size -gt $min_size ]]; then
        printf "%4d Bytes    %s\n" $size $file
    fi
}

if [ $# != 0 ]; then find_file_size_over $@; fi
