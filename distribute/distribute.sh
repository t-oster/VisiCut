#!/bin/bash
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
rm visicut/README.TXT
chmod +x visicut/*.jar
chmod +x visicut/VisiCut.*
echo "Compressing content..."
rm VisiCut.zip
zip -r VisiCut.zip visicut/
echo "Creating Mac OS Bundle"
rm -rf "VisiCut.app"
cp -r "mac/VisiCut.app" .
cp -r "visicut" "VisiCut.app/Contents/Resources/Java"
echo "Compressing Mac OS Bundle"
zip -r VisiCutMac.zip VisiCut.app
echo "Cleaning up..."
rm -rf visicut
rm -rf VisiCut.app
echo "done."
