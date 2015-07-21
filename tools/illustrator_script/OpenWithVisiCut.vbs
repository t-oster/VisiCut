'Exports the current document as SVG to dest
'dest contains the full path and file name to save to

destFile = "C:\temp\VCtemp.svg"
exportFileAsSVG (destFile)

Sub exportFileAsSVG (dest)
Set appRef = CreateObject("Illustrator.Application")
Set svgExportOptions = CreateObject("Illustrator.ExportOptionsSVG")

If appRef.Documents.Count > 0 Then
     svgExportOptions.EmbedRasterImages = True
     svgExportOptions.FontSubsetting = 7 'aiAllGlyphs
     Set docRef = appRef.ActiveDocument
     Call docRef.Export (dest, 3, svgExportOptions) ' 3 = aiSVG
End If
End Sub

'open document in VisiCut

strFileName = "C:\temp\VCtemp.svg"
Set oShell = CreateObject("WScript.Shell")

oShell.Run "visicut.exe "  & strFileName
