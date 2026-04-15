//******************************************************************
//* JCL Name: TSTGEN
//* Description: Execute Test Data Generator
//*   Runs TSTGEN00 to read a test configuration file and a
//*   random seed, then generates portfolio and transaction
//*   test datasets for benchmarking.
//* Program: TSTGEN00
//* Input:   TSTCFG   - Test configuration file (SHR)
//*          RANDSEED - Random seed file (SHR)
//* Output:  PORTOUT  - Generated portfolio data (NEW, FB/100)
//*          TRANOUT  - Generated transaction data (NEW, FB/100)
//******************************************************************
//TSTGEN00 JOB (ACCT#),'TEST DATA GEN',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=TSTGEN00
//STEPLIB  DD   DSN=TEST.LOAD.LIBRARY,DISP=SHR
//TSTCFG   DD   DSN=TEST.CONFIG.FILE,DISP=SHR
//PORTOUT  DD   DSN=TEST.PORTFOLIO.DATA,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(100,50),RLSE),
//             DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//TRANOUT  DD   DSN=TEST.TRANSACTION.DATA,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(100,50),RLSE),
//             DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//RANDSEED DD   DSN=TEST.RANDOM.SEED,DISP=SHR
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  