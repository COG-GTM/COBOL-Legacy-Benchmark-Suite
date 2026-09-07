#!/usr/bin/env python3
"""Static dependency extractor for the COBOL Legacy Benchmark Suite.

Scans src/ and produces:
  documentation/technical/dependency-graph.json  (machine readable)
  documentation/technical/dependency-graph.md    (Mermaid diagrams + tables)

Detected relations
  program -> program      CALL 'X' / EXEC CICS LINK|XCTL PROGRAM('X')
  program -> copybook     COPY X
  program -> VSAM/QSAM    SELECT ... ASSIGN TO ddname
  program -> DB2 table    EXEC SQL ... FROM|INTO|UPDATE|JOIN|DELETE FROM table
  program -> BMS mapset   EXEC CICS SEND|RECEIVE MAP(...) MAPSET(...)
  JCL      -> program     // EXEC PGM=X
  JCL      -> dataset     //DD DD DSN=...
  CICS CSD -> program     DEFINE TRANSACTION(...) PROGRAM(...)

Usage: python3 tools/depgraph/build_dependency_graph.py [--check]
  --check  exit 1 if generated files differ from the committed ones.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "src"
OUT_DIR = ROOT / "documentation" / "technical"
OUT_JSON = OUT_DIR / "dependency-graph.json"
OUT_MD = OUT_DIR / "dependency-graph.md"

SQL_KEYWORDS = {
    "SELECT", "FROM", "WHERE", "INTO", "VALUES", "SET", "AND", "OR", "NOT",
    "NULL", "COMMIT", "ROLLBACK", "WORK", "DECLARE", "SECTION", "BEGIN",
    "END", "CURSOR", "FOR", "OPEN", "CLOSE", "FETCH", "CONNECT", "RESET",
    "WITH", "UR", "CS", "ORDER", "BY", "GROUP", "HAVING", "AS", "ON",
    "CURRENT", "TIMESTAMP", "DATE", "TIME", "SQLCA", "INCLUDE", "TO",
    "RELEASE", "ALL", "LOCK", "TABLE", "IN", "MODE", "SHARE", "EXCLUSIVE",
    "TEMP", "SESSION", "SYSIBM", "SYSDUMMY1",
}

# Standard z/OS runtime / system routines that are not project programs.
SYSTEM_ROUTINES = {"ILBOABN0", "DELAY", "CEE3ABD", "DSNTIAR", "DSNHLI"}

# Well known z/OS DD names that are not application datasets.
SYSTEM_DD = {"STEPLIB", "SYSOUT", "SYSPRINT", "SYSUDUMP", "SYSIN", "SYSTSPRT",
             "SYSTSIN", "SYSABEND", "SYSDBOUT", "SYSMDUMP", "SYSUT1", "SYSUT2"}


def strip_cobol(text: str) -> str:
    """Remove comment lines (col 7 '*' or '/' or free-form '*>') and inline text after col 72."""
    out = []
    for line in text.splitlines():
        if len(line) >= 7 and line[6] in "*/":
            continue
        stripped = line.lstrip()
        if stripped.startswith("*"):
            continue
        out.append(line[:72] if len(line) > 72 else line)
    return "\n".join(out)


def rel(p: Path) -> str:
    return p.relative_to(ROOT).as_posix()


def parse_program(path: Path) -> dict:
    raw = path.read_text(encoding="utf-8", errors="replace")
    text = strip_cobol(raw)
    flat = re.sub(r"\s+", " ", text)

    m = re.search(r"PROGRAM-ID\.\s*([A-Z0-9-]+)", flat, re.I)
    program_id = m.group(1).upper() if m else path.stem.upper()

    # Header description: first comment block lines that look like prose.
    desc = []
    for line in raw.splitlines()[:40]:
        if len(line) >= 7 and line[6] == "*":
            body = line[7:].strip(" *-=")
            if body and not re.fullmatch(r"[-=*]+", body):
                desc.append(body)
    description = " ".join(desc[:3]) if desc else ""

    all_static = {c.upper() for c in re.findall(r"\bCALL\s+['\"]([A-Z0-9-]+)['\"]", flat, re.I)}
    calls_static = sorted(all_static - SYSTEM_ROUTINES)
    system_calls = sorted(all_static & SYSTEM_ROUTINES)
    calls_dynamic = sorted(set(
        c.upper() for c in re.findall(r"\bCALL\s+([A-Z][A-Z0-9-]*)\s+(?:USING|END-CALL|\.)", flat, re.I)
        if c.upper() not in SYSTEM_ROUTINES
    ))
    cics_links = sorted(set(
        c.upper() for c in re.findall(
            r"EXEC\s+CICS\s+(?:LINK|XCTL)\s+PROGRAM\s*\(\s*['\"]([A-Z0-9-]+)['\"]", flat, re.I)
    ))
    cics_commands = sorted(set(
        c.upper() for c in re.findall(r"EXEC\s+CICS\s+([A-Z]+(?:\s+(?:MAP|CONDITION|MAPSET))?)", flat, re.I)
    ))
    copybooks = sorted(set(
        c.upper() for c in re.findall(r"\bCOPY\s+([A-Z0-9-]+)\s*\.?", flat, re.I)
    ))
    files = []
    for name, dd in re.findall(
            r"SELECT\s+([A-Z0-9-]+)\s+ASSIGN\s+TO\s+([A-Z0-9-]+)", flat, re.I):
        org = re.search(
            rf"SELECT\s+{name}\s.*?ORGANIZATION\s+(?:IS\s+)?([A-Z]+)", flat, re.I | re.S)
        files.append({"file": name.upper(), "ddname": dd.upper(),
                      "organization": org.group(1).upper() if org else "SEQUENTIAL"})

    sql_blocks = re.findall(r"EXEC\s+SQL\s+(.*?)\s+END-EXEC", flat, re.I | re.S)
    tables: dict[str, set[str]] = {}
    sql_ops = set()
    for block in sql_blocks:
        b = block.upper()
        first = b.split()[0] if b.split() else ""
        sql_ops.add(first)
        for op, pat in (
            ("READ", r"\bFROM\s+([A-Z_][A-Z0-9_]*)"),
            ("WRITE", r"\bINSERT\s+INTO\s+([A-Z_][A-Z0-9_]*)"),
            ("WRITE", r"\bUPDATE\s+([A-Z_][A-Z0-9_]*)\s+SET"),
            ("WRITE", r"\bDELETE\s+FROM\s+([A-Z_][A-Z0-9_]*)"),
            ("READ", r"\bJOIN\s+([A-Z_][A-Z0-9_]*)"),
        ):
            for t in re.findall(pat, b):
                if t in SQL_KEYWORDS or t.startswith(":"):
                    continue
                tables.setdefault(t, set()).add(op)
    db2_tables = [{"table": t, "access": "/".join(sorted(ops))} for t, ops in sorted(tables.items())]

    mapsets = sorted(set(
        m.upper() for m in re.findall(r"MAPSET\s*\(\s*['\"]([A-Z0-9]+)['\"]", flat, re.I)))
    maps = sorted(set(
        m.upper() for m in re.findall(r"\bMAP\s*\(\s*['\"]([A-Z0-9]+)['\"]", flat, re.I)))

    paragraphs = re.findall(r"^\s{7}([0-9A-Z][0-9A-Z-]*)\s*\.\s*$", text, re.M)
    paragraphs = [p for p in paragraphs if p.upper() not in {
        "PROCEDURE DIVISION", "WORKING-STORAGE SECTION", "FILE SECTION",
        "LINKAGE SECTION", "IDENTIFICATION DIVISION", "ENVIRONMENT DIVISION",
        "DATA DIVISION", "INPUT-OUTPUT SECTION", "FILE-CONTROL", "CONFIGURATION SECTION"}]

    return {
        "program": program_id,
        "path": rel(path),
        "category": path.parent.name,
        "lines": len(raw.splitlines()),
        "empty": not raw.strip(),
        "description": description,
        "kind": "cics" if cics_commands else ("db2" if sql_blocks else "batch"),
        "calls": calls_static,
        "system_calls": system_calls,
        "dynamic_calls": calls_dynamic,
        "cics_links": cics_links,
        "cics_commands": cics_commands,
        "copybooks": copybooks,
        "files": files,
        "db2_tables": db2_tables,
        "sql_statements": sorted(s for s in sql_ops if s),
        "bms_mapsets": mapsets,
        "bms_maps": maps,
        "paragraphs": paragraphs,
        "uses_linkage": bool(re.search(r"LINKAGE\s+SECTION", flat, re.I)),
    }


def parse_copybook(path: Path) -> dict:
    raw = path.read_text(encoding="utf-8", errors="replace")
    flat = re.sub(r"\s+", " ", strip_cobol(raw))
    return {
        "copybook": path.stem.upper(),
        "path": rel(path),
        "category": path.parent.name,
        "lines": len(raw.splitlines()),
        "includes": sorted(set(c.upper() for c in re.findall(r"\bCOPY\s+([A-Z0-9-]+)", flat, re.I))),
        "level01": sorted(set(re.findall(r"\b01\s+([A-Z][A-Z0-9-]*)", flat, re.I))),
        "has_procedure_code": bool(re.search(r"\b(PERFORM|EXEC SQL|MOVE|IF)\b", flat, re.I)),
    }


def parse_jcl(path: Path) -> dict:
    raw = path.read_text(encoding="utf-8", errors="replace")
    lines = [l for l in raw.splitlines() if not l.startswith("//*")]
    text = "\n".join(lines)
    steps = []
    for m in re.finditer(r"^//(\S+)\s+EXEC\s+PGM=([A-Z0-9]+)", text, re.M):
        steps.append({"step": m.group(1), "program": m.group(2).upper()})
    dds = []
    for m in re.finditer(r"^//(\S+)\s+DD\s+(.*?)(?=^//|\Z)", text, re.M | re.S):
        dd, body = m.group(1), m.group(2)
        if dd in SYSTEM_DD:
            continue
        dsn = re.search(r"DSN=([A-Z0-9.&()]+)", body)
        dds.append({"ddname": dd, "dsn": dsn.group(1) if dsn else None})
    return {"jcl": path.stem.upper(), "path": rel(path), "category": path.parent.name,
            "steps": steps, "datasets": dds}


def parse_csd(path: Path) -> dict:
    raw = path.read_text(encoding="utf-8", errors="replace")
    flat = re.sub(r"\s+", " ", raw)
    transactions = [{"transaction": t, "program": p} for t, p in re.findall(
        r"DEFINE TRANSACTION\((\w+)\) PROGRAM\((\w+)\)", flat)]
    programs = re.findall(r"DEFINE PROGRAM\((\w+)\)", flat)
    mapsets = re.findall(r"DEFINE MAPSET\((\w+)\)", flat)
    files = re.findall(r"DEFINE FILE\((\w+)\)", flat)
    return {"path": rel(path), "transactions": transactions, "programs": programs,
            "mapsets": mapsets, "files": files}


def parse_db2(paths: list[Path]) -> list[dict]:
    tables = []
    for p in paths:
        raw = p.read_text(encoding="utf-8", errors="replace")
        for t in re.findall(r"CREATE\s+TABLE\s+([A-Z_][A-Z0-9_]*)", raw, re.I):
            tables.append({"table": t.upper(), "path": rel(p)})
    return tables


def build_model() -> dict:
    programs = [parse_program(p) for p in sorted(SRC.glob("programs/**/*.cbl"))]
    copybooks = [parse_copybook(p) for p in sorted(SRC.glob("copybook/**/*.cpy"))]
    jcl = [parse_jcl(p) for p in sorted(SRC.glob("jcl/**/*.jcl"))]
    csd = parse_csd(SRC / "cics" / "PORTDFN.csd")
    db2 = parse_db2(sorted(SRC.glob("database/db2/*.sql")))

    known_programs = {p["program"] for p in programs}
    known_copybooks = {c["copybook"] for c in copybooks}
    known_tables = {t["table"] for t in db2}

    called_by: dict[str, set[str]] = {}
    for p in programs:
        for target in p["calls"] + p["cics_links"]:
            called_by.setdefault(target, set()).add(p["program"])
    for j in jcl:
        for s in j["steps"]:
            called_by.setdefault(s["program"], set()).add(f"JCL:{j['jcl']}")
    for t in csd["transactions"]:
        called_by.setdefault(t["program"], set()).add(f"CICS:{t['transaction']}")

    for p in programs:
        p["called_by"] = sorted(called_by.get(p["program"], set()))
        p["unresolved_calls"] = sorted(
            c for c in p["calls"] + p["cics_links"]
            if c not in known_programs and c not in SYSTEM_ROUTINES)
        p["unresolved_copybooks"] = sorted(c for c in p["copybooks"] if c not in known_copybooks)
        p["unresolved_tables"] = sorted(
            t["table"] for t in p["db2_tables"] if t["table"] not in known_tables)

    copy_used_by: dict[str, set[str]] = {}
    for p in programs:
        for c in p["copybooks"]:
            copy_used_by.setdefault(c, set()).add(p["program"])
    for c in copybooks:
        c["used_by"] = sorted(copy_used_by.get(c["copybook"], set()))

    return {
        "generated_by": "tools/depgraph/build_dependency_graph.py",
        "programs": programs,
        "copybooks": copybooks,
        "jcl": jcl,
        "cics": csd,
        "db2_tables": db2,
        "summary": {
            "programs": len(programs),
            "copybooks": len(copybooks),
            "jcl_jobs": len(jcl),
            "db2_tables": len(db2),
            "empty_programs": [p["program"] for p in programs if p["empty"]],
            "unresolved_calls": sorted({c for p in programs for c in p["unresolved_calls"]}),
            "unresolved_copybooks": sorted({c for p in programs for c in p["unresolved_copybooks"]}),
            "unresolved_tables": sorted({t for p in programs for t in p["unresolved_tables"]}),
            "orphan_programs": sorted(
                p["program"] for p in programs
                if not p["called_by"] and p["category"] not in ("online",)),
        },
    }


# ----------------------------------------------------------------- Mermaid --

def mid(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9_]", "_", name)


def mermaid_program_graph(model: dict) -> str:
    programs = model["programs"]
    by_cat: dict[str, list[dict]] = {}
    for p in programs:
        by_cat.setdefault(p["category"], []).append(p)
    lines = ["graph LR"]
    for cat, progs in sorted(by_cat.items()):
        lines.append(f'  subgraph {cat}')
        for p in progs:
            label = f"{p['program']}<br/>(vacío)" if p["empty"] else p["program"]
            lines.append(f'    {mid(p["program"])}["{label}"]')
        lines.append("  end")
    lines.append('  subgraph entrypoints["Puntos de entrada"]')
    for j in model["jcl"]:
        if any(s["program"] in {p["program"] for p in programs} for s in j["steps"]):
            lines.append(f'    JCL_{mid(j["jcl"])}[/"JCL {j["jcl"]}"/]')
    for t in model["cics"]["transactions"]:
        lines.append(f'    TRX_{t["transaction"]}[/"CICS {t["transaction"]}"/]')
    lines.append("  end")
    known = {p["program"] for p in programs}
    for p in programs:
        for c in p["calls"]:
            if c in known:
                lines.append(f'  {mid(p["program"])} -->|CALL| {mid(c)}')
        for c in p["cics_links"]:
            if c in known:
                lines.append(f'  {mid(p["program"])} -.->|LINK| {mid(c)}')
    for j in model["jcl"]:
        for s in j["steps"]:
            if s["program"] in known:
                lines.append(f'  JCL_{mid(j["jcl"])} ==> {mid(s["program"])}')
    for t in model["cics"]["transactions"]:
        lines.append(f'  TRX_{t["transaction"]} ==> {mid(t["program"])}')
    return "\n".join(lines)


def mermaid_data_graph(model: dict) -> str:
    lines = ["graph LR"]
    files, tables, sets = set(), set(), set()
    for p in model["programs"]:
        for f in p["files"]:
            files.add(f["ddname"])
        for t in p["db2_tables"]:
            tables.add(t["table"])
        for s in p["bms_mapsets"]:
            sets.add(s)
    lines.append('  subgraph vsam["VSAM / QSAM (DDNAME)"]')
    for f in sorted(files):
        lines.append(f'    F_{mid(f)}[("{f}")]')
    lines.append("  end")
    lines.append('  subgraph db2["DB2"]')
    for t in sorted(tables):
        lines.append(f'    T_{mid(t)}[("{t}")]')
    lines.append("  end")
    lines.append('  subgraph bms["BMS"]')
    for s in sorted(sets):
        lines.append(f'    M_{mid(s)}>"{s}"]')
    lines.append("  end")
    for p in model["programs"]:
        if not (p["files"] or p["db2_tables"] or p["bms_mapsets"]):
            continue
        lines.append(f'  {mid(p["program"])}["{p["program"]}"]')
        for f in p["files"]:
            lines.append(f'  {mid(p["program"])} --> F_{mid(f["ddname"])}')
        for t in p["db2_tables"]:
            lines.append(f'  {mid(p["program"])} -->|{t["access"]}| T_{mid(t["table"])}')
        for s in p["bms_mapsets"]:
            lines.append(f'  {mid(p["program"])} <--> M_{mid(s)}')
    return "\n".join(lines)


def mermaid_copybook_graph(model: dict) -> str:
    lines = ["graph TD"]
    for c in model["copybooks"]:
        if c["used_by"]:
            lines.append(f'  C_{mid(c["copybook"])}[["{c["copybook"]}"]]')
    for p in model["programs"]:
        if p["copybooks"]:
            lines.append(f'  {mid(p["program"])}["{p["program"]}"]')
            for c in p["copybooks"]:
                lines.append(f'  {mid(p["program"])} --> C_{mid(c)}')
    return "\n".join(lines)


def md_table(headers: list[str], rows: list[list[str]]) -> str:
    out = ["| " + " | ".join(headers) + " |", "|" + "|".join(" --- " for _ in headers) + "|"]
    for r in rows:
        out.append("| " + " | ".join(str(x) if x else "—" for x in r) + " |")
    return "\n".join(out)


def program_doc_link(p: dict) -> str:
    return f"[{p['program']}](../programs/{p['program']}.md)"


def render_markdown(model: dict) -> str:
    s = model["summary"]
    parts = [
        "# Grafo de dependencias — COBOL Legacy Benchmark Suite",
        "",
        "> Generado automáticamente por `tools/depgraph/build_dependency_graph.py` a partir de `src/`.",
        "> No editar a mano: ejecutar `python3 tools/depgraph/build_dependency_graph.py` para regenerar.",
        "> La documentación funcional de cada programa está en [`documentation/programs/`](../programs/README.md).",
        "",
        "## Resumen",
        "",
        md_table(["Métrica", "Valor"], [
            ["Programas COBOL", s["programs"]],
            ["Copybooks", s["copybooks"]],
            ["Jobs JCL", s["jcl_jobs"]],
            ["Tablas DB2 (DDL)", s["db2_tables"]],
            ["Programas vacíos", ", ".join(s["empty_programs"])],
            ["CALL/LINK sin resolver (no existe el programa en `src/`)", ", ".join(s["unresolved_calls"])],
            ["COPY sin resolver (no existe el copybook en `src/`)", ", ".join(s["unresolved_copybooks"])],
            ["Tablas DB2 sin DDL", ", ".join(s["unresolved_tables"])],
            ["Programas sin invocador conocido (ni CALL, ni JCL, ni CICS)", ", ".join(s["orphan_programs"])],
        ]),
        "",
        "## 1. Grafo de llamadas entre programas",
        "",
        "Flechas continuas = `CALL` estático; discontinuas = `EXEC CICS LINK`; gruesas = punto de entrada (JCL `EXEC PGM=` o transacción CICS).",
        "",
        "```mermaid", mermaid_program_graph(model), "```",
        "",
        "## 2. Acceso a datos (VSAM, DB2, BMS)",
        "",
        "```mermaid", mermaid_data_graph(model), "```",
        "",
        "## 3. Uso de copybooks",
        "",
        "```mermaid", mermaid_copybook_graph(model), "```",
        "",
        "## 4. Detalle por programa",
        "",
    ]
    rows = []
    for p in model["programs"]:
        rows.append([
            program_doc_link(p), p["category"], p["lines"],
            ", ".join(p["calls"] + [f"{c} (LINK)" for c in p["cics_links"]]),
            ", ".join(p["called_by"]),
            ", ".join(p["copybooks"]),
            ", ".join(f"{f['ddname']} ({f['organization']})" for f in p["files"]),
            ", ".join(f"{t['table']} ({t['access']})" for t in p["db2_tables"]),
            ", ".join(p["bms_mapsets"]),
        ])
    parts.append(md_table(
        ["Programa", "Categoría", "Líneas", "Invoca a", "Invocado por", "Copybooks", "Ficheros (DD)", "Tablas DB2", "BMS"],
        rows))
    parts += ["", "## 5. Copybooks", ""]
    parts.append(md_table(
        ["Copybook", "Categoría", "Ruta", "Usado por", "Contiene lógica"],
        [[c["copybook"], c["category"], f"`{c['path']}`", ", ".join(c["used_by"]),
          "sí" if c["has_procedure_code"] else "no"] for c in model["copybooks"]]))
    parts += ["", "## 6. Jobs JCL", ""]
    parts.append(md_table(
        ["JCL", "Categoría", "Pasos (PGM)", "Datasets"],
        [[j["jcl"], j["category"],
          ", ".join(f"{st['step']}={st['program']}" for st in j["steps"]),
          ", ".join(f"{d['ddname']}→{d['dsn'] or 'SYSOUT'}" for d in j["datasets"])]
         for j in model["jcl"]]))
    parts += ["", "## 7. Recursos CICS (PORTDFN.csd)", ""]
    parts.append(md_table(
        ["Transacción", "Programa inicial"],
        [[t["transaction"], t["program"]] for t in model["cics"]["transactions"]]))
    parts.append("")
    parts.append("Programas definidos: " + ", ".join(model["cics"]["programs"])
                 + f". Mapsets: {', '.join(model['cics']['mapsets'])}. Ficheros: {', '.join(model['cics']['files'])}.")
    parts += ["", "## 8. Tablas DB2 (DDL)", ""]
    parts.append(md_table(["Tabla", "DDL"], [[t["table"], f"`{t['path']}`"] for t in model["db2_tables"]]))
    parts.append("")
    return "\n".join(parts)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()

    model = build_model()
    js = json.dumps(model, indent=2, ensure_ascii=False, sort_keys=True) + "\n"
    md = render_markdown(model)

    if args.check:
        ok = OUT_JSON.exists() and OUT_JSON.read_text() == js and OUT_MD.exists() and OUT_MD.read_text() == md
        print("up to date" if ok else "OUT OF DATE: run tools/depgraph/build_dependency_graph.py")
        return 0 if ok else 1

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(js)
    OUT_MD.write_text(md)
    print(f"wrote {rel(OUT_JSON)} and {rel(OUT_MD)}")
    print(json.dumps(model["summary"], indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
