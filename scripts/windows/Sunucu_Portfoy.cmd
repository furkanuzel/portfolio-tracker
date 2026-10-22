@echo off
title PortfoySunucusu
rem Starts the app with Maven from the project root. Uses mvn from PATH, or MAVEN_HOME.
cd /d "%~dp0..\.."
set "MVN=mvn"
where mvn >nul 2>&1 || set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
call "%MVN%" -q clean package cargo:run %PORTFOY_MAVEN_ARGS%
echo.
echo Portfoy sunucusu durdu veya bir hata olustu.
pause
