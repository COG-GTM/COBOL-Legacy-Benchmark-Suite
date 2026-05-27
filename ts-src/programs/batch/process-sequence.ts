/**
 * Process Sequence Manager.
 * Migrated from: src/programs/batch/PRCSEQ00.cbl
 *
 * Manages the ordering and dependency resolution of batch processes.
 * Implements a simple DAG-based task scheduler.
 */

import {
  ProcessSequenceRecord,
  ProcessSequenceKey,
  ReturnCode,
  BatchStatus,
} from '../../types';

export type ProcessSequenceFunction = 'INIT' | 'NEXT' | 'STAT' | 'TERM';

/** Internal tracking node for process execution. */
interface ProcessNode {
  record: ProcessSequenceRecord;
  status: string;
  returnCode: number;
}

export class ProcessSequence {
  private processes: Map<string, ProcessNode> = new Map();
  private executionOrder: string[] = [];
  private currentIndex = 0;

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  execute(func: ProcessSequenceFunction): number {
    switch (func) {
      case 'INIT':
        return this.initialize();
      case 'NEXT':
        return this.getNextProcess();
      case 'STAT':
        return this.checkStatus();
      case 'TERM':
        return this.terminate();
      default:
        console.error(`Invalid function code: ${func}`);
        return ReturnCode.Error;
    }
  }

  /** Register a process in the sequence. */
  addProcess(record: ProcessSequenceRecord): void {
    const key = this.buildKey(record.psrKey);
    this.processes.set(key, {
      record,
      status: BatchStatus.Ready,
      returnCode: 0,
    });
  }

  /** 1000-INITIALIZE – build execution order via topological sort. */
  private initialize(): number {
    try {
      this.executionOrder = this.topologicalSort();
      this.currentIndex = 0;
      console.log(`Process sequence initialised with ${this.executionOrder.length} processes`);
      return ReturnCode.Success;
    } catch (err) {
      console.error('Failed to build process sequence:', err);
      return ReturnCode.Error;
    }
  }

  /** 2000-GET-NEXT-PROCESS – return the next ready process whose deps are met. */
  private getNextProcess(): number {
    while (this.currentIndex < this.executionOrder.length) {
      const key = this.executionOrder[this.currentIndex];
      const node = this.processes.get(key);
      if (!node) {
        this.currentIndex++;
        continue;
      }

      if (node.status !== BatchStatus.Ready) {
        this.currentIndex++;
        continue;
      }

      // Check dependencies
      const depsOk = node.record.psrDependencies.every((dep) => {
        const depNode = this.findProcessById(dep.depProcessId);
        if (!depNode) return dep.depType !== 'REQ';
        return depNode.status === BatchStatus.Done;
      });

      if (depsOk) {
        node.status = BatchStatus.Active;
        return ReturnCode.Success;
      }

      this.currentIndex++;
    }

    return ReturnCode.Warning; // No more processes
  }

  /** 3000-CHECK-STATUS – report overall run status. */
  private checkStatus(): number {
    let hasErrors = false;
    let allDone = true;

    for (const [, node] of this.processes) {
      if (node.status === BatchStatus.Error) hasErrors = true;
      if (node.status !== BatchStatus.Done && node.status !== BatchStatus.Error) {
        allDone = false;
      }
    }

    if (hasErrors) return ReturnCode.Error;
    if (allDone) return ReturnCode.Success;
    return ReturnCode.Warning;
  }

  /** 4000-TERMINATE – display summary. */
  private terminate(): number {
    let completed = 0;
    let failed = 0;

    for (const [, node] of this.processes) {
      if (node.status === BatchStatus.Done) completed++;
      if (node.status === BatchStatus.Error) failed++;
    }

    console.log(`Process sequence complete: ${completed} succeeded, ${failed} failed`);
    return failed > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** Get the current process record. */
  getCurrentProcess(): ProcessSequenceRecord | undefined {
    if (this.currentIndex >= this.executionOrder.length) return undefined;
    const key = this.executionOrder[this.currentIndex];
    return this.processes.get(key)?.record;
  }

  /** Mark a process as done. */
  markComplete(processId: string, rc: number): void {
    const node = this.findProcessById(processId);
    if (node) {
      node.status = rc >= ReturnCode.Error ? BatchStatus.Error : BatchStatus.Done;
      node.returnCode = rc;
    }
  }

  private findProcessById(processId: string): ProcessNode | undefined {
    for (const [, node] of this.processes) {
      if (node.record.psrKey.psrProcessId === processId) {
        return node;
      }
    }
    return undefined;
  }

  /** Topological sort for dependency resolution. */
  private topologicalSort(): string[] {
    const keys = Array.from(this.processes.keys());
    const visited = new Set<string>();
    const result: string[] = [];

    const visit = (key: string): void => {
      if (visited.has(key)) return;
      visited.add(key);

      const node = this.processes.get(key);
      if (node) {
        for (const dep of node.record.psrDependencies) {
          const depKey = this.findKeyByProcessId(dep.depProcessId);
          if (depKey) visit(depKey);
        }
      }
      result.push(key);
    };

    for (const key of keys) {
      visit(key);
    }

    return result;
  }

  private findKeyByProcessId(processId: string): string | undefined {
    for (const [key, node] of this.processes) {
      if (node.record.psrKey.psrProcessId === processId) return key;
    }
    return undefined;
  }

  private buildKey(key: ProcessSequenceKey): string {
    return `${key.psrProcessId}|${key.psrVersion}`;
  }
}
