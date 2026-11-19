#!/usr/bin/env bash
# Compiles the GnuCOBOL-compatible portfolio programs and runs them against
# parity/fixtures, writing their console output to parity/baseline/.
# Requires GnuCOBOL 3.x (cobc). Dates and timestamps are masked.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
P="$ROOT/parity"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT
CPY="-I $ROOT/src/copybook/common"

python3 "$P/fixtures/generate.py"
cobc -m $CPY -o "$BUILD/PORTVALD.so" "$ROOT/src/programs/portfolio/PORTVALD.cbl"
for prog in PORTADD PORTUPDT PORTDEL PORTREAD; do
  # Some sources start with a comment '*' in column 8 instead of column 7,
  # which GnuCOBOL rejects; shift that banner line only (no logic change).
  sed '1s/^       \*/      */' "$ROOT/src/programs/portfolio/$prog.cbl" > "$BUILD/$prog.cbl"
  # PORTADD copies PORTFLIO into both FDs, making every PORT-* name
  # ambiguous; give the input FD its own prefix (READ ... INTO is unchanged).
  if [ "$prog" = PORTADD ]; then
    awk '/FD  INPUT-FILE/{f=1} f&&/COPY PORTFLIO\./{sub(/COPY PORTFLIO\./,"COPY PORTFLIO REPLACING LEADING ==PORT-== BY ==INPT-==.");f=0} {print}' \
      "$BUILD/$prog.cbl" > "$BUILD/$prog.tmp" && mv "$BUILD/$prog.tmp" "$BUILD/$prog.cbl"
  fi
  # 'ACCEPT ... FROM TIME STAMP' is IBM-only; GnuCOBOL needs CURRENT-DATE.
  sed -i 's/ACCEPT WS-TIMESTAMP FROM TIME STAMP/MOVE FUNCTION CURRENT-DATE TO WS-TIMESTAMP/' "$BUILD/$prog.cbl"
  cobc -x $CPY -o "$BUILD/$prog" "$BUILD/$prog.cbl"
done
cobc -x $CPY -o "$BUILD/MKPORT" "$P/cobol/MKPORT.cbl"
cobc -x -o "$BUILD/VALDRIVR" "$P/cobol/VALDRIVR.cbl"

export COB_LIBRARY_PATH="$BUILD"
export PORTFILE="$BUILD/portfile.idx"
mask() { sed -E 's/[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}\.[0-9]{2}\.[0-9]{2}\.[0-9]{6}/<TIMESTAMP>/g'; }
mkdir -p "$P/baseline"
"$BUILD/MKPORT"
INPTFILE="$P/fixtures/portadd.dat"  "$BUILD/PORTADD"  | mask > "$P/baseline/01-portadd.txt"
"$BUILD/PORTREAD" | mask > "$P/baseline/02-portread-after-add.txt"
UPDTFILE="$P/fixtures/portupdt.dat" "$BUILD/PORTUPDT" | mask > "$P/baseline/03-portupdt.txt"
"$BUILD/PORTREAD" | mask > "$P/baseline/04-portread-after-update.txt"
DELEFILE="$P/fixtures/portdel.dat" AUDFILE="$BUILD/audit.dat" "$BUILD/PORTDEL" | mask > "$P/baseline/05-portdel.txt"
# AUD-FILLER is never initialised by PORTDEL (low-values); show it as spaces.
tr '\0' ' ' < "$BUILD/audit.dat" | fold -w 62 | sed -E 's/^.{26}/<TIMESTAMP>/' > "$P/baseline/06-portdel-audit.txt"
"$BUILD/PORTREAD" | mask > "$P/baseline/07-portread-after-delete.txt"
VALDREQ="$P/fixtures/portvald.txt" "$BUILD/VALDRIVR" > "$P/baseline/08-portvald.txt"
echo "Baseline written to $P/baseline"
