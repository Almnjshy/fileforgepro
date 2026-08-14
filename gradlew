#!/bin/sh

#
# FileForge Pro — Gradle wrapper startup script
#

# Resolve script location
APP_HOME=$( cd "${APP_HOME:-$(dirname "$0")}" > /dev/null && pwd -P ) || exit

# Use the wrapper jar if present
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Error: gradle-wrapper.jar not found at $WRAPPER_JAR" >&2
    echo "Run 'gradle wrapper --gradle-version 8.8' to regenerate it," >&2
    echo "or open the project in Android Studio which will create it." >&2
    exit 1
fi

# Find java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "Error: Java not found. Install JDK 17+ or set JAVA_HOME." >&2
    exit 1
fi

exec "$JAVACMD" \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
