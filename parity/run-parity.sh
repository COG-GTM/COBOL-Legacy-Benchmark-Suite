#!/usr/bin/env bash
#
# PORTVALD parity harness.
#
#   1. compiles the untouched legacy module PORTVALD with GnuCOBOL
#   2. compiles the harness driver PVDRIVER
#   3. runs every case in parity/cases/portvald-cases.psv through the
#      compiled COBOL and records the outputs
#   4. fails if those outputs differ from the committed evidence file
#      parity/expected/portvald-cobol-output.psv
#   5. runs the Java test suite, which replays the same case table through
#      the port and compares it to that evidence file
#
# Usage:  parity/run-parity.sh            compare against the committed file
#         parity/run-parity.sh --record   overwrite the committed file
#
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$PWD"

BUILD="$ROOT/build"
CASES="$ROOT/parity/cases/portvald-cases.psv"
GOLDEN="$ROOT/parity/expected/portvald-cobol-output.psv"
ACTUAL="$BUILD/portvald-cobol-output.psv"

RECORD=0
if [[ "${1:-}" == "--record" ]]; then
  RECORD=1
fi

mkdir -p "$BUILD"

# The Java module targets release 17. Honour JAVA_HOME when it already points
# at a JDK 17 or newer, otherwise fall back to the newest JDK installed under
# /usr/lib/jvm so the harness runs on a box whose default java is older.
java_major() {
  "$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'
}
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" || "$(java_major "$JAVA_HOME")" -lt 17 ]]; then
  for candidate in /usr/lib/jvm/java-2*-openjdk* /usr/lib/jvm/java-17-openjdk*; do
    if [[ -x "$candidate/bin/java" && "$(java_major "$candidate")" -ge 17 ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || "$(java_major "$JAVA_HOME")" -lt 17 ]]; then
  echo "parity: a JDK 17 or newer is required; set JAVA_HOME" >&2
  exit 1
fi
echo "== using JAVA_HOME=$JAVA_HOME =="

echo "== compiling legacy module PORTVALD (unmodified) =="
cobc -m -I "$ROOT/src/copybook/common" -Wall \
     -o "$BUILD/PORTVALD.so" "$ROOT/src/programs/portfolio/PORTVALD.cbl"

echo "== compiling parity driver PVDRIVER =="
cobc -x -I "$ROOT/src/copybook/common" -Wall \
     -o "$BUILD/PVDRIVER" "$ROOT/parity/cobol/PVDRIVER.cbl"

echo "== executing PORTVALD over the case table =="
COB_LIBRARY_PATH="$BUILD" CASEFILE="$CASES" OUTFILE="$ACTUAL" "$BUILD/PVDRIVER"

if [[ "$RECORD" == "1" ]]; then
  cp "$ACTUAL" "$GOLDEN"
  echo "recorded $(grep -vc '^case_id|' "$GOLDEN") COBOL results into $GOLDEN"
else
  echo "== comparing against committed COBOL evidence =="
  diff -u "$GOLDEN" "$ACTUAL"
  echo "COBOL output matches parity/expected/portvald-cobol-output.psv"
fi

echo "== running the Java port against the same evidence =="
mvn -q -f "$ROOT/java/pom.xml" test

echo "parity harness passed"
