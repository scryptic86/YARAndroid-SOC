@echo off
REM Minimal gradlew launcher that uses system gradle if present, or falls back to C:\Gradle\gradle-9.1.0
where gradle >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

REM Try fallback installation (Gradle 8.6 expected by the project's wrapper)
IF EXIST "C:\Gradle\gradle-8.6\bin\gradle.bat" (
  "C:\Gradle\gradle-8.6\bin\gradle.bat" %*
  exit /b %ERRORLEVEL%
)

echo Gradle not found in PATH and fallback not present at C:\Gradle\gradle-8.6\bin\gradle.bat.
echo Please install Gradle 8.6 at C:\Gradle\gradle-8.6 or update the launcher.
exit /b 1
