      *================================================================*
      * DB2 Standard Procedures
      * Version: 1.0
      * Date: 2024
      *================================================================*
      
      *----------------------------------------------------------------*
      * SQL Error Handling
      *----------------------------------------------------------------*
       01  DB2-ERROR-HANDLING.
           05  DB2-ERROR-MESSAGE.
               10  FILLER          PIC X(9) VALUE 'SQLCODE: '.
               10  DB2-SQLCODE-TXT PIC X(6).
               10  FILLER          PIC X(9) VALUE ' STATE: '.
               10  DB2-STATE       PIC X(5).
               10  FILLER          PIC X(8) VALUE ' ERROR: '.
               10  DB2-ERROR-TEXT  PIC X(70).
           05  DB2-SAVE-STATUS     PIC X(5).
           05  DB2-RETRY-COUNT     PIC S9(4) COMP VALUE 0.
           05  DB2-MAX-RETRIES     PIC S9(4) COMP VALUE 3.
           05  DB2-RETRY-WAIT      PIC S9(4) COMP VALUE 100.
