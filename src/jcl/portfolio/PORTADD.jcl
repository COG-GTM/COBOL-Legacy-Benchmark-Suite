//******************************************************************
//* JCL Name:    PORTADD                                           *
//* Description: Execute Portfolio Addition Program                *
//* Program:     PORTADD                                           *
//* Author: [Author name]                                          *
//* Date Written: 2024-03-20                                       *
//*                                                                *
//* Reads new portfolio records from the input file, validates     *
//* them via PORTVALD, and writes them to the Portfolio Master      *
//* VSAM file. Logs all additions to the audit trail.              *
//******************************************************************
//PORTADD   JOB (ACCT),'ADD PORTFOLIO',
//          CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//*
//STEP1    EXEC PGM=PORTADD
//STEPLIB   DD DSN=YOUR.LOADLIB,DISP=SHR
//* Portfolio Master VSAM KSDS file (output - new records added)
//PORTFILE  DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//* Input file containing new portfolio records to add
//INPTFILE  DD DSN=PORTFOLIO.INPUT.FILE,DISP=OLD
//SYSOUT    DD SYSOUT=*
//SYSPRINT  DD SYSOUT=*
//SYSUDUMP  DD SYSOUT=*   