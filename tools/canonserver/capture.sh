#!/bin/bash
gphoto2 --capture-image-and-download >/dev/null 2>&1
	echo -ne "HTTP/1.0 200 OK\r\n"
	echo -ne "Content-Type: image/jpeg\r\n\r\n"
	cat capt0000.jpg
	rm capt0000.jpg
