#!/usr/bin/env python2 
'''
This extension strips everything which is not selected from
the current svg, saves it under "<currenttmpfilename>.svg" and
calls VisiCut on it.

Copyright (C) 2012 Thomas Oster, thomas.oster@rwth-aachen.de

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
'''

import sys
from lxml import etree
from subprocess import Popen
# Store the IDs of selected Elements
elements=[]
arguments=["--singleinstanceport", "6543"]
VISICUTBIN="visicut"

for arg in sys.argv[1:]:
	if arg[0] == "-":
		if len(arg) >= 5 and arg[0:5] == "--id=":
			elements +=[arg[5:]]
		elif len(arg) >= 13 and arg[0:13] == "--visicutbin=":
			VISICUTBIN=arg[13:]
		else:
			arguments += [arg]
	else:
		filename = arg

def removeAllButThem(element, elements):
	if element.get('id') in elements:
		return True
	else:
		keepSubtree = False
		for e in element:
			if not removeAllButThem(e, elements):
				element.remove(e)
			else:
				keepSubtree = True
		return keepSubtree

tree = etree.parse(filename)
if len(elements) > 0:
	removeAllButThem(tree.getroot(), elements)
tree.write(filename+".svg")
import daemonize
daemonize.createDaemon()
Popen([VISICUTBIN]+arguments+[filename+".svg"])
