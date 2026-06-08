# COBOL Legacy Benchmark Suite - Makefile
# Requires: GnuCOBOL (cobc)

COBC = cobc
COPYPATH = -I src/copybook/common -I src/copybook/batch -I src/copybook/online -I src/copybook/db2
BUILDDIR = build
BINDIR = $(BUILDDIR)/bin
PPDIR = $(BUILDDIR)/preprocessed

# Executable programs (standalone, no USING clause)
EXEC_DIRECT = \
	src/programs/batch/RPTAUD00.cbl \
	src/programs/batch/RPTPOS00.cbl \
	src/programs/batch/RPTSTA00.cbl \
	src/programs/portfolio/PORTADD.cbl \
	src/programs/portfolio/PORTDEL.cbl \
	src/programs/portfolio/PORTREAD.cbl \
	src/programs/portfolio/PORTTEST.cbl \
	src/programs/portfolio/PORTTRAN.cbl \
	src/programs/portfolio/PORTUPDT.cbl \
	src/programs/test/TSTGEN00.cbl \
	src/programs/test/TSTVAL00.cbl \
	src/programs/utility/UTLMNT00.cbl \
	src/programs/utility/UTLMON00.cbl \
	src/programs/utility/UTLVAL00.cbl

# Module programs (subprograms with USING clause)
MOD_DIRECT = \
	src/programs/batch/BCHCTL00.cbl \
	src/programs/batch/CKPRST.cbl \
	src/programs/batch/PRCSEQ00.cbl \
	src/programs/batch/RCVPRC00.cbl \
	src/programs/common/AUDPROC.cbl \
	src/programs/common/ERRPROC.cbl \
	src/programs/portfolio/PORTMSTR.cbl \
	src/programs/portfolio/PORTVALD.cbl

# Preprocessed executables (EXEC SQL/CICS, standalone)
EXEC_PP = \
	src/programs/batch/HISTLD00.cbl \
	src/programs/batch/RTNANA00.cbl \
	src/programs/online/ERRHNDL.cbl \
	src/programs/online/INQHIST.cbl \
	src/programs/online/INQONLN.cbl \
	src/programs/online/INQPORT.cbl

# Preprocessed modules (EXEC SQL/CICS, subprograms)
MOD_PP = \
	src/programs/batch/RTNCDE00.cbl \
	src/programs/common/DB2CMT.cbl \
	src/programs/common/DB2CONN.cbl \
	src/programs/common/DB2ERR.cbl \
	src/programs/common/DB2STAT.cbl \
	src/programs/online/CURSMGR.cbl \
	src/programs/online/DB2ONLN.cbl \
	src/programs/online/DB2RECV.cbl \
	src/programs/online/SECMGR.cbl

ALL_SRCS = $(EXEC_DIRECT) $(MOD_DIRECT) $(EXEC_PP) $(MOD_PP)

.PHONY: all clean check test dirs

all: dirs
	@ok=0; fail=0; total=0; \
	for src in $(EXEC_DIRECT); do \
		name=$$(basename $$src .cbl); total=$$((total+1)); \
		if $(COBC) -x $(COPYPATH) -o $(BINDIR)/$$name $$src 2>/dev/null; then \
			echo "OK   $$name"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name"; fail=$$((fail+1)); \
		fi; \
	done; \
	for src in $(MOD_DIRECT); do \
		name=$$(basename $$src .cbl); total=$$((total+1)); \
		if $(COBC) -m $(COPYPATH) -o $(BINDIR)/$$name.so $$src 2>/dev/null; then \
			echo "OK   $$name (module)"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name (module)"; fail=$$((fail+1)); \
		fi; \
	done; \
	for src in $(EXEC_PP); do \
		name=$$(basename $$src .cbl); total=$$((total+1)); \
		./tools/preprocess.sh $$src $(PPDIR)/$$name.cbl; \
		if $(COBC) -x $(COPYPATH) -o $(BINDIR)/$$name $(PPDIR)/$$name.cbl 2>/dev/null; then \
			echo "OK   $$name (preprocessed)"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name (preprocessed)"; fail=$$((fail+1)); \
		fi; \
	done; \
	for src in $(MOD_PP); do \
		name=$$(basename $$src .cbl); total=$$((total+1)); \
		./tools/preprocess.sh $$src $(PPDIR)/$$name.cbl; \
		if $(COBC) -m $(COPYPATH) -o $(BINDIR)/$$name.so $(PPDIR)/$$name.cbl 2>/dev/null; then \
			echo "OK   $$name (module, preprocessed)"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name (module, preprocessed)"; fail=$$((fail+1)); \
		fi; \
	done; \
	echo ""; \
	echo "Build complete: $$ok passed, $$fail failed out of $$total programs"

dirs:
	@mkdir -p $(BINDIR) $(PPDIR)

check: dirs
	@ok=0; fail=0; \
	for src in $(EXEC_DIRECT) $(MOD_DIRECT); do \
		name=$$(basename $$src .cbl); \
		if $(COBC) -fsyntax-only $(COPYPATH) $$src 2>/dev/null; then \
			echo "OK   $$name"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name"; fail=$$((fail+1)); \
		fi; \
	done; \
	for src in $(EXEC_PP) $(MOD_PP); do \
		name=$$(basename $$src .cbl); \
		./tools/preprocess.sh $$src $(PPDIR)/$$name.cbl; \
		if $(COBC) -fsyntax-only $(COPYPATH) $(PPDIR)/$$name.cbl 2>/dev/null; then \
			echo "OK   $$name"; ok=$$((ok+1)); \
		else \
			echo "FAIL $$name"; fail=$$((fail+1)); \
		fi; \
	done; \
	echo ""; \
	echo "Results: $$ok passed, $$fail failed out of $$((ok+fail)) programs"

# Test targets
test: dirs
	@echo "Building test programs..."
	@$(COBC) -x $(COPYPATH) -o $(BINDIR)/TSTGEN00 src/programs/test/TSTGEN00.cbl 2>/dev/null
	@$(COBC) -x $(COPYPATH) -o $(BINDIR)/TSTVAL00 src/programs/test/TSTVAL00.cbl 2>/dev/null
	@$(COBC) -x $(COPYPATH) -o $(BINDIR)/PORTTEST src/programs/portfolio/PORTTEST.cbl 2>/dev/null
	@echo ""
	@echo "=== PORTTEST (Portfolio Test Data Generator) ==="
	@cd $(BUILDDIR) && export dd_TESTFILE=testdata/porttest.dat && \
		mkdir -p testdata && ./bin/PORTTEST && echo "PASSED" || echo "FAILED"

clean:
	rm -rf $(BUILDDIR)
