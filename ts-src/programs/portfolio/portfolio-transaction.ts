/**
 * Portfolio Transaction Processing.
 * Migrated from: src/programs/portfolio/PORTTRAN.cbl
 *
 * Processes buy, sell, transfer, and fee transactions against portfolios.
 * All financial calculations use decimal.js for precision.
 */

import Decimal from 'decimal.js';
import {
  PortfolioRecord,
  TransactionRecord,
  TransactionType,
  TransactionStatus,
  PositionRecord,
  PositionStatus,
  ReturnCode,
} from '../../types';
import { VsamStore, VsamError } from '../../database/vsam-store';

export class PortfolioTransaction {
  private processedCount = 0;
  private errorCount = 0;

  constructor(
    private readonly portfolioStore: VsamStore<PortfolioRecord>,
    private readonly positionStore: VsamStore<PositionRecord>,
    private readonly transactionStore: VsamStore<TransactionRecord>,
  ) {}

  /** Process a single transaction – mirrors COBOL 0000-MAIN EVALUATE. */
  processTransaction(trn: TransactionRecord): number {
    try {
      switch (trn.trnType) {
        case TransactionType.Buy:
          return this.processBuy(trn);
        case TransactionType.Sell:
          return this.processSell(trn);
        case TransactionType.Transfer:
          return this.processTransfer(trn);
        case TransactionType.Fee:
          return this.processFee(trn);
        default:
          console.error(`Unknown transaction type: ${trn.trnType}`);
          return ReturnCode.Error;
      }
    } catch (err) {
      this.errorCount++;
      console.error(`Transaction processing error: ${err}`);
      return ReturnCode.Error;
    }
  }

  /** 1000-PROCESS-BUY – purchase investment units. */
  private processBuy(trn: TransactionRecord): number {
    const portfolioKey = `${trn.trnKey.trnPortfolioId}${trn.trnAccountNo}`;
    const portfolio = this.portfolioStore.read(portfolioKey);
    if (!portfolio) {
      console.error(`Portfolio not found: ${trn.trnKey.trnPortfolioId}`);
      return ReturnCode.Error;
    }

    const quantity = new Decimal(trn.trnQuantity);
    const price = new Decimal(trn.trnPrice);
    const amount = quantity.times(price);
    const fees = new Decimal(trn.trnFees);
    const totalCost = amount.plus(fees);

    // Check sufficient cash
    const cashBalance = new Decimal(portfolio.portFinancialInfo.portCashBalance);
    if (cashBalance.lt(totalCost)) {
      console.error('Insufficient cash balance for buy');
      return ReturnCode.Error;
    }

    // Update portfolio cash
    portfolio.portFinancialInfo.portCashBalance = cashBalance.minus(totalCost).toNumber();
    portfolio.portFinancialInfo.portTotalCost = new Decimal(portfolio.portFinancialInfo.portTotalCost)
      .plus(totalCost)
      .toNumber();
    this.portfolioStore.rewrite(portfolio);

    // Create or update position
    this.updatePosition(trn, quantity, amount);

    // Record the transaction
    trn.trnAmount = amount.toNumber();
    trn.trnStatus = TransactionStatus.Done;
    this.recordTransaction(trn);

    this.processedCount++;
    return ReturnCode.Success;
  }

  /** 2000-PROCESS-SELL – sell investment units. */
  private processSell(trn: TransactionRecord): number {
    const portfolioKey = `${trn.trnKey.trnPortfolioId}${trn.trnAccountNo}`;
    const portfolio = this.portfolioStore.read(portfolioKey);
    if (!portfolio) {
      console.error(`Portfolio not found: ${trn.trnKey.trnPortfolioId}`);
      return ReturnCode.Error;
    }

    const quantity = new Decimal(trn.trnQuantity);
    const price = new Decimal(trn.trnPrice);
    const amount = quantity.times(price);
    const fees = new Decimal(trn.trnFees);
    const netProceeds = amount.minus(fees);

    // Update portfolio cash (add proceeds)
    portfolio.portFinancialInfo.portCashBalance = new Decimal(portfolio.portFinancialInfo.portCashBalance)
      .plus(netProceeds)
      .toNumber();
    this.portfolioStore.rewrite(portfolio);

    // Update position (reduce quantity)
    this.updatePosition(trn, quantity.neg(), amount.neg());

    trn.trnAmount = amount.toNumber();
    trn.trnStatus = TransactionStatus.Done;
    this.recordTransaction(trn);

    this.processedCount++;
    return ReturnCode.Success;
  }

  /** 3000-PROCESS-TRANSFER. */
  private processTransfer(trn: TransactionRecord): number {
    trn.trnStatus = TransactionStatus.Done;
    this.recordTransaction(trn);
    this.processedCount++;
    return ReturnCode.Success;
  }

  /** 4000-PROCESS-FEE. */
  private processFee(trn: TransactionRecord): number {
    const portfolioKey = `${trn.trnKey.trnPortfolioId}${trn.trnAccountNo}`;
    const portfolio = this.portfolioStore.read(portfolioKey);
    if (!portfolio) {
      return ReturnCode.Error;
    }

    const feeAmount = new Decimal(trn.trnAmount);
    portfolio.portFinancialInfo.portCashBalance = new Decimal(portfolio.portFinancialInfo.portCashBalance)
      .minus(feeAmount)
      .toNumber();
    this.portfolioStore.rewrite(portfolio);

    trn.trnStatus = TransactionStatus.Done;
    this.recordTransaction(trn);

    this.processedCount++;
    return ReturnCode.Success;
  }

  /** Update or create a position record. */
  private updatePosition(
    trn: TransactionRecord,
    quantityDelta: Decimal,
    costDelta: Decimal,
  ): void {
    const posKey = `${trn.trnKey.trnPortfolioId}${trn.trnKey.trnDate}${trn.trnInvestmentId}`;
    const existing = this.positionStore.read(posKey);

    if (existing) {
      existing.posQuantity = new Decimal(existing.posQuantity).plus(quantityDelta).toNumber();
      existing.posCostBasis = new Decimal(existing.posCostBasis).plus(costDelta).toNumber();
      this.positionStore.rewrite(existing);
    } else {
      const position: PositionRecord = {
        posKey: {
          posPortfolioId: trn.trnKey.trnPortfolioId,
          posDate: trn.trnKey.trnDate,
          posInvestmentId: trn.trnInvestmentId,
        },
        posStatus: PositionStatus.Active,
        posAccountNo: trn.trnAccountNo,
        posDescription: '',
        posInvestmentType: '',
        posQuantity: quantityDelta.toNumber(),
        posCostBasis: costDelta.toNumber(),
        posMarketValue: costDelta.toNumber(),
        posPercentChange: 0,
        posLastValDate: trn.trnKey.trnDate,
      };
      this.positionStore.write(position);
    }
  }

  /** Record the completed transaction. */
  private recordTransaction(trn: TransactionRecord): void {
    try {
      this.transactionStore.write(trn);
    } catch (err) {
      if (err instanceof VsamError && err.statusCode === '22') {
        // Duplicate – update instead
        this.transactionStore.rewrite(trn);
      }
    }
  }

  getCounts(): { processed: number; errors: number } {
    return { processed: this.processedCount, errors: this.errorCount };
  }
}
