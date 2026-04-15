//******************************************************************
//* JCL Name:    UTLMNT                                            *
//* Description: Execute File Maintenance Utility                  *
//* Program:     UTLMNT00                                          *
//*                                                                *
//* Performs scheduled file maintenance: archives aged records,    *
//* reorganizes VSAM files, validates file integrity, and purges   *
//* expired data per retention rules in the control file.          *
//******************************************************************
//UTLMNT00 JOB (ACCT#),'FILE MAINTENANCE',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=UTLMNT00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: Control file with retention rules and maintenance params
//CTLFILE  DD   DSN=PROD.CONTROL.FILE,DISP=SHR
//* Output: Archived records (VB 32756 for variable-length records)
//ARCHFILE DD   DSN=PROD.ARCHIVE.FILE,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(100,50),RLSE),
//             DCB=(RECFM=VB,LRECL=32756,BLKSIZE=0)
//* Output: Maintenance activity report (FB 132)
//RPTFILE  DD   DSN=PROD.MAINTENANCE.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   