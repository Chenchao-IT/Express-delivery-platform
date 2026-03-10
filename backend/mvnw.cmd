@REM Maven Wrapper for Windows
@REM Requires maven-wrapper.jar in .mvn/wrapper/

setlocal
set MAVEN_PROJECTBASEDIR=%~dp0

if not exist "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" (
    echo Maven Wrapper JAR not found. Run: powershell -ExecutionPolicy Bypass -File setup-maven-wrapper.ps1
    exit /b 1
)

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java"

"%JAVA_EXE%" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  -classpath "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

exit /b %ERRORLEVEL%
