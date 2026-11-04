"""cobol_fields: COBOL data-field definitions for Python migrations.

Field layer (COMP / COMP-3 / COMP-X / COMP-5 / BINARY / DISPLAY), copybook
parsing into record layouts, and a field-by-field diff harness used to prove
a Python port writes the same bytes the COBOL program does.
"""
from .copybook import LayoutField, RecordLayout, load_layout, parse_copybook
from .diff import DiffReport, FieldDiff, diff_bytes, diff_files, diff_records
from .fields import (DIALECTS, GNUCOBOL, GNUCOBOL_IBM, IBM, CobolField,
                     CobolFieldError, Dialect, SignMode, Usage)
from .pic import Pic, PicKind, parse_pic

__all__ = [
    "CobolField", "CobolFieldError", "Dialect", "DIALECTS", "GNUCOBOL", "GNUCOBOL_IBM", "IBM",
    "SignMode", "Usage",
    "Pic", "PicKind", "parse_pic",
    "LayoutField", "RecordLayout", "load_layout", "parse_copybook",
    "DiffReport", "FieldDiff", "diff_bytes", "diff_files", "diff_records",
]
