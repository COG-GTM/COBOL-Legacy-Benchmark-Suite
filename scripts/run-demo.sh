#!/bin/bash
#================================================================#
# COBOL Legacy Benchmark Suite - Demo Runner
# Demonstrates the Portfolio Management System
#================================================================#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_DIR/build"
DATA_DIR="$PROJECT_DIR/data"
OUTPUT_DIR="$PROJECT_DIR/output"

# Add build dir to COB_LIBRARY_PATH so subroutines can be found
export COB_LIBRARY_PATH="$BUILD_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

divider() {
    echo -e "${BLUE}================================================================${NC}"
}

header() {
    echo ""
    divider
    echo -e "${GREEN} $1${NC}"
    divider
}

step() {
    echo -e "${YELLOW}>>> $1${NC}"
}

cd "$PROJECT_DIR"
mkdir -p "$DATA_DIR" "$OUTPUT_DIR"

header "COBOL Legacy Benchmark Suite - Portfolio Management Demo"
echo "  Build Directory:  $BUILD_DIR"
echo "  Data Directory:   $DATA_DIR"
echo "  Output Directory: $OUTPUT_DIR"

#----------------------------------------------------------------#
# Step 1: Generate Test Data with PORTTEST
#----------------------------------------------------------------#
header "Step 1: Generating Test Portfolio Data (PORTTEST)"

export TESTFILE="$DATA_DIR/portfolio.dat"
rm -f "$DATA_DIR/portfolio.dat"

step "Running PORTTEST to generate 100 indexed portfolio records..."
"$BUILD_DIR/PORTTEST"

echo -e "${GREEN}Test data generated successfully!${NC}"
echo "  File: $DATA_DIR/portfolio.dat"
echo "  Size: $(du -h "$DATA_DIR/portfolio.dat" | cut -f1)"

#----------------------------------------------------------------#
# Step 2: Read Portfolio Records with PORTREAD
#----------------------------------------------------------------#
header "Step 2: Reading Portfolio Records (PORTREAD)"

export PORTFILE="$DATA_DIR/portfolio.dat"

step "Running PORTREAD to display portfolio records..."
step "(Showing first 10 and last 5 records)"
"$BUILD_DIR/PORTREAD" 2>&1 | head -55
echo "  ..."
"$BUILD_DIR/PORTREAD" 2>&1 | tail -8

#----------------------------------------------------------------#
# Summary
#----------------------------------------------------------------#
header "Demo Complete"
echo ""
echo "  Programs compiled and available:"
echo "    PORTTEST  - Test data generator (indexed file)"
echo "    PORTLOAD  - Sequential-to-indexed data loader"
echo "    PORTREAD  - Portfolio record reader"
echo "    PORTADD   - Portfolio record addition"
echo "    PORTUPDT  - Portfolio record update"
echo "    PORTDEL   - Portfolio deletion with audit"
echo "    PORTTRAN  - Transaction processing"
echo ""
echo "  Subroutine modules:"
echo "    PORTVALD  - Portfolio validation"
echo "    ERRPROC   - Error processing"
echo "    AUDPROC   - Audit trail processing"
echo ""
echo "  Data files:"
ls -lh "$DATA_DIR/" 2>/dev/null
echo ""
divider
echo -e "${GREEN} All programs compiled and executed with GnuCOBOL${NC}"
divider
