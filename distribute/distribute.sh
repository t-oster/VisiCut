#!/bin/bash
echo "Get latest MaterialDB from git (Y/n)?"
read answer
if [ "$answer" != "n" ]
then
	pushd files/settings
	if [ -d VisiCut-MaterialDB/.git ]
	then
		echo "already git => we pull"
		pushd VisiCut-MaterialDB
		git pull
		popd
	else
		echo "we have to clone the repository first"
		rm -rf VisiCut-MaterialDB
		git clone git://github.com/t-oster/VisiCut-MaterialDB.git
	fi
	popd
	echo "sucessfully updated"
fi

echo "Determining Version:"
VERSION=$(cat ../src/com/t_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version)
VERSION=${VERSION#*=}
VERSION=${VERSION// /}
echo "Version is: \"$VERSION\""
echo "Building jar..."
cd ..
ant clean
ant jar
cd distribute
echo "Copying content..."
mkdir visicut
cp -r ../dist/* visicut/
cp -r files/* visicut/
cp ../README visicut/
cp ../COPYING.LESSER visicut/
cp ../LICENSE visicut/
rm visicut/README.TXT
chmod +x visicut/*.jar
chmod +x visicut/VisiCut.*
echo "Compressing content..."
rm VisiCut-*.zip
zip -r VisiCut-$VERSION.zip visicut/

echo ""
echo "****************************************************************"
echo "Mac OS Version: Building the Mac OS Version should work on all platforms"
echo "Build Mac OS Version (Y/n)?"
read answer
if [ "$answer" != "n" ]
then
  echo "Creating Mac OS Bundle"
  cp -r "mac/VisiCut.app" .
  cp -r "visicut" "VisiCut.app/Contents/Resources/Java"
  echo "Updating Bundle Info"
  cp "VisiCut.app/Contents/Info.plist" .
  cat Info.plist|sed s#VISICUTVERSION#"$VERSION"#g > VisiCut.app/Contents/Info.plist
  rm Info.plist
  echo "Compressing Mac OS Bundle"
  rm -r VisiCutMac-*.zip
  zip -r VisiCutMac-$VERSION.zip VisiCut.app
  echo "Cleaning up..."
  rm -rf VisiCut.app
fi

echo "Dir:$(pwd)"
echo "****************************************************************"
echo "Ubuntu Version: For Building you must have checkinstall and dpkg"
echo "and no VisiCut installation may be installed."
echo "Build Ubuntu Version (Y/n)?"
read answer
if [ "$answer" != "n" ]
then
  cp linux/description-pak ../
  cd ..
  make
  # hide doc directory from checkinstall
  mv doc doctmp
  sudo checkinstall --fstrans --type debian --install=no -y --pkgname visicut --pkgversion $VERSION --arch all --pkglicense LGPL --pkggroup other --pkgsource "http://hci.rwth-aachen.de/visicut" --pkgaltsource "https://github.com/t-oster/VisiCut" --pakdir distribute/ --maintainer "Thomas Oster <thomas.oster@rwth-aachen.de>" --requires "java-runtime" make install -e PREFIX=/usr
  rm description-pak
  sudo rm -rf doc-pak
  mv doctmp doc
  cd distribute
fi

echo "Dir: $(pwd)"
echo "****************************************************************"
echo "Arch Linux Version: For Building you must have pacman installed."
echo "Build Arch Linux Version (Y/n)?"
read answer
if [ "$answer" != "n" ]
then
  cd linux
  makepkg
  mv *.pkg.tar.xz ../
  echo "Cleaning up..."
  rm -rf src pkg
  cd ..
fi

echo "Cleaning up..."
rm -rf visicut  
echo "done."
