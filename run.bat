@echo off
setlocal enabledelayedexpansion

:: 1. Navigate reliably to project root directory
cd /d "%~dp0"

echo ================================================================
echo           NEXTSTEP - CAMPUS RECRUITMENT SYSTEM
echo                   Launcher and Build Script
echo ================================================================
echo.

:: 2. Check Java Development Kit [javac]
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java Development Kit [JDK] compiler 'javac' was not found in PATH.
    echo Please install Java JDK 17+ and add its bin directory to PATH.
    echo.
    pause
    exit /b 1
)

:: Check Java Runtime [java]
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java Runtime 'java' was not found in PATH.
    echo Please ensure Java is properly installed.
    echo.
    pause
    exit /b 1
)

:: 3. Check MySQL Connector/J JAR in lib/
if not exist "lib\mysql-connector-j-8.4.0.jar" (
    echo [ERROR] MySQL Connector/J JDBC driver not found!
    echo Expected file: lib\mysql-connector-j-8.4.0.jar
    echo.
    pause
    exit /b 1
)

:: 4. Check Database Configuration in config/db.properties
if not exist "config\db.properties" (
    echo [WARNING] Database configuration 'config\db.properties' not found!
    if exist "config\db.properties.example" (
        echo Creating 'config\db.properties' from example template...
        copy "config\db.properties.example" "config\db.properties" >nul
        echo 'config\db.properties' has been created.
        echo Please edit 'config\db.properties' with your MySQL root password before running NextStep.
        echo.
        pause
        exit /b 1
    ) else (
        echo [ERROR] Neither 'config\db.properties' nor 'config\db.properties.example' was found.
        echo.
        pause
        exit /b 1
    )
)

:: 5. Create bin directory if needed
if not exist "bin" mkdir "bin"

:: 6. Compile Java Sources
echo [1/2] Compiling NextStep Java source files...
javac -d bin -cp "lib\mysql-connector-j-8.4.0.jar" src\com\nextstep\model\*.java src\com\nextstep\database\*.java src\com\nextstep\dao\*.java src\com\nextstep\service\*.java src\com\nextstep\util\*.java src\com\nextstep\Main.java
set COMPILE_STATUS=%errorlevel%

if %COMPILE_STATUS% neq 0 (
    echo.
    echo [ERROR] Compilation failed with error code %COMPILE_STATUS%.
    echo Please check the Java source errors listed above.
    echo.
    pause
    exit /b %COMPILE_STATUS%
)

echo [2/2] Launching NextStep Campus Recruitment System...
echo.

:: 7. Execute Application with Classpath
java -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.nextstep.Main
set APP_STATUS=%errorlevel%

if %APP_STATUS% neq 0 (
    echo.
    echo [NOTICE] Application terminated with exit code %APP_STATUS%.
    pause
    exit /b %APP_STATUS%
)

exit /b 0
