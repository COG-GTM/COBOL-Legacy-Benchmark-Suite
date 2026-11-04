import shutil
import subprocess
import sys
from pathlib import Path

import pytest

HERE = Path(__file__).resolve().parent
PY_ROOT = HERE.parent
REPO = PY_ROOT.parent
COPYBOOKS = REPO / "src" / "copybook"
COBOL_FIXTURES = HERE / "cobol"

sys.path.insert(0, str(PY_ROOT))

COBC = shutil.which("cobc")
requires_cobc = pytest.mark.skipif(COBC is None, reason="GnuCOBOL (cobc) not installed")


def copybook_includes() -> list[str]:
    dirs = [COBOL_FIXTURES] + [d for d in COPYBOOKS.iterdir() if d.is_dir()]
    return [f"-I{d}" for d in dirs]


def compile_cobol(source: Path, workdir: Path, std: str = "default", extra: list[str] = ()) -> Path:
    exe = workdir / source.stem.lower()
    cmd = [COBC, "-x", "-w", f"-std={std}", *extra, *copybook_includes(), "-o", str(exe), str(source)]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(f"cobc failed: {' '.join(cmd)}\n{r.stdout}\n{r.stderr}")
    return exe


def run_cobol(exe: Path, workdir: Path) -> str:
    r = subprocess.run([str(exe)], cwd=workdir, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(f"{exe.name} failed rc={r.returncode}\n{r.stdout}\n{r.stderr}")
    return r.stdout


@pytest.fixture
def cobol_build(tmp_path):
    """Compile a COBOL fixture into tmp_path and return (exe, workdir)."""
    def _build(name: str, std: str = "default", extra: list[str] = ()):
        return compile_cobol(COBOL_FIXTURES / name, tmp_path, std, extra), tmp_path
    return _build
