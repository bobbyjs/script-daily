#!/usr/bin/env bash

function fedora_delete_old_kernel(){
    KERNEL_VERSION=`uname -r`
    # delete the suffix: x86_64
    KERNEL_VERSION=${KERNEL_VERSION%.*}
    echo "current kernel version is $KERNEL_VERSION"
    if [[ "$1" != "-y" ]]; then
	    echo "use argument -y to delete the old kernels"
	    return 1
    fi

    # rpm -qa | grep kernel
    dnf list --installed | grep "^kernel.*" | grep -v $KERNEL_VERSION |
    while read line; do
        package=$(echo $line | awk '{print $1}')
        version=$(echo $line | awk '{print $2}')
	    package=${package%%.*}
	    name="$package-$version"
	    echo "removing $name"
	    # never do this
	    # yum remove $name -y
    done
    echo "done."
}

# don't call it via source the script
if [ $# != 0 ]; then
    fedora_delete_old_kernel $@
fi
