@echo off
rem Starts the offline demo (sample positions, simulated prices) at http://localhost:8080/portfoy/
set "PORTFOY_MAVEN_ARGS=-Pdemo"
call "%~dp0Sunucu_Portfoy.cmd"
