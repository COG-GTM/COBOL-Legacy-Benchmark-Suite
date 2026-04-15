//******************************************************************
//* JCL Name:    TSTGEN                                            *
//* Description: Execute Test Data Generator                       *
//* Program:     TSTGEN00                                          *
//*                                                                *
//* Generates synthetic test data (portfolios and transactions)    *
//* based on configuration parameters. Output volume and type are  *
//* controlled by TSTCFG. Used for benchmarking and regression     *
//* testing of the batch processing pipeline.                       *
//******************************************************************
//TSTGEN00 JOB (ACCT#),'TEST DATA GEN',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=TSTGEN00
//STEPLIB  DD   DSN=TEST.LOAD.LIBRARY,DISP=SHR
//* Input: Test configuration (type, volume, parameters)
//TSTCFG   DD   DSN=TEST.CONFIG.FILE,DISP=SHR
//* Output: Generated portfolio test records (FB 100, 100+50 cyl)
//PORTOUT  DD   DSN=TEST.PORTFOLIO.DATA,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(100,50),RLSE),
//             DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//* Output: Generated transaction test records (FB 100, 100+50 cyl)
//TRANOUT  DD   DSN=TEST.TRANSACTION.DATA,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(100,50),RLSE),
//             DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//* Input: Random seed for reproducible test data generation
//RANDSEED DD   DSN=TEST.RANDOM.SEED,DISP=SHR
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   