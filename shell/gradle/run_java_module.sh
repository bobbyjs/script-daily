#!/usr/bin/env bash

current_dir=$(cd "$(dirname $0)" && pwd -P)
project_dir=$(cd "$current_dir/../.." && pwd -P)

module_name=$1
shift
if [ -z "$module_name" ]; then
  log_error "module_name is unset"
  exit 1
fi

jar_version=$($project_dir/gradlew :java:$module_name:printVersion -q)

jar_dir="$project_dir/java/$module_name/build/libs"
# springboot module
if [[ $($project_dir/gradlew :java:$module_name:tasks -q | grep bootJar) ]]; then
  jar_file="$jar_dir/$module_name-$jar_version.jar"
  if [ ! -f "$jar_file" ]; then
    $project_dir/gradlew :java:$module_name:bootJar
  fi
# fat jar project
else
  jar_file="$jar_dir/$module_name-$jar_version-all.jar"
  if [ ! -f "$jar_file" ]; then
      $project_dir/gradlew :java:$module_name:fatJar
  fi
fi

jvm_args=()
main_args=()
while [ $# -gt 0 ]; do
    if [[ $1 == -D* ]]; then
      # jvm_args+=( "$1" )
      jvm_args[${#jvm_args[@]}]="$1"
      # printf "jvm  %-32s : %32s\n" "${jvm_args[@]}" "$jvm_args"
    else
      #main_args=( "$1" )
      main_args[${#main_args[@]}]="$1"
      # printf "main %-32s : %32s\n" "${main_args[@]}" "$main_args"
    fi
    shift
done

java "${jvm_args[@]}" \
  -jar $jar_file "${main_args[@]}"
