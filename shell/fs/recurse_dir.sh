#!/usr/bin/env bash

# recurse_dir <dir> <func>
function recurse_dir() {
    echo "exec recurse_dir $1 $2"
    files=`ls -a $1`
    for f in $files; do
      [[ $f = '.' || $f = '..' ]] && continue
      new_f="$1/$f"

      if [ -f $new_f ]; then
        echo "exec $2 $new_f"
        $2 $new_f
      elif [ -d $new_f ]; then
        echo "exec recurse_dir $new_f $2"
        recurse_dir $new_f $2
      else
        echo "skip $new_f"
      fi

    done
}

if [ $# != 0 ]; then
    recurse_dir $@
fi
