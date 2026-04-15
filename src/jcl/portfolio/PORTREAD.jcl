//******************************************************************
//* JCL Name:    PORTREAD                                          *
//* Description: Execute Portfolio Reading Program                 *
//* Program:     PORTREAD                                          *
//* Author: [Author name]                                          *
//* Date Written: 2024-03-20                                       *
//*                                                                *
//* Reads and retrieves portfolio records from the Portfolio Master *
//* VSAM KSDS file by key. Supports both direct-key lookup and     *
//* sequential browse operations. Results written to SYSOUT.        *
//******************************************************************
//PORTREAD  JOB (ACCT),'READ PORTFOLIO',
//          CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//*
//STEP1    EXEC PGM=PORTREAD
//STEPLIB   DD DSN=YOUR.LOADLIB,DISP=SHR
//* Portfolio Master VSAM KSDS file (input - read only)
//PORTFILE  DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//SYSOUT    DD SYSOUT=*
//SYSPRINT  DD SYSOUT=*
//SYSUDUMP  DD SYSOUT=*   