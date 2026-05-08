#================================================================#
# COBOL Legacy Benchmark Suite - Makefile
# Compiles COBOL programs using GnuCOBOL (cobc)
#================================================================#

COBC       = cobc
BUILD_DIR  = build
DATA_DIR   = data
OUTPUT_DIR = output

# Copybook search paths
CPY_COMMON = -I src/copybook/common
CPY_BATCH  = -I src/copybook/batch
CPY_DB2    = -I src/copybook/db2
CPY_ONLINE = -I src/copybook/online
CPY_ALL    = $(CPY_COMMON) $(CPY_BATCH) $(CPY_DB2) $(CPY_ONLINE)

# Compiler flags
CFLAGS     = -Wall
MOD_FLAGS  = -m $(CPY_COMMON) $(CFLAGS)
EXE_FLAGS  = -x $(CPY_COMMON) $(CFLAGS)

# Subroutine modules (compiled as shared libraries)
MODULES = $(BUILD_DIR)/PORTVALD.so \
          $(BUILD_DIR)/ERRPROC.so \
          $(BUILD_DIR)/AUDPROC.so

# Executable programs
EXECUTABLES = $(BUILD_DIR)/PORTTEST \
              $(BUILD_DIR)/PORTLOAD \
              $(BUILD_DIR)/PORTREAD \
              $(BUILD_DIR)/PORTADD \
              $(BUILD_DIR)/PORTUPDT \
              $(BUILD_DIR)/PORTDEL \
              $(BUILD_DIR)/PORTTRAN

.PHONY: all clean dirs modules executables run

all: dirs modules executables
	@echo ""
	@echo "========================================"
	@echo " Build complete!"
	@echo " Modules:     $(words $(MODULES))"
	@echo " Executables: $(words $(EXECUTABLES))"
	@echo "========================================"
	@echo ""
	@ls -la $(BUILD_DIR)/

dirs:
	@mkdir -p $(BUILD_DIR) $(DATA_DIR) $(OUTPUT_DIR)

#----------------------------------------------------------------#
# Subroutine Modules
#----------------------------------------------------------------#
modules: dirs $(MODULES)

$(BUILD_DIR)/PORTVALD.so: src/programs/portfolio/PORTVALD.cbl
	$(COBC) $(MOD_FLAGS) -o $@ $<

$(BUILD_DIR)/ERRPROC.so: src/programs/common/ERRPROC.cbl
	$(COBC) $(MOD_FLAGS) -o $@ $<

$(BUILD_DIR)/AUDPROC.so: src/programs/common/AUDPROC.cbl
	$(COBC) $(MOD_FLAGS) -o $@ $<

#----------------------------------------------------------------#
# Executable Programs
#----------------------------------------------------------------#
executables: dirs modules $(EXECUTABLES)

$(BUILD_DIR)/PORTTEST: src/programs/portfolio/PORTTEST.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTLOAD: src/programs/portfolio/PORTLOAD.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTREAD: src/programs/portfolio/PORTREAD.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTADD: src/programs/portfolio/PORTADD.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTUPDT: src/programs/portfolio/PORTUPDT.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTDEL: src/programs/portfolio/PORTDEL.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

$(BUILD_DIR)/PORTTRAN: src/programs/portfolio/PORTTRAN.cbl
	$(COBC) $(EXE_FLAGS) -o $@ $<

#----------------------------------------------------------------#
# Run targets
#----------------------------------------------------------------#
run: all
	@echo ""
	@echo "========================================"
	@echo " Running CLBS Portfolio Demo"
	@echo "========================================"
	@bash scripts/run-demo.sh

clean:
	rm -rf $(BUILD_DIR)/*
	rm -rf $(DATA_DIR)/*
	rm -rf $(OUTPUT_DIR)/*
	@echo "Clean complete."
