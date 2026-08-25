#!/usr/bin/env bash
#
# Stage the legacy portfolio programs so GnuCOBOL can compile them, WITHOUT editing
# anything under src/. Every transform below is minimal, mechanical and behaviour-preserving;
# each is documented in modernized/CONTRACTS.md section 8 and in the per-program before/after
# artifact under docs/before-after/.
#
# Usage: golden/cobol/stage.sh
# Output: build/stage/*.cbl (staged sources), build/bin/* (executables)
#
set -euo pipefail
cd "$(dirname "$0")/../.."
ROOT=$PWD
SRC=src/programs/portfolio
STAGE=build/stage
BIN=build/bin
CPY=src/copybook/common

mkdir -p "$STAGE" "$BIN"

# ---------------------------------------------------------------------------
# Transform 1 (all programs): the banner comment on line 1 sits in column 8, not the
# fixed-format indicator column 7, so cobc parses it as code and reports
# "PROGRAM-ID header missing". Shift it one column left. Comment text only.
# ---------------------------------------------------------------------------
fix_banner() {
  sed '1s/^       \*/      */' "$1"
}

# PORTREAD, PORTUPDT, PORTVALD need nothing beyond transform 1.
for p in PORTREAD PORTUPDT PORTVALD; do
  fix_banner "$SRC/$p.cbl" > "$STAGE/$p.cbl"
done

# ---------------------------------------------------------------------------
# Transform 2 (PORTADD): both FD PORTFOLIO-FILE and FD INPUT-FILE do `COPY PORTFLIO`,
# making PORT-RECORD / PORT-KEY ambiguous ("PORT-RECORD requires qualification").
# The input FD's record is only ever used as the target of `READ INPUT-FILE INTO
# PORT-RECORD`, which does not name the FD record at all, so an anonymous 148-byte
# record is exactly equivalent. Record length is unchanged (CONTRACTS section 1.1).
# ---------------------------------------------------------------------------
fix_banner "$SRC/PORTADD.cbl" \
  | awk '
      /^       FD  INPUT-FILE\./ { print; infd=1; next }
      infd && /COPY PORTFLIO\./  { print "       01  INPUT-RECORD        PIC X(148)."; infd=0; next }
      { print }
    ' > "$STAGE/PORTADD.cbl"

# ---------------------------------------------------------------------------
# Transform 3 (PORTDEL): `ACCEPT WS-TIMESTAMP FROM TIME STAMP` is not valid GnuCOBOL
# (TIME STAMP is an IBM extension). Substituted with FUNCTION CURRENT-DATE, which yields
# the same kind of value into the same X(26) field. The audit timestamp is
# nondeterministic either way, so the parity harness normalizes it out of the diff and
# compares the remaining audit fields exactly.
# ---------------------------------------------------------------------------
fix_banner "$SRC/PORTDEL.cbl" \
  | sed 's/ACCEPT WS-TIMESTAMP FROM TIME STAMP/MOVE FUNCTION CURRENT-DATE TO WS-TIMESTAMP/' \
  > "$STAGE/PORTDEL.cbl"

# ---------------------------------------------------------------------------
# PORTTRAN and PORTMSTR are deliberately NOT staged.
#
# PORTTRAN needs the PORTREC copybook, which does not exist anywhere in this repository
# (CONTRACTS section 1.3). Supplying an invented copybook would make the "before" baseline
# a fabrication, so PORTTRAN expectations are marked DERIVED instead of executed.
#
# PORTMSTR passes USING on a program compiled as an executable and references LS-*/ERR-*
# fields that no copybook in this repo defines. Repairing it means writing new logic, not a
# mechanical transform, so its expectations are DERIVED too.
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Golden-set support programs (new, written for the harness; they use the real copybooks
# so every byte they emit is genuine COBOL output).
# ---------------------------------------------------------------------------
for p in GOLDGEN GOLDDUMP VALDRV; do
  cp "golden/cobol/$p.cbl" "$STAGE/$p.cbl"
done

echo "== compiling =="
cobc -x -I "$CPY" -o "$BIN/PORTADD"  "$STAGE/PORTADD.cbl"
cobc -x -I "$CPY" -o "$BIN/PORTREAD" "$STAGE/PORTREAD.cbl"
cobc -x -I "$CPY" -o "$BIN/PORTUPDT" "$STAGE/PORTUPDT.cbl"
cobc -x -I "$CPY" -o "$BIN/PORTDEL"  "$STAGE/PORTDEL.cbl"
cobc -m -I "$CPY" -o "$BIN/PORTVALD.so" "$STAGE/PORTVALD.cbl"
cobc -x -I "$CPY" -o "$BIN/GOLDGEN"  "$STAGE/GOLDGEN.cbl"
cobc -x -I "$CPY" -o "$BIN/GOLDDUMP" "$STAGE/GOLDDUMP.cbl"
cobc -x -I "$CPY" -o "$BIN/VALDRV"   "$STAGE/VALDRV.cbl"
echo "== ok =="
ls -1 "$BIN"
