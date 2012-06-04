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
SINGLEINSTANCEPORT=6543
VISICUTBIN="visicut"
# Store the IDs of selected Elements
elements=[]

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

def which(program):
  import os
  def is_exe(fpath):
    return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

  fpath, fname = os.path.split(program)
  if fpath:
    if is_exe(program):
      return program
  else:
    for path in os.environ["PATH"].split(os.pathsep):
      exe_file = os.path.join(path, program)
      if is_exe(exe_file):
        return exe_file

    return None


# Strip SVG to only contain selected elements

try:
	from lxml import etree
	tree = etree.parse(filename)
	if len(elements) > 0:
		removeAllButThem(tree.getroot(), elements)
	tree.write(filename+".svg")
except:
	sys.stderr.write("Python-LXML not installed. Can only send complete SVG\n")
	import shutil
	shutil.copyfile(filename, filename+".svg")
# Try to connect to running VisiCut instance
try:
  import socket
  s=socket.socket()
  s.connect(("localhost", SINGLEINSTANCEPORT))
  s.send(filename+".svg\n")
  s.close()
  sys.exit(0)
except SystemExit, e:
    sys.exit(e)
except:
  pass
# Try to start own VisiCut instance
try:
  arguments=["--singleinstanceport", str(SINGLEINSTANCEPORT)]
  if which(VISICUTBIN) == None:
    sys.stderr.write("Error: Can't find VisiCut at '"+VISICUTBIN+"'. Please start VisiCut manually, add '"+VISICUTBIN+"' to the PATH Variable or change the VISICUTBIN variable in the Inkscape Extension.\n")
  else:
    try:
      from subprocess import Popen    
      import daemonize
      daemonize.createDaemon()
    except:
      sys.stderr.write("Could not daemonize. Sorry, but Inkscape was blocked until VisiCut is closed")
    Popen([VISICUTBIN]+arguments+[filename+".svg"])
except:
  sys.stderr.write("Can not start VisiCut ("+str(sys.exc_info()[0])+"). Please start manually or change the VISICUTBIN variable in the Inkscape-Extension script\n")