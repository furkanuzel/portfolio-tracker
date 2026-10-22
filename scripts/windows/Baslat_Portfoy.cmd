@echo off
setlocal
chcp 65001 >nul
rem Starts the server if needed, then opens the app in Chrome/Edge app mode.
set "APP_URL=http://localhost:8080/portfoy/"

where java >nul 2>&1
if errorlevel 1 (
  echo Java bulunamadi. Java 17 veya ustunu kurup PATH'e ekle.
  pause
  exit /b 1
)

curl --silent --fail "%APP_URL%" >nul 2>&1
if errorlevel 1 (
  start "PortfoySunucusu" /min "%~dp0Sunucu_Portfoy.cmd"
  echo Portfoy uygulamasi baslatiliyor...
  for /l %%I in (1,1,90) do (
    curl --silent --fail "%APP_URL%" >nul 2>&1 && goto :openApp
    timeout /t 1 /nobreak >nul
  )
  echo Uygulama 90 saniye icinde baslatilamadi.
  echo Ayrintilar icin PortfoySunucusu penceresini kontrol et.
  pause
  exit /b 1
)

:openApp
set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%LocalAppData%\Google\Chrome\Application\chrome.exe"
if exist "%CHROME%" (
  start "" "%CHROME%" --app="%APP_URL%"
) else (
  start "" "%APP_URL%"
)
exit /b 0
