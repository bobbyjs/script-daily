#!/usr/bin/env bash

source $SCRIPT_SHELL_HOME/common/time.sh

function log_info() {
    echo -e "$(date +"%F %T"):$(get_millisecond)  \033[32m INFO\033[0m:\t$@"
}

function log_warn() {
    echo -e "$(date +"%F %T"):$(get_millisecond)  \033[33m WARN\033[0m:\t$@"
}

function log_error() {
    echo -e "$(date +"%F %T"):$(get_millisecond)  \033[31mERROR\033[0m:\t$@"
}
