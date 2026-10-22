@echo off
taskkill /FI "WINDOWTITLE eq PortfoySunucusu*" /T /F >nul 2>&1
if errorlevel 1 (
  echo Calisan portfoy sunucusu bulunamadi.
) else (
  echo Portfoy uygulamasi kapatildi.
)
timeout /t 2 /nobreak >nul
