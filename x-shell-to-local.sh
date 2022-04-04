#!/usr/bin/env bash

source_dir=$(cd "$(dirname $0)" && pwd -P)
[[ ! -d $HOME/.local/bin ]] && mkdir -p $HOME/.local/bin
target_dir=$HOME/.local/bin

# gradle
ln -s $source_dir/shell/gradle/add_gradle_submodule.sh $target_dir/add_gradle_submodule
ln -s $source_dir/shell/gradle/upgrade_gradle.sh $target_dir/upgrade_gradle

# docker
ln -s $source_dir/shell/docker/docker_build.sh $target_dir/docker_build

