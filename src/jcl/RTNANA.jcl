//******************************************************************
//* JCL Name: RTNANA
//* Description: Execute Return Code Analysis Program
//*   Runs RTNANA00 to analyze return codes from prior batch
//*   steps and produce a summary report.
//* Program: RTNANA00
//* Output:  RPTFILE - Return code analysis report (SYSOUT)
//******************************************************************
//RTNANA00 JOB (ACCT),'RETURN CODE ANALYSIS',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//RTNANA   EXEC PGM=RTNANA00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//RPTFILE  DD SYSOUT=*,
//            DCB=(RECFM=FBA,LRECL=133,BLKSIZE=0)
//SYSOUT   DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSPRINT DD SYSOUT=*  