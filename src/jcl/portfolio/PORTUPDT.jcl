//******************************************************************
//* JCL Name:    PORTUPDT                                          *
//* Description: Execute Portfolio Update Program                  *
//* Program:     PORTUPDT                                          *
//* Author: [Author name]                                          *
//* Date Written: 2024-03-20                                       *
//*                                                                *
//* Reads update records from the update file, validates changes   *
//* via PORTVALD, applies modifications to existing records in the *
//* Portfolio Master VSAM file, and logs before/after images.      *
//******************************************************************
//PORTUPDT  JOB (ACCT),'UPDATE PORTFOLIO',
//          CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//*
//STEP1    EXEC PGM=PORTUPDT
//STEPLIB   DD DSN=YOUR.LOADLIB,DISP=SHR
//* Portfolio Master VSAM KSDS file (input/output)
//PORTFILE  DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//* Input file containing portfolio update records
//UPDTFILE  DD DSN=PORTFOLIO.UPDATE.FILE,DISP=OLD
//SYSOUT    DD SYSOUT=*
//SYSPRINT  DD SYSOUT=*
//SYSUDUMP  DD SYSOUT=*   