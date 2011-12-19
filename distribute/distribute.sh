#!/bin/bash
echo "Get latest MaterialDB from git?(Y/n)"
read test
if [ "$test" != "n" ]
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
rm -rf visicut
rm -rf VisiCut.app
echo "done."
