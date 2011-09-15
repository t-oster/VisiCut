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
echo "Cleaning up..."
rm -rf visicut
echo "done."
