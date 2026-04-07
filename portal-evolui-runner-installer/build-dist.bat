@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "GOCMD="
where go >nul 2>&1
if %errorlevel% equ 0 (
  set "GOCMD=go"
) else (
  echo Go nao encontrado no PATH ^(variavel de ambiente^).
  set /p "GOCMD=Informe o caminho completo do go.exe ^(ex.: D:\Users\...\go1.26.1\bin\go.exe^): "
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
