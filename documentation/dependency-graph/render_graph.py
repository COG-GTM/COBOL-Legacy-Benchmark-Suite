#!/usr/bin/env python3
"""Render README.md (Mermaid) and graph.dot from deps.json."""
import json
import os
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(HERE, "deps.json")))
programs, copybooks, jcl, edges = d["programs"], d["copybooks"], d["jcl"], d["edges"]

GROUP_LABEL = {
    "batch": "Batch",
    "common": "Common (subrutinas)",
    "online": "Online (CICS)",
    "portfolio": "Portfolio",
    "test": "Test",
    "utility": "Utility",
}
KIND_ARROW = {
    "CALL": "-->",
    "LINK": "-.->",
    "XCTL": "-.->",
    "EXECPGM": "==>",
    "SQL": "-->",
    "SQL-INCLUDE": "-.->",
    "COPY": "-.->",
    "FILE": "-->",
}


PREFIX = {"program": "P_", "copybook": "C_", "table": "T_", "file": "F_", "jcl": "J_"}


def src_kind(e):
    if e["kind"] == "EXECPGM":
        return "jcl"
    if e.get("via_copybook"):
        return "copybook"
    return "program"


def dst_kind(e):
    k = e["kind"]
    if k in ("COPY", "SQL-INCLUDE"):
        return "copybook"
    if k == "SQL":
        return "table"
    if k == "FILE":
        return "file"
    return "program"


def nid(name, kind):
    return PREFIX[kind] + name.replace("-", "_").replace(".", "_")


def node(name, kind):
    n = nid(name, kind)
    if kind == "program":
        return f'{n}["{name}"]'
    if kind == "copybook":
        return f'{n}[["{name}"]]'
    if kind == "table":
        return f'{n}[("{name}")]'
    if kind == "file":
        return f'{n}[/"{name}"/]'
    if kind == "jcl":
        return f'{n}{{{{"{name}"}}}}'
    return f'{n}["{name}"]'


def mermaid(edge_kinds, include_nodes=None, direction="LR"):
    lines = [f"flowchart {direction}"]
    sel = [e for e in edges if e["kind"] in edge_kinds]
    if include_nodes is not None:
        sel = [e for e in sel if e["from"] in include_nodes or e["to"] in include_nodes]
    used = set()
    for e in sel:
        used.add((src_kind(e), e["from"]))
        used.add((dst_kind(e), e["to"]))
    by_group = defaultdict(list)
    for k, n in sorted(used, key=lambda x: (x[0], x[1])):
        if k == "program" and n in programs:
            by_group[programs[n]["group"]].append(node(n, k))
        elif k == "program":
            by_group["external"].append(node(n, k))
        else:
            by_group[{"jcl": "jcl", "copybook": "copybooks", "table": "db2", "file": "files"}[k]].append(node(n, k))
    titles = {
        "jcl": "JCL", "copybooks": "Copybooks", "db2": "Tablas DB2",
        "files": "Ficheros (DDNAME)", "external": "Externos / no resueltos",
    }
    for g in list(GROUP_LABEL) + ["jcl", "copybooks", "db2", "files", "external"]:
        if by_group.get(g):
            lines.append(f"  subgraph sg_{g}[\"{GROUP_LABEL.get(g, titles.get(g, g))}\"]")
            lines += [f"    {x}" for x in by_group[g]]
            lines.append("  end")
    def src(e):
        return nid(e["from"], src_kind(e))

    def dst(e):
        return nid(e["to"], dst_kind(e))

    for e in sel:
        lines.append(f"  {src(e)} {KIND_ARROW[e['kind']]}|{e['kind']}| {dst(e)}")
    lines.append("  classDef unresolved stroke-dasharray: 5 5,stroke:#c00;")
    unres = sorted({dst(e) for e in sel if not e["resolved"]})
    if unres:
        lines.append("  class " + ",".join(unres) + " unresolved;")
    return "\n".join(lines)


def table_programs():
    out = ["| Grupo | Programa | Fuente | Llama a | Llamado por | Documentación |", "|---|---|---|---|---|---|"]
    for name in sorted(programs, key=lambda n: (programs[n]["group"], n)):
        g = programs[name]["group"]
        calls = sorted({e["to"] for e in edges if e["from"] == name and e["kind"] in ("CALL", "LINK", "XCTL")})
        callers = sorted({e["from"] for e in edges if e["to"] == name and e["kind"] in ("CALL", "LINK", "XCTL", "EXECPGM")})
        out.append(
            f"| {g} | `{name}` | [{os.path.basename(programs[name]['path'])}](../../{programs[name]['path']}) | "
            f"{', '.join(calls) or '—'} | {', '.join(callers) or '—'} | [{name}.md](../programs/{g}/{name}.md) |"
        )
    return "\n".join(out)


def main():
    counts = defaultdict(int)
    for e in edges:
        counts[e["kind"]] += 1
    unresolved = sorted({(e["kind"], e["from"], e["to"]) for e in edges if not e["resolved"]})
    md = f"""# Grafo de dependencias — CLBS (COBOL Legacy Benchmark Suite)

Generado automáticamente por `extract_deps.py` (extracción estática) y `render_graph.py` (render).
Regenerar: `python3 documentation/dependency-graph/extract_deps.py && python3 documentation/dependency-graph/render_graph.py`.

Inventario: **{len(programs)} programas**, **{len(copybooks)} copybooks**, **{len(jcl)} JCL**, **{len(edges)} aristas**
({', '.join(f'{k}: {v}' for k, v in sorted(counts.items()))}).

Leyenda de aristas: `-->|CALL|` llamada COBOL estática · `-.->|LINK|` EXEC CICS LINK/XCTL · `==>|EXECPGM|` paso JCL
· `-.->|COPY|` copybook · `-->|SQL|` tabla DB2 · `-->|FILE|` fichero VSAM/QSAM (DDNAME).
Una arista `CALL` cuyo origen es un copybook (p. ej. `DBPROC → ERRPROC`) indica código procedimental incluido: todo programa que copie ese copybook hereda la dependencia.
Nodos con borde rojo discontinuo = destino no resuelto en el repositorio (rutinas del sistema como IDCAMS, ILBOABN0).

La documentación funcional de cada programa está en [`documentation/programs/<grupo>/`](../programs/).

## 1. Flujo de control entre programas (CALL / CICS LINK / JCL)

```mermaid
{mermaid({"CALL", "LINK", "XCTL", "EXECPGM"})}
```

## 2. Acceso a datos (tablas DB2 y ficheros VSAM/QSAM)

```mermaid
{mermaid({"SQL", "FILE"})}
```

## 3. Uso de copybooks

```mermaid
{mermaid({"COPY", "SQL-INCLUDE"})}
```

## 4. Índice de programas

{table_programs()}

## 5. Destinos no resueltos

{chr(10).join(f'- `{k}` `{f}` → `{t}`' for k, f, t in unresolved) or '- Ninguno'}

## Archivos

- `deps.json` — aristas y nodos (fuente de verdad)
- `graph.dot` — mismo grafo en formato Graphviz (`dot -Tsvg graph.dot -o graph.svg`)
"""
    open(os.path.join(HERE, "README.md"), "w").write(md)

    dot = ["digraph CLBS {", "  rankdir=LR; node [fontname=Helvetica];"]
    shape = {"program": "box", "copybook": "note", "table": "cylinder", "file": "parallelogram", "jcl": "hexagon"}
    nodes = {}
    for e in edges:
        nodes[(src_kind(e), e["from"])] = True
        nodes[(dst_kind(e), e["to"])] = True
    for k, n in sorted(nodes):
        extra = ' style=dashed color=red' if k == "program" and n not in programs else ''
        dot.append(f'  "{PREFIX[k]}{n}" [label="{n}" shape={shape[k]}{extra}];')
    for e in edges:
        style = "dashed" if e["kind"] in ("COPY", "SQL-INCLUDE", "LINK", "XCTL") else "solid"
        dot.append(f'  "{nid(e["from"], src_kind(e))}" -> "{nid(e["to"], dst_kind(e))}" [label="{e["kind"]}" style={style}];')
    dot.append("}")
    open(os.path.join(HERE, "graph.dot"), "w").write("\n".join(dot) + "\n")


if __name__ == "__main__":
    main()
