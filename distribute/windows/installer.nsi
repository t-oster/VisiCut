; Taken from http://nsis.sourceforge.net/Simple_installer_with_JRE_check by weebib
; Use it as you desire.
 
; Credit given to so many people of the NSIS forum.
 
!define AppName "VisiCut"
!define AppVersion "$%VERSION%"
!define ShortName "VisiCut"
!define Vendor "RWTH Aachen University"
 
!include "MUI.nsh"
!include "Sections.nsh"
!include "EnvVarUpdate.nsh"
!include "FileAssociation.nsh"
 
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
;  !insertmacro MUI_PAGE_LICENSE "license-with-jre.txt"
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
  LangString TEXT_PRODVER_TITLE ${LANG_ENGLISH} "Installed version of ${AppName}"
  LangString TEXT_PRODVER_SUBTITLE ${LANG_ENGLISH} "Installation cancelled"
 
;--------------------------------
;Reserve Files
 
  ;Only useful for BZIP2 compression
 
 
  !insertmacro MUI_RESERVEFILE_INSTALLOPTIONS
 
;--------------------------------
;Installer Sections
 
Section "Installation of ${AppName}" SecAppFiles
  ; Full install, cannot be unselected
  ; If you add more sections be sure to add them here as well
  SectionIn 1 RO

  ; Try to run uninstaller of previous version
  ; This ensures that if the new installer installs other files or at a new location, then the old files still work properls.
  ; Assumption: The installation path is the same as before. (Without this assumption, the code would be much more complicated.)
  ; from: https://nsis.sourceforge.io/When_I_use_ExecWait_uninstaller.exe_it_doesn%27t_wait_for_the_uninstaller
  Push $0
  ExecWait '"$INSTDIR\uninstall.exe" /S _?=$INSTDIR' $0
  Pop $0

  SetShellVarContext all ; Start menu (etc.) is changed for all users

  SetOutPath $INSTDIR
  File /r "visicut\"

  ; if Inkscape is installed in standard location (not Portable App etc.)
  IfFileExists "C:\Program Files\Inkscape\share\inkscape\extensions\README.md" install_extension done_install_extension
  ; then:
  install_extension:
    ; install the extension system-wide
    SetOutPath "C:\Program Files\Inkscape\share\inkscape\extensions\visicut"
    File /r "visicut\inkscape_extension"
  done_install_extension:
  ; end if.

  ;Store install folder
  WriteRegStr HKLM "SOFTWARE\${Vendor}\${ShortName}" "" $INSTDIR
 
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "DisplayName" "${AppName}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoModify" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${ShortName}" "NoRepair" "1"

  ; insert visicut to path
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
    Messagebox MB_OK|MB_ICONEXCLAMATION "Warning: The PATH environment variable is too long, the installer is unable to modify it! If you install VisiCut to the default directory, everything will still work."
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

  SetShellVarContext all ; Start menu (etc.) is changed for all users

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
  Call SetupSections
 
FunctionEnd
 
Function myPreInstfiles
 
  Call RestoreSections
  SetAutoClose true
 
FunctionEnd
 
Function RestoreSections
  !insertmacro SelectSection ${SecAppFiles}
  !insertmacro SelectSection ${SecCreateShortcut}
 
FunctionEnd
 
Function SetupSections
  !insertmacro UnselectSection ${SecAppFiles}
  !insertmacro UnselectSection ${SecCreateShortcut}
FunctionEnd
 
;--------------------------------
;Uninstaller Section
 
Section "Uninstall"

  ; remove visicut from path
  Push $0
  Push $1

  SetShellVarContext all ; Start menu (etc.) is changed for all users


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
  RMDir /r "$SMPROGRAMS\${AppName}"

  ; remove files
  RMDir /r "$INSTDIR"
 
SectionEnd
