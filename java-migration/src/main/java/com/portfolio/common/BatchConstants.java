package com.portfolio.common;

public final class BatchConstants {
  private BatchConstants() {}

  public static final String BCT_STAT_READY = "R",
      BCT_STAT_ACTIVE = "A",
      BCT_STAT_WAITING = "W",
      BCT_STAT_DONE = "D",
      BCT_STAT_ERROR = "E";
  public static final int MAX_PREREQ = 10,
      MAX_RESTARTS = 3,
      WAIT_INTERVAL = 300,
      MAX_WAIT_TIME = 3600,
      MAX_DEP_RC = 4,
      HISTLD_COMMIT_THRESHOLD = 1000;
  public static final String DEP_REQUIRED = "R",
      DEP_OPTIONAL = "O",
      DEP_EXCLUSIVE = "X",
      PSR_DEP_HARD = "H",
      PSR_DEP_SOFT = "S";
  public static final String MSG_STARTING = "Process starting...",
      MSG_COMPLETE = "Process completed successfully",
      MSG_FAILED = "Process failed - check errors",
      MSG_WAITING = "Waiting for prerequisites";
}
