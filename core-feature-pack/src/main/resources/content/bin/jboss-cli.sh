#!/bin/sh
# CLI launcher script. The actual launcher is wildfly-cli.sh.
# This file is in charge to make the actual launcher safe from patching CLI 
# from the CLI.
# NB: This file can be safely patched at execution time.
# It is no more running once the CLI launcher is running.
#
DIR=`dirname "$0"`
ORIGINAL_SCRIPT="$DIR"/wildfly-cli.sh
RUNNING_SCRIPT="$DIR"/wildfly-cli-private-runner.sh
cp "$ORIGINAL_SCRIPT" "$RUNNING_SCRIPT" 2>/dev/null
if [ $? -ne 0 ]; then
  echo "CLI runner script has not been created, patching this CLI running instance can lead to unpredictable behavior."
  RUNNING_SCRIPT="$ORIGINAL_SCRIPT"
fi
exec "$RUNNING_SCRIPT" "$@"
# Any line that would follow would not be executed, exec has replaced the process.
