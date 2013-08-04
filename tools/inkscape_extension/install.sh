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
cp visicut_export.inx $TARGET
cp visicut_export_replace.inx $TARGET
cp visicut_export.py $TARGET
cp daemonize.py $TARGET

echo "installation done."
