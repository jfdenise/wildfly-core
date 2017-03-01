@echo off

rem -------------------------------------------------------------------------
rem
rem
rem
rem CLI launcher script. The actual launcher is cli-launcher.bat.
rem This file is in charge of making the actual launcher safe from patching CLI 
rem scripts from the CLI.
rem 
rem
rem
rem WARNING: You must preserve the line numbers of this file. 
rem That is required to allow for patching CLI scripts using CLI.
rem
rem
rem
rem -------------------------------------------------------------------------

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)
set "ORIGINAL_SCRIPT=%DIRNAME%\cli-launcher.bat"
set "RUNNING_SCRIPT=%DIRNAME%\cli-private-launcher.bat"
copy "%ORIGINAL_SCRIPT%" "%RUNNING_SCRIPT%" >NUL
if %errorlevel% neq 0 (
  if "x%JBOSS_CLI_NO_PATCH_WARNING%" == "x" (
    echo WARNING: CLI launcher script has not been created, patching this CLI running instance can lead to unpredictable behavior.
  )
  set "RUNNING_SCRIPT=%ORIGINAL_SCRIPT%"
)
call "%RUNNING_SCRIPT%" %*
exit /B %errorlevel%
