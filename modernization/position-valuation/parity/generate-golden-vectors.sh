#!/usr/bin/env bash
#
# Regenerates the golden vector files consumed by the Java parity tests
# by executing the ORIGINAL COBOL under GnuCOBOL.
#
#   parity/cobol/PVDRIVE.cbl -> calls the unmodified src/programs/portfolio/PORTVALD.cbl
#   parity/cobol/PARITHM.cbl -> re-executes the packed-decimal arithmetic of
#                               PORTTRAN 2210/2220/2240 and RPTPOS00 2110
#                               using the production PIC clauses
#   parity/cobol/PDIVZER.cbl -> the RPTPOS00 divide-by-zero condition
#   parity/cobol/PUPDMOV.cbl -> the alphanumeric-to-numeric MOVE of PORTUPDT 2200
#
# Usage:  ./generate-golden-vectors.sh          (from any directory)
#
# Requires GnuCOBOL (cobc). Output is written to
# src/test/resources/parity/*.csv and MUST be committed.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE="$(dirname "$HERE")"
REPO="$(cd "$MODULE/../.." && pwd)"
BUILD="$HERE/build"
OUT="$MODULE/src/test/resources/parity"

mkdir -p "$BUILD" "$OUT"

cobc -m -o "$BUILD/PORTVALD.so" -I "$REPO/src/copybook/common" \
     "$REPO/src/programs/portfolio/PORTVALD.cbl"
cobc -x -o "$BUILD/PVDRIVE" "$HERE/cobol/PVDRIVE.cbl"
cobc -x -o "$BUILD/PARITHM" "$HERE/cobol/PARITHM.cbl"
cobc -x -o "$BUILD/PDIVZER" "$HERE/cobol/PDIVZER.cbl"
cobc -x -o "$BUILD/PUPDMOV" "$HERE/cobol/PUPDMOV.cbl"

COB_LIBRARY_PATH="$BUILD" "$BUILD/PVDRIVE" > "$OUT/portvald-golden.csv"
"$BUILD/PARITHM" > "$OUT/arithmetic-golden.csv"
"$BUILD/PDIVZER" | tail -n +2 >> "$OUT/arithmetic-golden.csv"
"$BUILD/PUPDMOV" > "$OUT/portupdt-move-golden.csv"

echo "wrote $OUT/portvald-golden.csv ($(wc -l < "$OUT/portvald-golden.csv") lines)"
echo "wrote $OUT/arithmetic-golden.csv ($(wc -l < "$OUT/arithmetic-golden.csv") lines)"
echo "wrote $OUT/portupdt-move-golden.csv ($(wc -l < "$OUT/portupdt-move-golden.csv") lines)"
