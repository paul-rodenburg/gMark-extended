@echo off
setlocal

rem Navigate to the directory containing this batch file.
cd /d "%~dp0" || exit /b 1

rem Build the JAR.
call gradlew.bat -PrefName=ev2.2 :client:shadowJar || exit /b 1

rem Run the JAR.
java -jar "client\build\libs\gMark-vev2.2.jar"
exit /b %errorlevel%
