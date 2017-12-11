#!/bin/bash
echo "This will install the inkscape-extension for VisiCut"
echo "Do you want to continue? (y/n)"
read answ
if [ "$answ" != "y" ]
then
	echo "Abort."
	exit
fi
echo "Do you want to install it for the current user only? (y/n)"
read answ
if [ "$answ" == "y" ]
then
	TARGET=~/.config/inkscape/extensions/
else
	TARGET="/usr/share/inkscape/extensions/"
	echo "WARNING: Root privileges required for system-wide install"
	echo "If it fails, try 'sudo ./install.sh'"
fi

mkdir -p $TARGET
d=$(dirname $0)
cp $d/visicut_export.inx $TARGET
cp $d/visicut_export_replace.inx $TARGET
cp $d/visicut_export.py $TARGET
cp $d/daemonize.py $TARGET

echo "installation done."
