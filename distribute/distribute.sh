#!/bin/bash
set -e # exit 1 if any command fails
set -o pipefail
cd "$(dirname "$0")"
# set -x # uncomment for debugging
if [ "$1" == "--nocompile" ]
then
   COMPILE=0
else
   COMPILE=1
fi
echo "Determining Version (may be overridden with environment variable VERSION):"
# VisiCutBuilder writes the setting in that file:
VERSION=${VERSION:-$(cat ../src/main/resources/de/thomas_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version)}
# remove "Application.version =   " prefix which comes from the VisiCutApp.properties file
VERSION=${VERSION#*=}
VERSION=${VERSION// /}
# normally running ./distribute.sh doesn't write the above file. Then, fall back to what git describe tells us
VERSION=${VERSION:-$(git describe --tags || echo unknown)+devel}
echo "Version is: \"$VERSION\""
if [ "$COMPILE" == 1 ]
then
	echo "Building jar..."
	cd ..
	make clean > /dev/null
	VERSION="$VERSION" make > /dev/null || { echo "Compilation failed. Please run 'make' to see what failed."; exit 1; }
	cd distribute
fi

echo "Removing leftover files"
rm -rf visicut

echo "Copying content..."
mkdir visicut
cp -r ../target/visicut*full.jar visicut/Visicut.jar
cp -r files/* visicut/
cp ../README.md visicut/README
cp ../COPYING.LESSER visicut/
cp ../LICENSE visicut/
chmod +x visicut/*.jar
chmod +x visicut/VisiCut.*
mkdir -p visicut/inkscape_extension
cp ../tools/inkscape_extension/*.py visicut/inkscape_extension/
cp ../tools/inkscape_extension/*.inx visicut/inkscape_extension/
mkdir -p visicut/illustrator_script
cp ../tools/illustrator_script/*.scpt visicut/illustrator_script/

# check_sha256 <filename> <hash>
# -> returns 0 if file exists and its hash is correct, 1 otherwise.
function check_sha256() {
    test -e "$1" || return 1
    sha256sum "$1" | grep -q "$2" || { echo "SHA256 hash of $1 does not match. Expected: $2, Got:"; sha256sum $1; return 1; }
    return 0
}

# URL and hash of the OpenJRE ZIP file for Windows.
# You can override this with environment variables.
# The distribution by Oracle has evil license terms, so we use the OpenJDK JRE from https://adoptium.net/
WINDOWS_JRE_URL=${WINDOWS_JRE_URL:-"https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jre_x64_windows_hotspot_17.0.5_8.zip"}
WINDOWS_JRE_SHA256=${WINDOWS_JRE_SHA256:-"d25a2d44c1bd9c57d49c5e98de274cd40970ab057fe304b52eb459de4ee5d8a5"}

if which makensis > /dev/null
then
	mkdir -p cache
	echo "Downloading OpenJRE for Windows (may be overridden with WINDOWS_JRE_URL and WINDOWS_JRE_SHA256)"
	# download JRE if not existing or wrong file contents
    check_sha256 cache/jre.zip $WINDOWS_JRE_SHA256 || wget "$WINDOWS_JRE_URL" -O cache/jre.zip
    # check if downloaded JRE is correct (to exclude incomplete download)
    check_sha256 cache/jre.zip $WINDOWS_JRE_SHA256 || exit 1
    echo "Building Windows launcher and installer"
	# Copy files to wintmp/
	rm -rf wintmp
	mkdir wintmp
	cp -r windows/* wintmp/
	[ -d wintmp/stream ] || mkdir wintmp/stream
	cp -r visicut/* wintmp/stream/
	cp ../src/main/resources/de/thomas_oster/visicut/gui/resources/splash*.png wintmp/stream
	cp ../tools/inkscape_extension/* wintmp/

	# build setup.exe installer
	# and VisiCut.exe launcher executable
	cat windows/installer.nsi|sed s#VISICUTVERSION#"$VERSION"#g > wintmp/installer.nsi
	pushd wintmp > /dev/null
	# Unpack JRE. We assume that this creates a subfolder with "jre" in its name
	unzip -q ../cache/jre.zip
    mv *jre*/ stream/jre/
	test -e stream/jre/bin/java.exe || { echo "Cannot find java.exe in JRE ZIP file"; exit 1; }
	test -e stream/jre/legal/java.base/LICENSE || { echo "Cannot find license information in JRE ZIP file"; exit 1; }
	# Build license text for Windows installer
    {
        cat ../../LICENSE
        echo "Java Runtime for Windows:"
        echo -e "- OpenJRE (GPLv2 with Classpath exception). License details follow:"
        find stream/jre/legal/ -type f -print -exec cat '{}' ';'
    }  > stream/license-with-jre.txt
	makensis launcher.nsi > /dev/null || exit 1 # build VisiCut.exe
	cp VisiCut.exe ../visicut/ # copy VisiCut.exe so that it is included in the platform independent ZIP
	mv VisiCut.exe ./stream/
	makensis installer.nsi > /dev/null || exit 1 # build setup.exe
	popd
	mv wintmp/setup.exe VisiCut-$VERSION-Windows-Installer.exe || exit 1
	zip VisiCut-$VERSION-Windows-Installer.zip VisiCut-$VERSION-Windows-Installer.exe	# for github upload

	# cleanup
	rm -rf wintmp
else
	echo "NSIS not found. The resulting ZIP will be missing the Visicut.exe launcher for Windows."
	echo "Not building Windows installer."
fi

echo "Compressing content..."
[ -f VisiCut-$VERSION.zip ] && rm VisiCut-$VERSION.zip
zip -r VisiCut-$VERSION.zip visicut/  > /dev/null || exit 1

echo ""
echo "****************************************************************"
echo "Mac OS Version: Building the Mac OS Version should work on all platforms"
echo "Build Mac OS Version (Y/n)?"
# URL and hash of the OpenJRE ZIP file for OSX.
# You can override this with environment variables.
# The distribution by Oracle has evil license terms, so we use the OpenJDK JRE from https://adoptopenjdk.net/
OSX_JRE_URL=${OSX_JRE_URL:-"https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.5%2B10/OpenJDK11U-jre_x64_mac_hotspot_11.0.5_10.tar.gz"}
OSX_JRE_SHA256=${OSX_JRE_SHA256:-"dfd212023321ebb41bce8cced15b4668001e86ecff6bffdd4f2591ccaae41566"}
read answer || true
if [ "$answer" != "n" ]
then
  echo "Creating Mac OS Bundle"
  [ -d VisiCut.app ] && rm -rf VisiCut.app
  cp -r "mac/VisiCut.app" .
  mkdir -p "VisiCut.app/Contents/Resources/Java"
  cp -r visicut/* "VisiCut.app/Contents/Resources/Java/"
  mkdir "VisiCut.app/Contents/Java"
  mv "VisiCut.app/Contents/Resources/Java/Visicut.jar" "VisiCut.app/Contents/Java/"
  cp ../src/main/resources/de/thomas_oster/visicut/gui/resources/splash*.png "VisiCut.app/Contents/Resources/Java"
  echo "Updating Bundle Info"
  cp "VisiCut.app/Contents/Info.plist" .
  cat Info.plist|sed s#VISICUTVERSION#"$VERSION"#g > VisiCut.app/Contents/Info.plist
  rm Info.plist
  mkdir -p mac/jre
  echo "Downloading OpenJRE for OSX (may be overridden with OSX_JRE_URL and OSX_JRE_SHA256)"
  # download JRE if not existing or wrong file contents
  check_sha256 mac/jre/jre.tar.gz $OSX_JRE_SHA256 || wget "$OSX_JRE_URL" -O mac/jre/jre.tar.gz
  # check if downloaded JRE is correct (to exclude incomplete download)
  check_sha256 mac/jre/jre.tar.gz $OSX_JRE_SHA256 || exit 1
  echo "Inserting JRE into the app bundle..:"
  mkdir -p VisiCut.app/Contents/Plugins
  tar -xf mac/jre/jre.tar.gz -C VisiCut.app/Contents/Plugins
  mv VisiCut.app/Contents/Plugins/jdk-11.0.5+10-jre VisiCut.app/Contents/Plugins/JRE
  echo "Compressing Mac OS Bundle"
  rm -rf VisiCutMac-$VERSION.zip
  zip -r VisiCutMac-$VERSION.zip VisiCut.app > /dev/null || exit 1
fi

echo "Dir:$(pwd)"
echo "****************************************************************"
echo "Ubuntu Version: For Building you must have checkinstall and dpkg"
echo "and no VisiCut installation may be installed."
echo "Build Ubuntu Version (Y/n)?"
read answer || true
if [ "$answer" != "n" ]
then
  pushd . > /dev/null
  cp linux/*-pak ../
  cd ..
  # hide doc directory from checkinstall
  # mv doc doctmp
  test -f /usr/bin/visicut && { echo "error: please first uninstall visicut"; exit 1; }
  fakeroot checkinstall --fstrans --reset-uid --type debian --install=no -y --pkgname visicut --pkgversion $VERSION --arch all --pkglicense LGPL --pkggroup other --pkgsource "http://visicut.org" --pkgaltsource "https://github.com/t-oster/VisiCut" --pakdir distribute/ --maintainer "'Thomas Oster <thomas.oster@rwth-aachen.de>'" --requires 'bash,openjdk-11-jre\|openjdk-17-jre,potrace' make install -e PREFIX=/usr > /dev/null || { echo "error"; exit 1; }
  rm -rf *-pak
  # mv doctmp doc
  popd > /dev/null
fi

echo "Dir: $(pwd)"
echo "****************************************************************"
echo "Arch Linux Version: For Building you must have pacman installed."
echo "Build Arch Linux Version (Y/n)?"
read answer || true
if [ "$answer" != "n" ]
then
  cd linux
  ARCHVERSION=$(echo $VERSION|sed "s#-#_#g")
  cat PKGBUILD | sed "s#pkgver=VERSION#pkgver=$ARCHVERSION#g" > PKGBUILD-tmp
  makepkg -p PKGBUILD-tmp > /dev/null || exit 1
  mv *.pkg.tar.xz ../
  echo "Cleaning up..."
  rm -rf src pkg PKGBUILD-tmp
  cd ..
fi

echo "Cleaning up..."
rm -rf visicut
echo "done."
