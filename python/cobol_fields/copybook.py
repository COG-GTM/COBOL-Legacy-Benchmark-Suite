"""Copybook parser: COBOL record description -> flat field layout.

Handles the constructs this suite's copybooks use: level numbers, group
items, FILLER, PIC/PICTURE, USAGE (COMP, COMP-3, COMP-5, COMP-X, BINARY,
PACKED-DECIMAL, DISPLAY), SIGN LEADING/TRAILING [SEPARATE], OCCURS n TIMES,
REDEFINES, VALUE (ignored), 66/77/88 levels, comments, and fixed-format
sequence/indicator columns.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field as dc_field
from decimal import Decimal
from pathlib import Path
from typing import Any, Iterator

from .fields import CobolField, Dialect, IBM, SignMode, Usage
from .pic import PicKind, parse_pic

_USAGE_WORDS = {
    "COMP", "COMP-1", "COMP-2", "COMP-3", "COMP-4", "COMP-5", "COMP-X",
    "COMPUTATIONAL", "COMPUTATIONAL-1", "COMPUTATIONAL-2", "COMPUTATIONAL-3",
    "COMPUTATIONAL-4", "COMPUTATIONAL-5", "COMPUTATIONAL-X",
    "BINARY", "PACKED-DECIMAL", "DISPLAY",
}


@dataclass
class Item:
    level: int
    name: str
    pic: str | None = None
    usage: str | None = None
    sign: SignMode | None = None        # None = not specified; inherited from the group
    occurs: int = 1
    redefines: str | None = None
    children: list["Item"] = dc_field(default_factory=list)
    parent: "Item | None" = dc_field(default=None, repr=False)
    line_no: int = 0

    @property
    def is_group(self) -> bool:
        return bool(self.children)


@dataclass(frozen=True)
class LayoutField:
    """An elementary field placed at a byte offset inside a record."""
    name: str          # qualified name, e.g. POS-DATA.POS-QUANTITY or X(2)
    field: CobolField
    offset: int
    redefines: bool = False

    @property
    def length(self) -> int:
        return self.field.length

    @property
    def end(self) -> int:
        return self.offset + self.length

    @property
    def short_name(self) -> str:
        return self.name.rsplit(".", 1)[-1]


class RecordLayout:
    """Flattened elementary fields of one 01-level record."""

    def __init__(self, name: str, fields: list[LayoutField], length: int, dialect: Dialect):
        self.name = name
        self.fields = fields
        self.length = length
        self.dialect = dialect
        self._by_name: dict[str, LayoutField] = {}
        counts: dict[str, int] = {}
        for f in fields:
            counts[f.short_name] = counts.get(f.short_name, 0) + 1
        for f in fields:
            self._by_name[f.name] = f
            # allow unqualified lookup when it is unambiguous
            if counts[f.short_name] == 1:
                self._by_name[f.short_name] = f
        #: key used in decode() output: short name unless it is ambiguous
        self._key = {f.name: (f.short_name if counts[f.short_name] == 1 else f.name) for f in fields}

    def __iter__(self) -> Iterator[LayoutField]:
        return iter(self.fields)

    def __getitem__(self, name: str) -> LayoutField:
        return self._by_name[name]

    @property
    def data_fields(self) -> list[LayoutField]:
        """Fields that own storage (REDEFINES views excluded)."""
        return [f for f in self.fields if not f.redefines]

    def decode(self, raw: bytes, *, strict: bool = True, encoding: str | None = None,
               include_redefines: bool = False) -> dict[str, Any]:
        if len(raw) != self.length:
            raise ValueError(f"{self.name}: record is {len(raw)} bytes, layout is {self.length}")
        out: dict[str, Any] = {}
        for f in self.fields:
            if f.redefines and not include_redefines:
                continue
            out[self._key[f.name]] = f.field.decode(raw[f.offset:f.end], strict=strict, encoding=encoding)
        return out

    def encode(self, values: dict[str, Any], *, truncate: bool = False,
               encoding: str | None = None) -> bytes:
        """Build a record. Missing fields get COBOL INITIALIZE values
        (spaces for text, zero for numerics)."""
        buf = bytearray(self.length)
        unknown = set(values) - set(self._by_name)
        if unknown:
            raise KeyError(f"{self.name}: unknown field(s) {sorted(unknown)}")

        def supplied(f: LayoutField):
            if f.name in values:
                return True, values[f.name]
            if f.short_name in values and self._by_name.get(f.short_name) is f:
                return True, values[f.short_name]
            return False, None

        # storage-owning fields first (defaults = COBOL INITIALIZE) ...
        for f in self.data_fields:
            given, v = supplied(f)
            if not given:
                v = Decimal(0) if f.field.is_numeric else ""
            buf[f.offset:f.end] = f.field.encode(v, truncate=truncate, encoding=encoding)
        # ... then REDEFINES views that were supplied explicitly overwrite their bytes,
        # unless an owning field covering the same bytes was also supplied.
        for f in self.fields:
            if not f.redefines:
                continue
            given, v = supplied(f)
            if not given:
                continue
            for g in self.data_fields:
                if g.offset < f.end and f.offset < g.end and supplied(g)[0]:
                    raise ValueError(f"{self.name}: {f.name} and {g.name} overlap; supply only one view")
            buf[f.offset:f.end] = f.field.encode(v, truncate=truncate, encoding=encoding)
        return bytes(buf)

    def describe(self) -> str:
        rows = [("OFFSET", "LEN", "FIELD", "DEFINITION")]
        for f in self.fields:
            tag = " (REDEFINES)" if f.redefines else ""
            rows.append((str(f.offset), str(f.length), f.name, f.field.describe() + tag))
        w = [max(len(r[i]) for r in rows) for i in range(4)]
        lines = [f"{self.name}: {self.length} bytes"]
        for r in rows:
            lines.append("  ".join(c.ljust(w[i]) for i, c in enumerate(r)).rstrip())
        return "\n".join(lines)


# --------------------------------------------------------------------------
# tokenising
# --------------------------------------------------------------------------
_SEQ_AREA = re.compile(r"^.{6}([ *\-/D$])")


def _logical_lines(text: str) -> Iterator[tuple[int, str]]:
    """Strip fixed-format columns and comments; yield (line_no, code)."""
    for i, line in enumerate(text.splitlines(), 1):
        line = line.rstrip("\n").rstrip("\r")
        if not line.strip():
            continue
        code = line
        m = _SEQ_AREA.match(line)
        if m and (line[:6].strip() == "" or line[:6].isdigit()):
            ind = m.group(1)
            if ind in "*/$":
                continue
            code = line[7:72] if len(line) > 72 and line[72:].strip() and not line[72:].strip(" ").startswith(".") else line[7:]
        stripped = code.strip()
        if stripped.startswith("*") or stripped.startswith("*>"):
            continue
        if "*>" in code:
            code = code.split("*>", 1)[0]
        yield i, code


def _sentences(text: str) -> Iterator[tuple[int, list[str]]]:
    """Group logical lines into period-terminated sentences, tokenised."""
    buf: list[str] = []
    start = 0
    for line_no, code in _logical_lines(text):
        if not buf:
            start = line_no
        # protect PIC strings such as 9(5).99 or $$,$$9.99 from being split on '.'
        tokens = re.findall(r"'[^']*'|\"[^\"]*\"|[^\s.]+(?:\.(?=[^\s]))*[^\s.]*|\.", code)
        for tok in tokens:
            if tok == ".":
                if buf:
                    yield start, buf
                buf = []
            else:
                buf.append(tok)
    if buf:
        yield start, buf


def _parse_item(line_no: int, tokens: list[str]) -> Item | None:
    try:
        level = int(tokens[0])
    except ValueError:
        raise ValueError(f"line {line_no}: expected a level number, got {tokens[0]!r}") from None
    if level in (66, 88):
        return None
    name = "FILLER"
    rest = tokens[1:]
    if rest and rest[0].upper() not in _USAGE_WORDS and rest[0].upper() not in (
            "PIC", "PICTURE", "USAGE", "OCCURS", "REDEFINES", "VALUE", "VALUES", "SIGN", "JUSTIFIED",
            "JUST", "SYNC", "SYNCHRONIZED", "BLANK"):
        name = rest[0].upper()
        rest = rest[1:]
    item = Item(level=level, name=name, line_no=line_no)
    i = 0
    up = [t.upper() for t in rest]
    while i < len(up):
        t = up[i]
        if t in ("PIC", "PICTURE"):
            i += 1
            if up[i] == "IS":
                i += 1
            item.pic = rest[i]
        elif t == "USAGE":
            i += 1
            if up[i] == "IS":
                i += 1
            item.usage = up[i]
        elif t in _USAGE_WORDS:
            item.usage = t
        elif t == "OCCURS":
            i += 1
            item.occurs = int(up[i])
            # OCCURS n TO m TIMES DEPENDING ON x: fixed-size the maximum
            if i + 1 < len(up) and up[i + 1] == "TO":
                item.occurs = int(up[i + 2])
                i += 2
        elif t == "REDEFINES":
            i += 1
            item.redefines = up[i]
        elif t == "SIGN":
            i += 1
            if up[i] == "IS":
                i += 1
            leading = up[i] == "LEADING"
            separate = i + 1 < len(up) and up[i + 1] == "SEPARATE"
            if separate:
                i += 1
                if i + 1 < len(up) and up[i + 1] == "CHARACTER":
                    i += 1
            item.sign = {
                (True, True): SignMode.LEADING_SEPARATE,
                (True, False): SignMode.LEADING,
                (False, True): SignMode.TRAILING_SEPARATE,
                (False, False): SignMode.TRAILING,
            }[(leading, separate)]
        elif t in ("VALUE", "VALUES"):
            # skip the literal(s); VALUE does not affect layout
            i += 1
            while i + 1 < len(up) and up[i + 1] in ("THRU", "THROUGH"):
                i += 2
        # TIMES, INDEXED BY, ASCENDING KEY, SYNC, JUST, BLANK WHEN ZERO: no layout effect
        i += 1
    return item


def parse_items(text: str) -> list[Item]:
    """Parse copybook text into a forest of 01-level items."""
    roots: list[Item] = []
    stack: list[Item] = []
    for line_no, tokens in _sentences(text):
        if not tokens or not tokens[0].isdigit():
            continue  # e.g. a stray COPY/EJECT/SKIP line
        item = _parse_item(line_no, tokens)
        if item is None:
            continue
        if item.level in (1, 77):
            roots.append(item)
            stack = [item]
            continue
        while stack and stack[-1].level >= item.level:
            stack.pop()
        if not stack:
            raise ValueError(f"line {line_no}: level {item.level:02d} {item.name} has no parent")
        item.parent = stack[-1]
        stack[-1].children.append(item)
        stack.append(item)
    return roots


# --------------------------------------------------------------------------
# layout
# --------------------------------------------------------------------------
def _elementary(item: Item, dialect: Dialect) -> CobolField:
    pic = item.pic
    usage = item.usage or "DISPLAY"
    if pic is None:
        u = Usage.parse(usage)
        if u in (Usage.COMP_1, Usage.COMP_2):
            raise NotImplementedError(f"{item.name}: COMP-1/COMP-2 are out of scope")
        raise ValueError(f"line {item.line_no}: elementary item {item.name} has no PICTURE")
    # a group's USAGE and SIGN apply to its elementary children (see _inherit_usage)
    return CobolField(item.name, parse_pic(pic), Usage.parse(usage), item.sign or SignMode.TRAILING, dialect)


def _inherit_usage(item: Item, usage: str | None, sign: SignMode | None = None) -> None:
    if item.usage is None and usage is not None:
        item.usage = usage
    if item.sign is None and sign is not None:
        item.sign = sign
    for c in item.children:
        _inherit_usage(c, item.usage, item.sign)


def _size(item: Item, dialect: Dialect) -> int:
    if item.is_group:
        return _group_extent(item, dialect)[0] * item.occurs
    return _elementary(item, dialect).length * item.occurs


def _group_extent(item: Item, dialect: Dialect) -> tuple[int, dict[str, int]]:
    """Size of one occurrence of a group and the relative start of each child.

    A REDEFINES item may be longer than the item it overlays; COBOL reserves
    the larger extent, so the next sibling starts after the longest view."""
    starts: dict[str, int] = {}
    ends: dict[str, int] = {}
    cursor = 0
    for c in item.children:
        if c.redefines:
            try:
                start = starts[c.redefines]
            except KeyError:
                raise ValueError(
                    f"line {c.line_no}: {c.name} REDEFINES {c.redefines} which is not a sibling") from None
            starts[c.name] = start
            ends[c.name] = start + _size(c, dialect)
            # whichever view is longest decides where the next sibling goes
            cursor = max(cursor, ends[c.name])
            continue
        starts[c.name] = cursor
        ends[c.name] = cursor + _size(c, dialect)
        cursor = ends[c.name]
    return cursor, starts


def _place(item: Item, offset: int, prefix: str, dialect: Dialect,
           out: list[LayoutField], in_redefines: bool) -> int:
    """Append LayoutFields for ``item`` starting at ``offset``; return size."""
    unit = _size(item, dialect) // item.occurs if item.occurs else 0
    for occ in range(1, item.occurs + 1):
        base = offset + (occ - 1) * unit
        label = item.name if item.occurs == 1 else f"{item.name}({occ})"
        qname = f"{prefix}{label}" if item.name != "FILLER" or not item.is_group else prefix.rstrip(".")
        if item.is_group:
            child_prefix = f"{qname}." if qname else ""
            _, starts = _group_extent(item, dialect)
            for c in item.children:
                _place(c, base + starts[c.name], child_prefix, dialect, out,
                       in_redefines or bool(c.redefines))
        else:
            if item.name == "FILLER":
                qname = f"{prefix}FILLER@{base}"
            out.append(LayoutField(qname, _elementary(item, dialect), base, in_redefines))
    return unit * item.occurs


def build_layout(root: Item, dialect: Dialect = IBM) -> RecordLayout:
    _inherit_usage(root, None)
    fields: list[LayoutField] = []
    length = _place(root, 0, "", dialect, fields, False)
    # qualified names start with the 01 name; drop it for readability
    trimmed = []
    for f in fields:
        name = f.name
        if name.startswith(root.name + "."):
            name = name[len(root.name) + 1:]
        trimmed.append(LayoutField(name, f.field, f.offset, f.redefines))
    return RecordLayout(root.name, trimmed, length, dialect)


def parse_copybook(text: str, dialect: Dialect = IBM) -> list[RecordLayout]:
    return [build_layout(r, dialect) for r in parse_items(text)]


def load_layout(path: str | Path, record: str | None = None, dialect: Dialect = IBM) -> RecordLayout:
    """Load one record layout from a copybook file.

    ``record`` selects the 01-level by name when the copybook defines several
    (e.g. DBTBLS.cpy); otherwise the copybook must contain exactly one.
    """
    layouts = parse_copybook(Path(path).read_text(encoding="utf-8", errors="replace"), dialect)
    if record:
        for l in layouts:
            if l.name == record.upper():
                return l
        raise KeyError(f"{path}: no 01-level named {record}; have {[l.name for l in layouts]}")
    if len(layouts) != 1:
        raise ValueError(f"{path}: defines {len(layouts)} records {[l.name for l in layouts]}; pass record=")
    return layouts[0]
