#!/usr/bin/env bash

function uuid() {
    v=$(uuidgen 2&> /dev/null || echo "")
    if [ -z $v ]; then
        echo "$(date +%s)-$RANDOM"
    else
        echo $v
    fi
}