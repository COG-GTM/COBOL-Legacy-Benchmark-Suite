#!/usr/bin/env python3
"""Rewrite SECMGR.cbl EXEC CICS/SQL blocks into GnuCOBOL CALLs to stubs.
Host variables are taken from the source, so the AUTHFILE key actually used
(:SEC-USER-ID vs :WS-USER-ID) is preserved."""
import re, sys
src = open(sys.argv[1]).read()
src = src.replace("       01  WS-DB2-AREA.\n           EXEC SQL INCLUDE SQLCA END-EXEC.\n",
                  "       01  WS-DB2-AREA         PIC S9(9) COMP-5.\n       01  SQLCODE             PIC S9(9) COMP-5.\n")
src = src.replace("DFHRESP(NORMAL)", "0")
# pre-existing: ERRHND.cpy already declares its own 01 level
src = src.replace("       01  WS-ERROR-AREA.\n", "")
def repl(m):
    kind, body, dot = m.group(1), " ".join(m.group(2).split()), m.group(3) or ""
    ind = "           "
    if kind == "CICS":
        if body == "RETURN":
            return ind + "GOBACK" + dot
        if body.startswith("ASSIGN"):
            uid = re.search(r"USERID\(([^)]+)\)", body).group(1)
            resp = re.search(r"RESP\(([^)]+)\)", body)
            out = ind + "CALL 'CICSASGN' USING " + uid + " STUB-RESP"
            if resp:
                out += "\n" + ind + "MOVE STUB-RESP TO " + resp.group(1)
            return out + dot
    if kind == "SQL":
        if body.startswith("SELECT COUNT(*)"):
            into = re.search(r"INTO :(\S+)", body).group(1)
            u = re.search(r"USER_ID = :(\S+)", body).group(1)
            r = re.search(r"RESOURCE = :(\S+)", body).group(1)
            a = re.search(r"ACCESS_TYPE = :(\S+)", body).group(1)
            return (ind + f"CALL 'SQLAUTH' USING {u} {r}\n" +
                    ind + f"    {a} {into} SQLCODE" + dot)
        if body.startswith("INSERT INTO AUDITLOG"):
            return ind + "MOVE 0 TO SQLCODE" + dot
    raise SystemExit("unhandled EXEC: " + body)
src = re.sub(r"^ +EXEC (CICS|SQL)\s(.*?)END-EXEC(\.?)", repl, src, flags=re.S | re.M)
src = src.replace("       LINKAGE SECTION.\n",
                  "       01  STUB-RESP           PIC S9(8) COMP.\n       LINKAGE SECTION.\n")
sys.stdout.write(src)
