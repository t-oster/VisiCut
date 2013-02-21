#!/bin/bash
# Performs some sanity and code cleanness tests. Intended to be run before every commit

echo "Checking for copryight header"
if ! [ -f copyrightheader ]
then
	echo "File 'copyrightheader' is missing"
	exit 1
fi
HEADERSIZE=$(wc -l < copyrightheader)
ERRORS=0
for f in $(find src -name '*.java')
do
	if ! cat $f|head -n $HEADERSIZE|diff - copyrightheader > /dev/null
	then
		echo "Copyright header mismatch on $f"
		ERRORS=1
	fi
done
exit $ERRORS
