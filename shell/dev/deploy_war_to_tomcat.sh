#!/usr/bin/env bash

function info() {
    echo -e "\033[32m$@\033[0m"
}

function warn() {
    echo -e "\033[33m$@\033[0m"
}

function error() {
    echo -e "\033[31m$@\033[0m"
}

function deploy_war_to_tomcat () {
    CATALINA_BASE=$1
    if [ "x" = "x$CATALINA_BASE" ]; then
        error "too short argument list, require one at least"
        info "usage: $0 CATALINA_BASE [WARPATH] [JAVA_OPTS]"
        return 1
    fi

    if [ ! -d $CATALINA_BASE ]; then
        error "tomcat home you specified is not exist"
        return 1
    fi

    shift 2&> /dev/null
    WARPATH=$1
    if [ "-" = "$(echo $WARPATH | cut -c 1)" -o "x" = "x$WARPATH" ]; then
        # show warning in yellow bgcolor
        warn "no war path, so try to search it"
        # try to find a war in the root diretory of a gradle project
        if [ -x "gradlew" ]; then
            info "found 'gradlew' in the work diretory, so try to use './build/libs/ROOT.war'"
            WARPATH="./build/libs/ROOT.war"
        fi

        if [ ! -f  $WARPATH ]; then
            error -e "fail to detect war path, abort"
            return 1
        fi
    else
        shift 2&> /dev/null
    fi

    if [ ! -d "$CATALINA_BASE/webapps" ]; then
        error -e "tomcat webapps is not exist"
        return 1
    fi

    info "executing '$CATALINA_BASE/bin/shutdown.sh'"
    $CATALINA_BASE/bin/shutdown.sh
    info "executing 'bash -c \"rm -rf $CATALINA_BASE/webapps/*\"'"
    bash -c "rm -rf $CATALINA_BASE/webapps/*"
    info "executing 'cp -f to $WARPATH $CATALINA_BASE/webapps/ROOT.war'"
    cp -f $WARPATH $CATALINA_BASE/webapps/ROOT.war

    EXTRA_OPTS=$@
    # JAVA_OPTS
    info "adding extra JAVA_OPTS '$EXTRA_OPTS'"
    echo "export JAVA_OPTS=\"\$JAVA_OPTS $EXTRA_OPTS\"" > $CATALINA_BASE/bin/setenv.sh
    info "executing '$CATALINA_BASE/bin/startup.sh'"
    $CATALINA_BASE/bin/startup.sh
}

# don't call it via source the script
if [ $# != 0 ]; then
    deploy_war_to_tomcat $@
fi
