#!/usr/bin/env python3
"""Rewrite INQONLN.cbl into a GnuCOBOL-compilable program: EXEC CICS -> CALLs to stubs."""
import re, sys
src, errhnd, out = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(src).read()
errbody = "".join(l for l in open(errhnd) if "01  ERROR-HANDLING" not in l)
s = s.replace("""       01  WS-COMMAREA.
           COPY INQCOM.""", """       01  WS-COMMAREA.
           05 WS-COMMAREA-FUNCTION    PIC X(4).
           05 WS-COMMAREA-ACCOUNT-NO  PIC X(10).
           05 WS-COMMAREA-RESP        PIC S9(8) COMP.
           05 WS-ERROR-MESSAGE        PIC X(80).
       01  EIBRESP                    PIC S9(8) COMP VALUE 0.
       01  EIBRESP2                   PIC S9(8) COMP VALUE 0.""", 1)
s = s.replace("""       01  WS-ERROR-AREA.
           COPY ERRHND.""", "       01  WS-ERROR-AREA.\n" + errbody, 1)
s = re.sub(r"       LINKAGE SECTION\..*?(?=       PROCEDURE DIVISION)", "", s, flags=re.S)
def conv(m):
    body = " ".join(m.group(1).split())
    period = m.group(2)
    arg = lambda k: re.search(k + r"\(([^)]*)\)", body).group(1)
    if body.startswith("HANDLE CONDITION"): stmt = "CONTINUE"
    elif body.startswith("RECEIVE"): stmt = "CALL 'RECVSTUB' USING %s" % arg("INTO")
    elif body.startswith("LINK"):
        stmt = "CALL %s USING %s" % (arg("PROGRAM"), arg("COMMAREA"))
    elif body.startswith("ASSIGN"): stmt = "CALL 'ASGNSTUB' USING %s" % arg("USERID")
    elif body.startswith("SEND TEXT"): stmt = "CALL 'SENDTXT' USING %s" % arg("FROM")
    elif body.startswith("SEND"): stmt = "CALL 'SENDSTUB' USING BY CONTENT %s" % arg("MAP")
    elif body.startswith("RETURN") or body.startswith("ABEND"): stmt = "CALL 'RETSTUB'"
    else: raise SystemExit("unhandled: " + body)
    return "           " + stmt + period
s = re.sub(r"[ ]*EXEC CICS(.*?)END-EXEC(\.?)", conv, s, flags=re.S)
open(out, "w").write(s)
