@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "GOCMD="
if defined PORTAL_EVOLUI_MONITOR_GO set "GOCMD=%PORTAL_EVOLUI_MONITOR_GO%"

if not defined GOCMD (
  where go >nul 2>&1
  if not errorlevel 1 set "GOCMD=go"
)

if not defined GOCMD (
  for /d %%D in ("%USERPROFILE%\sdk\go*") do (
    if exist "%%~D\bin\go.exe" set "GOCMD=%%~D\bin\go.exe"
  )
)

if not defined GOCMD (
  echo Go nao encontrado.
  echo Defina PORTAL_EVOLUI_MONITOR_GO com o caminho completo do go.exe ou adicione Go ao PATH.
  set /p "GOCMD=Informe o caminho completo do go.exe: "
  if not defined GOCMD (
    echo Caminho vazio. Abortando.
    exit /b 1
  )
)

"%GOCMD%" version >nul 2>&1
if errorlevel 1 (
  echo Nao foi possivel executar Go: "%GOCMD%"
  exit /b 1
)

if not exist "dist" mkdir "dist"

echo.
echo [build-dist] Windows amd64...
set "GOOS=windows"
set "GOARCH=amd64"
"%GOCMD%" build -trimpath -ldflags="-s -w" -o "dist\portal-evolui-monitor-windows-amd64.exe" ./cmd/monitor
if errorlevel 1 (
  set "GOOS="
  set "GOARCH="
  exit /b 1
)

echo [build-dist] Linux amd64...
set "GOOS=linux"
set "GOARCH=amd64"
"%GOCMD%" build -trimpath -ldflags="-s -w" -o "dist\portal-evolui-monitor-linux-amd64" ./cmd/monitor
set "GOOS="
set "GOARCH="
if errorlevel 1 exit /b 1

echo.
echo Concluido. Saida em "%~dp0dist\"
dir /b "dist\portal-evolui-monitor-windows-amd64.exe" "dist\portal-evolui-monitor-linux-amd64" 2>nul
exit /b 0
