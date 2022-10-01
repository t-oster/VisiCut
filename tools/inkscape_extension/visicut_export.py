#!/usr/bin/env python3
# -*- coding: utf-8 -*-
'''
This extension strips everything which is not selected from
the current svg, saves it and
calls VisiCut on it.

Copyright (C) 2012 Thomas Oster, thomas.oster@rwth-aachen.de
Copyright (C) 2014-2022 Max Gaukler, development@maxgaukler.de

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
import tempfile
import unicodedata
import codecs
import random
import string
import socket

try:
    from os import fsencode
except ImportError:
    fsencode = lambda x: x.encode(sys.getfilesystemencoding())

DEVNULL = open(os.devnull, 'w')

def get_single_instance_port():
    """
    get the single instance port used by VisiCut.
    CAUTION: This code must behave exactly the same as ApplicationInstanceManager.getSingleInstancePort() in Visicut.
    """
    port = 6543
    if (sys.platform == "linux"):
        d = os.environ.get("DISPLAY")
        if (d != None):
            d = d.split(':')[1].split('.')[0]
            port += int(d)

    if (sys.platform == "win32"):
        d = os.environ.get("SESSIONNAME")
        if d == None:
            # no Terminal Services installed
            pass
        else:
            # get session ID
            try:
                CREATE_NO_WINDOW = 0x08000000
                id = subprocess.check_output("powershell -Command (get-process -pid $pid).sessionid",creationflags=CREATE_NO_WINDOW)
                id = int(id.strip())
                port += 2 + id
            except:
                sys.stderr.write("Warning: Cannot determine session ID. please report this.\n")
    return port

# if Visicut or Inkscape cannot be found, change these lines here to VISICUTDIR="C:/Programs/Visicut" or wherever you installed it.
# please use forward slashes (/), not backslashes (\).
#
# example:
# VISICUTDIR="C:/Program Files (x86)/VisiCut/"
# INKSCAPEDIR="C:/Program Files (x86)/Inkscape/"
VISICUTDIR = ""
INKSCAPEDIR = ""

# whether to add (true) or replace (false) current visicut's content
IMPORT = True
# Store the IDs of selected Elements
elements = []

arguments=[]

for arg in sys.argv[1:]:
    if arg[0] == "-":
        if len(arg) >= 5 and arg[0:5] == "--id=":
            elements += [arg[5:]]
        elif len(arg) >= 13 and arg[0:13] == "--visicutbin=":
            # unused
            pass
            # VISICUTBIN=arg[13:]
        elif len(arg) >= 9 and arg[0:9] == "--import=":
            IMPORT = "true" in arg[9:]
        else:
            arguments += [arg]
    else:
        filename = arg

if IMPORT:
    # do not replace old file
    arguments += ["--add"]

# find executable in the PATH
def which(program, extraPaths=[]):
    pathlist = extraPaths + os.environ["PATH"].split(os.pathsep) + [""]
    if "nt" in os.name:  # Windows
        if not program.lower().endswith(".exe"):
            program += ".exe"
        programfiles = os.environ.get("ProgramFiles", "C:\\Program Files\\")
        programfiles86 = os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)\\")
        # also look in %ProgramFiles%/yourProgram/yourProgram.exe
        pathlist += [programfiles + "\\" + program + "\\", programfiles86 + "\\" + program + "\\"]

    def is_exe(fpath):
        return os.path.isfile(fpath) and (os.access(fpath, os.X_OK) or fpath.endswith(".exe"))
    for path in pathlist:
        exe_file = os.path.join(path, program)
        if is_exe(exe_file):
            return exe_file
    raise Exception("Cannot find executable {0} in PATH={1}.\n\n"
                    "Please report this bug on https://github.com/t-oster/VisiCut/issues\n\n"
                    "For a quick fix: Set VISICUTDIR and INKSCAPEDIR in "
                    "{2}"
                    .format(repr(program), repr(pathlist), os.path.realpath(__file__)))


def inkscape_version():
    """Return Inkscape version number as float, e.g. version "0.92.4" --> return: float 0.92"""
    version = subprocess.check_output([INKSCAPEBIN, "--version"],  stderr=DEVNULL).decode('ASCII', 'ignore')
    assert version.startswith("Inkscape ")
    match = re.match("Inkscape ([0-9]+\.[0-9]+).*", version)
    assert match is not None
    version_float = float(match.group(1))
    return version_float
    


# Strip SVG to only contain selected elements, convert objects to paths, unlink clones
# Inkscape version: takes care of special cases where the selected objects depend on non-selected ones.
# Examples are linked clones, flowtext limited to a shape and linked flowtext boxes (overflow into the next box).
#
# Inkscape is called with certain "verbs" (gui actions) to do the required cleanup
# The idea is similar to http://bazaar.launchpad.net/~nikitakit/inkscape/svg2sif/view/head:/share/extensions/synfig_prepare.py#L181 , but more primitive - there is no need for more complicated preprocessing here
def stripSVG_inkscape(src, dest, elements):
    version = inkscape_version()
    
    # create temporary file for opening with inkscape.
    # delete this file later so that it will disappear from the "recently opened" list.
    tmpfile = tempfile.NamedTemporaryFile(delete=False, prefix='temp-visicut-', suffix='.svg')
    tmpfile.close()
    tmpfile = tmpfile.name
    import shutil
    shutil.copyfile(src, tmpfile)


    if version < 1:
        # inkscape 0.92 long-term-support release. Will be in Linux distributions until 2025 or so
        # Selection commands: select items, invert selection, delete
        selection = []
        for el in elements:
            selection += ["--select=" + el]

        if len(elements) > 0:
            # selection += ["--verb=FitCanvasToSelection"] # TODO add a user configuration option whether to keep the page size (and by this the position relative to the page)
            selection += ["--verb=EditInvertInAllLayers", "--verb=EditDelete"]


        hidegui = ["--without-gui"]

        # currently this only works with gui because of a bug in inkscape: https://bugs.launchpad.net/inkscape/+bug/843260
        hidegui = []

        command = [INKSCAPEBIN] + hidegui + [tmpfile, "--verb=UnlockAllInAllLayers", "--verb=UnhideAllInAllLayers"] + selection + ["--verb=EditSelectAllInAllLayers", "--verb=EditUnlinkClone", "--verb=ObjectToPath", "--verb=FileSave", "--verb=FileQuit"]
    elif version < 1.2:
        # Inkscape 1.0 (released ca 2020) or 1.1
        # inkscape --select=... --verbs=...
        # (see inkscape --help, inkscape --verb-list)
        command = [INKSCAPEBIN, tmpfile, "--batch-process"]
        verbs = ["ObjectToPath", "UnlockAllInAllLayers"]
        if elements: # something is selected
            # --select=object1,object2,object3,...
            command += ["--select=" + ",".join(elements)]
        else:
            verbs += ["EditSelectAllInAllLayers"]
        verbs += ["UnhideAllInAllLayers", "EditInvertInAllLayers", "EditDelete", "EditSelectAllInAllLayers", "EditUnlinkClone", "ObjectToPath", "FileSave"]
        # --verb=action1;action2;...
        command += ["--verb=" + ";".join(verbs)]
        
        
        DEBUG = False
        if DEBUG:
            # Inkscape sometimes silently ignores wrong verbs, so we need to double-check that everything's right
            for verb in verbs:
                verb_list = [line.split(":")[0] for line in subprocess.check_output([INKSCAPEBIN, "--verb-list"], stderr=DEVNULL).split("\n")]
                if verb not in verb_list:
                    sys.stderr.write("Inkscape does not have the verb '{}'. Please report this as a VisiCut bug.".format(verb))
    else:
        # Inkscape 1.2 (released 2022)
        # inkscape --export-overwrite --actions=action1;action2...
        # (see inkscape --help, inkscape --action-list)
        # (for debugging, you can look at the intermediate state by running inkscape --with-gui --actions=... my_filename.svg)
        # Note that it is (almost?) impossible to find a sequence that works in all cases.
        # Cases to consider:
        # - selecting whole groups
        # - selecting objects within a group
        # - selecting across groups/layers (e.g., enter group, select something, then Shift-click to select things from other layers)
        # Difficulties with Inkscape:
        # - "invert selection" does not behave as expected in all these cases,
        #   for example if a group is selected then inverting can select the elements within.
        # - Inkscape has a wonderful --export-id commandline switch, but it only works correctly with one ID

        # Solution:
        actions = ["unlock-all"]
        if elements:
            # something was selected when calling the plugin.
            # -> Recreate that selection
            # - select objects
            actions += ["select-by-id:" + ",".join(elements)]
        else:
            # - select all
            actions += ["select-all:all"]
        # - convert to path
        actions +=  ["clone-unlink", "object-to-path"]
        if elements:
            # ensure that only the selection is exported:
            # - create group of selection
            actions += ["selection-group"]
            # - set group ID to a known value. Use a pseudo-random value to avoid collisions
            target_group_id = "TARGET-GROUP-" + "".join(random.sample(string.ascii_lowercase, 20))
            actions += ["object-set-attribute:id," + target_group_id]
            # - set export options: use only the target group ID, nothing else
            actions += ["export-id-only:true", "export-id:" + target_group_id]
        # - do export (keep position on page)
        actions += ["export-area-page"]

        command = [INKSCAPEBIN, tmpfile, "--export-overwrite", "--actions=" + ";".join(actions)]
        
    try:
        #sys.stderr.write(" ".join(command))
        # run inkscape, buffer output
        subprocess.check_output(command, stderr=subprocess.STDOUT, universal_newlines=True)
    except subprocess.CalledProcessError as e:
        sys.stderr.write("Error: cleaning the document with inkscape failed. Something might still be shown in visicut, but it could be incorrect.\nInkscape's output was:\n" + e.output)

    # move output to the intended destination filename
    os.rename(tmpfile, dest)


"""
Get document name (original filename) from Inkscape SVG

Inkscape saves the file to a random temporary name.
However, the original filename is stored inside the SVG.
"""


def get_original_filename(filename):
    docname = None

    # parse SVG for docname tag
    with codecs.open(filename, "r", encoding='utf-8') as f:
        for line in f:
            if 'sodipodi:docname="' in line:
                matches = re.search('sodipodi:docname="(.*).svg"', line)
                if not matches:
                    break
                try:
                    docname = matches.group(1)
                except IndexError:
                    # something is wrong with this line
                    break
                # unescape XML string

                docname = docname.replace('&lt;', '<')
                docname = docname.replace('&gt;', '>')
                docname = docname.replace('&quot;', '"')
                docname = docname.replace('&amp;', '&')

                # normalize accented characters (äöü -> aou)
                docname = unicodedata.normalize('NFKD', docname).encode('ASCII', 'ignore').decode('ASCII')
                break

    if not docname:
        # failed to read filename from SVG, return original one
        docname = os.path.basename(filename)
        if str.endswith(docname, ".svg"):
            docname = docname[:-4]
        if str.startswith(docname, "ink_ext_"):
            # inkscape temporary file, the name is useless
            docname = "new"

    # sanitize the filename:
    # filter out special characters (@/\& ...)
    docname = "".join(x for x in docname if (x.isalnum() or x in "._- "))
    docname = docname + ".svg"
    return docname

# find executable paths
import platform
if platform.system() == 'Darwin':
    VISICUTBIN = which("VisiCut.MacOS", [VISICUTDIR])
elif "nt" in os.name:  # Windows
    VISICUTBIN = which("VisiCut.exe", [VISICUTDIR])
else:
    VISICUTBIN = which("VisiCut.Linux", [VISICUTDIR, "/usr/share/visicut"])
INKSCAPEBIN = which("inkscape", [INKSCAPEDIR])

tmpdir = tempfile.mkdtemp(prefix='temp-visicut-')
dest_filename = os.path.join(tmpdir, get_original_filename(filename))

# remove all non-selected elements and convert inkscape-specific elements (text-to-path etc.)
stripSVG_inkscape(src=filename, dest=dest_filename, elements=elements)

# Try to connect to running VisiCut instance
# Note: this step may be omitted, as VisiCut will do the same.
# However, doing it here saves 2-3 seconds of waiting time on Windows.
s = socket.socket()
try:
    s.connect(("localhost", get_single_instance_port()))
except socket.error:
    pass
else:
    if IMPORT:
        s.send(b"@" + fsencode(dest_filename) + b"\n")
    else:
        s.send(fsencode(dest_filename) + b"\n")
    sys.exit(0)
finally:
    s.close()

# Try to start own VisiCut instance
try:
    creationflags = 0
    close_fds = False
    if os.name == "nt":
        DETACHED_PROCESS = 8  # start as "daemon"
        creationflags = DETACHED_PROCESS
        close_fds = True
    else:
        try:
            import daemonize
            daemonize.createDaemon()
        except:
            sys.stderr.write("Could not daemonize. Sorry, but Inkscape was blocked until VisiCut is closed")
    cmd = [VISICUTBIN] + arguments + [dest_filename]
    Popen(cmd, creationflags=creationflags, close_fds=close_fds)
except:
    sys.stderr.write("Can not start VisiCut (" + str(sys.exc_info()[0]) + "). Please start manually or change the VISICUTDIR variable in the Inkscape-Extension script\n")

# TODO (complicated, probably WONTFIX): cleanup temporary directories -- this is really difficult because we need to make sure that visicut no longer needs the file, even for reloading!
