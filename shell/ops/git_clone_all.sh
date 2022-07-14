#!/usr/bin/env bash

function git_clone_all() {
    [[ -z "$1" ]] && (echo "usage: git_clone_all [dir] [-y]" && return 1)
    cd "$1"
    work_dir=$(pwd -P)

    for category in $(ls); do
      [[ ! -d "$category" ]] && continue

      for project in $(ls $work_dir/$category); do
        current=$work_dir/$category/$project
        [[ ! -d "$current" ]] && (echo "$current is not a dir" && continue 1)
        cd $current || (echo "failed to cd $current" && continue 1)
        echo "working on $current"
        [[ ! -d ".git" ]] && (echo ".git is not in pwd" && continue 1)
        git branch --show-current || (echo "not a git repo" && continue 1)
        [[ $(git status | grep '^nothing to commit') ]] || (echo "has uncommitted changes" && continue 1)
        [[ "$2" = "-y" ]] && (echo "performing: git pull --all" && git pull --all)
      done
    done
}

if [ $# != 0 ]; then git_clone_all $@; fi
