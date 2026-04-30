/**
 * History Loader (Batch)
 * Ported from: src/programs/batch/HISTLD00.cbl
 *
 * Creates history records with before/after images (matching HISTREC.cpy structure).
 * Records action codes: A=Add, C=Change, D=Delete.
 * Bulk inserts into PositionHistory table.
 *
 * The original COBOL program reads from a VSAM history file and inserts into DB2
 * with commit every 1000 records. This modern version processes PositionUpdate
 * records and creates history entries with JSON before/after images.
 */

import { Prisma } from "@prisma/client";
import { prisma } from "../../lib/prisma";
import { CheckpointManager } from "../../lib/checkpoint";
import {
  HistoryRecordType,
  CheckpointPhase,
  ReturnCode,
  type HistoryEntry,
  type PositionUpdate,
  type CheckpointState,
} from "../../types";

export interface HistoryLoaderResult {
  returnCode: number;
  recordsWritten: number;
  errorsEncountered: number;
}

const COMMIT_THRESHOLD = 1000;

/**
 * Load position history records into the database.
 * Mirrors HISTLD00 processing: read history entries, insert to DB,
 * commit every COMMIT_THRESHOLD records.
 */
export async function loadHistory(
  positionUpdates: PositionUpdate[],
  runDate: Date
): Promise<HistoryLoaderResult> {
  const checkpoint = new CheckpointManager("HISTLD00", runDate);
  const resumeState = await checkpoint.initialize();

  const startIndex = resumeState?.recordsProcessed ?? 0;
  let recordsWritten = resumeState?.recordsProcessed ?? 0;
  let errorsEncountered = resumeState?.recordsError ?? 0;

  const state: CheckpointState = {
    programId: "HISTLD00",
    recordsRead: startIndex,
    recordsProcessed: recordsWritten,
    recordsError: errorsEncountered,
    lastKey: resumeState?.lastKey ?? null,
    phase: CheckpointPhase.PROCESS,
  };

  const historyEntries = await buildHistoryEntries(positionUpdates);

  // Process in batches matching COBOL's commit threshold
  for (let i = startIndex; i < historyEntries.length; i += COMMIT_THRESHOLD) {
    const batch = historyEntries.slice(
      i,
      Math.min(i + COMMIT_THRESHOLD, historyEntries.length)
    );

    try {
      const written = await insertHistoryBatch(batch);
      recordsWritten += written;
      state.recordsRead = Math.min(
        i + COMMIT_THRESHOLD,
        historyEntries.length
      );
      state.recordsProcessed = recordsWritten;
      state.lastKey = batch[batch.length - 1].portfolioId;
    } catch (error) {
      errorsEncountered += batch.length;
      state.recordsError = errorsEncountered;
    }

    const shouldContinue = await checkpoint.takeCheckpoint(state);
    if (!shouldContinue) {
      break;
    }
  }

  const returnCode =
    errorsEncountered === 0
      ? ReturnCode.SUCCESS
      : errorsEncountered <= historyEntries.length * 0.1
        ? ReturnCode.WARNING
        : ReturnCode.ERROR;

  if (returnCode <= ReturnCode.WARNING) {
    await checkpoint.markComplete(state);
  } else {
    await checkpoint.markFailed(state, "Too many errors during history load");
  }

  return {
    returnCode,
    recordsWritten,
    errorsEncountered,
  };
}

/**
 * Build history entries from position updates.
 * Fetches current position state for before-images.
 */
async function buildHistoryEntries(
  updates: PositionUpdate[]
): Promise<HistoryEntry[]> {
  const entries: HistoryEntry[] = [];

  for (const update of updates) {
    const position = await prisma.position.findUnique({
      where: {
        portfolioId_investmentId: {
          portfolioId: update.portfolioId,
          investmentId: update.investmentId,
        },
      },
    });

    const beforeImage = position
      ? {
          portfolioId: position.portfolioId,
          investmentId: position.investmentId,
          quantity: position.quantity.toString(),
          costBasis: position.costBasis.toString(),
          marketValue: position.marketValue.toString(),
          status: position.status,
        }
      : null;

    const afterImage = position
      ? {
          portfolioId: position.portfolioId,
          investmentId: position.investmentId,
          quantity: position.quantity.toString(),
          costBasis: position.costBasis.toString(),
          marketValue: position.marketValue.toString(),
          status: position.status,
          quantityDelta: update.quantityDelta.toString(),
          costBasisDelta: update.costBasisDelta.toString(),
        }
      : null;

    entries.push({
      portfolioId: update.portfolioId,
      recordType: HistoryRecordType.POSITION,
      actionCode: update.actionCode,
      beforeImage,
      afterImage,
      reasonCode: update.actionCode,
    });
  }

  return entries;
}

/**
 * Bulk insert a batch of history records.
 * Mirrors 2200-LOAD-TO-DB2 in HISTLD00.cbl.
 * Handles duplicate key (SQLCODE -803) by skipping.
 */
async function insertHistoryBatch(entries: HistoryEntry[]): Promise<number> {
  const now = new Date();
  const timeStr = now.toISOString().slice(11, 17).replace(/:/g, "");
  let written = 0;

  const data = entries.map((entry, index) => ({
    portfolioId: entry.portfolioId,
    histDate: now,
    histTime: timeStr,
    seqNo: String(index).padStart(4, "0"),
    recordType: entry.recordType,
    actionCode: entry.actionCode,
    beforeImage: entry.beforeImage as Prisma.InputJsonValue,
    afterImage: entry.afterImage as Prisma.InputJsonValue,
    reasonCode: entry.reasonCode ?? null,
    processDate: now,
    processUser: "BATCH",
  }));

  // Use skipDuplicates to mirror COBOL's SQLCODE -803 handling (CONTINUE)
  const result = await prisma.positionHistory.createMany({
    data,
    skipDuplicates: true,
  });

  written = result.count;
  return written;
}
