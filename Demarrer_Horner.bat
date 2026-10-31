@echo off
rem ==========================================================================
rem  HORNER JAVA LAB - lanceur Windows
rem  Sans argument : ouvre l'interface graphique.
rem  Avec arguments : les transmet a la ligne de commande, par exemple
rem      Demarrer_Horner.bat eval --coeffs "2,-6,2,-1" --x 3
rem ==========================================================================
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"

set "JAR=target\horner-java-lab-1.0.0.jar"
set "JAVA_EXE="

rem 1. JAVA_HOME, 2. java present dans le PATH
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
    for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
)
if not defined JAVA_EXE (
    echo [ERREUR] Java est introuvable.
    echo Installez Java 21, par exemple avec :  winget install EclipseAdoptium.Temurin.21.JDK
    echo puis rouvrez cette fenetre.
    pause
    exit /b 1
)

rem Verification de la version (21 ou plus)
set "JAVA_VERSION="
for /f "tokens=3" %%V in ('call "%JAVA_EXE%" -version 2^>^&1 ^| findstr /i "version"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~V"
for /f "delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"
if "%JAVA_MAJOR%"=="1" set "JAVA_MAJOR=8"
if not defined JAVA_MAJOR set "JAVA_MAJOR=0"
if %JAVA_MAJOR% LSS 21 (
    echo [ERREUR] Java %JAVA_VERSION% detecte : Java 21 ou plus recent est requis.
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo [ERREUR] %JAR% est introuvable.
    echo Construisez d'abord le projet :  mvn clean package
    pause
    exit /b 2
)

if "%~1"=="" (
    rem Interface graphique sans fenetre console (javaw), si disponible
    if exist "%JAVA_EXE:java.exe=javaw.exe%" (
        start "" "%JAVA_EXE:java.exe=javaw.exe%" -jar "%JAR%"
    ) else (
        "%JAVA_EXE%" -jar "%JAR%"
    )
    exit /b 0
)

"%JAVA_EXE%" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%JAR%" %*
exit /b %ERRORLEVEL%
