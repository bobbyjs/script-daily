#!/usr/bin/env bash

SCRIPT_HOME=$(cd "$(dirname $0)" && pwd -P)
SCRIPT_SHELL_HOME=$SCRIPT_HOME/shell

exec_cmd="bash $SCRIPT_SHELL_HOME/dev/run_java_module.sh"

alias es-op="$exec_cmd es-op \$@"
alias fs-op="$exec_cmd fs-op \$@"
alias mongo-op="$exec_cmd mongo-op \$@"
alias send-mail="$exec_cmd send-mail \$@"

alias text-find="$exec_cmd text-find \$@"
alias json-op="$exec_cmd json-op \$@"
alias jdbc-op="$exec_cmd jdbc-op \$@"
alias eval-op="$exec_cmd eval-op \$@"

unset exec_cmd
