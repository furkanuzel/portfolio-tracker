Option Explicit

Dim shell, fso, baseDir, appUrl, browserPath, profilePath, i
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

baseDir = fso.GetParentFolderName(WScript.ScriptFullName)
appUrl = "http://localhost:8080/portfoy/"

If Not AppIsReady(appUrl) Then
    shell.Run Chr(34) & baseDir & "\Sunucu_Portfoy.cmd" & Chr(34), 0, False
    For i = 1 To 60
        WScript.Sleep 1000
        If AppIsReady(appUrl) Then Exit For
    Next
End If

If Not AppIsReady(appUrl) Then
    MsgBox "Portfoy uygulamasi baslatilamadi. Java ve Maven ayarlarini kontrol et.", 16, "Portfoy"
    WScript.Quit 1
End If

browserPath = shell.ExpandEnvironmentStrings("%ProgramFiles%") & "\Google\Chrome\Application\chrome.exe"
If Not fso.FileExists(browserPath) Then browserPath = shell.ExpandEnvironmentStrings("%ProgramFiles(x86)%") & "\Google\Chrome\Application\chrome.exe"
If Not fso.FileExists(browserPath) Then browserPath = shell.ExpandEnvironmentStrings("%LocalAppData%") & "\Google\Chrome\Application\chrome.exe"
If Not fso.FileExists(browserPath) Then browserPath = shell.ExpandEnvironmentStrings("%ProgramFiles(x86)%") & "\Microsoft\Edge\Application\msedge.exe"
profilePath = shell.ExpandEnvironmentStrings("%LocalAppData%") & "\PortfoyTakipApp"

If fso.FileExists(browserPath) Then
    shell.Run Chr(34) & browserPath & Chr(34) & " --user-data-dir=" & Chr(34) & profilePath & Chr(34) & " --no-first-run --app=" & Chr(34) & appUrl & Chr(34), 1, True
    StopServer
Else
    shell.Run appUrl, 1, False
End If

Function AppIsReady(url)
    Dim http
    On Error Resume Next
    Set http = CreateObject("MSXML2.XMLHTTP.6.0")
    http.Open "GET", url, False
    http.Send
    AppIsReady = (Err.Number = 0 And http.Status = 200)
    Err.Clear
    On Error GoTo 0
End Function

Sub StopServer
    Dim wmi, processes, process
    On Error Resume Next
    Set wmi = GetObject("winmgmts:\\.\root\cimv2")
    Set processes = wmi.ExecQuery("SELECT ProcessId, CommandLine FROM Win32_Process WHERE Name='cmd.exe'")
    For Each process In processes
        If Not IsNull(process.CommandLine) Then
            If InStr(1, process.CommandLine, "Sunucu_Portfoy.cmd", vbTextCompare) > 0 Then
                shell.Run "taskkill /PID " & process.ProcessId & " /T /F", 0, True
            End If
        End If
    Next
    On Error GoTo 0
End Sub
