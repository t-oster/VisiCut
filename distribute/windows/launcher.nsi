; Java Launcher
;--------------
 
;You want to change the next four lines
Name "VisiCut"
Caption "a userfriendly tool for Lasercutting"
Icon "VisiCut.ico"
OutFile "VisiCut.exe"
 
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow
RequestExecutionLevel user
; uncomment next two lines to show debug output (DetailPrint messages). Also uncomment the "DEBUG" message later in this file to force the setup to wait before exiting.
; ShowInstDetails show
; SilentInstall normal
 
;You want to change the next two lines too
!define CLASSPATH ".;lib"
!define JARFILE "Visicut.jar"
!define PRGARGS ""

!include "FileFunc.nsh"
!include LogicLib.nsh
!insertmacro GetParameters

Var /GLOBAL maximumJavaRAM
Var /GLOBAL s_SystemMemoryMB
Var /GLOBAL s_SystemMemoryGB
Var /GLOBAL ARGV
Var /GLOBAL VISICUTCOMMAND

Section ""
  ${GetParameters} $ARGV
  DetailPrint "argv = $ARGV"

  StrCmp $ARGV "--debug" 0 +3
  ; if commandline is "--debug": show JRE with console
    Call GetJREConsole
    Goto +2
  ; else:
    ; show JRE without console
    Call GetJRE
  ; end
  Pop $R0

  Call s_QuerySystemMemory
  DetailPrint "system RAM $s_SystemMemoryMB MB"

  ; maximumJavaRAM= systemRAM / 2
  IntOp $maximumJavaRAM $s_SystemMemoryMB / 2

  ; limit maximumJavaRAM to 2GB (same as Linux launcher, should be enough for everything)
  ${If} $maximumJavaRAM > 2048
    StrCpy $maximumJavaRAM "2048"
  ${EndIf}
  DetailPrint "choosing java RAM limit = $maximumJavaRAM MB"


  Call startVisicut

  IfErrors 0 end
  ClearErrors
  ; if errors: retry with lower RAM (workaround if the JVM is 32bit or too stupid to allocate some memory)
    DetailPrint "failed (maybe Java couldn't start because the RAM size was set too high). retrying with smaller RAM."
    StrCpy $maximumJavaRAM "1024"
    Call startVisicut
  IfErrors 0 end
    MessageBox MB_OK|MB_ICONSTOP "Error running Java with command $\n $VISICUTCOMMAND"
  end:
  ; if no errors: just exit.

  ; uncomment to wait before end
  ; MessageBox MB_OK "DEBUG"
SectionEnd
 
Function startVisicut
  Var /GLOBAL VMARGS
  StrCpy $2 $maximumJavaRAM
  DetailPrint "java ram: $2 MB"
  StrCpy $VMARGS "-Xms256m -Xmx$2m"
  StrCpy $VISICUTCOMMAND '"$R0" $VMARGS -classpath "${CLASSPATH}" -jar ${JARFILE} ${PRGARGS} $ARGV'
  SetOutPath $EXEDIR
  ExecWait $VISICUTCOMMAND
FunctionEnd
 
Function GetJRE
  # save C:\path\to\javaw.exe into $R0
  push "javaw.exe"
  call GetJREPath
FunctionEnd

Function GetJREConsole
  # save C:\path\to\java.exe (with console output) into $R0
  push "java.exe"
  call GetJREPath
FunctionEnd


# Ask the system for how much memory it has available. When finished,
# the variables $s_SystemMemoryMB and $s_SystemMemoryGB will be initialized
# LICENSE for this function:
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
Function s_QuerySystemMemory
    # We will need to use variables $0 through $9, so save away any existing values.
    Push $0
    Push $1
    Push $2
    Push $3
    Push $4
    Push $5
    Push $6
    Push $7
    Push $8
    Push $9

    # Call GlobalMemoryStatusEx using the System plugin.
    # For more info: http://nsis.sourceforge.net/Docs/System/System.html
    System::Alloc 64
    Pop $0
    # GlobalMemoryStatusEx requires the first parameter to be the size
    # of the whole structure. Everything else is zeroed out.
    System::Call "*$0(i 64, i 0, l 0, l 0, l 0, l 0, l 0, l 0, l 0)"
    System::Call "Kernel32::GlobalMemoryStatusEx(i r0)" # Read status into $0
    System::Call "*$0(i.r1, i.r2, l.r3, l.r4, l.r5, l.r6, l.r7, l.r8, l.r9)"
    # Machine's physical memory value (in bytes) is the third parameter. See MSDN:
    # http://msdn.microsoft.com/en-us/library/windows/desktop/aa366770(v=vs.85).aspx
    System::Int64Op $3 / 1048576 # 1024 * 1024
    Pop $s_SystemMemoryMB
    System::Int64Op $s_SystemMemoryMB / 1024
    Pop $s_SystemMemoryGB
    System::Free $0

    # Restore old values
    Pop $9
    Pop $8
    Pop $7
    Pop $6
    Pop $5
    Pop $4
    Pop $3
    Pop $2
    Pop $1
    Pop $0

FunctionEnd

Function GetJREPath
;
;  Find JRE path (parameter 0: java.exe or javaw.exe)
;  1 - in .\jre directory (JRE Installed with application)
;  2 - in JAVA_HOME environment variable
;  3 - in the registry
;  4 - assume javaw.exe in current dir or PATH
  Pop $0
  Push $R0
  Push $R1
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\$0"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""

  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\$0"
  IfErrors 0 JreFound

  ClearErrors
  SetRegView 64
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\$0"

  IfErrors 0 JreFound

  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\$0"

  IfErrors 0 JreFound

  ClearErrors
  SetRegView 32
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\$0"

  IfErrors 0 JreFound

  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\$0"

  IfErrors 0 JreFound
  StrCpy $R0 "$0"
 
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd
