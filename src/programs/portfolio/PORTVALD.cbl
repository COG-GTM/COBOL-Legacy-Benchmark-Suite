      *================================================================*
      * Program Name: PORTVALD
      * Description: Portfolio Validation Subroutine
      *             Callable service that validates individual
      *             portfolio data elements against business rules.
      *
      * Validation Types (via LS-VALIDATE-TYPE):
      *   I - Portfolio ID: Must start with 'PORT' + 4 numeric digits
      *   A - Account Number: Must be 10 numeric digits, non-zero
      *   T - Investment Type: Must be STK, BND, MMF, or ETF
      *   M - Amount: Must be within configured min/max range
      *
      * Called By:   PORTMSTR, PORTADD, PORTTRAN, and other programs
      *              needing field-level validation.
      *
      * Copybooks:   PORTVAL  - Validation constants and work areas
      *
      * Return Codes: VAL-SUCCESS or specific VAL-INVALID-* code
      *
      * Author: [Author name]
      * Date Written: 2024-03-20
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PORTVALD.
       
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-ZOS.
       OBJECT-COMPUTER. IBM-ZOS.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           COPY PORTVAL.
           
       LINKAGE SECTION.
      *    Request area passed by calling program
       01  LS-VALIDATION-REQUEST.
      *    Validation type: I=ID, A=Account, T=Type, M=Amount
           05  LS-VALIDATE-TYPE    PIC X(1).
               88  LS-VAL-ID         VALUE 'I'.
               88  LS-VAL-ACCT       VALUE 'A'.
               88  LS-VAL-TYPE       VALUE 'T'.
               88  LS-VAL-AMT        VALUE 'M'.
      *    Value to validate (left-justified, space-padded)
           05  LS-INPUT-VALUE      PIC X(50).
      *    Result code set by this program
           05  LS-RETURN-CODE      PIC S9(4) COMP.
      *    Descriptive error message (spaces if valid)
           05  LS-ERROR-MSG        PIC X(50).
       
       PROCEDURE DIVISION USING LS-VALIDATION-REQUEST.
      *----------------------------------------------------------------*
      * Main dispatch: route to the appropriate validation routine     *
      * based on the requested validation type code.                   *
      *----------------------------------------------------------------*
       0000-MAIN.
           INITIALIZE VAL-WORK-AREAS
           
           EVALUATE TRUE
               WHEN LS-VAL-ID
                   PERFORM 1000-VALIDATE-ID
               WHEN LS-VAL-ACCT
                   PERFORM 2000-VALIDATE-ACCOUNT
               WHEN LS-VAL-TYPE
                   PERFORM 3000-VALIDATE-TYPE
               WHEN LS-VAL-AMT
                   PERFORM 4000-VALIDATE-AMOUNT
               WHEN OTHER
                   MOVE VAL-INVALID-ID TO LS-RETURN-CODE
                   MOVE 'Invalid validation type' TO LS-ERROR-MSG
           END-EVALUATE
           
           GOBACK
           .
           
       1000-VALIDATE-ID.
      *----------------------------------------------------------------*
      * 1000-VALIDATE-ID: Portfolio ID must start with 'PORT' prefix   *
      * followed by exactly 4 numeric digits (e.g., PORT0001).         *
      *----------------------------------------------------------------*
           IF LS-INPUT-VALUE(1:4) NOT = VAL-ID-PREFIX
               MOVE VAL-INVALID-ID TO LS-RETURN-CODE
               MOVE VAL-ERR-ID TO LS-ERROR-MSG
               EXIT PARAGRAPH
           END-IF
           
           MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK
           IF VAL-NUMERIC-CHECK IS NOT NUMERIC
               MOVE VAL-INVALID-ID TO LS-RETURN-CODE
               MOVE VAL-ERR-ID TO LS-ERROR-MSG
               EXIT PARAGRAPH
           END-IF
           
           MOVE VAL-SUCCESS TO LS-RETURN-CODE
           MOVE SPACES TO LS-ERROR-MSG
           .
           
       2000-VALIDATE-ACCOUNT.
      *----------------------------------------------------------------*
      * 2000-VALIDATE-ACCOUNT: Account number must be exactly 10       *
      * numeric digits and cannot be all zeros.                        *
      *----------------------------------------------------------------*
           IF LS-INPUT-VALUE IS NOT NUMERIC
           OR LS-INPUT-VALUE = ZEROS
               MOVE VAL-INVALID-ACCT TO LS-RETURN-CODE
               MOVE VAL-ERR-ACCT TO LS-ERROR-MSG
               EXIT PARAGRAPH
           END-IF
           
           MOVE VAL-SUCCESS TO LS-RETURN-CODE
           MOVE SPACES TO LS-ERROR-MSG
           .
           
       3000-VALIDATE-TYPE.
      *----------------------------------------------------------------*
      * 3000-VALIDATE-TYPE: Investment type must be one of:            *
      *   STK (Stock), BND (Bond), MMF (Money Market), ETF (ETF)      *
      *----------------------------------------------------------------*
           IF LS-INPUT-VALUE NOT = 'STK'
              AND NOT = 'BND'
              AND NOT = 'MMF'
              AND NOT = 'ETF'
               MOVE VAL-INVALID-TYPE TO LS-RETURN-CODE
               MOVE VAL-ERR-TYPE TO LS-ERROR-MSG
               EXIT PARAGRAPH
           END-IF
           
           MOVE VAL-SUCCESS TO LS-RETURN-CODE
           MOVE SPACES TO LS-ERROR-MSG
           .
           
       4000-VALIDATE-AMOUNT.
      *----------------------------------------------------------------*
      * 4000-VALIDATE-AMOUNT: Numeric amount must fall within the      *
      * configured VAL-MIN-AMOUNT and VAL-MAX-AMOUNT boundaries.       *
      *----------------------------------------------------------------*
           MOVE LS-INPUT-VALUE TO VAL-TEMP-NUM
           
           IF VAL-TEMP-NUM < VAL-MIN-AMOUNT
           OR VAL-TEMP-NUM > VAL-MAX-AMOUNT
               MOVE VAL-INVALID-AMT TO LS-RETURN-CODE
               MOVE VAL-ERR-AMT TO LS-ERROR-MSG
               EXIT PARAGRAPH
           END-IF
           
           MOVE VAL-SUCCESS TO LS-RETURN-CODE
           MOVE SPACES TO LS-ERROR-MSG
           .  