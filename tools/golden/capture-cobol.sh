#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD_DIR="$REPO_ROOT/build"
RUN_DIR="$REPO_ROOT/golden/cobol-run"
INPUTS_DIR="$REPO_ROOT/golden/inputs"
MANIFEST="$INPUTS_DIR/manifest.json"
PATCH_DIR="$REPO_ROOT/golden/patches"
INCLUDE_DIR="$BUILD_DIR/src/copybook/common"

die() {
  printf 'capture-cobol: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || die "expected artifact missing: $1"
}

run_compile() {
  printf 'capture-cobol: %s\n' "$*" >&2
  "$@"
}

require_file "$MANIFEST"
command -v cobc >/dev/null 2>&1 || die "cobc is not installed"
command -v node >/dev/null 2>&1 || die "node is not installed"

rm -rf "$BUILD_DIR" "$RUN_DIR"
mkdir -p "$BUILD_DIR/src" "$BUILD_DIR/cobol" "$RUN_DIR/.scratch"
cp -a "$REPO_ROOT/src/." "$BUILD_DIR/src/"
cp -a "$REPO_ROOT/golden/cobol/." "$BUILD_DIR/cobol/"

for program in PORTADD PORTUPDT PORTDEL; do
  patch -d "$BUILD_DIR" -p0 --fuzz=0 --forward < "$PATCH_DIR/$program.patch"
done

run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/PORTADD" \
  "$BUILD_DIR/src/programs/portfolio/PORTADD.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/PORTREAD" \
  "$BUILD_DIR/src/programs/portfolio/PORTREAD.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/PORTUPDT" \
  "$BUILD_DIR/src/programs/portfolio/PORTUPDT.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/PORTDEL" \
  "$BUILD_DIR/src/programs/portfolio/PORTDEL.cbl"
run_compile cobc -m -I "$INCLUDE_DIR" -o "$BUILD_DIR/PORTVALD.so" \
  "$BUILD_DIR/src/programs/portfolio/PORTVALD.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/GLDLOAD" \
  "$BUILD_DIR/cobol/GLDLOAD.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/GLDDUMP" \
  "$BUILD_DIR/cobol/GLDDUMP.cbl"
run_compile cobc -x -I "$INCLUDE_DIR" -o "$BUILD_DIR/GLDVALD" \
  "$BUILD_DIR/cobol/GLDVALD.cbl"

version="$(cobc --version | head -n 1)"
indexed_handler="$(cobc --info | awk -F: '/indexed file handler/ {gsub(/^[[:space:]]+/, "", $2); print $2; exit}')"
[[ -n "$version" ]] || die "could not determine GnuCOBOL version"
[[ -n "$indexed_handler" ]] || die "could not determine indexed file handler"
capture_date="$(date +%Y%m%d)"

node - "$REPO_ROOT/golden/config/golden-config.json" "$capture_date" <<'NODE'
const fs = require('node:fs');
const [configPath, captureRunDate] = process.argv.slice(2);
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
config.captureRunDate = captureRunDate;
fs.writeFileSync(configPath, `${JSON.stringify(config, null, 2)}\n`);
NODE

export COB_LIBRARY_PATH="$BUILD_DIR"

case_rows="$(node - "$MANIFEST" <<'NODE'
const fs = require('node:fs');
const manifest = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
for (const entry of manifest.cases) {
  if (entry.program === 'PORTTRAN') continue;
  const files = Object.fromEntries(entry.files.map((file) => [file.file.split('/').at(-1), file]));
  const inputRoot = 'golden/' + entry.files[0].file.split('/').slice(0, -1).join('/');
  process.stdout.write([
    entry.program,
    entry.id,
    inputRoot,
    files['seed.dat']?.records ?? '',
    files['input.dat']?.records ?? '',
  ].join('\t') + '\n');
}
NODE
)"

captured_cases=()
while IFS=$'\t' read -r program case_id input_root seed_records input_records; do
  [[ -n "$program" && -n "$case_id" ]] || continue
  case_input_dir="$REPO_ROOT/$input_root"
  case_out_dir="$RUN_DIR/$program/$case_id"
  case_scratch="$RUN_DIR/.scratch/$program/$case_id"
  mkdir -p "$case_out_dir" "$case_scratch"

  seed_file="$case_input_dir/seed.dat"
  input_file="$case_input_dir/input.dat"
  port_file="$case_scratch/portfile"
  if [[ "$program" != "PORTVALD" ]]; then
    require_file "$seed_file"
  fi
  if [[ -n "$input_records" ]]; then
    require_file "$input_file"
  fi

  rm -f "$port_file" "$case_out_dir/stdout.txt" "$case_out_dir/exit-code.txt" \
    "$case_out_dir/dump.dat" "$case_out_dir/audit.dat"

  if [[ "$program" != "PORTVALD" ]]; then
    [[ -f "$seed_file" ]] || die "$program/$case_id missing seed.dat"
    [[ "$seed_records" =~ ^[0-9]+$ ]] ||
      die "$program/$case_id manifest seed record count is invalid: $seed_records"
    env PORTFILE="$port_file" SEEDFILE="$seed_file" \
      "$BUILD_DIR/GLDLOAD" >"$case_scratch/gldload.txt" 2>&1
    seed_loaded="$(awk -F': ' '/^Seed records loaded:/ {print $2; exit}' \
      "$case_scratch/gldload.txt" | tr -d '[:space:]')"
    seed_errors="$(awk -F': ' '/^Seed load errors:/ {print $2; exit}' \
      "$case_scratch/gldload.txt" | tr -d '[:space:]')"
    expected_seed_loaded="$(printf '%07d' "$seed_records")"
    [[ "$seed_errors" == "0000000" ]] ||
      die "$program/$case_id GLDLOAD reported errors: ${seed_errors:-missing}"
    [[ "$seed_loaded" == "$expected_seed_loaded" ]] ||
      die "$program/$case_id GLDLOAD loaded ${seed_loaded:-missing}, expected $expected_seed_loaded"
    require_file "$port_file"
  fi

  case "$program" in
    PORTADD)
      if env PORTFILE="$port_file" INPTFILE="$input_file" \
        "$BUILD_DIR/PORTADD" >"$case_out_dir/stdout.txt" 2>&1; then
        program_rc=$?
      else
        program_rc=$?
      fi
      ;;
    PORTREAD)
      if env PORTFILE="$port_file" \
        "$BUILD_DIR/PORTREAD" >"$case_out_dir/stdout.txt" 2>&1; then
        program_rc=$?
      else
        program_rc=$?
      fi
      ;;
    PORTUPDT)
      if env PORTFILE="$port_file" UPDTFILE="$input_file" \
        "$BUILD_DIR/PORTUPDT" >"$case_out_dir/stdout.txt" 2>&1; then
        program_rc=$?
      else
        program_rc=$?
      fi
      ;;
    PORTDEL)
      if env PORTFILE="$port_file" DELEFILE="$input_file" \
        AUDFILE="$case_scratch/audit.dat" "$BUILD_DIR/PORTDEL" \
        >"$case_out_dir/stdout.txt" 2>&1; then
        program_rc=$?
      else
        program_rc=$?
      fi
      ;;
    PORTVALD)
      if env VALDFILE="$input_file" "$BUILD_DIR/GLDVALD" \
        >"$case_out_dir/stdout.txt" 2>&1; then
        program_rc=$?
      else
        program_rc=$?
      fi
      ;;
    *)
      die "unsupported capture program: $program"
      ;;
  esac
  printf '%s\n' "$program_rc" > "$case_out_dir/exit-code.txt"
  require_file "$case_out_dir/stdout.txt"
  require_file "$case_out_dir/exit-code.txt"

  if [[ "$program" == "PORTDEL" ]]; then
    require_file "$case_scratch/audit.dat"
    cp "$case_scratch/audit.dat" "$case_out_dir/audit.dat"
    require_file "$case_out_dir/audit.dat"
  fi

  if [[ "$program" != "PORTVALD" ]]; then
    env PORTFILE="$port_file" DUMPFILE="$case_out_dir/dump.dat" \
      "$BUILD_DIR/GLDDUMP" >"$case_scratch/glddump.txt" 2>&1
    require_file "$case_out_dir/dump.dat"
    dump_size="$(wc -c < "$case_out_dir/dump.dat")"
    (( dump_size % 148 == 0 )) ||
      die "$program/$case_id dump.dat is $dump_size bytes, not a multiple of 148"
    dump_records="$((dump_size / 148))"
    dump_reported="$(awk -F': ' '/^Records dumped:/ {print $2; exit}' \
      "$case_scratch/glddump.txt" | tr -d '[:space:]')"
    expected_dumped="$(printf '%07d' "$dump_records")"
    [[ "$dump_reported" == "$expected_dumped" ]] ||
      die "$program/$case_id GLDDUMP reported ${dump_reported:-missing}, expected $expected_dumped"
  fi

  captured_cases+=("$program/$case_id")
done <<< "$case_rows"

[[ "${#captured_cases[@]}" -gt 0 ]] || die "manifest produced no capturable cases"

if ! grep -q 'Duplicate record:' "$RUN_DIR/PORTADD/ADD-002/stdout.txt"; then
  die "PORTADD/ADD-002 did not show duplicate path"
fi
if ! grep -q 'Record not found:' "$RUN_DIR/PORTDEL/DEL-002/stdout.txt"; then
  die "PORTDEL/DEL-002 did not show not-found path"
fi
if ! grep -q '^Total Records Read: 0000000$' "$RUN_DIR/PORTREAD/RED-002/stdout.txt"; then
  die "PORTREAD/RED-002 did not report zero records"
fi

node - "$RUN_DIR/metadata.json" "$version" "$indexed_handler" "$capture_date" \
  "${captured_cases[@]}" <<'NODE'
const fs = require('node:fs');
const [metadataPath, gnucobolVersion, indexedFileHandler, captureRunDate, ...cases] =
  process.argv.slice(2);
const metadata = {
  gnucobolVersion,
  indexedFileHandler,
  captureRunDate,
  cases: cases.map((value) => {
    const [program, id] = value.split('/');
    return { program, id };
  }),
};
fs.writeFileSync(metadataPath, `${JSON.stringify(metadata, null, 2)}\n`);
NODE
require_file "$RUN_DIR/metadata.json"
printf 'Captured %d COBOL golden case(s) in %s\n' "${#captured_cases[@]}" "$RUN_DIR"
