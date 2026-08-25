# Capture-only COBOL patches

The capture script copies the pristine sources into `build/src/` and applies
these patches there. No source under `src/` is modified.

- `PORTADD.patch`: the first comment line has `*` in column 8 instead of the
  fixed-format indicator column, and the second `PORTFLIO` copy must be
  prefixed to avoid duplicate data names from the two FDs. The master
  `PORT-RECORD` buffer remains the target of the input `READ`, so the
  program's data flow is unchanged.
- `PORTUPDT.patch`: the first comment line has `*` in column 8 instead of the
  fixed-format indicator column.
- `PORTDEL.patch`: the first comment line has `*` in column 8 instead of the
  fixed-format indicator column, and GnuCOBOL does not accept `TIME STAMP` as
  an `ACCEPT` source. `FUNCTION CURRENT-DATE` supplies the equivalent
  host-clock timestamp to the existing X(26) audit field.
