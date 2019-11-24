#!/bin/bash
echo "Checking for rsvg..."
if which rsvg-convert >/dev/null 2>&1
then
	echo "found rsvg-convert."
else
	echo "no rsvg-convert found. skipping generation of splash" >&2
	cp src/main/resources/de/thomas_oster/visicut/gui/resources/splash{-fallback,}.png
	exit
fi
echo "Determining Version (may be overridden with environment variable VERSION)"
cd "$(dirname $0)"
VERSION=${VERSION:-$(cat ./src/main/resources/de/thomas_oster/visicut/gui/resources/VisicutApp.properties |grep Application.version)}
VERSION=${VERSION#*=}
VERSION=${VERSION// /}
echo "Version is: \"$VERSION\""
echo "Version is: $VERSION"
echo "Generating SVG"
cat splashsource.svg|sed s#insert#$VERSION#g# > splash.svg
echo "Converting to png"
rsvg-convert -w 404 -h 304 splash.svg > src/main/resources/de/thomas_oster/visicut/gui/resources/splash.png
echo "cleaning..."
rm splash.svg
echo "done."
