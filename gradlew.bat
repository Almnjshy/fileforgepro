@if "%DEBUG%"=="" @echo off
@rem FileForge Pro — Gradle wrapper startup script (Windows)

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo Error: gradle-wrapper.jar not found at %WRAPPER_JAR% 1>&2
    echo Run 'gradle wrapper --gradle-version 8.8' to regenerate it. 1>&2
    exit /b 1
)

if defined JAVA_HOME (
    set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
    set JAVACMD=java
)

"%JAVACMD%" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
