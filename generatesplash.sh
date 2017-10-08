#!/bin/bash
echo "Checking for rsvg..."
if which rsvg-convert >/dev/null 2>&1
then
	echo "found."
else
	echo "no rsvg-convert found. skipping generation of splash"
	exit
fi
echo "Determining Version..."
VERSION=$(cat src/com/t_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version|cut -d'=' -f2|tr -d ' ')
echo "Version is: $VERSION"
echo "Generating SVG"
cat splashsource.svg|sed s#insert#$VERSION#g# > splash.svg
echo "Converting to png"
rsvg-convert -w 404 -h 304 splash.svg > src/com/t_oster/visicut/gui/resources/splash.png
echo "cleaning..."
rm splash.svg
echo "done."
