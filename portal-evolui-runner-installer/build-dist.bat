@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Caminho do go.exe: 1) PORTAL_RUNNER_INSTALLER_GO  2) PATH  3) %USERPROFILE%\sdk\go*\bin (SDK local)
set "GOCMD="
if defined PORTAL_RUNNER_INSTALLER_GO set "GOCMD=%PORTAL_RUNNER_INSTALLER_GO%"

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
  echo Defina PORTAL_RUNNER_INSTALLER_GO com o caminho completo do go.exe ou adicione Go ao PATH.
  echo Exemplo: set PORTAL_RUNNER_INSTALLER_GO=D:\Users\fzucato\sdk\go1.26.1\bin\go.exe
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
"%GOCMD%" build -trimpath -ldflags="-s -w" -o "dist\portal-evolui-runner-installer-windows-amd64.exe" .
if errorlevel 1 (
  set "GOOS="
  set "GOARCH="
  exit /b 1
)

echo [build-dist] Linux amd64...
set "GOOS=linux"
set "GOARCH=amd64"
"%GOCMD%" build -trimpath -ldflags="-s -w" -o "dist\portal-evolui-runner-installer-linux-amd64" .
set "GOOS="
set "GOARCH="
if errorlevel 1 exit /b 1

echo.
echo Concluido. Saida em "%~dp0dist\"
dir /b "dist\portal-evolui-runner-installer-windows-amd64.exe" "dist\portal-evolui-runner-installer-linux-amd64" 2>nul
exit /b 0
