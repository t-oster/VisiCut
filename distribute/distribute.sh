#!/usr/bin/env bash

set -euo pipefail

# small optimization: if a RAM disk is available, use it for building
# saving writes on an SSD will extend its life, after all!
# FIXME: disabled because it is not guaranteed that this will be large enough to build VisiCut.
#if [[ "${TMPDIR:-}" == "" ]] && [[ -d /dev/shm ]]; then
#    export TMPDIR=/dev/shm
#fi

log() {
    if [[ -t 0 ]]; then
        tput setaf 2
        tput bold
    fi
    echo "== $* =="
    if [[ -t 0 ]]; then
        tput sgr0
    fi
}

# targets to be built need to be passed on the commandline
# if no targets are provided, we display a help text
if [[ "${1:-}" == "" ]]; then
    log "Usage: [env NO_BUILD=1] $0 <targets>"
    echo
    log "Available targets:"
    log "    - zip"
    log "    - windows-nsis"
    log "    - macos-bundle"
    log "    - linux-appimage"
    log "    - linux-checkinstall"
    echo
    log "Available environment variables:"
    log "    - \$NO_BUILD=[...]: if set to any string, $0 won't build a JAR (saves build time if the script ran already)"
    exit 2
fi

# we move all build artifacts back to the script's original working directory
old_cwd="$(readlink -f "$PWD")"

# make it easier to work from the script
distribute_dir="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"

# we assume the distribute directory is a direct subdirectory of the project root directory (i.e., the Git repository)
project_root_dir="$(readlink -f "$distribute_dir"/..)"

if [[ "${VERSION:-}" != "" ]]; then
    log "Using user-provided version \$VERSION=$VERSION"
else
    pushd "$project_root_dir"

    properties=src/main/resources/de/thomas_oster/visicut/gui/resources/VisicutApp.properties
    VERSION="$(grep Application.version < "$properties" | cut -d= -f2 | tr -d ' ')"

    # usually, distribute.sh doesn't create the properties file, so we implement a Git based fallback
    if [[ "$VERSION" != "" ]]; then
        log "Using properties-provided version $VERSION"
    else
        VERSION="$(git describe --tags || echo unknown)+devel"
    fi

    popd
fi

export VERSION

if [ "${NO_BUILD:-}" == "" ]; then
    log "Building VisiCut JAR"
    # TODO: make out-of-source builds possible
	pushd "$project_root_dir"
	make clean jar
	popd
fi

# we "ab"use the deployment directory to create the "visicut directory"
# this directory contains all of VisiCut's binaries and resources
# consider it a semi-portable bundle (a JVM is needed to make it complete)
# the resulting directory can then be put into various types of packages
visicut_dir="$(readlink -f "$distribute_dir"/visicut)"

if [[ -d "$visicut_dir" ]]; then
    log "Cleaning up old visicut directory in $visicut_dir"
    rm -r "$visicut_dir"
fi

log "Creating visicut directory in $visicut_dir"

mkdir "$visicut_dir"

cp -v "$project_root_dir"/target/visicut-*-full.jar "$visicut_dir"/Visicut.jar
cp -R -v "$distribute_dir"/files/* "$visicut_dir"/
chmod +x "$visicut_dir"/{*.jar,VisiCut.*}

cp -v "$project_root_dir"/README.md "$visicut_dir"/README
cp -v "$project_root_dir"/{COPYING.LESSER,LICENSE} "$visicut_dir"/

mkdir -p "$visicut_dir"/{inkscape_extension,illustrator_script}
cp -v "$project_root_dir"/tools/inkscape_extension/{*.py,*.inx} "$visicut_dir"/inkscape_extension/
cp -v "$project_root_dir"/tools/illustrator_script/*.scpt "$visicut_dir"/illustrator_script/

download_and_extract_jdk() {
    local url="$1"
    local hash="$2"

    pushd "$build_dir"

    wget --content-disposition "$url"
    downloaded_file="$(find . -iname '*.zip' -or -iname '*.tar*' -type f | head -n1)"
    echo "$hash  $downloaded_file" | sha256sum -c

    if [[ "$downloaded_file" == *.zip ]]; then
        unzip ./*.zip
    elif [[ "$downloaded_file" == ./*.tar* ]]; then
        tar -xvf ./*.tar*
    else
        # should never happen
        log "Unknown archive type"
        exit 4
    fi

    mv ./*jre/ jre

    popd
}

# now that the visicut directory has been set up, we can perform platform-specific bundling
for target in "$@"; do
    log "Building for target $target"

    build_dir="$(mktemp -d -t visicut-build-$target-XXXXXX)"
    cleanup() {
        if [[ -d "$build_dir" ]]; then
            rm -rf "${build_dir:?}"/
        fi
    }
    trap cleanup EXIT

    build_windows_launcher() {
        # build launcher
        pushd "$build_dir"
        # need to copy the NSIS files to the build dir, since relative paths are relative to the config directory
        cp -Rv "$distribute_dir"/windows "$build_dir"/windows-launcher
        pushd windows-launcher
        makensis launcher.nsi
        popd
        mv -v windows-launcher/VisiCut.exe visicut/
        rm -rf windows-launcher/
        popd
    }

    case "$target" in
        zip)
            pushd "$build_dir"
            cp -Rv "$visicut_dir" visicut
            build_windows_launcher
            zip -r visicut.zip visicut
            destination="$old_cwd/VisiCut-$VERSION.zip"
            mv visicut.zip "$destination"
            popd
            echo "Success: Created ZIP at $destination"
            ;;

        windows-nsis)
            install -D "$project_root_dir"/src/main/resources/de/thomas_oster/visicut/gui/resources/splash*.png "$build_dir"/

            jre_url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_windows_hotspot_17.0.5_8.zip"
            jre_hash="d25a2d44c1bd9c57d49c5e98de274cd40970ab057fe304b52eb459de4ee5d8a5"
            download_and_extract_jdk "$jre_url" "$jre_hash"

            # validate JRE
            test -e "$build_dir"/jre/bin/java.exe
            test -e "$build_dir"/jre/legal/java.base/LICENSE

            # set up installer-specific license information
            cat >"$build_dir"/license-with-jre.txt <<EOF
"$(cat "$project_root_dir"/LICENSE)"

Java runtime for Windows: OpenJRE \(GPLv2 with Classpath exception\)
"$(find "$build_dir"/jre/legal -type f -exec cat '{}' \;)"
EOF

            # copy visicut directory for packaging
            cp -R "$visicut_dir"/ "$build_dir"/visicut

            # place JRE in visicut dir
            mv "$build_dir"/jre "$build_dir"/visicut/jre

            build_windows_launcher

            # build the installer
            pushd "$build_dir"
            # need to copy the NSIS files to the build dir, since relative paths are relative to the config directory
            cp -Rv "$distribute_dir"/windows/* "$build_dir"/
            makensis installer.nsi
            popd

            filename_prefix="VisiCut-$VERSION-Windows-Installer"
            mv "$build_dir"/setup.exe "$filename_prefix".exe
            echo "Success: Built Windows EXE Installer in $(pwd)/${filename_prefix}.exe"
            ;;

        macos-bundle)
            jre_url="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.5%2B10/OpenJDK11U-jre_x64_mac_hotspot_11.0.5_10.tar.gz"
            jre_hash="dfd212023321ebb41bce8cced15b4668001e86ecff6bffdd4f2591ccaae41566"
            download_and_extract_jdk "$jre_url" "$jre_hash"

            # prepare bundle directory
            pushd "$build_dir"

            # copy app template directory
            cp -R "$distribute_dir"/mac/VisiCut.app .

            # copy visicut directory contents into macOS style location
            mkdir -p VisiCut.app/Contents/Resources/Java/
            cp -Rv "$visicut_dir"/* VisiCut.app/Contents/Resources/Java/

            # however, the JAR needs to be put into another location
            mkdir -p VisiCut.app/Contents/Java
            mv VisiCut.app/Contents/Resources/Java/Visicut.jar VisiCut.app/Contents/Java/

            cp "$project_root_dir"/src/main/resources/de/thomas_oster/visicut/gui/resources/splash*.png VisiCut.app/Contents/Resources/Java

            # update version
            # unfortunately, this is the most elegant way we can use the file as a template
            sed -i s#VISICUTVERSION#"$VERSION"#g VisiCut.app/Contents/Info.plist

            # deploy jre
            mkdir -p VisiCut.app/Contents/Plugins/
            mv jre/* VisiCut.app/Contents/Plugins/

            # create bundle
            zip -r bundle.zip VisiCut.app/

            # build final filename
            filename_prefix="VisiCutMac-$VERSION"
            mv bundle.zip "$old_cwd"/"$filename_prefix".zip

            popd
            ;;

        linux-appimage)
            pushd "$build_dir"

            # appimagecraft needs a jre and visicut dir next to its config file, so let's do that!
            jre_url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_linux_hotspot_17.0.5_8.tar.gz"
            jre_hash="11326464a14b63e6328d1d2088a23fb559c0e36b3f380e4c1f8dcbe160a8b95e"
            download_and_extract_jdk "$jre_url" "$jre_hash"

            # also, some desktop integration stuff
            cp "$distribute_dir"/linux/VisiCut.desktop "$build_dir"/
            cp "$project_root_dir"/src/main/resources/de/thomas_oster/visicut/gui/resources/icon.png "$build_dir"/visicut.png

            cp -R "$visicut_dir" "$build_dir"/visicut

            # we use the same Linux launcher for every bundle, so let's put it into our newly created visicut dir
#            cp "$distribute_dir"/linux/VisiCut.Linux "$build_dir"/visicut/

            cp "$distribute_dir"/linux/appimagecraft.yml .
            appimagecraft

            mv -v ./VisiCut*.AppImage* "$old_cwd"

            popd
            ;;

        linux-checkinstall)
            pushd "$build_dir"

            # copy helper scripts
            cp "$distribute_dir"/linux/*-pak .
            # copy the whole source tree
            cp -r "$project_root_dir"/*  "$build_dir"

            test -f /usr/bin/visicut && { echo "error: please first uninstall visicut"; exit 1; }

            fakeroot checkinstall --fstrans --reset-uid --type debian --install=no -y \
                --pkgname visicut --pkgversion "$VERSION" --arch all --pkglicense LGPL \
                --pkggroup other --pkgsource "http://visicut.org" \
                --pkgaltsource "https://github.com/t-oster/VisiCut" \
                --pakdir distribute/ --maintainer "'Thomas Oster <thomas.oster@rwth-aachen.de>'" \
                --requires 'bash,openjdk-11-jre\|openjdk-17-jre,potrace' \
                make install -e PREFIX=/usr

            mv -v ./distribute/*.deb "$old_cwd"

            popd
            ;;

        *)
            log "Unknown target $target. Exiting."
            exit 1
            ;;
    esac

    cleanup
done
