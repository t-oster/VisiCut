#!/usr/bin/env python2 
# -*- coding: utf-8 -*-
'''
This extension strips everything which is not selected from
the current svg, saves it under "<currenttmpfilename>.svg" and
calls VisiCut on it.

Copyright (C) 2012 Thomas Oster, thomas.oster@rwth-aachen.de
Copyright (C) 2014 Max Gaukler, development@maxgaukler.de

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
import os
import re
import subprocess
from subprocess import Popen
import traceback

SINGLEINSTANCEPORT=6543

# if on linux, add display variable to singleinstanceport
if (sys.platform == "linux"):
    d=os.environ.get("DISPLAY")
    if (d != None):
        d=d.split(':')[1].split('.')[0]
        SINGLEINSTANCEPORT+=int(d)

# if on Windows with Terminal Services, choose a singleinstanceport unique for each session ID.
# note: we cannot use SESSIONNAME here because it can change when disconnecting and reconnecting a session!
#       (think of SESSIONNAME like a display that can be connected to different session IDs)
if (sys.platform == "win32"):
    d=os.environ.get("SESSIONNAME")
    if d == None:
        # no Terminal Services installed
        pass
    else:
        # get ID by parsing output of `query session`:
        # the relevant line looks like:
        #  >rdp-tcp#0         Fablab                   12  Aktiv   rdpwd               
        # where "12" is the ID.
        query=Popen(["query", "session"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        query_output=query.communicate()[0]
        
        id=None
        for line in query_output.splitlines():
            if line.startswith(">"): # current session
                numbers=re.findall("[0-9]+", line)
                id=int(numbers[-1]) # ID is the last number on the line
                break
        assert id,  "could not parse TS session ID"
        assert 0 < id < 1000
        SINGLEINSTANCEPORT += 2 + id

# if Visicut or Inkscape cannot be found, change these lines here to VISICUTBIN="C:/Programs/Visicut" or wherever you installed it.
VISICUTBIN=None
INKSCAPEBIN=None

#wether to add (true) or replace (false) current visicut's content
IMPORT="true"
# Store the IDs of selected Elements
elements=[]

for arg in sys.argv[1:]:
 if arg[0] == "-":
  if len(arg) >= 5 and arg[0:5] == "--id=":
   elements +=[arg[5:]]
  elif len(arg) >= 13 and arg[0:13] == "--visicutbin=":
   VISICUTBIN=arg[13:]
  elif len(arg) >= 9 and arg[0:9] == "--import=":
   IMPORT=arg[9:]
  else:
   arguments += [arg]
 else:
  filename = arg

# find executable in the PATH
def which(program, extraPaths=[]):
    pathlist=os.environ["PATH"].split(os.pathsep)+[""]
    if "nt" in os.name: # Windows
        if not program.lower().endswith(".exe"):
            program += ".exe"
        pathlist.append(os.environ.get("ProgramFiles","C:\Program Files\\"))
        pathlist.append(os.environ.get("ProgramFiles(x86)","C:\Program Files (x86)\\"))
    def is_exe(fpath):
        return os.path.isfile(fpath) and (os.access(fpath, os.X_OK) or fpath.endswith(".exe"))
    for path in pathlist:
      exe_file = os.path.join(path, program)
      if is_exe(exe_file):
        return exe_file
    raise Exception("Cannot find executable {0} in PATH={1}.\n\n"
                    "Please make sure inkscape and visicut are in your PATH."
                    "Otherwise set VISICUTBIN and INKSCAPEBIN in "
                    "visicut_export.py in your inkscape extension directory."
                    .format(str(program), str(pathlist)))

#def removeAllButThem(element, elements):
# if element.get('id') in elements:
#  return True
# else:
#  keepSubtree = False
#  for e in element:
#   if not removeAllButThem(e, elements):
#    element.remove(e)
#   else:
#    keepSubtree = True
#  return keepSubtree
#
## Strip SVG to only contain selected elements
## LXML version
#def stripSVG_lxml(src,dest,elements):
# try:
#  from lxml import etree
#  tree = etree.parse(src)
#  if len(elements) > 0:
#   removeAllButThem(tree.getroot(), elements)
#  tree.write(dest)
# except:
#  sys.stderr.write("Python-LXML not installed. Can only send complete SVG\n")
#  import shutil
#  shutil.copyfile(src, dest)


# Strip SVG to only contain selected elements, convert objects to paths, unlink clones
# Inkscape version: takes care of special cases where the selected objects depend on non-selected ones.
# Examples are linked clones, flowtext limited to a shape and linked flowtext boxes (overflow into the next box).
#
# Inkscape is called with certain "verbs" (gui actions) to do the required cleanup
# The idea is similar to http://bazaar.launchpad.net/~nikitakit/inkscape/svg2sif/view/head:/share/extensions/synfig_prepare.py#L181 , but more primitive - there is no need for more complicated preprocessing here
def stripSVG_inkscape(src,dest,elements):
 # Selection commands: select items, invert selection, delete
 selection=[]
 for el in elements:
  selection += ["--select="+el]
 if len(elements)>0:
  #selection += ["--verb=FitCanvasToSelection"] # TODO add a user configuration option whether to keep the page size (and by this the position relative to the page)
  selection += ["--verb=EditInvertInAllLayers","--verb=EditDelete"]
 import shutil
 shutil.copyfile(src, dest)
 hidegui=["--without-gui"]
 # currently this only works with gui  because of a bug in inkscape: https://bugs.launchpad.net/inkscape/+bug/843260
 hidegui=[]
 
 command = [INKSCAPEBIN]+hidegui+[dest,"--verb=UnlockAllInAllLayers","--verb=UnhideAllInAllLayers"] + selection + ["--verb=EditSelectAllInAllLayers","--verb=EditUnlinkClone","--verb=ObjectToPath","--verb=FileSave","--verb=FileClose"]
 inkscape_output="(not yet run)"
 try:
  # run inkscape, buffer output
  inkscape=subprocess.Popen(command, stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  inkscape_output=inkscape.communicate()[0]
  errors = False
  # see if the output contains someting interesting (an error or an important warning)
  for line in inkscape_output.splitlines():
   # ignore empty/blank lines
   if (line.isspace() or line==""):
    continue
   # ignore GTK_IS_MISC warnings - they occur sometimes (at least in debian squeeze) even if everything works perfectly
   if "gtk_misc_set_alignment: assertion `GTK_IS_MISC (misc)' failed" in line:
    continue
   # something else happened - but since inkscape outputs much stuff, don't notify the user
   #errors = True
  #if errors:
  # sys.stderr.write("Error: cleaning the document with inkscape failed. Something might still be shown in visicut, but it could be incorrect.\nInkscape's output was:\n" + inkscape_output)
 except:
  sys.stderr.write("Error: cleaning the document with inkscape failed. Something might still be shown in visicut, but it could be incorrect. Exception information: \n" + str(sys.exc_info()[0]) + "Inkscape's output was:\n" + inkscape_output)
 

# find executable paths
if not VISICUTBIN:
    VISICUTBIN=which("visicut")
if not INKSCAPEBIN:
    INKSCAPEBIN=which("inkscape")

# remove all non-selected elements and convert inkscape-specific elements (text-to-path etc.)
stripSVG_inkscape(src=filename,dest=filename+".svg",elements=elements)

# Try to connect to running VisiCut instance
try:
  import socket
  s=socket.socket()
  s.connect(("localhost", SINGLEINSTANCEPORT))
  if (IMPORT == "true" or IMPORT == true or IMPORT == "\"true\""):
    s.send("@"+filename+".svg\n")
  else:
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

  creationflags=0
  close_fds=False
  if os.name=="nt":
      DETACHED_PROCESS = 8 # start as "daemon"
      creationflags=DETACHED_PROCESS
      close_fds=True
  else:
      try:
        import daemonize
        daemonize.createDaemon()
      except:
        sys.stderr.write("Could not daemonize. Sorry, but Inkscape was blocked until VisiCut is closed")
  cmd=[VISICUTBIN]+arguments+[filename+".svg"]
  Popen(cmd,creationflags=creationflags,close_fds=close_fds)
except:
  sys.stderr.write("Can not start VisiCut ("+str(sys.exc_info()[0])+"). Please start manually or change the VISICUTBIN variable in the Inkscape-Extension script\n")
