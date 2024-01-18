@echo off
title aCis gameserver console
:start
REM -------------------------------------
REM Default parameters for a basic server.
SET debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5000
java %debug% -Xmx2G -cp ./libs/*; net.sf.l2j.gameserver.GameServer
REM -------------------------------------
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin have restarted, please wait.
echo.
goto start
:error
echo.
echo Server have terminated abnormaly.
echo.
:end
echo.
echo Server terminated.
echo.
pause
