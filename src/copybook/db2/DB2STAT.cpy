      *================================================================*
      * Copybook Name: DB2STAT
      * Description: DB2 Statistics Record Layout
      * Author: [Author name]
      * Date Written: 2024-03-20
      *================================================================*
       FD  DB2-STATS.
       01  DB2-STAT-RECORD.
           05  STAT-KEY.
               10  STAT-SUBSYS-ID     PIC X(4).
               10  STAT-DATE          PIC X(8).
               10  STAT-TIME          PIC X(6).
           05  STAT-DATA.
               10  STAT-BUFFER-HITS   PIC 9(9) COMP.
               10  STAT-GETPAGES      PIC 9(9) COMP.
               10  STAT-SYNC-IO       PIC 9(9) COMP.
               10  STAT-DEADLOCKS     PIC 9(5) COMP.
               10  STAT-TIMEOUTS      PIC 9(5) COMP.
           05  STAT-FILLER            PIC X(50).
