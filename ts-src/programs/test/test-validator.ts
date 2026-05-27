/**
 * Test Validation Suite.
 * Migrated from: src/programs/test/TSTVAL00.cbl
 *
 * Runs functional, integration, performance, and error-handling tests
 * against the migrated system to validate translation accuracy.
 */

import Decimal from 'decimal.js';
import {
  ReturnCode,
  PortfolioRecord,
  PortfolioStatus,
  TransactionType,
} from '../../types';
import { VsamStore } from '../../database/vsam-store';
import { PortfolioValidation } from '../portfolio/portfolio-validation';
import { PortfolioMaster } from '../portfolio/portfolio-master';
import { PortfolioTest } from '../portfolio/portfolio-test';

export type TestSuiteType = 'FUNC' | 'INTG' | 'PERF' | 'ERRR';

interface TestResult {
  name: string;
  passed: boolean;
  message: string;
}

export class TestValidator {
  private results: TestResult[] = [];

  /** Dispatch – mirrors COBOL 0000-MAIN EVALUATE. */
  async run(suiteType: TestSuiteType): Promise<number> {
    this.results = [];

    switch (suiteType) {
      case 'FUNC':
        this.runFunctionalTests();
        break;
      case 'INTG':
        this.runIntegrationTests();
        break;
      case 'PERF':
        this.runPerformanceTests();
        break;
      case 'ERRR':
        this.runErrorTests();
        break;
      default:
        return ReturnCode.Error;
    }

    this.printResults();
    const failed = this.results.filter((r) => !r.passed).length;
    return failed > 0 ? ReturnCode.Error : ReturnCode.Success;
  }

  /** 1000-FUNCTIONAL-TESTS. */
  private runFunctionalTests(): void {
    const validator = new PortfolioValidation();

    // Test valid portfolio ID
    this.assert(
      'Valid portfolio ID',
      validator.validatePortfolioId('PORT0001') === ReturnCode.Success,
      'PORT0001 should be valid',
    );

    // Test invalid portfolio ID
    this.assert(
      'Invalid portfolio ID prefix',
      validator.validatePortfolioId('XXXX0001') !== ReturnCode.Success,
      'XXXX0001 should be invalid',
    );

    // Test valid account number
    this.assert(
      'Valid account number',
      validator.validateAccountNo('ACCT000001') === ReturnCode.Success,
      'ACCT000001 should be valid',
    );

    // Test valid investment type
    this.assert(
      'Valid investment type STK',
      validator.validateInvestmentType('STK') === ReturnCode.Success,
      'STK should be valid',
    );

    // Test invalid investment type
    this.assert(
      'Invalid investment type',
      validator.validateInvestmentType('XXX') !== ReturnCode.Success,
      'XXX should be invalid',
    );

    // Test amount validation
    this.assert(
      'Valid amount',
      validator.validateAmount(1000.00) === ReturnCode.Success,
      '1000.00 should be valid',
    );

    this.assert(
      'Amount too small',
      validator.validateAmount(0) !== ReturnCode.Success,
      '0 should be invalid',
    );

    // Decimal precision test
    const a = new Decimal('0.1');
    const b = new Decimal('0.2');
    const sum = a.plus(b);
    this.assert(
      'Decimal precision (0.1 + 0.2 = 0.3)',
      sum.eq('0.3'),
      `Expected 0.3, got ${sum.toString()}`,
    );
  }

  /** 2000-INTEGRATION-TESTS. */
  private runIntegrationTests(): void {
    const store = new VsamStore<PortfolioRecord>(
      (r) => `${r.portKey.portId}${r.portKey.portAccountNo}`,
    );
    const master = new PortfolioMaster(store);
    const testGen = new PortfolioTest();
    const portfolios = testGen.generatePortfolios(5);

    // Create
    for (const p of portfolios) {
      const result = master.execute('CREA', p);
      this.assert(
        `Create portfolio ${p.portKey.portId}`,
        result.rc === ReturnCode.Success,
        `RC=${result.rc}`,
      );
    }

    // Read
    const readResult = master.execute('READ', portfolios[0]);
    this.assert(
      'Read portfolio PORT0001',
      readResult.rc === ReturnCode.Success && readResult.record !== undefined,
      `RC=${readResult.rc}`,
    );

    // Update
    portfolios[0].portClientInfo.portClientName = 'Updated Client';
    const updResult = master.execute('UPDT', portfolios[0]);
    this.assert(
      'Update portfolio PORT0001',
      updResult.rc === ReturnCode.Success,
      `RC=${updResult.rc}`,
    );

    // Delete
    const delResult = master.execute('DELE', portfolios[4]);
    this.assert(
      'Delete portfolio PORT0005',
      delResult.rc === ReturnCode.Success,
      `RC=${delResult.rc}`,
    );

    // Verify size
    this.assert(
      'Store has 4 records after delete',
      store.size === 4,
      `Size=${store.size}`,
    );
  }

  /** 3000-PERFORMANCE-TESTS. */
  private runPerformanceTests(): void {
    const store = new VsamStore<PortfolioRecord>(
      (r) => `${r.portKey.portId}${r.portKey.portAccountNo}`,
    );
    const testGen = new PortfolioTest();
    const count = 1000;

    const start = Date.now();
    const portfolios = testGen.generatePortfolios(count);
    for (const p of portfolios) {
      store.write(p);
    }
    const writeTime = Date.now() - start;

    this.assert(
      `Write ${count} records`,
      writeTime < 5000,
      `Time=${writeTime}ms`,
    );

    const readStart = Date.now();
    for (const p of portfolios) {
      store.read(`${p.portKey.portId}${p.portKey.portAccountNo}`);
    }
    const readTime = Date.now() - readStart;

    this.assert(
      `Read ${count} records`,
      readTime < 5000,
      `Time=${readTime}ms`,
    );
  }

  /** 4000-ERROR-TESTS. */
  private runErrorTests(): void {
    const store = new VsamStore<PortfolioRecord>(
      (r) => `${r.portKey.portId}${r.portKey.portAccountNo}`,
    );
    const master = new PortfolioMaster(store);
    const testGen = new PortfolioTest();
    const portfolios = testGen.generatePortfolios(1);

    // Create, then try duplicate
    master.execute('CREA', portfolios[0]);
    const dupResult = master.execute('CREA', portfolios[0]);
    this.assert(
      'Duplicate create returns warning',
      dupResult.rc === ReturnCode.Warning,
      `RC=${dupResult.rc}`,
    );

    // Read non-existent
    const fakeRecord = testGen.generatePortfolios(1)[0];
    fakeRecord.portKey.portId = 'PORT9999';
    const readResult = master.execute('READ', fakeRecord);
    this.assert(
      'Read non-existent returns warning',
      readResult.rc === ReturnCode.Warning,
      `RC=${readResult.rc}`,
    );

    // Delete non-existent
    const delResult = master.execute('DELE', fakeRecord);
    this.assert(
      'Delete non-existent returns warning',
      delResult.rc === ReturnCode.Warning,
      `RC=${delResult.rc}`,
    );
  }

  private assert(name: string, condition: boolean, message: string): void {
    this.results.push({ name, passed: condition, message });
  }

  private printResults(): void {
    console.log('');
    console.log('='.repeat(70));
    console.log('TEST VALIDATION RESULTS');
    console.log('='.repeat(70));

    for (const r of this.results) {
      const status = r.passed ? 'PASS' : 'FAIL';
      console.log(`  [${status}] ${r.name} – ${r.message}`);
    }

    const passed = this.results.filter((r) => r.passed).length;
    const failed = this.results.filter((r) => !r.passed).length;
    console.log('-'.repeat(70));
    console.log(`Total: ${this.results.length}  Passed: ${passed}  Failed: ${failed}`);
    console.log('='.repeat(70));
  }

  getResults(): TestResult[] {
    return [...this.results];
  }
}
