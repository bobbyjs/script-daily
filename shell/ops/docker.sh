function docker_save_images() {
    docker image ls | tail -n +2 | while read line; do
        name=$(echo $line | awk '{print $1}')
        ver=$(echo $line | awk '{print $2}')
        img=$(echo $line | awk '{print $3}')
        output_name=$(echo $name | sed 's/docker.io\///' | sed 's/library\///' | sed 's/\//_/g')
        if [[ $ver = "latest" ]]; then ver=; else ver="-$ver"; fi
        docker save $img -o "$output_name$ver.tar" &
    done
}

function docker_find() {
    docker ps -a --format "{{.Names}}" | grep $1
}
