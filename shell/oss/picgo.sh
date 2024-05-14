#!/usr/bin/env bash

# nvm ls-remote
# nvm install 20
# npm i -g yarn
# yarn global add picgo
# ln -s "$(which node)" /usr/local/bin/node

# picgo set uploader

if [[ -z $1 ]]; then
  echo "require one param: <file>"
  exit 1
fi

echo "picgo upload \"$1\""
picgo upload "$1"

exit $?
