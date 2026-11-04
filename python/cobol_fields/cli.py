"""Command line: inspect layouts, dump record files, diff two record files.

    python -m cobol_fields layout src/copybook/common/POSREC.cpy
    python -m cobol_fields dump   src/copybook/common/POSREC.cpy positions.dat --dialect gnucobol
    python -m cobol_fields diff   src/copybook/common/POSREC.cpy cobol.dat python.dat
"""
from __future__ import annotations

import argparse
import json
import sys
from decimal import Decimal
from pathlib import Path

from .copybook import load_layout
from .diff import diff_files, iter_records
from .fields import DIALECTS, Dialect



def _dialect(args) -> Dialect:
    d = DIALECTS[args.dialect]
    if args.encoding:
        d = Dialect(d.name, d.binary_size, args.encoding, d.binary_truncate)
    return d


def _json_default(o):
    if isinstance(o, Decimal):
        return str(o)
    raise TypeError(type(o))


def cmd_layout(args) -> int:
    layout = load_layout(args.copybook, args.record, _dialect(args))
    print(layout.describe())
    return 0


def cmd_dump(args) -> int:
    layout = load_layout(args.copybook, args.record, _dialect(args))
    data = Path(args.file).read_bytes()
    if len(data) % layout.length:
        print(f"warning: {len(data)} bytes is not a multiple of record length {layout.length}",
              file=sys.stderr)
    records = []
    for i, raw in enumerate(iter_records(data, layout.length), 1):
        rec = layout.decode(raw, strict=not args.lenient)
        if args.json:
            records.append(rec)
        else:
            print(f"--- record {i}")
            for k, v in rec.items():
                shown = repr(v) if isinstance(v, str) else v
                print(f"  {k:<40} {shown}")
    if args.json:
        json.dump(records, sys.stdout, default=_json_default, indent=2)
        print()
    return 0


def cmd_diff(args) -> int:
    layout = load_layout(args.copybook, args.record, _dialect(args))
    rep = diff_files(layout, args.expected, args.actual, strict=False, ignore=args.ignore or ())
    for d in rep.diffs[: args.max]:
        print(d)
    if len(rep.diffs) > args.max:
        print(f"... {len(rep.diffs) - args.max} more")
    print(rep.summary())
    return 0 if rep.ok else 1


def main(argv=None) -> int:
    p = argparse.ArgumentParser(prog="cobol_fields", description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--dialect", choices=DIALECTS, default="ibm",
                        help="ibm: COMP 2/4/8 bytes, EBCDIC cp037 (default). gnucobol: 1/2/4/8, ASCII. "
                             "gnucobol-ibm: cobc -std=ibm (2/4/8 bytes, ASCII)")
    common.add_argument("--encoding", help="override text encoding, e.g. cp037, cp1047, ascii, latin-1")
    common.add_argument("--record", help="01-level name when the copybook defines several")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("layout", parents=[common], help="print offsets, lengths and definitions")
    s.add_argument("copybook")
    s.set_defaults(fn=cmd_layout)

    s = sub.add_parser("dump", parents=[common], help="decode a record file")
    s.add_argument("copybook")
    s.add_argument("file")
    s.add_argument("--json", action="store_true")
    s.add_argument("--lenient", action="store_true", help="do not fail on invalid sign/zone bytes")
    s.set_defaults(fn=cmd_dump)

    s = sub.add_parser("diff", parents=[common], help="compare two record files field by field (exit 1 on mismatch)")
    s.add_argument("copybook")
    s.add_argument("expected", help="reference file, normally written by the COBOL program")
    s.add_argument("actual", help="file written by the migrated code")
    s.add_argument("--ignore", action="append", metavar="FIELD",
                   help="field to skip (e.g. an audit timestamp); repeatable")
    s.add_argument("--max", type=int, default=50, help="max mismatches to print")
    s.set_defaults(fn=cmd_diff)

    args = p.parse_args(argv)
    return args.fn(args)
