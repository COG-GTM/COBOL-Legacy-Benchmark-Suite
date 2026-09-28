#!/usr/bin/env bash
# Compiles the unmodified COBOL PORTVALD and the test driver with GnuCOBOL, then runs the Java test
# suite with live COBOL parity enabled.
#
#   java/run-parity.sh                  verify: Java == live COBOL, and recorded golden == live COBOL
#   java/run-parity.sh --update-golden  also rewrite src/test/resources/parity/portvald-cobol-golden.tsv
set -euo pipefail

JAVA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${JAVA_DIR}/.." && pwd)"
COBOL_BUILD="${JAVA_DIR}/target/cobol"
GOLDEN="${JAVA_DIR}/src/test/resources/parity/portvald-cobol-golden.tsv"
LIVE="${JAVA_DIR}/target/parity/portvald-cobol-live.tsv"

update_golden=false
case "${1:-}" in
  "") ;;
  --update-golden) update_golden=true ;;
  *) echo "usage: $0 [--update-golden]" >&2; exit 2 ;;
esac

mkdir -p "${COBOL_BUILD}"
cobc --version | head -1
set -x
cobc -m -I "${REPO_ROOT}/src/copybook/common" -Wall \
  -o "${COBOL_BUILD}/PORTVALD.so" "${REPO_ROOT}/src/programs/portfolio/PORTVALD.cbl"
cobc -x -Wall -o "${COBOL_BUILD}/PVDRIVER" "${JAVA_DIR}/src/test/cobol/PVDRIVER.cbl"
{ set +x; } 2>/dev/null

if "${update_golden}"; then
  # Record only: the live-vs-golden check is expected to fail until the new file is copied in.
  mvn -B -q -f "${JAVA_DIR}/pom.xml" test \
    -Dparity.cobol.driver="${COBOL_BUILD}/PVDRIVER" -Dparity.cobol.libpath="${COBOL_BUILD}" \
    -Dtest='PortvaldParityTest$Live#javaMatchesLiveCobol' -Dsurefire.failIfNoSpecifiedTests=false
  cp "${LIVE}" "${GOLDEN}"
  echo "updated ${GOLDEN}"
fi

set -x
mvn -B -f "${JAVA_DIR}/pom.xml" test \
  -Dparity.cobol.driver="${COBOL_BUILD}/PVDRIVER" -Dparity.cobol.libpath="${COBOL_BUILD}"
