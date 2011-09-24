#!/bin/bash
echo "Determining Version:"
VERSION=$(cat ../src/com/t_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version)
VERSION=${VERSION#*=}
VERSION=${VERSION// /}
echo "Version is: \"$VERSION\""
cp linux/description-pak ../
cd ..
make
# hide doc directory from checkinstall
mv doc doctmp
sudo checkinstall --fstrans --type debian --install=no -y --pkgname visicut --pkgversion $VERSION --arch all --pkglicense LGPL --pkggroup other --pkgsource "http://hci.rwth-aachen.de/visicut" --pkgaltsource "https://github.com/t-oster/VisiCut" --pakdir distribute/ --maintainer "Thomas Oster <thomas.oster@rwth-aachen.de>" --requires "java-runtime" make install -e PREFIX=/usr
rm description-pak
sudo rm -rf doc-pak
mv doctmp doc
