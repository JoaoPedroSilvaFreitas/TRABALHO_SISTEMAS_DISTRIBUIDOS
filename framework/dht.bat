@echo off
setlocal enabledelayedexpansion

rem Script auxiliar para compilar, rodar e depurar o anel DHT deste projeto (Windows).
rem
rem Uso:
rem   dht.bat compilar
rem   dht.bat no <porta> [ipBootstrap] [portaBootstrap]
rem   dht.bat status-server [porta]
rem   dht.bat visualizador
rem   dht.bat listar
rem   dht.bat parar-tudo

set "DIR_SCRIPT=%~dp0"
set "DIR_SRC=%DIR_SCRIPT%src\framework"
set "DIR_BUILD=%DIR_SCRIPT%build"

if "%1"=="compilar" goto compilar
if "%1"=="no" goto no
if "%1"=="status-server" goto status_server
if "%1"=="visualizador" goto visualizador
if "%1"=="listar" goto listar
if "%1"=="parar-tudo" goto parar_tudo
goto uso

:compilar
echo Compilando classes Java em %DIR_BUILD% ...
if not exist "%DIR_BUILD%" mkdir "%DIR_BUILD%"
javac -d "%DIR_BUILD%" "%DIR_SRC%\*.java"
if errorlevel 1 exit /b 1
echo OK. Rode a partir de uma pasta de trabalho, ex:
echo   cd C:\dht_test ^&^& java -cp "%DIR_BUILD%" framework.Main 8080
exit /b 0

:no
if "%2"=="" (
  echo Uso: dht.bat no ^<porta^> [ipBootstrap] [portaBootstrap]
  exit /b 1
)
if not exist "%DIR_BUILD%\framework" (
  echo Classes ainda nao compiladas. Rodando "dht.bat compilar" primeiro...
  call "%~f0" compilar
)
echo Iniciando no DHT na porta %2 (Ctrl+C ou "sair" para encerrar)...
java -cp "%DIR_BUILD%" framework.Main %2 %3 %4
exit /b 0

:status_server
set "PORTA=%2"
if "%PORTA%"=="" set "PORTA=9000"
echo Servindo dht_status/ em http://localhost:%PORTA%
python "%DIR_SCRIPT%status_server.py" %PORTA%
exit /b 0

:visualizador
echo Abrindo %DIR_SCRIPT%dht_ring_viz.html ...
start "" "%DIR_SCRIPT%dht_ring_viz.html"
exit /b 0

:listar
echo Processos framework.Main em execucao:
wmic process where "commandline like '%%framework.Main%%'" get processid,commandline 2>nul | findstr /i "framework.Main"
if errorlevel 1 echo   (nenhum)
exit /b 0

:parar_tudo
echo Encerrando todos os processos framework.Main...
for /f "tokens=1" %%p in ('wmic process where "commandline like '%%framework.Main%%'" get processid ^| findstr /r "[0-9]"') do (
  taskkill /PID %%p /F >nul 2>&1
)
echo Concluido.
exit /b 0

:uso
echo Uso: dht.bat {compilar^|no ^<porta^> [ipBootstrap] [portaBootstrap]^|status-server [porta]^|visualizador^|listar^|parar-tudo}
echo.
echo Exemplo de fluxo completo (em janelas separadas):
echo   dht.bat compilar
echo   dht.bat status-server
echo   dht.bat no 8080
echo   dht.bat no 8081 127.0.0.1 8080
echo   dht.bat visualizador
exit /b 1
