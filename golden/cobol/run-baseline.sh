#!/usr/bin/env bash
#
# Capture the COBOL "before" baseline for the parity harness.
#
# Every program is run against a FRESHLY regenerated golden input set, so runs are isolated
# and repeatable. Program stdout and the resulting KSDS/audit state (via GOLDDUMP) are
# written to golden/expected/ as the EXPECTED-RESULTS the JS side is diffed against.
#
# Executed here: PORTADD, PORTREAD, PORTUPDT, PORTDEL, PORTVALD.
# NOT executed: PORTTRAN (missing PORTREC copybook) and PORTMSTR (undefined LS-*/ERR-*
# fields, USING on an executable). Their expectations are DERIVED - see
# golden/expected/DERIVED.md and modernized/CONTRACTS.md section 7.
#
set -euo pipefail
cd "$(dirname "$0")/../.."
BIN=build/bin
WORK=build/work
OUT=golden/expected
IN=golden/input

mkdir -p "$WORK" "$OUT" "$IN"

export COB_PRE_LOAD=PORTVALD
export COB_LIBRARY_PATH="$PWD/$BIN"

# GnuCOBOL resolves `ASSIGN TO name` through DD_<name>.
regen() {
  rm -f "$WORK"/*
  DD_PORTFILE="$WORK/portfile" \
  DD_SEEDFLAT="$IN/seed-portfolios.dat" \
  DD_ADDINPT="$IN/add-deck.dat" \
  DD_UPDTINP="$IN/update-deck.dat" \
  DD_DELEINP="$IN/delete-deck.dat" \
  DD_TRANINP="$IN/transaction-deck.dat" \
  "$BIN/GOLDGEN" > /dev/null
}

dump() {
  DD_PORTFILE="$WORK/portfile" DD_AUDFILE="$WORK/audit" "$BIN/GOLDDUMP"
}

# PORTADD stamps created records from ACCEPT ... FROM DATE YYYYMMDD, so the create-path
# dates are whatever day the baseline was captured. Record it: the parity harness masks
# exactly this value rather than pretending the field is stable.
date -u +%Y%m%d > "$OUT/run-date.txt"

echo "== PORTVALD (validation subroutine, executed) =="
"$BIN/VALDRV" > "$OUT/portvald.txt"

echo "== seed state (executed) =="
regen
dump > "$OUT/seed-state.txt"

echo "== PORTREAD (list, executed) =="
regen
DD_PORTFILE="$WORK/portfile" "$BIN/PORTREAD" > "$OUT/portread.stdout.txt"

echo "== PORTADD (create + duplicate detection, executed) =="
regen
DD_PORTFILE="$WORK/portfile" DD_INPTFILE="$IN/add-deck.dat" \
  "$BIN/PORTADD" > "$OUT/portadd.stdout.txt"
dump > "$OUT/portadd.state.txt"

echo "== PORTUPDT (field-level update, executed) =="
regen
DD_PORTFILE="$WORK/portfile" DD_UPDTFILE="$IN/update-deck.dat" \
  "$BIN/PORTUPDT" > "$OUT/portupdt.stdout.txt"
dump > "$OUT/portupdt.state.txt"

echo "== PORTDEL (delete + audit write, executed) =="
regen
DD_PORTFILE="$WORK/portfile" DD_DELEFILE="$IN/delete-deck.dat" \
  DD_AUDFILE="$WORK/audit" "$BIN/PORTDEL" > "$OUT/portdel.stdout.txt"
dump > "$OUT/portdel.state.txt"

echo "== baseline captured in $OUT =="
