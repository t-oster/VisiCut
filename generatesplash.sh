#!/bin/bash
echo "Checking for inkscape..."
if which inkscape >/dev/null 2>&1
then
	echo "found."
else
	echo "no inkscape found. skipping generation of splash"
	exit
fi
echo "Determining Version..."
VERSION=$(cat src/com/t_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version|cut -d'=' -f2|tr -d ' ')
echo "Version is: $VERSION"
echo "Generating SVG"
cat splashsource.svg|sed s#insert#$VERSION#g# > splash.svg
echo "Converting to png"
inkscape -e src/com/t_oster/visicut/gui/resources/splash.png splash.svg
echo "cleaning..."
rm splash.svg
echo "done."
