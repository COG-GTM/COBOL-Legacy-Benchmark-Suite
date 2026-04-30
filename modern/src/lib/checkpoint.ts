/**
 * Checkpoint/Restart Module
 * Ported from: src/copybook/common/CKPRST.cpy and src/programs/batch/CKPRST.cbl
 *
 * Replaces VSAM-based checkpoint/restart with database-backed idempotent
 * job processing. Tracks processed record counts and allows resumption
 * from the last committed checkpoint.
 */

import { prisma } from "./prisma";
import {
  CheckpointStatus,
  CheckpointPhase,
  type CheckpointState,
} from "../types";

export class CheckpointManager {
  private programId: string;
  private runDate: Date;
  private commitFrequency: number;
  private maxErrors: number;
  private maxRestarts: number;
  private recordsSinceCommit = 0;

  constructor(
    programId: string,
    runDate: Date,
    options?: {
      commitFrequency?: number;
      maxErrors?: number;
      maxRestarts?: number;
    }
  ) {
    this.programId = programId;
    this.runDate = runDate;
    this.commitFrequency = options?.commitFrequency ?? 1000;
    this.maxErrors = options?.maxErrors ?? 100;
    this.maxRestarts = options?.maxRestarts ?? 3;
  }

  /**
   * Initialize checkpoint processing.
   * If a previous run exists and failed, attempt restart.
   * Mirrors PROC-INIT from CKPRST.cbl.
   */
  async initialize(): Promise<CheckpointState | null> {
    const existing = await prisma.checkpoint.findUnique({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
    });

    if (existing) {
      if (
        existing.status === CheckpointStatus.FAILED ||
        existing.status === CheckpointStatus.ACTIVE
      ) {
        if (existing.restartCount >= this.maxRestarts) {
          throw new Error(
            `Max restarts (${this.maxRestarts}) exceeded for ${this.programId}`
          );
        }

        await prisma.checkpoint.update({
          where: { id: existing.id },
          data: {
            status: CheckpointStatus.RESTARTED,
            restartCount: existing.restartCount + 1,
            lastTime: new Date(),
          },
        });

        return {
          programId: existing.programId,
          recordsRead: existing.recordsRead,
          recordsProcessed: existing.recordsProc,
          recordsError: existing.recordsError,
          lastKey: existing.lastKey,
          phase: existing.phase as CheckpointPhase,
        };
      }

      if (existing.status === CheckpointStatus.COMPLETE) {
        return null;
      }
    }

    await prisma.checkpoint.upsert({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
      update: {
        status: CheckpointStatus.ACTIVE,
        runTime: new Date().toISOString().slice(11, 17).replace(/:/g, ""),
        lastTime: new Date(),
      },
      create: {
        programId: this.programId,
        runDate: this.runDate,
        runTime: new Date().toISOString().slice(11, 17).replace(/:/g, ""),
        status: CheckpointStatus.ACTIVE,
        commitFreq: this.commitFrequency,
        maxErrors: this.maxErrors,
        maxRestarts: this.maxRestarts,
      },
    });

    return null;
  }

  /**
   * Take a checkpoint after processing a record.
   * Mirrors PROC-TAKE-CHECKPOINT from CKPRST.cbl.
   * Commits to DB every `commitFrequency` records.
   */
  async takeCheckpoint(state: CheckpointState): Promise<boolean> {
    this.recordsSinceCommit++;

    if (state.recordsError > this.maxErrors) {
      await this.markFailed(state, "Maximum error count exceeded");
      return false;
    }

    if (this.recordsSinceCommit >= this.commitFrequency) {
      await this.commitCheckpoint(state);
      this.recordsSinceCommit = 0;
    }

    return true;
  }

  /**
   * Commit the current checkpoint state to the database.
   * Mirrors PROC-COMMIT-CHECKPOINT from CKPRST.cbl.
   */
  async commitCheckpoint(state: CheckpointState): Promise<void> {
    await prisma.checkpoint.update({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
      data: {
        recordsRead: state.recordsRead,
        recordsProc: state.recordsProcessed,
        recordsError: state.recordsError,
        lastKey: state.lastKey,
        phase: state.phase,
        lastTime: new Date(),
        status: CheckpointStatus.ACTIVE,
      },
    });
  }

  /**
   * Mark the checkpoint as complete.
   */
  async markComplete(state: CheckpointState): Promise<void> {
    await prisma.checkpoint.update({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
      data: {
        recordsRead: state.recordsRead,
        recordsProc: state.recordsProcessed,
        recordsError: state.recordsError,
        lastKey: state.lastKey,
        phase: CheckpointPhase.TERMINATE,
        lastTime: new Date(),
        status: CheckpointStatus.COMPLETE,
      },
    });
  }

  /**
   * Mark the checkpoint as failed.
   */
  async markFailed(state: CheckpointState, reason: string): Promise<void> {
    await prisma.checkpoint.update({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
      data: {
        recordsRead: state.recordsRead,
        recordsProc: state.recordsProcessed,
        recordsError: state.recordsError,
        lastKey: state.lastKey,
        lastTime: new Date(),
        status: CheckpointStatus.FAILED,
      },
    });
  }

  /**
   * Get the current checkpoint state for resumption.
   */
  async getCheckpointState(): Promise<CheckpointState | null> {
    const checkpoint = await prisma.checkpoint.findUnique({
      where: {
        programId_runDate: {
          programId: this.programId,
          runDate: this.runDate,
        },
      },
    });

    if (!checkpoint) return null;

    return {
      programId: checkpoint.programId,
      recordsRead: checkpoint.recordsRead,
      recordsProcessed: checkpoint.recordsProc,
      recordsError: checkpoint.recordsError,
      lastKey: checkpoint.lastKey,
      phase: checkpoint.phase as CheckpointPhase,
    };
  }
}
