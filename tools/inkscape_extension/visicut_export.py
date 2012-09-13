#!/usr/bin/env python2 
# -*- coding: utf-8 -*-
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
import os
SINGLEINSTANCEPORT=6543
VISICUTBIN="visicut"
INKSCAPEBIN="inkscape"
#If on Windows, add .exe extension
if "windows" in os.name:
  if not VISICUTBIN.lower().endswith(".exe"):
    VISICUTBIN += ".exe"
  if not INKSCAPEBIN.lower().endswith(".exe"):
    INKSCAPEBIN += ".exe"
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
# LXML version
def stripSVG_lxml(src,dest,elements):
 try:
  from lxml import etree
  tree = etree.parse(src)
  if len(elements) > 0:
   removeAllButThem(tree.getroot(), elements)
  tree.write(dest)
 except:
  sys.stderr.write("Python-LXML not installed. Can only send complete SVG\n")
  import shutil
  shutil.copyfile(src, dest)

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
 
 import subprocess
 import os
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
   errors = False # True
  if errors:
   sys.stderr.write("Error: cleaning the document with inkscape failed. Something might still be shown in visicut, but it could be incorrect.\nInkscape's output was:\n" + inkscape_output)
 except:
  sys.stderr.write("Error: cleaning the document with inkscape failed. Something might still be shown in visicut, but it could be incorrect. Exception information: \n" + str(sys.exc_info()[0]) + "Inkscape's output was:\n" + inkscape_output)
 
 # visicut accepts inkscape-svg - no need to export as plain svg
 # call([INKSCAPEBIN,"--without-gui",dest,"--export-plain-svg="+dest])

# Try to use inkscape to strip unused elements, but if not in PATH, try to use
# lxml
if which(INKSCAPEBIN) == None:
  stripSVG_lxml(src=filename,dest=filename+".svg",elements=elements)
else:
  stripSVG_inkscape(src=filename,dest=filename+".svg",elements=elements)

# SVG -> PDF -> SVG (unused idea, pixelates some items, sometimes crashes inkscape)
# TODO make this user configurable
#if which(INKSCAPEBIN) != None:
#  call([INKSCAPEBIN,"-z",filename+".svg","-T","--export-pdf="+filename+".clean.pdf"])
#  call([INKSCAPEBIN,"-z",filename+".clean.pdf","--export-plain-svg="+filename+".clean.svg"])
#  call([INKSCAPEBIN,"-z",filename+".clean.svg","--verb=EditSelectAllInAllLayers","--verb=SelectionUnGroup","--verb=FileSave","--verb=FileClose"])

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
