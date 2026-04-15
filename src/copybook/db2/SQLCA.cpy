      *================================================================*
      * Copybook Name: SQLCA                                           *
      * Description:  SQL Communication Area and Status Codes           *
      * Version: 1.0                                                   *
      * Date: 2024                                                     *
      *                                                                *
      * Includes the standard DB2 SQLCA (SQL Communication Area) and   *
      * defines named constants for commonly checked SQLSTATE values.   *
      * The SQLCA provides SQLCODE and SQLSTATE after every SQL call.  *
      * Programs check these values to determine success or failure.    *
      *                                                                *
      * Required by: All programs that execute SQL statements           *
      *================================================================*
      *    Include the standard DB2 SQLCA structure
           EXEC SQL INCLUDE SQLCA END-EXEC.
      
      *    Named SQLSTATE constants for common conditions
       01  SQL-STATUS-CODES.
           05  SQL-SUCCESS           PIC X(5) VALUE '00000'.
           05  SQL-NOT-FOUND        PIC X(5) VALUE '02000'.
           05  SQL-DUP-KEY          PIC X(5) VALUE '23505'.
           05  SQL-DEADLOCK         PIC X(5) VALUE '40001'.
           05  SQL-TIMEOUT          PIC X(5) VALUE '40003'.
           05  SQL-CONNECTION-ERROR PIC X(5) VALUE '08001'.
           05  SQL-DB-ERROR         PIC X(5) VALUE '58004'.  