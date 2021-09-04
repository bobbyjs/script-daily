#!/usr/bin/env bash

function fib() {
  if [ $1 -le 2 ]; then
    echo 1
    return
  fi
  a=1; b=1; c=1; i=3
  while [[ $i -le $1 ]]; do
    c=$(($a + $b))
    a=$b
    b=$c
    i=$(($i + 1))
  done
  echo $c
}

#function fib() {
#  if [ $1 -le 2 ]; then
#    echo 1
#    return
#  fi
#  a=$(fib $(($1 - 1)))
#  b=$(fib $(($1 - 2)))
#  echo $(($a + $b))
#}

