#!/usr/bin/env bash
# Regenerates the RPTPOS00 / RPTAUD00 / RPTSTA00 golden report files from the GnuCOBOL reference
# programs in src/test/cobol and checks them against the committed Java goldens.
#
# Inputs are the committed PORTTEST data set under src/test/resources/porttest (kept in sync with
# PortTestFixtures by PortTestFixturesTest). Outputs are compared byte-for-byte with
# src/test/resources/golden/<ReportTest>/porttestReportFile.dat, which the Java tests also assert
# against, so Java == golden == COBOL.
#
#   ./cobol-parity.sh           # compile, run, compare (exit 1 on any difference)
#   ./cobol-parity.sh --update  # overwrite the golden .dat files with the COBOL output
set -euo pipefail

MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${MODULE_DIR}/../.." && pwd)"
COBOL_SRC="${MODULE_DIR}/src/test/cobol"
PORTTEST="${MODULE_DIR}/src/test/resources/porttest"
GOLDEN="${MODULE_DIR}/src/test/resources/golden"
BUILD="${MODULE_DIR}/target/cobol"
UPDATE="${1:-}"

export RPTDATE="${RPTDATE:-240409}"   # ACCEPT FROM DATE equivalent of PortTestFixtures.REPORT_DATE

command -v cobc >/dev/null || { echo "cobc (GnuCOBOL) not found on PATH" >&2; exit 2; }
mkdir -p "${BUILD}"

compile() {
  # IBM COMP sizes (2/4/8 bytes, big-endian) to match BinaryField in clbs-common; the default
  # dialect is kept otherwise so GnuCOBOL's filename mapping resolves the env-var data set names.
  cobc -x -fbinary-size=2-4-8 -fbinary-byteorder=big-endian \
    -I "${REPO_ROOT}/src/copybook/common" -I "${REPO_ROOT}/src/copybook/batch" \
    -o "${BUILD}/$1" "${COBOL_SRC}/$1.cbl" 2> >(grep -v 'not terminated by a newline' >&2 || true)
}

status=0
compare() {
  local actual="$1" golden="$2"
  if [[ "${UPDATE}" == "--update" ]]; then
    cp "${actual}" "${golden}"
    echo "updated ${golden#${MODULE_DIR}/}"
  elif cmp -s "${actual}" "${golden}"; then
    echo "match   ${golden#${MODULE_DIR}/} ($(stat -c %s "${actual}") bytes)"
  else
    echo "DIFF    ${golden#${MODULE_DIR}/}" >&2
    cmp "${actual}" "${golden}" >&2 || true
    status=1
  fi
}

compile RPTPOS00R
compile RPTAUD00R
compile RPTSTA00R

POSMSTRE="${PORTTEST}/POSMSTRE.dat" RPTFILE="${BUILD}/RPTPOS00.RPTFILE" "${BUILD}/RPTPOS00R"
AUDITLOG="${PORTTEST}/AUDITLOG.dat" ERRLOG="${PORTTEST}/ERRLOG.dat" \
  RPTFILE="${BUILD}/RPTAUD00.RPTFILE" "${BUILD}/RPTAUD00R"
DB2STATS="${PORTTEST}/DB2STATS.dat" BCHSTATS="${PORTTEST}/BCHSTATS.dat" \
  RPTFILE="${BUILD}/RPTSTA00.RPTFILE" "${BUILD}/RPTSTA00R"

compare "${BUILD}/RPTPOS00.RPTFILE" "${GOLDEN}/PositionReportTest/porttestReportFile.dat"
compare "${BUILD}/RPTAUD00.RPTFILE" "${GOLDEN}/AuditReportTest/porttestReportFile.dat"
compare "${BUILD}/RPTSTA00.RPTFILE" "${GOLDEN}/StatisticsReportTest/porttestReportFile.dat"

exit "${status}"
