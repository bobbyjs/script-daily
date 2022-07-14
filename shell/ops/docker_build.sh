#!/usr/bin/env bash

function docker_build() {
    if [[ "$1" = "--help" || "$1" = "-h" ]]; then
        log_info "Usage: $0 IMAGE_NAME DOCKER_DIR [DOCKERFILE_NAME=Dockerfile]"
        return 0
    fi

    IMAGE_NAME=$1
    DOCKER_DIR=$2
    DOCKERFILE_NAME=$3

    [[ -z $DOCKERFILE_NAME ]] && DOCKERFILE_NAME=Dockerfile
    if [[ -z $IMAGE_NAME || -z $DOCKER_DIR ]]; then
        log_error "require at least two argument as IMAGE_NAME and DOCKER_DIR"
        return 1
    fi

    # absolute path
    DOCKER_DIR=`cd $DOCKER_DIR && pwd`
    cd $DOCKER_DIR || (error "you haven no permission to cd $DOCKER_DIR" && return 1)

    # if no tag
    if [[ -z `echo $IMAGE_NAME | grep :` ]]; then
        repository=$IMAGE_NAME
        tag="latest"
    else
        repository=`echo $IMAGE_NAME | cut -d: -f1`
        tag=`echo $IMAGE_NAME | cut -d: -f2`
    fi


    if [[ -n `docker image ls | grep $repository | grep $tag` ]]; then
        log_warn "$IMAGE_NAME already exists"
        log_warn "Done"
        return 1
    fi

    log_info "Building docker image $IMAGE_NAME"
    docker build -f ./$DOCKERFILE_NAME -t $IMAGE_NAME .
    log_info "Done"
}

if [ $# != 0 ]; then docker_build $@; fi
