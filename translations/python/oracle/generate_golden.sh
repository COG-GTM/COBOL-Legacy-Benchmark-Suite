#!/usr/bin/env bash
#
# Regenerate the golden file for the PORTVALD translation pair by running the
# COBOL program under GnuCOBOL. Run this whenever the COBOL source or the case
# file changes; the parity tests compare the Python translation against the
# generated file.
#
# Usage: translations/python/oracle/generate_golden.sh
#
set -euo pipefail

ORACLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAIR_DIR="$(dirname "${ORACLE_DIR}")"
REPO_ROOT="$(cd "${PAIR_DIR}/../.." && pwd)"
BUILD_DIR="${PAIR_DIR}/build"
GOLDEN_FILE="${PAIR_DIR}/tests/golden/portvald.txt"

mkdir -p "${BUILD_DIR}" "$(dirname "${GOLDEN_FILE}")"

cobc -m -I "${REPO_ROOT}/src/copybook/common" \
    -o "${BUILD_DIR}/PORTVALD.so" \
    "${REPO_ROOT}/src/programs/portfolio/PORTVALD.cbl"

cobc -x -o "${BUILD_DIR}/pvdrivr" "${ORACLE_DIR}/PVDRIVR.cbl"

COB_LIBRARY_PATH="${BUILD_DIR}" \
CASES="${ORACLE_DIR}/portvald_cases.txt" \
GOLDEN="${GOLDEN_FILE}" \
    "${BUILD_DIR}/pvdrivr"

echo "wrote ${GOLDEN_FILE}"
