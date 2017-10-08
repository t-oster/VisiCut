; Taken from http://nsis.sourceforge.net/Simple_installer_with_JRE_check by weebib
; Use it as you desire.
 
; Credit given to so many people of the NSIS forum.
 
!define AppName "VisiCut"
!define AppVersion "VISICUTVERSION"
!define ShortName "VisiCut"
!define JRE_VERSION "1.8"
!define Vendor "RWTH Aachen University"
 
!include "MUI.nsh"
!include "Sections.nsh"
!include "EnvVarUpdate.nsh"
!include "FileAssociation.nsh"
 
Var InstallJRE
Var JREPath
 
 
;--------------------------------
;Configuration
 
  ;General
  Name "${AppName}"
  OutFile "setup.exe"
 
  ;Folder selection page
  InstallDir "$PROGRAMFILES\${SHORTNAME}"
 
  ;Get install folder from registry if available
  InstallDirRegKey HKLM "SOFTWARE\${Vendor}\${ShortName}" ""
 
; Installation types
;InstType "full"	; Uncomment if you want Installation types
 
;--------------------------------
;Pages
 
  ; License page
  ; !insertmacro MUI_PAGE_LICENSE "${NSISDIR}\Contrib\Modern UI\License.txt"
  ; This page checks for JRE. It displays a dialog based on JRE.ini if it needs to install JRE
  ; Otherwise you won't see it.
  Page custom CheckInstalledJRE
 
  ; Define headers for the 'Java installation successfully' page
  !define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Java installation complete"
  !define MUI_PAGE_HEADER_TEXT "Installing Java runtime"
  !define MUI_PAGE_HEADER_SUBTEXT "Please wait while we install the Java runtime"
  !define MUI_INSTFILESPAGE_FINISHHEADER_SUBTEXT "Java runtime installed successfully."
  !insertmacro MUI_PAGE_INSTFILES
  !define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Installation complete"
  !define MUI_PAGE_HEADER_TEXT "Installing"
  !define MUI_PAGE_HEADER_SUBTEXT "Please wait while ${AppName} is being installed."
; Uncomment the next line if you want optional components to be selectable
;  !insertmacro MUI_PAGE_COMPONENTS
  !define MUI_PAGE_CUSTOMFUNCTION_PRE myPreInstfiles
  !define MUI_PAGE_CUSTOMFUNCTION_LEAVE RestoreSections
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
 
;--------------------------------
;Modern UI Configuration
 
  !define MUI_ABORTWARNING
 
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"
 
;--------------------------------
;Language Strings
 
  ;Description
  LangString DESC_SecAppFiles ${LANG_ENGLISH} "Application files copy"
 
  ;Header
  LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "Java Runtime Environment"
  LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "Installation"
  LangString TEXT_PRODVER_TITLE ${LANG_ENGLISH} "Installed version of ${AppName}"
  LangString TEXT_PRODVER_SUBTITLE ${LANG_ENGLISH} "Installation cancelled"
 
;--------------------------------
;Reserve Files
 
  ;Only useful for BZIP2 compression
 
 
  ReserveFile "jre.ini"
  !insertmacro MUI_RESERVEFILE_INSTALLOPTIONS
 
;--------------------------------
;Installer Sections
 
Section -installjre jre
  Push $0
  Push $1
 
;  MessageBox MB_OK "Inside JRE Section"
  Strcmp $InstallJRE "yes" InstallJRE JREPathStorage
  DetailPrint "Starting the JRE installation"
InstallJRE:
  File /oname=$TEMP\jre_setup.exe j2re-setup.exe
;  MessageBox MB_OK "Installing JRE"
  DetailPrint "Launching JRE setup"
  ; commandline switches see https://www.java.com/de/download/help/silent_install.xml
  ; we leave out the silent-install switch /s for now to find problems easier
  ; there could be problems with closing browser windows etc.
  ; SPONSORS=0 disables the silly "Ask toolbar" crapware bundled with Java
  ExecWait '"$TEMP\jre_setup.exe" SPONSORS=0' $0
  DetailPrint "Setup finished"
  Delete "$TEMP\jre_setup.exe"
  StrCmp $0 "0" InstallVerif 0
  Push "The JRE setup has been abnormally interrupted."
  Goto ExitInstallJRE
 
InstallVerif:
  DetailPrint "Checking the JRE Setup's outcome"
;  MessageBox MB_OK "Checking JRE outcome"
  Push "${JRE_VERSION}"
  Call DetectJRE  
  Pop $0	  ; DetectJRE's return value
  Push "JRE cannot be found after running setup. Please report this error. (bundled setup may be outdated)"
  StrCmp $0 "0" ExitInstallJRE 0
  StrCmp $0 "-1" ExitInstallJRE 0
  Pop $9 ; if the code didn't jump to ExitInstallJRE, remove the message from the stack. $9 is never used
  Goto JavaExeVerif
  Push "The JRE setup failed"; never executed??
  Goto ExitInstallJRE ; never executed??
 
JavaExeVerif:
  IfFileExists $0 JREPathStorage 0
  Push "The following file : $0, cannot be found."
  Goto ExitInstallJRE
 
JREPathStorage:
;  MessageBox MB_OK "Path Storage"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $1
  StrCpy $JREPath $0
  Goto End
 
ExitInstallJRE:
  Pop $1
  MessageBox MB_OK "The setup is about to be interrupted for the following reason : $1"
  MessageBox MB_OK "Please try installing Java 8 (JRE) from java.com yourself. Make sure to uncheck 'Ask Toolbar' and any other offers in the setup."
  Pop $1 	; Restore $1
  Pop $0 	; Restore $0
  Abort
End:
  Pop $1	; Restore $1
  Pop $0	; Restore $0
SectionEnd
 
Section "Installation of ${AppName}" SecAppFiles
  SectionIn 1 RO	; Full install, cannot be unselected
			; If you add more sections be sure to add them here as well
  SetOutPath $INSTDIR
File /r "stream\"
; If you need the path to JRE, you can either get it here for from $JREPath
;  !insertmacro MUI_INSTALLOPTIONS_READ $0 "jre.ini" "UserDefinedSection" "JREPath"
;  MessageBox MB_OK "JRE Read: $0"
  ;Store install folder
  WriteRegStr HKLM "SOFTWARE\${Vendor}\${ShortName}" "" $INSTDIR
 
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "DisplayName" "${AppName}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoModify" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoRepair" "1"

  ; instert visicut to path
  Push $0
  Push $1
  ; string length check taken from CMake/Modules/NSIS.template.in
  ; if the path is too long for a NSIS variable NSIS will return a 0
  ; length string.  If we find that, then warn and skip any path
  ; modification as it will trash the existing path.
  ReadEnvStr $0 PATH
  StrLen $1 $0
  IntCmp $1 0 CheckPathLength_ShowPathWarning CheckPathLength_Done CheckPathLength_Done
    CheckPathLength_ShowPathWarning:
    Messagebox MB_OK|MB_ICONEXCLAMATION "Warning: The PATH envirnoment variable is too long, the installer is unable to modify it! If you install VisiCut to the default directory, everything will still work."
    Goto AddToPath_done
  CheckPathLength_Done:
  ; update path if it is safe:
  ${EnvVarUpdate} $0 "PATH" "A" "HKLM" "$INSTDIR" 
  AddToPath_done:
  Pop $1
  Pop $0
  
  ; register file extensions
  ${registerExtension} "$INSTDIR\VisiCut.exe" ".ls" "VisiCut LaserScript File"
  ${registerExtension} "$INSTDIR\VisiCut.exe" ".plf" "VisiCut Portable Laser File"
  ${registerExtension} "$INSTDIR\VisiCut.exe" ".svg" "SVG File"
  ${registerExtension} "$INSTDIR\VisiCut.exe" ".dxf" "DXF File"
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
 
SectionEnd
 
 
Section "Start menu shortcuts" SecCreateShortcut
  SectionIn 1	; Can be unselected
  CreateDirectory "$SMPROGRAMS\${AppName}"
  CreateShortCut "$SMPROGRAMS\${AppName}\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\${AppName}\${AppName}.lnk" "$INSTDIR\${AppName}.exe" "" "$INSTDIR\${AppName}.exe" 0
; Etc
SectionEnd
 
;--------------------------------
;Descriptions
 
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAppFiles} $(DESC_SecAppFiles)
!insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Installer Functions
 
Function .onInit
 
  ;Extract InstallOptions INI Files
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "jre.ini"
  Call SetupSections
 
FunctionEnd
 
Function myPreInstfiles
 
  Call RestoreSections
  SetAutoClose true
 
FunctionEnd
 
Function CheckInstalledJRE
;  MessageBox MB_OK "Checking Installed JRE Version"
  Push "${JRE_VERSION}"
  Call DetectJRE
;  Messagebox MB_OK "Done checking JRE version"
  Exch $0	; Get return value from stack
  StrCmp $0 "0" NoFound
  StrCmp $0 "-1" FoundOld
  Goto JREAlreadyInstalled
 
FoundOld:
;  MessageBox MB_OK "Old JRE found"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "${AppName} requires a more recent version of the Java Runtime Environment than the one found on your computer. The installation of JRE ${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
NoFound:
;  MessageBox MB_OK "JRE not found"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "No Java Runtime Environment could be found. JRE v${JRE_VERSION} will be installed."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
MustInstallJRE:
  Exch $0	; $0 now has the installoptions page return value
  ; Do something with return value here
  Pop $0	; Restore $0
  StrCpy $InstallJRE "yes"
  Return
 
JREAlreadyInstalled:
;  MessageBox MB_OK "No download: ${TEMP2}"
;  MessageBox MB_OK "JRE already installed"
  StrCpy $InstallJRE "no"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $JREPATH
  Pop $0		; Restore $0
  Return
 
FunctionEnd
 
; Returns: 0 - JRE not found. -1 - JRE found but too old. Otherwise - Path to JAVA EXE
 
; DetectJRE. Version requested is on the stack.
; Returns (on stack)	"0" on failure (java too old or not installed), otherwise path to java interpreter
; Stack value will be overwritten!
 
Function DetectJRE
  Exch $0	; Get version requested  
		; Now the previous value of $0 is on the stack, and the asked for version of JDK is in $0
  Push $1	; $1 = Java version string (ie 1.5.0)
  Push $2	; $2 = Javahome
  Push $3	; $3 and $4 are used for checking the major/minor version of java
  Push $4

  SetRegView 64
  Call :DetectJava
  StrCmp $2 "" 0 GetJRE
  SetRegView 32
  Call :DetectJava
  StrCmp $2 "" 0 GetJRE
  Goto NoFound

DetectJava:
;  MessageBox MB_OK "Detecting JRE"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
;  MessageBox MB_OK "Read : $1"
  StrCmp $1 "" DetectJDK
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
;  MessageBox MB_OK "Read 3: $2"
  StrCmp $2 "" DetectJDK
  Return
 
DetectJDK:
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
;  MessageBox MB_OK "Detect Read : $1"
  StrCmp $1 "" EndDetectJava
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$1" "JavaHome"
;  MessageBox MB_OK "Detect Read 3: $2"
  StrCmp $2 "" EndDetectJava

EndDetectJava:
  Return
  
GetJRE:
; $0 = version requested. $1 = version found. $2 = javaHome
;  MessageBox MB_OK "Getting JRE"
  IfFileExists "$2\bin\java.exe" 0 NoFound
  StrCpy $3 $0 1			; Get major version. Example: $1 = 1.5.0, now $3 = 1
  StrCpy $4 $1 1			; $3 = major version requested, $4 = major version found
;  MessageBox MB_OK "Want $3 , found $4"
  IntCmp $4 $3 0 FoundOld FoundNew
  StrCpy $3 $0 1 2
  StrCpy $4 $1 1 2			; Same as above. $3 is minor version requested, $4 is minor version installed
;  MessageBox MB_OK "Want $3 , found $4" 
  IntCmp $4 $3 FoundNew FoundOld FoundNew
 
NoFound:
;  MessageBox MB_OK "JRE not found"
  DetailPrint "JRE not found"
  Push "0"
  Goto DetectJREEnd
 
FoundOld:
  MessageBox MB_OK "JRE too old: $3 is older than $4"
;  Push ${TEMP2}
  Push "-1"
  Goto DetectJREEnd  
FoundNew:
;  MessageBox MB_OK "JRE is new: $3 is newer than $4"
 
  Push "$2\bin\java.exe"
;  Push "OK"
;  Return
   Goto DetectJREEnd
DetectJREEnd:
	; Top of stack is return value, then r4,r3,r2,r1
	Exch	; => r4,rv,r3,r2,r1,r0
	Pop $4	; => rv,r3,r2,r1r,r0
	Exch	; => r3,rv,r2,r1,r0
	Pop $3	; => rv,r2,r1,r0
	Exch 	; => r2,rv,r1,r0
	Pop $2	; => rv,r1,r0
	Exch	; => r1,rv,r0
	Pop $1	; => rv,r0
	Exch	; => r0,rv
	Pop $0	; => rv 
FunctionEnd

Function RestoreSections
  !insertmacro UnselectSection ${jre}
  !insertmacro SelectSection ${SecAppFiles}
  !insertmacro SelectSection ${SecCreateShortcut}
 
FunctionEnd
 
Function SetupSections
  !insertmacro SelectSection ${jre}
  !insertmacro UnselectSection ${SecAppFiles}
  !insertmacro UnselectSection ${SecCreateShortcut}
FunctionEnd
 
;--------------------------------
;Uninstaller Section
 
Section "Uninstall"

  ; remove visicut from path
  Push $0
  Push $1
  ; string length check taken from CMake/Modules/NSIS.template.in
  ; if the path is too long for a NSIS variable NSIS will return a 0
  ; length string.  If we find that, then warn and skip any path
  ; modification as it will trash the existing path.
  ReadEnvStr $0 PATH
  StrLen $1 $0
  IntCmp $1 0 CheckPathLength_ShowPathWarning CheckPathLength_Done CheckPathLength_Done
    CheckPathLength_ShowPathWarning:
    Messagebox MB_OK|MB_ICONEXCLAMATION "Warning! PATH too long, installer unable to modify PATH!"
    Goto AddToPath_done
  CheckPathLength_Done:
  ; update path if it is safe:
  ${un.EnvVarUpdate} $0 "PATH" "R" "HKLM" "$INSTDIR"
  AddToPath_done:
  Pop $1
  Pop $0
  

  ; remove file associations
  ${unregisterExtension} ".plf" "VisiCut Portable Laser File"
  ${unregisterExtension} ".svg" "SVG File"
  
 
  ; remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}"
  DeleteRegKey HKLM  "SOFTWARE\${Vendor}\${AppName}"
  ; remove shortcuts, if any.
  Delete "$SMPROGRAMS\${AppName}\*.*"

  ; remove files
  RMDir /r "$INSTDIR"
 
SectionEnd
