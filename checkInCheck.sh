#!/usr/bin/env bash

set -e
export SBT_OPTS="$SBT_OPTS -Xss2m"
sbt clean compile test