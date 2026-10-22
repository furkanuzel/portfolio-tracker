Option Explicit

Dim shell, wmi, processes, process
Set shell = CreateObject("WScript.Shell")
Set wmi = GetObject("winmgmts:\\.\root\cimv2")
Set processes = wmi.ExecQuery("SELECT ProcessId, CommandLine FROM Win32_Process WHERE Name='cmd.exe'")

For Each process In processes
    If Not IsNull(process.CommandLine) Then
        If InStr(1, process.CommandLine, "Sunucu_Portfoy.cmd", vbTextCompare) > 0 Then
            shell.Run "taskkill /PID " & process.ProcessId & " /T /F", 0, True
        End If
    End If
Next
