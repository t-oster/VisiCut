#!/usr/bin/env bash

set -euxo pipefail

# make it easier to work from the script
distribute_dir="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"

image_name="visicut-distribution"

docker build -t "$image_name" -f "$distribute_dir"/Dockerfile "$distribute_dir"

extra_args=()
if [[ -t 0 ]]; then
    extra_args+=("-t")
fi

# mount current working directory as /cwd so that the resulting artifacts show up in it
# also mount distribute/'s parent directory so that distribute.sh has access to all the necessary files
# the reason is that we cannot predict where this script is called from but want to avoid any difference in executing
# this script instead of distribute.sh directly
docker run \
    "${extra_args[@]}" \
    --rm \
    -e BUILD \
    -e TMPDIR=/ramdisk \
    -i \
    -w /cwd \
    -v "$PWD":/cwd \
    -v "$distribute_dir/..":/visicut \
    --user "$(id -u)" \
    --tmpfs "/ramdisk:uid=$(id -u),gid=$(id -g),exec" \
    "$image_name" \
    /visicut/distribute/distribute.sh "$@"
