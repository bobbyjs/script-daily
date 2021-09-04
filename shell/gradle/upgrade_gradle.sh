#!/usr/bin/env bash

source "$SCRIPT_HOME/common/log.sh"

function upgrade_gradle() {
    gv=$1
    root=$2
    dt=$3

    if [ -z "$gv" ]; then
      log_error "gradle version is required"
      exit 1
    fi
    [[ -z $root ]] && root=.
    root=$(cd $root && pwd -P)
    [[ -z $dt ]] && dt=all

    dirs=$(ls "$root")
    for dir in $dirs; do
      [[ ! -d "$root/$dir" ]] && continue

      if [ -f "$root/$dir/gradle/wrapper/gradle-wrapper.properties" ]; then
        v=$(grep distributionUrl "$root/$dir/gradle/wrapper/gradle-wrapper.properties" |
        cut -d '/' -f5 | cut -d'-' -f2)
        if [[ "$v" != "$gv" ]]; then
            cd "$root/$dir" || exit 1
            log_info "upgrade $root/$dir from $v to $gv"
            "$root/$dir/gradlew" wrapper --distribution-type $dt --gradle-version $gv
        fi
      else
        ds=$(ls "$root/$dir")
        for d in $ds; do
            [[ ! -f "$root/$dir/$d/gradle/wrapper/gradle-wrapper.properties" ]] && continue
            v=$(grep distributionUrl "$root/$dir/$d/gradle/wrapper/gradle-wrapper.properties" |
             cut -d '/' -f5 | cut -d'-' -f2)
            if [[ "$v" != "$gv" ]]; then
              cd "$root/$dir/$d" || exit 1
              log_info "upgrade $root/$dir/$d from $v to $gv"
              "$root/$dir/$d/gradlew" wrapper --distribution-type $dt --gradle-version $gv
            fi
        done
      fi
    done
}

# don't call it via source the script
if [ $# != 0 ]; then
    upgrade_gradle $@
fi
