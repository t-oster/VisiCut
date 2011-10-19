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
FILE=capt0000.jpg
gphoto2 --capture-image-and-download >/dev/null 2>&1
mogrify -rotate "180>" $FILE
	echo -ne "HTTP/1.0 200 OK\r\n"
	echo -ne "Content-Type: image/jpeg\r\n"
	echo -ne "Content-Length: $(stat -c %s $FILE)\r\n"
	echo -ne "Connection: close\r\n"
	echo -ne "\r\n"	
cat $FILE
	while read line
	do
		echo $line > /dev/null
	done
	rm -f $FILE
