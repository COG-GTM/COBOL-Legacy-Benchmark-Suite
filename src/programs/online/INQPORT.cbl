        IDENTIFICATION DIVISION.
       PROGRAM-ID. INQPORT.
      *****************************************************************
      * Portfolio Position Inquiry Handler                              
      * - Retrieves current portfolio positions                        *
      * - Formats position data for display                            *
      * - Handles VSAM and DB2 access                                  *
      *****************************************************************
       
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       COPY INQCOM.
           
           COPY POSREC.
           
       01  WS-DB2-POSITION          PIC X(200).
           
       01  WS-FLAGS.
           05 WS-RESPONSE-CODE        PIC S9(8) COMP.
           05 WS-POSITION-FOUND       PIC X VALUE 'N'.
              88 POSITION-EXISTS           VALUE 'Y'.
              88 NO-POSITION               VALUE 'N'.
              
       01  WS-MAP-FIELDS.
           05 WS-ACCOUNT-LABEL        PIC X(10) VALUE 'Account:'.
           05 WS-FUND-LABEL          PIC X(10) VALUE 'Fund ID:'.
           05 WS-UNITS-LABEL         PIC X(10) VALUE 'Units:'.
           05 WS-COST-LABEL          PIC X(15) VALUE 'Cost Basis:'.
           05 WS-VALUE-LABEL         PIC X(15) VALUE 'Market Value:'.
           
       LINKAGE SECTION.
       01  DFHCOMMAREA              PIC X(200).
           
       PROCEDURE DIVISION.
           PERFORM P100-INIT-PROGRAM
              THRU P100-EXIT.
              
           PERFORM P200-GET-POSITION
              THRU P200-EXIT.
              
           IF POSITION-EXISTS
              PERFORM P300-FORMAT-DISPLAY
                 THRU P300-EXIT
           ELSE
              PERFORM P900-NOT-FOUND
                 THRU P900-EXIT
           END-IF.
              
           EXEC CICS RETURN END-EXEC.
           
       P100-INIT-PROGRAM.
           MOVE LOW-VALUES TO POSITION-RECORD
           MOVE DFHCOMMAREA TO INQCOM-AREA.
           
           EXEC CICS HANDLE CONDITION
                     ERROR(P999-ERROR-ROUTINE)
                     NOTFND(P900-NOT-FOUND)
           END-EXEC.
       P100-EXIT.
           EXIT.
           
       P200-GET-POSITION.
           MOVE INQCOM-ACCOUNT-NO 
             TO POS-PORTFOLIO-ID.
             
           EXEC CICS READ FILE('POSFILE')
                     INTO(POSITION-RECORD)
                     RIDFLD(POS-PORTFOLIO-ID)
                     RESP(WS-RESPONSE-CODE)
           END-EXEC.
           
           IF WS-RESPONSE-CODE = 0
              SET POSITION-EXISTS TO TRUE
           ELSE
              SET NO-POSITION TO TRUE
           END-IF.
       P200-EXIT.
           EXIT.
           
       P300-FORMAT-DISPLAY.
           EXEC CICS SEND MAP('POSMAP')
                     MAPSET('INQSET')
                     FROM(POSITION-RECORD)
                     ERASE
                     RESP(WS-RESPONSE-CODE)
           END-EXEC.
       P300-EXIT.
           EXIT.
           
       P900-NOT-FOUND.
           MOVE 'Position not found for account' 
             TO INQCOM-ERROR-MSG OF INQCOM-AREA.
           MOVE INQCOM-AREA TO DFHCOMMAREA.
       P900-EXIT.
           EXIT.
           
       P999-ERROR-ROUTINE.
           MOVE 'Error accessing position data' 
             TO INQCOM-ERROR-MSG OF INQCOM-AREA.
           MOVE WS-RESPONSE-CODE 
             TO INQCOM-RESPONSE-CODE OF INQCOM-AREA.
           MOVE INQCOM-AREA TO DFHCOMMAREA.
       P999-EXIT.
           EXIT.
