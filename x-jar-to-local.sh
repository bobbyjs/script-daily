#!/usr/bin/env bash

exec_cmd="sh $SCRIPT_SHELL_HOME/gradle/run_java_module.sh"

alias es-migrate="$exec_cmd es-migrate \$@"
alias fs-migrate="$exec_cmd fs-migrate \$@"
alias json-op="$exec_cmd json-op \$@"
alias mongo-migrate="$exec_cmd mongo-migrate \$@"
alias send-mail="$exec_cmd send-mail \$@"
alias text-find="$exec_cmd text-find \$@"

unset exec_cmd
