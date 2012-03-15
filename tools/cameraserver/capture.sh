#!/bin/bash
#
# This file is part of VisiCut.
# Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
# RWTH Aachen University - 52062 Aachen, Germany
# 
#     VisiCut is free software: you can redistribute it and/or modify
#     it under the terms of the GNU Lesser General Public License as published by
#     the Free Software Foundation, either version 3 of the License, or
#     (at your option) any later version.
# 
#    VisiCut is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU Lesser General Public License for more details.
# 
#     You should have received a copy of the GNU Lesser General Public License
#     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
#

# This script processes one HTTP request by capturing a photo
# and sending back an JPEG image
source config.sh

# wait for GET
read line
# take the snapshot
if [ "$TOOL" == "gphoto2" ]
then
	# delete the file if it exists
	rm -f capt0000.jpg
	gphoto2 --capture-image-and-download >/dev/null 2>&1
	IMAGEFILE=capt0000.jpg
elif [ "$TOOL" == "snap" ]
then
	
	$SNAPBIN >/dev/null 2>&1
	IMAGEFILE=snapshot.jpg
fi
	
# rotate the image
	if [ "$ROTATION" != "0" ]
	then
		mogrify -rotate "$ROTATION>" $IMAGEFILE
	fi
# output an HTTP header
	eval $(stat -s $IMAGEFILE)
	echo -ne "HTTP/1.0 200 OK\r\n"
	echo -ne "Content-Type: image/jpeg\r\n"
	echo -ne "Content-Length: $st_size\r\n"
	echo -ne "Connection: close\r\n"
	echo -ne "\r\n"	
#output the file
	cat $IMAGEFILE
#process remaining input
while read line
do
	echo $line > /dev/null
done
