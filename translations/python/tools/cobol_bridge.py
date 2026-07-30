"""Call the original COBOL programs from Python so translations can be diffed.

The bridge compiles a program with GnuCOBOL (``cobc -m``) and invokes it through
``libcob``'s ``cob_call`` with a raw linkage buffer, which is what a mainframe
CALL BY REFERENCE does. It is a test aid only: it is never imported by the
translated programs.
"""

from __future__ import annotations

import ctypes
import os
import subprocess
from ctypes.util import find_library
from pathlib import Path
from shutil import which

REPO_ROOT = Path(__file__).resolve().parents[3]
COPYBOOK_ROOT = REPO_ROOT / "src" / "copybook"

_runtime: _CobolRuntime | None = None


class CobolUnavailableError(RuntimeError):
    """Raised when GnuCOBOL is not installed on the machine running the tests."""


class _CobolRuntime:
    """Owns the single ``libcob`` initialisation allowed per process."""

    def __init__(self, module_dir: Path) -> None:
        library = find_library("cob") or "libcob.so.4"
        try:
            self._libcob = ctypes.CDLL(library)
        except OSError as error:  # pragma: no cover - depends on the machine
            raise CobolUnavailableError(f"libcob is not loadable: {error}") from error

        os.environ["COB_LIBRARY_PATH"] = str(module_dir)
        self._libcob.cob_init(ctypes.c_int(0), None)
        self._libcob.cob_call.restype = ctypes.c_int

    def call(self, program: str, linkage: bytearray) -> None:
        buffer = (ctypes.c_char * len(linkage)).from_buffer(linkage)
        parameters = (ctypes.c_void_p * 1)()
        parameters[0] = ctypes.cast(buffer, ctypes.c_void_p)
        status = self._libcob.cob_call(program.encode(), ctypes.c_int(1), parameters)
        if status != 0:
            raise RuntimeError(f"cob_call({program}) returned {status}")


def compile_module(source: Path, module_dir: Path) -> Path:
    """Compile a COBOL program into a callable module in ``module_dir``."""
    if which("cobc") is None:
        raise CobolUnavailableError("cobc (GnuCOBOL) is not on PATH")

    module_dir.mkdir(parents=True, exist_ok=True)
    module = module_dir / f"{source.stem}.so"
    command = [
        "cobc",
        "-m",
        "-I",
        str(COPYBOOK_ROOT / "common"),
        "-o",
        str(module),
        str(source),
    ]
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise CobolUnavailableError(f"cobc failed: {result.stderr.strip()}")
    return module


def call_program(program: str, linkage: bytearray, module_dir: Path) -> None:
    """Call a compiled COBOL program with ``linkage`` as its single parameter."""
    global _runtime
    if _runtime is None:
        _runtime = _CobolRuntime(module_dir)
    _runtime.call(program, linkage)
