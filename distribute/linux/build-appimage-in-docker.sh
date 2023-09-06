#!/usr/bin/env bash

set -e
set -x

# calculate path to project root relative to this file
project_root=$(readlink -f $(dirname "$0"))/../..

# change to project root directory
cd "$project_root"

# build Docker image to cache dependencies between builds
image=visicut-appimage-build
dockerfile=distribute/linux/Dockerfile.build-appimage
docker build -t "$image" -f "$dockerfile" $(dirname "$dockerfile")

docker run --rm -i --user "$(id -u)" -v "$project_root":/ws --workdir /ws "$image" sh -x <<EOF
export APPIMAGE_EXTRACT_AND_RUN=1
git config --global --add safe.directory /ws
appimagecraft
EOF
