#!/usr/bin/env bash

# <exclusion>\n *<artifactId>(.*?)</artifactId>\n *<groupId>(.*?)</groupId>\n *</exclusion>
# exclude group: '$2', module: '$1'
function maven_dep_to_gradle() {
    echo
}

# don't call it via source the script
if [ $# != 0 ]; then
    maven_dep_to_gradle $@
fi
