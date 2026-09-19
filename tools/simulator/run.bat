@echo off
setlocal
set "ROOT=%~dp0..\.."
set "CORESRC=%ROOT%\core\src\main\java"
set "SIMSRC=%~dp0src"
set "OUT=%~dp0out"
if not exist "%OUT%" mkdir "%OUT%"
javac -encoding UTF-8 -d "%OUT%" -sourcepath "%CORESRC%;%SIMSRC%" "%SIMSRC%\marrow\simulator\Simulator.java"
if errorlevel 1 exit /b %errorlevel%
java -cp "%OUT%" marrow.simulator.Simulator %*
endlocal
