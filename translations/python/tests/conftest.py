"""Shared fixtures for the translation tests."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

VECTOR_DIR = Path(__file__).parent / "vectors"


def load_vectors(program: str) -> list[dict[str, object]]:
    """Return the recorded COBOL behaviour vectors for ``program``."""
    document = json.loads((VECTOR_DIR / f"{program.lower()}.json").read_text())
    return document["cases"]


@pytest.fixture(scope="session")
def cobol_module_dir(tmp_path_factory: pytest.TempPathFactory) -> Path:
    """Directory holding the COBOL modules compiled for equivalence testing."""
    return tmp_path_factory.mktemp("cobol-modules")
