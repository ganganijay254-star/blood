@echo off
setlocal enabledelayedexpansion

REM **BLOODCARE SERVER STARTUP SCRIPT**
REM This script sets up and starts your BloodCare application

echo.
echo ========================================
echo  BLOODCARE SERVER STARTUP
echo ========================================
echo.

REM Set Java 17 as default
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Java Home: %JAVA_HOME%
echo.

REM Check if MySQL is running (adjust if your MySQL uses a different port)
echo Checking MySQL connection...
netstat -an | find ":3306" >nul
if errorlevel 1 (
    echo WARNING: MySQL doesn't appear to be running on port 3306
    echo Please ensure MySQL is started before proceeding
    echo.
)

echo.
echo Starting BloodCare Backend Server on port 8082...
echo Please wait, this may take a minute...
echo.

REM Clear Maven cache to avoid Java version conflicts
if exist "%USERPROFILE%\.m2\repository\org\springframework\boot" (
    echo Clearing Maven Spring Boot cache...
    rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\boot" 2>nul
)

REM Try to run with Maven
cd /d "C:\Users\Man\OneDrive\Desktop\BloodCare\BloodCare"
call mvnw.cmd spring-boot:run -Dmaven.test.skip=true -o

if errorlevel 1 (
    echo.
    echo ERROR: Failed to start server
    echo.
    echo SOLUTION:
    echo 1. Make sure Java 17 is installed
    echo 2. Make sure MySQL is running on localhost:3306
    echo 3. Check that no other application is using port 8082
    echo.
)

pause

