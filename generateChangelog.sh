#!/bin/bash
if [ "$1" == '--until' ]
then
	UNTIL=$2
fi
git log --decorate=full|{
	while read line
	do
		if echo $line|grep refs/tags/ > /dev/null
		then
			TAG=$(echo $line|grep -oP 'refs/tags/\K[^,)]*')
			if [ "$UNTIL" == "$TAG" ]
			then
				exit
			fi
			echo ""
			echo "=== Verison $TAG ==="
		fi
		if echo $line|grep 'CHANGELOG:' > /dev/null
		then
			echo " * " $(echo $line|grep -oP 'CHANGELOG:\K.*')
		fi
	done
}
