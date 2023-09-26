#!/usr/bin/env bash

proj_dir=$(cd "$(dirname $0)"/.. && pwd -P)
jar_path=$proj_dir/build/libs/jdbc-op-0.1-all.jar
conf_dir=$proj_dir/bench

function jdbc-op() {
  if [ ! -f $jar_path ]; then
    $proj_dir/gradlew -p $proj_dir fatJar
  fi
  java -jar $jar_path $@
}

function test_all_types() {
  datasource_type=$1 && shift
  jdbc-op type-table test.tb_all_types -S $datasource_type \
    -f $conf_dir/types/$datasource_type.txt \
    -b 1000 --cn 't_$name' -c '列类型: $type' $@
}


