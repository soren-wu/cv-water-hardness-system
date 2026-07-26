@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------

@if "%DEBUG%"=="" @echo off
@setlocal

set "MAVEN_WRAPPER_DIR=%~dp0.mvn\wrapper"

if not "%MAVEN_PROJECTBASEDIR%"=="" set "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%"
if "%MAVEN_HOME%"=="" set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

set "CLASSWORLDS_JAR=%MAVEN_WRAPPER_DIR%\maven-wrapper.jar"
set "CLASSWORLDS_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if not exist "%CLASSWORLDS_JAR%" (
    echo Maven Wrapper JAR not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%CLASSWORLDS_JAR%'"
)

set "MAVEN_CMD_LINE_ARGS=%*"

"%JAVA_HOME%\bin\java.exe" ^
  -classpath "%CLASSWORLDS_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %CLASSWORLDS_LAUNCHER% ^
  %MAVEN_CMD_LINE_ARGS%

@endlocal
