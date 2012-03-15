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
PORT=8088
source config.sh
if [ "$TOOL" == "gphoto2" ]
then
	if [ ! -x "$GPHOTO2BIN" ]
	then
		echo "Could not find gphoto2 ($GPHOTO2BIN)"
		exit 1
	fi
	CAMERAS=$("$GPHOTO2BIN" --auto-detect|tail -n +3)
elif [ "$TOOL" == "snap" ]
then
	if [ ! -x "$SNAPBIN" ]
	then
		echo "Could not find gphoto2 ($SNAPBIN)"
		exit 1
	fi
	CAMERAS=$($SNAPBIN -l|tail -n +1)
else
	echo "Error: Unknown tool $TOOL"
	exit 1
fi
if [ "$CAMERAS" == "" ]
then
	echo "Error: No camera detected"
	exit 1
else
	echo "Detected Cameras:"
	echo $CAMERAS
fi
echo "Starting Webserver. Listening on http://localhost:$PORT"
echo ""
echo "VisiCut Users: Please set your Camera URL to http://localhost:$PORT/visicam.jpg"
echo ""
echo "Pleas press Ctrl+C to exit the server"
shutdown()
{
	echo "Caught SIGTERM"
	echo "removing fifo"
	rm fifo
	exit 0
}
rm -f fifo
mkfifo fifo
trap 'shutdown' SIGTERM SIGINT
while :
do 
	echo "Waiting for Connection..."
	if [ -x "$(which netcat)" ]
	then
		netcat -l -p $PORT < fifo | ./capture.sh > fifo
	elif [ -x "$(which nc)" ]
	then
		nc -l $PORT < fifo | ./capture.sh > fifo
	else
		echo "Neither netcat, nor nc seems to work"
		exit 1
	fi
	echo "Request completed."
done
