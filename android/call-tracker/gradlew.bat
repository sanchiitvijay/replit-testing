@echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
  echo Gradle wrapper JAR is missing: %CLASSPATH%
  exit /b 1
)

if defined JAVA_HOME (
  set JAVA=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA=java.exe
)

"%JAVA%" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
