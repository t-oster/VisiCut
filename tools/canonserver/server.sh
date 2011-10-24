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
IP=$(ifconfig  | grep 'inet Adresse:'| grep -v '127.0.0.1' | cut -d: -f2 | awk '{ print $1}')
CAMERAS=$(gphoto2 --auto-detect|tail -n +3)
if [ "$CAMERAS" == "" ]
then
	echo "Error: No camera detected"
	exit 1
else
	echo "Detected Cameras:"
	echo $CAMERAS
fi
echo "Starting Webserver. Listening on http://$IP:$PORT"
shutdown()
{
	echo "Caught SIGTERM"
	echo "removing fifo"
	rm fifo
	exit 0
}
mkfifo fifo
trap 'shutdown' SIGTERM SIGINT
while :
do 
	nc -l $PORT < fifo | ./capture.sh > fifo
	echo "Request completed."
done
