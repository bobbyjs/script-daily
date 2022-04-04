#!/usr/bin/env bash

exec_cmd="sh $SCRIPT_HOME/gradle/run_java_module.sh"

alias es-migrate="$exec_cmd es-migrate \$@"
alias fs-migrate="$exec_cmd fs-migrate \$@"
alias json-op="$exec_cmd json-op \$@"
alias mongo-migrate="$exec_cmd mongo-migrate \$@"
alias send-mail="$exec_cmd send-mail \$@"

unset exec_cmd
