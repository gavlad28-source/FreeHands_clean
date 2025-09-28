@echo off
echo Setting up Gradle Wrapper...

set GRADLE_VERSION=8.13
set GRADLE_DIST=gradle-%GRADLE_VERSION%-bin.zip
set GRADLE_URL=https://services.gradle.org/distributions/%GRADLE_DIST%
set GRADLE_USER_HOME=%~dp0\.gradle
set WRAPPER_DIR=%~dp0gradle\wrapper

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

if not exist "%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%GRADLE_URL%', '%TEMP%\%GRADLE_DIST%')"
    
    echo Extracting Gradle...
    powershell -Command "Expand-Archive -Path '%TEMP%\%GRADLE_DIST%' -DestinationPath '%TEMP%\gradle-temp' -Force"
    
    echo Setting up Gradle Wrapper...
    mkdir "%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\"
    xcopy /E /I /Y "%TEMP%\gradle-temp\gradle-%GRADLE_VERSION%" "%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\gradle-%GRADLE_VERSION%"
    
    echo Cleaning up...
    del "%TEMP%\%GRADLE_DIST%"
    rmdir /S /Q "%TEMP%\gradle-temp"
) else (
    echo Gradle %GRADLE_VERSION% is already installed.
)

echo Creating Gradle Wrapper files...
echo @rem>"%WRAPPER_DIR%\gradle-wrapper.jar"
echo @rem This is a placeholder. The actual JAR will be created by Gradle.>>"%WRAPPER_DIR%\gradle-wrapper.jar"

echo distributionBase=GRADLE_USER_HOME>"%WRAPPER_DIR%\gradle-wrapper.properties"
echo distributionPath=wrapper/dists>>"%WRAPPER_DIR%\gradle-wrapper.properties"
echo distributionUrl=https\://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip>>"%WRAPPER_DIR%\gradle-wrapper.properties"
echo zipStoreBase=GRADLE_USER_HOME>>"%WRAPPER_DIR%\gradle-wrapper.properties"
echo zipStorePath=wrapper/dists>>"%WRAPPER_DIR%\gradle-wrapper.properties"

echo.
echo Gradle Wrapper setup complete.
echo Please run 'gradlew.bat build' to build the project.

pause
