/**
 * Return Code Handler.
 * Migrated from: src/programs/batch/RTNCDE00.cbl
 *
 * Manages return codes across a batch run: init, set, get, log, and analyse.
 */

import { Knex } from 'knex';
import {
  RcRequestArea,
  RcRequestType,
  RcStatusCode,
  ReturnCode,
} from '../../types';

export class ReturnCodeHandler {
  private area: RcRequestArea;

  constructor(private readonly db: Knex) {
    this.area = this.createEmpty();
  }

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async execute(requestType: RcRequestType): Promise<number> {
    switch (requestType) {
      case RcRequestType.Init:
        return this.initialize();
      case RcRequestType.Set:
        return this.setCode();
      case RcRequestType.Get:
        return this.getCode();
      case RcRequestType.Log:
        return this.logCode();
      case RcRequestType.Analyze:
        return this.analyzeCode();
      default:
        return ReturnCode.Error;
    }
  }

  /** 1000-INITIALIZE – reset counters. */
  private initialize(): number {
    this.area.codesArea.currentCode = 0;
    this.area.codesArea.highestCode = 0;
    this.area.codesArea.newCode = 0;
    this.area.codesArea.statusCode = RcStatusCode.Normal;
    this.area.analysisData.totalCount = 0;
    this.area.analysisData.minCode = 9999;
    this.area.analysisData.maxCode = 0;
    this.area.analysisData.startTimestamp = new Date().toISOString();
    return ReturnCode.Success;
  }

  /** 2000-SET-CODE – set a new return code and update severity. */
  private setCode(): number {
    const code = this.area.codesArea.newCode;
    this.area.codesArea.currentCode = code;

    if (code > this.area.codesArea.highestCode) {
      this.area.codesArea.highestCode = code;
    }

    // Update status based on severity thresholds
    if (code === 0) {
      this.area.codesArea.statusCode = RcStatusCode.Normal;
    } else if (code <= 4) {
      this.area.codesArea.statusCode = RcStatusCode.Warning;
    } else if (code <= 8) {
      this.area.codesArea.statusCode = RcStatusCode.Error;
    } else {
      this.area.codesArea.statusCode = RcStatusCode.Severe;
    }

    // Update analysis
    this.area.analysisData.totalCount++;
    if (code < this.area.analysisData.minCode) this.area.analysisData.minCode = code;
    if (code > this.area.analysisData.maxCode) this.area.analysisData.maxCode = code;

    return ReturnCode.Success;
  }

  /** 3000-GET-CODE – retrieve the current and highest codes. */
  private getCode(): number {
    return ReturnCode.Success;
  }

  /** 4000-LOG-CODE – write the current state to the RTNCODES table. */
  private async logCode(): Promise<number> {
    try {
      await this.db('RTNCODES').insert({
        TIMESTAMP: new Date().toISOString(),
        PROGRAM_ID: 'RTNCDE00',
        RETURN_CODE: this.area.codesArea.currentCode,
        HIGHEST_CODE: this.area.codesArea.highestCode,
        STATUS_CODE: this.area.codesArea.statusCode,
        MESSAGE_TEXT: '',
      });
      return ReturnCode.Success;
    } catch (err) {
      console.error(`Error logging return code: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 5000-ANALYZE-CODE – finalise analysis data. */
  private analyzeCode(): number {
    this.area.analysisData.endTimestamp = new Date().toISOString();

    console.log(
      `RC Analysis: total=${this.area.analysisData.totalCount}, ` +
      `min=${this.area.analysisData.minCode}, max=${this.area.analysisData.maxCode}, ` +
      `highest=${this.area.codesArea.highestCode}`,
    );
    return ReturnCode.Success;
  }

  /** Get the request area for external inspection. */
  getArea(): RcRequestArea {
    return { ...this.area };
  }

  /** Set a new code value (convenience). */
  setNewCode(code: number): void {
    this.area.codesArea.newCode = code;
  }

  private createEmpty(): RcRequestArea {
    return {
      requestType: RcRequestType.Init,
      codesArea: {
        currentCode: 0,
        highestCode: 0,
        newCode: 0,
        statusCode: RcStatusCode.Normal,
      },
      analysisData: {
        startTimestamp: '',
        endTimestamp: '',
        minCode: 9999,
        maxCode: 0,
        totalCount: 0,
      },
    };
  }
}
