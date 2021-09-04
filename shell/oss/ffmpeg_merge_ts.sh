#!/bin/bash

# ffmpeg_merge_ts -p prefixname -s subffixname -n 1..36 -o

function ffmpeg_merge_ts_help() {
    echo "ffmpeg_merge_ts"
    echo "\t-h, --help\tprint help information and exit"
    echo "\t-p, --prefix\tprefixname"
    echo "\t-s, --suffix\tsuffixname"
    echo "\t-r, --range\tlike 1..36, "
    echo "\t-o, --output\toutputname"
    echo "\t-P, --pad\tpad with 0, default no padding"
    echo "\t-f, --first\tfirst number, default is 1"
    echo "\t-l, --last\tlast number, specify it to ignore '--range'"
    echo "merge {prefix}{range}{suffix]}.ts to {output}[.mkv]"
}

# exmaple, ffmpeg_merge_ts -r 0..99 -p some -o awesome
# to merge some000.ts, some001.ts, ..., some099.ts to awesome.mkv
function ffmpeg_merge_ts (){
    while [ "$@" ]; do
        arg=$1
        case $arg in
        -h | --help)
            ffmpeg_merge_ts_help
            return 0 ;;
        -p | --prefix)
            shift
            prefix=$1
            shift ;;
        -s | --suffix)
            shift
            suffix=$1
            shift ;;
        -r | --range)
            shift
            range=$1
            shift ;;
        -P | --pad)
            pad=1
            shift ;;
        -o |--output )
            shift
            output=$1
            shift ;;
        -f |--first )
            shift
            first=$1
            shift ;;
        -l |--last )
            shift
            last=$1
            shift ;;
        *)
            log_error "invaild option $arg"
            exit 1 ;;
        esac
    done;

    # default no padding with 0
    pad=${pad=0}
    first=${first=1}

    # has ,
    if [ `echo $range | grep ","` ]; then
        first=`echo $range | cut -d, -f1`
        last=`echo $range | cut -d, -f2`
    elif [ `echo $range | grep ";"` ]; then
        first=`echo $range | cut -d; -f1`
        last=`echo $range | cut -d; -f2`
    elif [ `echo $range | grep "\.\."` ]; then
        first=`echo $range | cut -d; -f1`
        last=`echo $range | cut -d; -f3`
    elif [ "x" != "x$range" ]; then
        log_error "invaild value for '-r, --range'"
        exit 1
    fi

    width=${#last}
    norce="ffmpeg-$(uuid)"
    filelist=$norce.list

    if [[ -n `echo $output | cut -d. -f1` ]]; then
        output=$norce.mkv
    elif [[ -n `echo $output | cut -d. -f2` ]]; then
        outfile=$output.mkv
    fi

    log_info "generate filelist to $filelist"
    if [[ -z $first || -z $last ]]; then
        log_error "invalid usage, run '$0 -h' for help"
        return 1
    fi

    each=$first
    while (( $each <= $last ))
    do
        name=$each
        if [ $pad == 1 ]; then
            name=$(printf "%.${width}d" $each)
        fi
        echo file \'$prefix$name.ts\'
        let "each++"
    done > $filelist

    log_info "merge ts to $output"
    ffmpeg -f concat -i $filelist -c copy -bsf:a aac_adtstoasc $output
}

# don't call it via source the script
if [ $# != 0 ]; then
    ffmpeg_merge_ts $@
fi
