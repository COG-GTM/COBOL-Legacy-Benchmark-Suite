//*================================================================*
//* JCL Name: VSAMREDF
//* Description: VSAM Redefinition for Date Field Migration
//* Purpose: Delete and redefine VSAM clusters with new key lengths
//*          to accommodate 10-digit date format (YYYY-MM-DD)
//* Author: Date Migration Team
//* Date Written: 2024-12-18
//*
//* KEY LENGTH CHANGES:
//* - TRANHIST: 20 -> 22 bytes (+2 for date hyphens)
//* - POSHIST:  18 -> 20 bytes (+2 for date hyphens)
//* - BCHCTL:   Key includes date field, length increases by 2
//* - CKPRST:   Key includes date field, length increases by 2
//*================================================================*
//VSAMREDF JOB (ACCT),'VSAM REDEFINE',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,REGION=0M
//*
//*================================================================*
//* STEP 1: DELETE AND REDEFINE TRANSACTION HISTORY (TRANHIST)
//* Old Key: TRN-DATE(8) + TRN-TIME(6) + TRN-PORTFOLIO-ID(8) = 22
//* New Key: TRN-DATE(10) + TRN-TIME(6) + TRN-PORTFOLIO-ID(8) = 24
//*================================================================*
//DEFTRN   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DELETE PORTFOLIO.TRANHIST.VSAM CLUSTER PURGE
  SET MAXCC = 0
  DEFINE CLUSTER                                        -
         (NAME(PORTFOLIO.TRANHIST.VSAM)                 -
          VOLUMES(VSAM01)                               -
          CYLINDERS(100 20)                             -
          KEYS(24 0)                                    -
          RECORDSIZE(302 302)                           -
          FREESPACE(10 10)                              -
          INDEXED                                       -
          SHAREOPTIONS(2 3))                            -
         DATA                                           -
         (NAME(PORTFOLIO.TRANHIST.VSAM.DATA))           -
         INDEX                                          -
         (NAME(PORTFOLIO.TRANHIST.VSAM.INDEX))
/*
//*
//*================================================================*
//* STEP 2: DELETE AND REDEFINE POSITION HISTORY (POSHIST)
//* Old Key: POS-PORTFOLIO-ID(8) + POS-DATE(8) + POS-INVESTMENT-ID(10) = 26
//* New Key: POS-PORTFOLIO-ID(8) + POS-DATE(10) + POS-INVESTMENT-ID(10) = 28
//*================================================================*
//DEFPOS   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DELETE PORTFOLIO.POSHIST.VSAM CLUSTER PURGE
  SET MAXCC = 0
  DEFINE CLUSTER                                        -
         (NAME(PORTFOLIO.POSHIST.VSAM)                  -
          VOLUMES(VSAM01)                               -
          CYLINDERS(100 20)                             -
          KEYS(28 0)                                    -
          RECORDSIZE(352 352)                           -
          FREESPACE(10 10)                              -
          INDEXED                                       -
          SHAREOPTIONS(2 3))                            -
         DATA                                           -
         (NAME(PORTFOLIO.POSHIST.VSAM.DATA))            -
         INDEX                                          -
         (NAME(PORTFOLIO.POSHIST.VSAM.INDEX))
/*
//*
//*================================================================*
//* STEP 3: DELETE AND REDEFINE BATCH CONTROL FILE
//* Old Key: BCT-JOB-NAME(8) + BCT-PROCESS-DATE(8) + BCT-SEQUENCE-NO(4) = 20
//* New Key: BCT-JOB-NAME(8) + BCT-PROCESS-DATE(10) + BCT-SEQUENCE-NO(4) = 22
//*================================================================*
//DEFBCH   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DELETE PORTFOLIO.BCHCTL.VSAM CLUSTER PURGE
  SET MAXCC = 0
  DEFINE CLUSTER                                        -
         (NAME(PORTFOLIO.BCHCTL.VSAM)                   -
          VOLUMES(VSAM01)                               -
          CYLINDERS(10 5)                               -
          KEYS(22 0)                                    -
          RECORDSIZE(500 500)                           -
          FREESPACE(20 20)                              -
          INDEXED                                       -
          SHAREOPTIONS(2 3))                            -
         DATA                                           -
         (NAME(PORTFOLIO.BCHCTL.VSAM.DATA))             -
         INDEX                                          -
         (NAME(PORTFOLIO.BCHCTL.VSAM.INDEX))
/*
//*
//*================================================================*
//* STEP 4: DELETE AND REDEFINE CHECKPOINT FILE
//* Old Key: CKR-PROGRAM-ID(8) + CKR-RUN-DATE(8) = 16
//* New Key: CKR-PROGRAM-ID(8) + CKR-RUN-DATE(10) = 18
//*================================================================*
//DEFCKP   EXEC PGM=IDCAMS
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
//*================================================================*
//* END OF JCL
//*================================================================*
