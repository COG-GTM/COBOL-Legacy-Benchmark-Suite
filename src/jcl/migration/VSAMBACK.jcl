//*================================================================*
//* JCL Name: VSAMBACK
//* Description: VSAM Backup for Date Field Migration
//* Purpose: Create backup copies of all VSAM files before
//*          date field migration from 8 to 10 digits
//* Author: Date Migration Team
//* Date Written: 2024-12-18
//*================================================================*
//VSAMBACK JOB (ACCT),'VSAM BACKUP',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,REGION=0M
//*
//*================================================================*
//* STEP 1: BACKUP TRANSACTION HISTORY FILE (TRANHIST)
//*================================================================*
//BKTRN    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.TRANHIST.VSAM,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.TRANHIST.BACKUP,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,20)),
//            DCB=(RECFM=VB,LRECL=304,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
//*
//*================================================================*
//* STEP 2: BACKUP POSITION HISTORY FILE (POSHIST)
//*================================================================*
//BKPOS    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.POSHIST.BACKUP,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,20)),
//            DCB=(RECFM=VB,LRECL=354,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
//*
//*================================================================*
//* STEP 3: BACKUP PORTFOLIO MASTER FILE (PORTMSTR)
//*================================================================*
//BKPORT   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.PORTMSTR.VSAM,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.PORTMSTR.BACKUP,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(50,10)),
//            DCB=(RECFM=VB,LRECL=404,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
//*
//*================================================================*
//* STEP 4: BACKUP BATCH CONTROL FILE
//*================================================================*
//BKBCH    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.BCHCTL.VSAM,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.BCHCTL.BACKUP,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(10,5)),
//            DCB=(RECFM=VB,LRECL=500,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
//*
//*================================================================*
//* STEP 5: BACKUP CHECKPOINT FILE
//*================================================================*
//BKCKP    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.CKPRST.VSAM,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.CKPRST.BACKUP,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(5,2)),
//            DCB=(RECFM=VB,LRECL=420,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
//*================================================================*
//* END OF JCL
//*================================================================*
