#!/usr/bin/env python3
"""Extract static dependencies from the CLBS COBOL/JCL sources into deps.json.

Edge kinds:
  CALL     program -> program   (COBOL CALL 'X' / CALL X)
  COPY     program -> copybook  (COPY X)
  LINK     program -> program   (EXEC CICS LINK/XCTL PROGRAM('X'))
  SQL      program -> table     (EXEC SQL ... FROM/INTO/UPDATE/JOIN table)
  FILE     program -> dataset   (SELECT f ASSIGN TO dd)
  EXECPGM  jcl     -> program   (JCL EXEC PGM=X)
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src")
ROOT = os.path.normpath(ROOT)

programs = {}
for group in sorted(os.listdir(os.path.join(ROOT, "programs"))):
    gdir = os.path.join(ROOT, "programs", group)
    for f in sorted(os.listdir(gdir)):
        if f.lower().endswith(".cbl"):
            programs[f[:-4].upper()] = {
                "group": group,
                "path": os.path.relpath(os.path.join(gdir, f), os.path.dirname(ROOT)),
            }

copybooks = {}
for group in sorted(os.listdir(os.path.join(ROOT, "copybook"))):
    gdir = os.path.join(ROOT, "copybook", group)
    for f in sorted(os.listdir(gdir)):
        if f.lower().endswith(".cpy"):
            copybooks[f[:-4].upper()] = {
                "group": group,
                "path": os.path.relpath(os.path.join(gdir, f), os.path.dirname(ROOT)),
            }


def strip_comments(text):
    out = []
    for line in text.splitlines():
        # fixed-format indicator column 7 '*' or '/' is a comment; also free-format '*>'
        if len(line) >= 7 and line[6] in "*/":
            continue
        if line.lstrip().startswith("*"):
            continue
        line = line.split("*>")[0]
        out.append(line)
    return "\n".join(out)


RE_CALL = re.compile(r"\bCALL\s+'?\"?([A-Z0-9][A-Z0-9-]{0,7})'?\"?", re.I)
RE_COPY = re.compile(r"\bCOPY\s+([A-Z0-9][A-Z0-9-]*)", re.I)
RE_LINK = re.compile(r"EXEC\s+CICS\s+(LINK|XCTL)\b.*?PROGRAM\s*\(\s*'?([A-Z0-9-]+)'?\s*\)", re.I | re.S)
RE_SQL = re.compile(r"EXEC\s+SQL(.*?)END-EXEC", re.I | re.S)
RE_TABLE = re.compile(r"\b(?:FROM|INTO|UPDATE|JOIN|TABLE)\s+([A-Z][A-Z0-9_]*(?:\.[A-Z][A-Z0-9_]*)?)", re.I)
RE_SELECT = re.compile(r"\bSELECT\s+([A-Z0-9-]+)\s+ASSIGN\s+TO\s+([A-Z0-9-]+)", re.I)
RE_EXECPGM = re.compile(r"EXEC\s+PGM=([A-Z0-9]+)", re.I)

SQL_NOISE = {"DUAL", "CURRENT", "NULL", "WS", "SQLCA", "DCLGEN"}

edges = []
for name, meta in programs.items():
    with open(os.path.join(os.path.dirname(ROOT), meta["path"]), errors="replace") as fh:
        raw = fh.read()
    text = strip_comments(raw)
    for m in RE_CALL.finditer(text):
        t = m.group(1).upper()
        if t in programs or not t.startswith("WS"):
            edges.append({"from": name, "to": t, "kind": "CALL", "resolved": t in programs})
    for m in RE_COPY.finditer(text):
        t = m.group(1).upper()
        edges.append({"from": name, "to": t, "kind": "COPY", "resolved": t in copybooks})
    for m in RE_LINK.finditer(text):
        t = m.group(2).upper()
        edges.append({"from": name, "to": t, "kind": m.group(1).upper(), "resolved": t in programs})
    for m in RE_SQL.finditer(text):
        body = m.group(1)
        if re.search(r"\b(INCLUDE|DECLARE\s+\w+\s+CURSOR|OPEN|CLOSE|FETCH|COMMIT|ROLLBACK|WHENEVER|CONNECT)\b", body, re.I) and not re.search(r"\bFROM\b", body, re.I):
            for inc in re.finditer(r"INCLUDE\s+([A-Z0-9-]+)", body, re.I):
                edges.append({"from": name, "to": inc.group(1).upper(), "kind": "SQL-INCLUDE", "resolved": inc.group(1).upper() in copybooks})
            continue
        for t in RE_TABLE.finditer(body):
            tbl = t.group(1).upper()
            if tbl.startswith(":") or tbl in SQL_NOISE or tbl.startswith("WS-") or tbl.startswith("SQL"):
                continue
            edges.append({"from": name, "to": tbl, "kind": "SQL", "resolved": True})
    for m in RE_SELECT.finditer(text):
        edges.append({"from": name, "to": m.group(2).upper(), "kind": "FILE", "resolved": True, "file": m.group(1).upper()})

# copybooks may embed procedural code (e.g. DBPROC contains CALL 'ERRPROC'):
# record those as edges from the copybook so the indirect dependency is visible
for name, meta in copybooks.items():
    with open(os.path.join(os.path.dirname(ROOT), meta["path"]), errors="replace") as fh:
        text = strip_comments(fh.read())
    for m in RE_CALL.finditer(text):
        t = m.group(1).upper()
        if t in programs:
            edges.append({"from": name, "to": t, "kind": "CALL", "resolved": True, "via_copybook": True})

jcl = {}
for dirpath, _, files in os.walk(os.path.join(ROOT, "jcl")):
    for f in sorted(files):
        if not f.lower().endswith(".jcl"):
            continue
        p = os.path.join(dirpath, f)
        job = f[:-4].upper()
        jcl[job] = {"path": os.path.relpath(p, os.path.dirname(ROOT))}
        with open(p, errors="replace") as fh:
            for m in RE_EXECPGM.finditer(fh.read()):
                t = m.group(1).upper()
                edges.append({"from": job, "to": t, "kind": "EXECPGM", "resolved": t in programs})

# de-duplicate preserving order
seen = set()
uniq = []
for e in edges:
    key = (e["from"], e["to"], e["kind"])
    if key not in seen:
        seen.add(key)
        uniq.append(e)

out = {"programs": programs, "copybooks": copybooks, "jcl": jcl, "edges": uniq}
dst = os.path.join(os.path.dirname(os.path.abspath(__file__)), "deps.json")
with open(dst, "w") as fh:
    json.dump(out, fh, indent=2, sort_keys=True)
print(f"{len(programs)} programs, {len(copybooks)} copybooks, {len(jcl)} JCL, {len(uniq)} edges -> {dst}", file=sys.stderr)
