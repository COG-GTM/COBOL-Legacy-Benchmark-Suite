      *================================================================*
      * Program Name: PDIVZER
      * Description: Isolated parity harness for the RPTPOS00
      *              2110-FORMAT-POSITION divide-by-zero condition
      *              (POS-PREVIOUS-VALUE = 0). RPTPOS00 codes the
      *              COMPUTE with no ON SIZE ERROR clause, so the
      *              behaviour is undefined by the standard; this
      *              harness records what the runtime actually does.
      *              Kept separate from PARITHM because the case can
      *              terminate the run unit.
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PDIVZER.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CUR-VALUE           PIC S9(13)V9(2) COMP-3 VALUE +100.
       01  WS-PRV-VALUE           PIC S9(13)V9(2) COMP-3 VALUE 0.
       01  WS-CHANGE-PCT          PIC +ZZ9.99 VALUE '+999.99'.

       PROCEDURE DIVISION.
       0000-MAIN.
           DISPLAY 'op|a|b|result|edited'
           COMPUTE WS-CHANGE-PCT =
               (WS-CUR-VALUE - WS-PRV-VALUE) /
                WS-PRV-VALUE * 100
           DISPLAY 'PZ|100.0000|0.0000|'
                   FUNCTION TRIM(WS-CHANGE-PCT) '|'
                   WS-CHANGE-PCT
           GOBACK.
