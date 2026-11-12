      *****************************************************************
      * CODEC PARITY RECORD - one field per USAGE this suite supports
      *****************************************************************
       01  CODEC-RECORD.
           05  C-S4-COMP          PIC S9(4) COMP.
           05  C-U2-COMP          PIC 9(2) COMP.
           05  C-S8-COMP          PIC S9(8) COMP.
           05  C-S18-COMP         PIC S9(18) COMP.
           05  C-S4V2-COMP        PIC S9(4)V99 COMP.
           05  C-S9-BIN           PIC S9(9) BINARY.
           05  C-S4-COMP5         PIC S9(4) COMP-5.
           05  C-U4-COMPX         PIC 9(4) COMP-X.
           05  C-X2-COMPX         PIC X(2) COMP-X.
           05  C-S7V2-COMP3       PIC S9(7)V99 COMP-3.
           05  C-U5-COMP3         PIC 9(5) COMP-3.
           05  C-S5V2-DISP        PIC S9(5)V99.
           05  C-U6-DISP          PIC 9(6).
           05  C-SEP-DISP         PIC S9(5) SIGN LEADING SEPARATE.
           05  C-TEXT             PIC X(10).
