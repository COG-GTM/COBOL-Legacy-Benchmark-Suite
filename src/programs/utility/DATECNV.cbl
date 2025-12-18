       IDENTIFICATION DIVISION.
       PROGRAM-ID. DATECNV.
      *================================================================*
      * Program Name: DATECNV
      * Description: Date Format Conversion Utility
      * Purpose: Convert between 8-digit (YYYYMMDD) and 10-digit
      *          (YYYY-MM-DD) date formats
      * Author: Date Migration Team
      * Date Written: 2024-12-18
      * Maintenance Log:
      * Date       Author        Description
      * ---------- ------------- -------------------------------------
      * 2024-12-18 Migration     Initial Creation for date migration
      *================================================================*
      *
      * FUNCTIONS:
      * 1 = Convert 8-digit to 10-digit (YYYYMMDD -> YYYY-MM-DD)
      * 2 = Convert 10-digit to 8-digit (YYYY-MM-DD -> YYYYMMDD)
      * 3 = Convert ACCEPT FROM DATE output to 10-digit
      *
      *================================================================*
       
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       
       01  WS-WORK-FIELDS.
           05  WS-VALID-DATE        PIC X(1) VALUE 'Y'.
               88  WS-DATE-VALID    VALUE 'Y'.
               88  WS-DATE-INVALID  VALUE 'N'.
           05  WS-YEAR              PIC 9(4).
           05  WS-MONTH             PIC 9(2).
           05  WS-DAY               PIC 9(2).
           05  WS-MAX-DAYS          PIC 9(2).
           05  WS-LEAP-YEAR         PIC X(1).
               88  WS-IS-LEAP       VALUE 'Y'.
               88  WS-NOT-LEAP      VALUE 'N'.
           05  WS-REMAINDER         PIC 9(4).
       
       LINKAGE SECTION.
       
       01  LS-DATE-CONVERSION-AREA.
           05  LS-INPUT-DATE-8.
               10  LS-IN-YYYY       PIC X(4).
               10  LS-IN-MM         PIC X(2).
               10  LS-IN-DD         PIC X(2).
           05  LS-OUTPUT-DATE-10.
               10  LS-OUT-YYYY      PIC X(4).
               10  LS-OUT-HYPHEN1   PIC X(1).
               10  LS-OUT-MM        PIC X(2).
               10  LS-OUT-HYPHEN2   PIC X(1).
               10  LS-OUT-DD        PIC X(2).
           05  LS-WORK-AREA.
               10  LS-WORK-DATE-8   PIC X(8).
               10  LS-WORK-DATE-10  PIC X(10).
               10  LS-RETURN-CODE   PIC 9(2).
           05  LS-FUNCTION-CODE     PIC X(1).
       
       PROCEDURE DIVISION USING LS-DATE-CONVERSION-AREA.
       
       0000-MAIN-PROCESS.
           INITIALIZE LS-RETURN-CODE
           SET WS-DATE-VALID TO TRUE
           
           EVALUATE LS-FUNCTION-CODE
               WHEN '1'
                   PERFORM 1000-CONVERT-8-TO-10
               WHEN '2'
                   PERFORM 2000-CONVERT-10-TO-8
               WHEN '3'
                   PERFORM 3000-CONVERT-ACCEPT-DATE
               WHEN OTHER
                   MOVE 01 TO LS-RETURN-CODE
           END-EVALUATE
           
           GOBACK.
       
      *================================================================*
      * 1000-CONVERT-8-TO-10
      * Convert YYYYMMDD to YYYY-MM-DD
      *================================================================*
       1000-CONVERT-8-TO-10.
           IF LS-INPUT-DATE-8 = SPACES OR LOW-VALUES
               MOVE 01 TO LS-RETURN-CODE
               GO TO 1000-EXIT
           END-IF
           
           PERFORM 9000-VALIDATE-DATE-8
           
           IF WS-DATE-INVALID
               MOVE 02 TO LS-RETURN-CODE
               GO TO 1000-EXIT
           END-IF
           
           MOVE LS-IN-YYYY TO LS-OUT-YYYY
           MOVE '-'        TO LS-OUT-HYPHEN1
           MOVE LS-IN-MM   TO LS-OUT-MM
           MOVE '-'        TO LS-OUT-HYPHEN2
           MOVE LS-IN-DD   TO LS-OUT-DD
           
           MOVE LS-OUTPUT-DATE-10 TO LS-WORK-DATE-10
           MOVE 00 TO LS-RETURN-CODE.
       
       1000-EXIT.
           EXIT.
       
      *================================================================*
      * 2000-CONVERT-10-TO-8
      * Convert YYYY-MM-DD to YYYYMMDD
      *================================================================*
       2000-CONVERT-10-TO-8.
           IF LS-OUTPUT-DATE-10 = SPACES OR LOW-VALUES
               MOVE 01 TO LS-RETURN-CODE
               GO TO 2000-EXIT
           END-IF
           
           IF LS-OUT-HYPHEN1 NOT = '-' OR
              LS-OUT-HYPHEN2 NOT = '-'
               MOVE 01 TO LS-RETURN-CODE
               GO TO 2000-EXIT
           END-IF
           
           MOVE LS-OUT-YYYY TO LS-IN-YYYY
           MOVE LS-OUT-MM   TO LS-IN-MM
           MOVE LS-OUT-DD   TO LS-IN-DD
           
           PERFORM 9000-VALIDATE-DATE-8
           
           IF WS-DATE-INVALID
               MOVE 02 TO LS-RETURN-CODE
               GO TO 2000-EXIT
           END-IF
           
           MOVE LS-INPUT-DATE-8 TO LS-WORK-DATE-8
           MOVE 00 TO LS-RETURN-CODE.
       
       2000-EXIT.
           EXIT.
       
      *================================================================*
      * 3000-CONVERT-ACCEPT-DATE
      * Convert ACCEPT FROM DATE YYYYMMDD output to YYYY-MM-DD
      * Note: ACCEPT FROM DATE always returns 8 digits
      *================================================================*
       3000-CONVERT-ACCEPT-DATE.
           PERFORM 1000-CONVERT-8-TO-10.
       
      *================================================================*
      * 9000-VALIDATE-DATE-8
      * Validate 8-digit date format
      *================================================================*
       9000-VALIDATE-DATE-8.
           SET WS-DATE-VALID TO TRUE
           
           IF LS-INPUT-DATE-8 NOT NUMERIC
               SET WS-DATE-INVALID TO TRUE
               GO TO 9000-EXIT
           END-IF
           
           MOVE LS-IN-YYYY TO WS-YEAR
           MOVE LS-IN-MM   TO WS-MONTH
           MOVE LS-IN-DD   TO WS-DAY
           
           IF WS-YEAR < 1900 OR WS-YEAR > 2099
               SET WS-DATE-INVALID TO TRUE
               GO TO 9000-EXIT
           END-IF
           
           IF WS-MONTH < 01 OR WS-MONTH > 12
               SET WS-DATE-INVALID TO TRUE
               GO TO 9000-EXIT
           END-IF
           
           PERFORM 9100-CHECK-LEAP-YEAR
           PERFORM 9200-GET-MAX-DAYS
           
           IF WS-DAY < 01 OR WS-DAY > WS-MAX-DAYS
               SET WS-DATE-INVALID TO TRUE
           END-IF.
       
       9000-EXIT.
           EXIT.
       
      *================================================================*
      * 9100-CHECK-LEAP-YEAR
      * Determine if year is a leap year
      *================================================================*
       9100-CHECK-LEAP-YEAR.
           SET WS-NOT-LEAP TO TRUE
           
           DIVIDE WS-YEAR BY 4 GIVING WS-YEAR 
                               REMAINDER WS-REMAINDER
           IF WS-REMAINDER = 0
               SET WS-IS-LEAP TO TRUE
               DIVIDE WS-YEAR BY 100 GIVING WS-YEAR
                                     REMAINDER WS-REMAINDER
               IF WS-REMAINDER = 0
                   SET WS-NOT-LEAP TO TRUE
                   DIVIDE WS-YEAR BY 400 GIVING WS-YEAR
                                         REMAINDER WS-REMAINDER
                   IF WS-REMAINDER = 0
                       SET WS-IS-LEAP TO TRUE
                   END-IF
               END-IF
           END-IF
           
           MOVE LS-IN-YYYY TO WS-YEAR.
       
      *================================================================*
      * 9200-GET-MAX-DAYS
      * Get maximum days for the month
      *================================================================*
       9200-GET-MAX-DAYS.
           EVALUATE WS-MONTH
               WHEN 01 MOVE 31 TO WS-MAX-DAYS
               WHEN 02 
                   IF WS-IS-LEAP
                       MOVE 29 TO WS-MAX-DAYS
                   ELSE
                       MOVE 28 TO WS-MAX-DAYS
                   END-IF
               WHEN 03 MOVE 31 TO WS-MAX-DAYS
               WHEN 04 MOVE 30 TO WS-MAX-DAYS
               WHEN 05 MOVE 31 TO WS-MAX-DAYS
               WHEN 06 MOVE 30 TO WS-MAX-DAYS
               WHEN 07 MOVE 31 TO WS-MAX-DAYS
               WHEN 08 MOVE 31 TO WS-MAX-DAYS
               WHEN 09 MOVE 30 TO WS-MAX-DAYS
               WHEN 10 MOVE 31 TO WS-MAX-DAYS
               WHEN 11 MOVE 30 TO WS-MAX-DAYS
               WHEN 12 MOVE 31 TO WS-MAX-DAYS
               WHEN OTHER MOVE 0 TO WS-MAX-DAYS
           END-EVALUATE.
