//*================================================================*
//* JCL Name: DATAMIGR
//* Description: Data Migration for Date Field Conversion
//* Purpose: Convert date fields from 8-digit (YYYYMMDD) to
//*          10-digit (YYYY-MM-DD) format in all VSAM files
//* Author: Date Migration Team
//* Date Written: 2024-12-18
//*
//* PREREQUISITES:
//* 1. Run VSAMBACK.jcl to create backups
//* 2. Run VSAMREDF.jcl to redefine clusters with new key lengths
//* 3. Compile DATECNV.cbl conversion program
//*
//* EXECUTION ORDER:
//* 1. VSAMBACK - Backup all files
//* 2. VSAMREDF - Redefine clusters
//* 3. DATAMIGR - Convert and reload data (this JCL)
//* 4. VSAMVALD - Validate migration
//*================================================================*
//DATAMIGR JOB (ACCT),'DATA MIGRATION',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,REGION=0M
//*
//*================================================================*
//* STEP 1: CONVERT AND LOAD TRANSACTION HISTORY
//* Reads backup, converts dates, writes to new VSAM
//*================================================================*
//MIGTRN   EXEC PGM=TRNMIGR
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//INFILE   DD DSN=PORTFOLIO.TRANHIST.BACKUP,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.TRANHIST.VSAM,DISP=SHR
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*================================================================*
//* STEP 2: CONVERT AND LOAD POSITION HISTORY
//*================================================================*
//MIGPOS   EXEC PGM=POSMIGR
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//INFILE   DD DSN=PORTFOLIO.POSHIST.BACKUP,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*================================================================*
//* STEP 3: CONVERT AND LOAD PORTFOLIO MASTER
//* Note: PORTMSTR date fields changed from PIC 9(8) to PIC X(10)
//*================================================================*
//MIGPORT  EXEC PGM=PORTMIGR
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//INFILE   DD DSN=PORTFOLIO.PORTMSTR.BACKUP,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.PORTMSTR.VSAM,DISP=SHR
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*================================================================*
//* STEP 4: CONVERT AND LOAD BATCH CONTROL
//*================================================================*
//MIGBCH   EXEC PGM=BCHMIGR
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//INFILE   DD DSN=PORTFOLIO.BCHCTL.BACKUP,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.BCHCTL.VSAM,DISP=SHR
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*================================================================*
//* STEP 5: CLEAR CHECKPOINT FILE (New checkpoints will be created)
//* Note: Old checkpoints are incompatible with new date format
//*================================================================*
//CLRCKP   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DELETE PORTFOLIO.CKPRST.VSAM CLUSTER PURGE
  SET MAXCC = 0
  DEFINE CLUSTER                                        -
         (NAME(PORTFOLIO.CKPRST.VSAM)                   -
          VOLUMES(VSAM01)                               -
          CYLINDERS(5 2)                                -
          KEYS(18 0)                                    -
          RECORDSIZE(418 418)                           -
          FREESPACE(20 20)                              -
          INDEXED                                       -
          SHAREOPTIONS(2 3))                            -
         DATA                                           -
         (NAME(PORTFOLIO.CKPRST.VSAM.DATA))             -
         INDEX                                          -
         (NAME(PORTFOLIO.CKPRST.VSAM.INDEX))
/*
//*
//*================================================================*
//* STEP 6: GENERATE MIGRATION SUMMARY REPORT
//*================================================================*
//SUMMARY  EXEC PGM=MIGRPT
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//TRANHIST DD DSN=PORTFOLIO.TRANHIST.VSAM,DISP=SHR
//POSHIST  DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//PORTMSTR DD DSN=PORTFOLIO.PORTMSTR.VSAM,DISP=SHR
//BCHCTL   DD DSN=PORTFOLIO.BCHCTL.VSAM,DISP=SHR
//REPORT   DD SYSOUT=*
//SYSOUT   DD SYSOUT=*
//*================================================================*
//* END OF JCL
//*================================================================*
