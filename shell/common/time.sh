#!/usr/bin/env bash

function get_millisecond() {
    # in GNU Linux
    ts=$(date +"%N")
    if [[ $ts != 'N' ]]; then
      echo $(( $ts / 1000000 ))
      return 0
    fi
    ts=$(perl -MTime::HiRes -e 'printf("%.0f\n",Time::HiRes::time()*1000)')
    echo $(( $ts % 1000 ))
}
