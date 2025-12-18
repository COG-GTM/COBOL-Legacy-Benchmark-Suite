      *================================================================*
      * Copybook Name: DATECNV
      * Description: Date Conversion Area Definitions
      * Purpose: Support conversion between 8-digit (YYYYMMDD) and
      *          10-digit (YYYY-MM-DD) date formats
      * Author: Date Migration Team
      * Date Written: 2024-12-18
      * Maintenance Log:
      * Date       Author        Description
      * ---------- ------------- -------------------------------------
      * 2024-12-18 Migration     Initial Creation for date migration
      *================================================================*
       01  DATE-CONVERSION-AREA.
           05  DC-INPUT-DATE-8.
               10  DC-IN-YYYY        PIC X(4).
               10  DC-IN-MM          PIC X(2).
               10  DC-IN-DD          PIC X(2).
           05  DC-OUTPUT-DATE-10.
               10  DC-OUT-YYYY       PIC X(4).
               10  DC-OUT-HYPHEN1    PIC X(1).
               10  DC-OUT-MM         PIC X(2).
               10  DC-OUT-HYPHEN2    PIC X(1).
               10  DC-OUT-DD         PIC X(2).
           05  DC-WORK-AREA.
               10  DC-WORK-DATE-8    PIC X(8).
               10  DC-WORK-DATE-10   PIC X(10).
               10  DC-RETURN-CODE    PIC 9(2).
                   88  DC-SUCCESS       VALUE 00.
                   88  DC-INVALID-INPUT VALUE 01.
                   88  DC-INVALID-DATE  VALUE 02.
           05  DC-FUNCTION-CODE      PIC X(1).
               88  DC-FUNC-8-TO-10   VALUE '1'.
               88  DC-FUNC-10-TO-8   VALUE '2'.
               88  DC-FUNC-ACCEPT    VALUE '3'.
      *================================================================*
      * USAGE:
      * 1. To convert YYYYMMDD to YYYY-MM-DD:
      *    MOVE '1' TO DC-FUNCTION-CODE
      *    MOVE input-date TO DC-INPUT-DATE-8
      *    PERFORM CONVERT-DATE-FORMAT
      *    Result in DC-OUTPUT-DATE-10
      *
      * 2. To convert YYYY-MM-DD to YYYYMMDD:
      *    MOVE '2' TO DC-FUNCTION-CODE
      *    MOVE input-date TO DC-OUTPUT-DATE-10
      *    PERFORM CONVERT-DATE-FORMAT
      *    Result in DC-INPUT-DATE-8
      *
      * 3. To convert ACCEPT FROM DATE output:
      *    MOVE '3' TO DC-FUNCTION-CODE
      *    MOVE accept-date TO DC-INPUT-DATE-8
      *    PERFORM CONVERT-DATE-FORMAT
      *    Result in DC-OUTPUT-DATE-10
      *================================================================*
