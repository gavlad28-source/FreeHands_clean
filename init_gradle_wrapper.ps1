# Create wrapper directory if it doesn't exist
$wrapperDir = "$PSScriptRoot\gradle\wrapper"
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

# Download Gradle distribution
$gradleVersion = "8.4"
$gradleUrl = "https://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip"
$gradleDistZip = "$env:TEMP\gradle-${gradleVersion}-bin.zip"
$gradleDistDir = "$env:TEMP\gradle-${gradleVersion}"

Write-Host "Downloading Gradle $gradleVersion..."
try {
    Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleDistZip -ErrorAction Stop
    Write-Host "Gradle $gradleVersion downloaded successfully"
} catch {
    Write-Host "Failed to download Gradle: $_" -ForegroundColor Red
    exit 1
}

# Extract Gradle
Write-Host "Extracting Gradle..."
Expand-Archive -Path $gradleDistZip -DestinationPath $env:TEMP -Force

# Create wrapper files
Write-Host "Creating Gradle wrapper files..."

# Create gradle-wrapper.properties
@"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@ | Out-File -FilePath "$wrapperDir\gradle-wrapper.properties" -Encoding utf8

# Create gradlew.bat
@"
@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME
set APP_HOME=%APP_HOME:\\=/%
set APP_HOME=%APP_HOME:/\=/%
set APP_HOME=%APP_HOME:/\\=%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java
set JAVA_EXE_PATH=%JAVA_EXE%
if "%JAVA_EXE_PATH%" == "" goto error

goto execute

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

:omega
"@ | Out-File -FilePath "$PSScriptRoot\gradlew.bat" -Encoding ascii

# Create gradlew (Unix shell script)
@"
#!/bin/sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
darwin=false
msys=false
darwin=false
case "`uname`" in
  CYGWIN* )
    msys=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $msys ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# For Cygwin, switch paths to Windows format before running java
if $cygwin || $msys ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=`expr $i + 1`
    done
    case $i in
        0) set -- ;;
        1) set -- "$args0" ;;
        2) set -- "$args0" "$args1" ;;
        3) set -- "$args0" "$args1" "$args2" ;;
        4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/ ; done
    echo " "
}
APP_ARGS=`save "$@"`

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

exec "$JAVACMD" "$@"
"@ | Out-File -FilePath "$PSScriptRoot\gradlew" -Encoding ascii

# Set executable permissions on the Unix script
if ($IsLinux -or $IsMacOS) {
    chmod +x "$PSScriptRoot\gradlew"
}

# Download the Gradle wrapper JAR from Maven Central
$gradleWrapperVersion = "8.13"
$gradleWrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v${gradleVersion}/gradle/wrapper/gradle-wrapper.jar"
$gradleWrapperJar = "$wrapperDir\gradle-wrapper.jar"

Write-Host "Downloading Gradle Wrapper JAR..."
try {
    Invoke-WebRequest -Uri $gradleWrapperUrl -OutFile $gradleWrapperJar -ErrorAction Stop
    Write-Host "Gradle Wrapper JAR downloaded successfully" -ForegroundColor Green
} catch {
    Write-Host "Failed to download Gradle Wrapper JAR: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Gradle wrapper initialization complete!" -ForegroundColor Green
Write-Host "You can now run './gradlew tasks' to see available tasks."
