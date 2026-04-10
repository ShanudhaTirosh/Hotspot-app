#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle wrapper script for POSIX-compatible shells.
#
set -e

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
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

if [ "$APP_HOME" ] ; then
    SAVE_HOME="$APP_HOME"
    APP_HOME=`dirname "$0"`
    APP_HOME=`cd "$APP_HOME" && pwd`
    if [ -z "$APP_HOME" ] ; then APP_HOME="$SAVE_HOME" ; fi
fi

GRADLE_OPTS="$GRADLE_OPTS \"-Dorg.gradle.appname=$APP_BASE_NAME\""

# Build the JVM arguments
JVM_OPTS="-Xmx256m -Xms64m"
DEFAULT_JVM_OPTS='"-Dfile.encoding=UTF-8" "-Duser.country=US" "-Duser.language=en" "-Duser.variant"'

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' in PATH."
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
