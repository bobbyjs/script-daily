#!/usr/bin/env bash

source "$SCRIPT_SHELL_HOME/common/log.sh"

function upgrade_gradle() {
    gv=$1
    root=$2
    dt=$3

    if [ -z "$gv" ]; then
        log_info "gradle version is not pass, try to obtain it from https://gradle.org/releases"
        gv=$(curl https://gradle.org/releases/ | grep resources-contents -A1 | tail -1 | cut -d'"' -f2)
        if [ -z "$gv" ]; then
            log_error "gradle version is required"
            return 1
        fi
    fi
    [[ -z $root ]] && root=.
    root=$(cd $root && pwd -P)
    [[ -z $dt ]] && dt=all

    old_pwd=$(pwd -P)
    log_info "upgrade_gradle $gv $root $dt"
    dirs=$(ls "$root")
    echo "$dirs" | while read dir; do
        if [ ! -d "$root/$dir" ]; then log_warn "skip since not a dir: $root/$dir" ; continue; fi

        if [ -f "$root/$dir/gradle/wrapper/gradle-wrapper.properties" ]; then
            v=$(grep distributionUrl "$root/$dir/gradle/wrapper/gradle-wrapper.properties" |
                cut -d '/' -f5 | cut -d'-' -f2)
            if [[ "$v" != "$gv" ]]; then
                cd "$root/$dir" || return 1
                log_info "upgrade $root/$dir from $v to $gv"
                "$root/$dir/gradlew" wrapper --distribution-type $dt --gradle-version $gv
                cd $old_pwd
                echo "done $root/$dir"
            else
                log_warn "$root/$dir: already $gv $dt"
            fi
        else
            dirs=$(ls "$root/$dir")
            echo "$dirs" | while read d; do
                if [ ! -f "$root/$dir/$d/gradle/wrapper/gradle-wrapper.properties" ]; then
                    log_info "not a gradle project: $root/$dir/$d"; continue
                fi
                v=$(grep distributiconUrl "$root/$dir/$d/gradle/wrapper/gradle-wrapper.properties" |
                    cut -d '/' -f5 | cut -d'-' -f2)
                if [[ "$v" != "$gv" ]]; then
                    cd "$root/$dir/$d" || return 1
                    log_info "upgrade $root/$dir/$d from $v to $gv"
                    "$root/$dir/$d/gradlew" wrapper --distribution-type $dt --gradle-version $gv
                    cd $old_pwd
                    echo "done $root/$dir/$d"
                else
                    log_warn "$root/$dir/$d: already $gv $dt"
                fi
                echo "loop done $root/$dir/$d"
            done
        fi
        echo "loop done $root/$dir"
    done
}

# don't call it via source the script
if [ $# != 0 ]; then
    upgrade_gradle $@
fi
