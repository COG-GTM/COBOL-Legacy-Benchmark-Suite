#!/bin/bash
# preprocess.sh - Strip EXEC SQL/EXEC CICS blocks for GnuCOBOL compilation
# Replaces embedded SQL/CICS with CONTINUE statements in procedure div
# Removes EXEC SQL blocks in data division (INCLUDE, DECLARE SECTION)
# Usage: ./preprocess.sh input.cbl output.cbl

INPUT="$1"
OUTPUT="$2"

if [ -z "$INPUT" ] || [ -z "$OUTPUT" ]; then
    echo "Usage: $0 input.cbl output.cbl"
    exit 1
fi

awk '
BEGIN { in_exec = 0; in_proc = 0 }
/PROCEDURE DIVISION/ { in_proc = 1 }
/EXEC SQL|EXEC CICS/ {
    in_exec = 1
    # Check if this is a data-division EXEC (INCLUDE, DECLARE SECTION)
    is_data_exec = 0
    if (/INCLUDE|DECLARE SECTION/) is_data_exec = 1
    if (!in_proc) is_data_exec = 1

    # If END-EXEC on same line, close immediately
    if (/END-EXEC/) {
        in_exec = 0
        if (!is_data_exec) {
            if (/END-EXEC[[:space:]]*\./) {
                printf "%s\n", "           CONTINUE."
            } else {
                printf "%s\n", "           CONTINUE"
            }
        }
    }
    next
}
in_exec && /END-EXEC/ {
    in_exec = 0
    if (in_proc) {
        if (/END-EXEC[[:space:]]*\./) {
            printf "%s\n", "           CONTINUE."
        } else {
            printf "%s\n", "           CONTINUE"
        }
    }
    next
}
in_exec { next }
{ print }
' "$INPUT" > "$OUTPUT"
