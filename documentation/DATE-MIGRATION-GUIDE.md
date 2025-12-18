# Date Field Migration Guide: 8 to 10 Digits (YYYYMMDD to YYYY-MM-DD)

## Overview

This document describes the migration of all date fields from 8-digit format (YYYYMMDD) to 10-digit format (YYYY-MM-DD) in the COBOL-Legacy-Benchmark-Suite portfolio management system.

## Scope of Changes

### Copybooks Modified

| Copybook | Field(s) Changed | Old Format | New Format |
|----------|------------------|------------|------------|
| TRNREC.cpy | TRN-DATE | PIC X(08) | PIC X(10) |
| POSREC.cpy | POS-DATE | PIC X(08) | PIC X(10) |
| HISTREC.cpy | HIST-DATE | PIC X(08) | PIC X(10) |
| PORTFLIO.cpy | PORT-CREATE-DATE, PORT-LAST-MAINT, PORT-LAST-TRANS | PIC 9(8) | PIC X(10) |
| BCHCTL.cpy | BCT-PROCESS-DATE | PIC X(8) | PIC X(10) |
| CKPRST.cpy | CK-RUN-DATE, CKR-RUN-DATE | PIC X(8) | PIC X(10) |

### New Components Created

| Component | Type | Purpose |
|-----------|------|---------|
| DATECNV.cpy | Copybook | Date conversion area definitions |
| DATECNV.cbl | Program | Date conversion utility routines |

### Programs Modified

| Program | Change Description |
|---------|-------------------|
| RPTPOS00.cbl | Added DATECNV copybook, conversion after ACCEPT FROM DATE |
| RPTAUD00.cbl | Added DATECNV copybook, conversion after ACCEPT FROM DATE |
| RPTSTA00.cbl | Added DATECNV copybook, conversion after ACCEPT FROM DATE |
| PORTADD.cbl | Added DATECNV copybook, updated WS-CURRENT-DATE to PIC X(10) |
| PORTMSTR.cbl | Added DATECNV copybook, conversion after ACCEPT FROM DATE |
| PORTTEST.cbl | Added DATECNV copybook, updated WS-CURRENT-DATE to PIC X(10) |
| BCHCTL00.cbl | Updated LS-PROCESS-DATE to PIC X(10) |
| PRCSEQ00.cbl | Updated LS-PROCESS-DATE to PIC X(10) |
| RCVPRC00.cbl | Updated LS-PROCESS-DATE to PIC X(10) |

### VSAM Key Length Changes

| File | Old Key Length | New Key Length | Change |
|------|---------------|----------------|--------|
| TRANHIST | 20 bytes | 22 bytes | +2 bytes |
| POSHIST | 18 bytes | 20 bytes | +2 bytes |
| BCHCTL | 20 bytes | 22 bytes | +2 bytes |
| CKPRST | 16 bytes | 18 bytes | +2 bytes |

## Migration Procedure

### Pre-Migration Checklist

1. Verify no batch jobs are in restart-pending state
2. Confirm all online transactions are complete
3. Obtain DBA approval for DB2 interface changes
4. Schedule maintenance window (minimum 4 hours)
5. Notify operations team

### Execution Steps

1. **Stop Processing**
   - Stop all batch jobs
   - Disable online transactions (CICS)

2. **Backup (VSAMBACK.jcl)**
   - Execute backup JCL to create copies of all VSAM files
   - Verify backup completion and record counts

3. **Redefine VSAM Clusters (VSAMREDF.jcl)**
   - Delete existing clusters
   - Define new clusters with updated key lengths and record sizes

4. **Migrate Data (DATAMIGR.jcl)**
   - Convert date fields from YYYYMMDD to YYYY-MM-DD
   - Load converted data into new VSAM clusters
   - Clear checkpoint file (old checkpoints are incompatible)

5. **Validate Migration (VSAMVALD.jcl)**
   - Verify record counts match pre-migration
   - Validate date format in sample records
   - Run test batch job

6. **Recompile Programs**
   - Compile all modified programs in dependency order
   - Link-edit and deploy to load libraries

7. **Resume Processing**
   - Enable online transactions
   - Start batch processing

### Compilation Order

1. DATECNV.cpy (new copybook)
2. Common copybooks (TRNREC, POSREC, HISTREC, PORTFLIO)
3. Batch copybooks (BCHCTL, CKPRST)
4. DB2 copybooks (DBTBLS - no changes needed)
5. DATECNV.cbl (utility program)
6. Batch programs (RPTPOS00, RPTAUD00, RPTSTA00, BCHCTL00, PRCSEQ00, RCVPRC00)
7. Portfolio programs (PORTADD, PORTMSTR, PORTTEST)
8. Online programs (if modified)

## Rollback Procedure

### When to Rollback

- Data conversion errors exceed threshold
- Critical batch jobs fail after migration
- Online transactions show errors
- Record counts don't match

### Rollback Steps

1. **Stop All Processing**
   - Stop batch jobs immediately
   - Disable online transactions

2. **Restore VSAM Files**
   ```
   //RESTORE  EXEC PGM=IDCAMS
   //SYSIN    DD *
     DELETE PORTFOLIO.TRANHIST.VSAM CLUSTER PURGE
     DELETE PORTFOLIO.POSHIST.VSAM CLUSTER PURGE
     DELETE PORTFOLIO.PORTMSTR.VSAM CLUSTER PURGE
     DELETE PORTFOLIO.BCHCTL.VSAM CLUSTER PURGE
     DELETE PORTFOLIO.CKPRST.VSAM CLUSTER PURGE
   /*
   ```

3. **Redefine Original Clusters**
   - Use original IDCAMS definitions with old key lengths
   - TRANHIST: KEY LENGTH=20
   - POSHIST: KEY LENGTH=18

4. **Reload from Backup**
   ```
   //RELOAD   EXEC PGM=IDCAMS
   //SYSIN    DD *
     REPRO INFILE(BACKUP) OUTFILE(VSAM)
   /*
   ```

5. **Restore Original Programs**
   - Deploy backup copies of all modified programs
   - Verify load library contents

6. **Resume Processing**
   - Enable online transactions
   - Start batch processing with original programs

## Technical Notes

### ACCEPT FROM DATE Behavior

The COBOL `ACCEPT FROM DATE YYYYMMDD` statement always returns 8 digits regardless of the target field size. All programs using this statement must call the DATECNV conversion routine:

```cobol
ACCEPT DC-INPUT-DATE-8 FROM DATE YYYYMMDD
MOVE '1' TO DC-FUNCTION-CODE
CALL 'DATECNV' USING DATE-CONVERSION-AREA
MOVE DC-OUTPUT-DATE-10 TO target-field
```

### PIC 9(8) to PIC X(10) Semantic Change

The PORTFLIO.cpy copybook changed date fields from numeric (PIC 9(8)) to alphanumeric (PIC X(10)). This affects:
- Numeric comparisons (now string comparisons)
- MOVE statements with numeric sources
- Any arithmetic operations on date fields

### Checkpoint/Restart Compatibility

Old checkpoint records are incompatible with the new date format. After migration:
- All pending restarts must be cleared before migration
- New checkpoints will use YYYY-MM-DD format
- Do not attempt to restart jobs from pre-migration checkpoints

### DB2 Interface

The DBTBLS.cpy copybook already uses PIC X(10) for date fields, indicating DB2 returns dates in ISO format (YYYY-MM-DD). No changes were required for DB2 interface.

## JCL Scripts

| Script | Purpose | Location |
|--------|---------|----------|
| VSAMBACK.jcl | Backup all VSAM files | src/jcl/migration/ |
| VSAMREDF.jcl | Redefine VSAM clusters with new key lengths | src/jcl/migration/ |
| DATAMIGR.jcl | Convert and reload data | src/jcl/migration/ |
| VSAMVALD.jcl | Validate migration results | src/jcl/migration/ |

## Contact Information

For questions or issues during migration, contact:
- Date Migration Team
- DBA Team for DB2 issues
- Operations Team for JCL execution
