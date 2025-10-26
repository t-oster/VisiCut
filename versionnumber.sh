#!/usr/bin/env bash
set -e

# This script determines the version number and prints it to stdout.
# The version is determined from the VERSION environment variable, or automatically from Git.

project_root_dir="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
cd $project_root_dir

if [[ "${VERSION:-}" != "" ]]; then
    echo "Using user-provided version \$VERSION=$VERSION" >&2
else
    echo "Determining version from git (use VERSION env variable to override)" >&2

    # as the GitHub actions workflow creates a continuous tag on the main branch's HEAD to create prereleases for every push, we must ignore those tags
    # we need to ignore this tag to get a proper version number
    # if the command fails, we must abort at this point, as we cannot fall back to some generic name like "unknown" without breaking at least the Debian package build
    if ! VERSION="$(git describe --tags --exclude 'continuous' --dirty='-modified')"; then
        echo "Error: could not fetch proper version number with git, try git fetch -a, or define the VERSION environment variable to set the version" >&2
        exit 2
    fi
fi
echo $VERSION
