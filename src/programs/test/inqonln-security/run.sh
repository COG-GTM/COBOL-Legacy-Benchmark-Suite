#!/usr/bin/env bash
# Regression test for INQONLN authorization order (GnuCOBOL, no CICS needed).
# xform.py rewrites each EXEC CICS in the real INQONLN.cbl into a CALL to the
# stubs in stubs.cbl, which print the order of events.
# Usage: src/programs/test/inqonln-security/run.sh   (from any directory)
set -euo pipefail
T=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$T/../../../.." && pwd)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
python3 "$T/xform.py" "$ROOT/src/programs/online/INQONLN.cbl" \
        "$ROOT/src/copybook/online/ERRHND.cpy" "$W/INQONLN.cbl"
cobc -x -o "$W/inq" "$W/INQONLN.cbl" "$T/stubs.cbl"

FAILS=0
check() { # name func validate authorize audit expected-events...
  local name=$1 func=$2 v=$3 a=$4 l=$5; shift 5
  local got want
  got=$(SCN_FUNC=$func SCN_V=$v SCN_A=$a SCN_L=$l "$W/inq" | sed -n 's/^EVT //p')
  want=$(printf '%s\n' "$@")
  if [ "$got" = "$want" ]; then echo "PASS $name"
  else echo "FAIL $name"; { diff <(echo "$want") <(echo "$got") || true; } | sed 's/^/    /'
       FAILS=$((FAILS+1)); fi
}
ACCT=0000012345
DENY=("SECMGR V rc=+00000000" "SECMGR A rc=+00000008"
      "AUDITLOG-ROW user=ALICE program=INQONLN access=DENIED"
      "ERROR-SCREEN" 'SEND-TEXT "Access denied"' "CICS-RETURN")
OK=("SECMGR V rc=+00000000" "SECMGR A rc=+00000000"
    "AUDITLOG-ROW user=ALICE program=INQONLN access=READ")

check "denied portfolio inquiry sends no data"  INQP 0 8 0 "RECEIVE func=INQP" "${DENY[@]}"
check "denied history inquiry sends no data"    INQH 0 8 0 "RECEIVE func=INQH" "${DENY[@]}"
check "user validation failure is denied"       INQP 8 0 0 "RECEIVE func=INQP" \
      "SECMGR V rc=+00000008" "AUDITLOG-ROW user=ALICE program=INQONLN access=DENIED" \
      "ERROR-SCREEN" 'SEND-TEXT "User validation failed"' "CICS-RETURN"
check "authorized portfolio inquiry"            INQP 0 0 0 "RECEIVE func=INQP" "${OK[@]}" \
      "DATA-SENT POSMAP acct=$ACCT" "RECEIVE func=EXIT" "${OK[@]}" "CICS-RETURN"
check "authorized history inquiry"              INQH 0 0 0 "RECEIVE func=INQH" "${OK[@]}" \
      "DATA-SENT HISMAP acct=$ACCT" "RECEIVE func=EXIT" "${OK[@]}" "CICS-RETURN"
check "audit write failure fails closed"        INQP 0 0 12 "RECEIVE func=INQP" \
      "SECMGR V rc=+00000000" "SECMGR A rc=+00000000" "SECMGR L rc=+00000012" \
      "ERROR-SCREEN" 'SEND-TEXT "Audit logging failed"' "CICS-RETURN"
check "authorized menu"                         MENU 0 0 0 "RECEIVE func=MENU" "${OK[@]}" \
      "SEND INQMNU" "RECEIVE func=EXIT" "${OK[@]}" "CICS-RETURN"

[ "$FAILS" -eq 0 ] && echo "ALL PASSED" || { echo "$FAILS FAILED"; exit 1; }
