#!/usr/bin/env bash
# Builds SECMGR with CICS/DB2 stubs (GnuCOBOL) and runs TSTSECMG.
# usage: src/programs/test/secmgr/run.sh [path/to/SECMGR.cbl]
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../../../.." && pwd)
SRC=${1:-$ROOT/src/programs/online/SECMGR.cbl}
OUT=$(mktemp -d); trap 'rm -rf "$OUT"' EXIT
python3 "$HERE/secmgr_stub.py" "$SRC" > "$OUT/SECMGR.cbl"
cobc -m -I "$ROOT/src/copybook/online" -o "$OUT/SECMGR.so" "$OUT/SECMGR.cbl"
cobc -m -o "$OUT/SECSTUBS.so" "$HERE/SECSTUBS.cbl"
cobc -x -o "$OUT/tstsecmg" "$HERE/TSTSECMG.cbl"
cd "$OUT"
COB_LIBRARY_PATH="$OUT" COB_PRE_LOAD=SECSTUBS ./tstsecmg
