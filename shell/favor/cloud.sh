#!/usr/bin/env bash

function echo_save_my_images() {
    repo=$1
    [[ -z $repo ]] && repo=jerrywill
    docker images | grep $repo | while read i; do
        name=`echo $i | awk '{print $1}'`
        image_name=`echo $name | cut -d '/' -f 2`
        image_version=`echo $i | awk '{print $2}'`
        echo docker save $name:$image_version -o $image_name-$image_version.tar
    done
}
