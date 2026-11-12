"""Field-by-field comparison of COBOL-format record files.

This is the validation step of a migration: run the same inputs through the
COBOL program and the Python port, then diff the *records they wrote* field
by field. Byte-identical files are the goal; when they differ this tells you
which field, which record, and whether it is a rounding, truncation, sign or
padding problem rather than "files differ".
"""
from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal
from pathlib import Path
from typing import Any, Iterable, Iterator

from .copybook import LayoutField, RecordLayout


@dataclass(frozen=True)
class FieldDiff:
    record_no: int
    field: str
    expected: Any
    actual: Any
    expected_hex: str
    actual_hex: str
    reason: str

    def __str__(self) -> str:
        return (f"record {self.record_no} {self.field}: expected {self.expected!r} "
                f"[{self.expected_hex}] got {self.actual!r} [{self.actual_hex}] ({self.reason})")


@dataclass
class DiffReport:
    layout: str
    records_expected: int = 0
    records_actual: int = 0
    diffs: list[FieldDiff] = field(default_factory=list)
    trailing_bytes: tuple[int, int] = (0, 0)

    @property
    def ok(self) -> bool:
        return (not self.diffs and self.records_expected == self.records_actual
                and self.trailing_bytes == (0, 0))

    def summary(self) -> str:
        lines = [f"layout {self.layout}: {self.records_expected} expected record(s), "
                 f"{self.records_actual} actual record(s)"]
        if self.trailing_bytes != (0, 0):
            lines.append(f"  trailing partial record bytes: expected={self.trailing_bytes[0]} "
                         f"actual={self.trailing_bytes[1]}")
        if self.diffs:
            by_field: dict[str, int] = {}
            for d in self.diffs:
                by_field[d.field] = by_field.get(d.field, 0) + 1
            lines.append(f"  {len(self.diffs)} field mismatch(es):")
            for name, n in by_field.items():
                lines.append(f"    {name}: {n}")
        lines.append("  RESULT: " + ("MATCH" if self.ok else "MISMATCH"))
        return "\n".join(lines)


def _classify(f: LayoutField, exp: Any, act: Any, exp_raw: bytes, act_raw: bytes) -> str:
    if exp_raw == act_raw:
        return "identical bytes"
    if isinstance(exp, str) and isinstance(act, str):
        if exp.rstrip() == act.rstrip():
            return "padding differs"
        if exp.strip() == act.strip():
            return "justification differs"
        return "text differs"
    if isinstance(exp, Decimal) and isinstance(act, Decimal):
        if exp == act:
            return "same value, different encoding (sign nibble / zone)"
        if exp == -act:
            return "sign flipped"
        scale = Decimal(1).scaleb(-f.field.scale)
        if abs(exp - act) <= scale:
            return "rounding / truncation in last decimal place"
        digits = 10 ** f.field.pic.int_digits if f.field.pic.int_digits else 1
        if (exp - act) % Decimal(digits) == 0:
            return "high-order digit truncation"
        return "numeric value differs"
    return "value differs"


def diff_records(layout: RecordLayout, expected: bytes, actual: bytes, record_no: int = 1,
                 *, strict: bool = False, ignore: Iterable[str] = ()) -> list[FieldDiff]:
    ignored = set(ignore)
    out: list[FieldDiff] = []
    for f in layout.data_fields:
        if f.name in ignored or f.short_name in ignored:
            continue
        e_raw, a_raw = expected[f.offset:f.end], actual[f.offset:f.end]
        if e_raw == a_raw:
            continue
        try:
            e_val = f.field.decode(e_raw, strict=strict)
        except Exception as exc:  # undecodable data is itself a finding
            e_val = f"<undecodable: {exc}>"
        try:
            a_val = f.field.decode(a_raw, strict=strict)
        except Exception as exc:
            a_val = f"<undecodable: {exc}>"
        out.append(FieldDiff(record_no, f.name, e_val, a_val, e_raw.hex(), a_raw.hex(),
                             _classify(f, e_val, a_val, e_raw, a_raw)))
    return out


def iter_records(data: bytes, length: int) -> Iterator[bytes]:
    for i in range(0, len(data) - len(data) % length, length):
        yield data[i:i + length]


def diff_bytes(layout: RecordLayout, expected: bytes, actual: bytes, *,
               strict: bool = False, ignore: Iterable[str] = ()) -> DiffReport:
    rep = DiffReport(layout.name)
    rep.records_expected = len(expected) // layout.length
    rep.records_actual = len(actual) // layout.length
    rep.trailing_bytes = (len(expected) % layout.length, len(actual) % layout.length)
    for i, (e, a) in enumerate(zip(iter_records(expected, layout.length),
                                   iter_records(actual, layout.length)), 1):
        rep.diffs.extend(diff_records(layout, e, a, i, strict=strict, ignore=ignore))
    return rep


def diff_files(layout: RecordLayout, expected: str | Path, actual: str | Path, *,
               strict: bool = False, ignore: Iterable[str] = ()) -> DiffReport:
    return diff_bytes(layout, Path(expected).read_bytes(), Path(actual).read_bytes(),
                      strict=strict, ignore=ignore)
