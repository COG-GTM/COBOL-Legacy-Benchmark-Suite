/**
 * Portfolio Validation.
 * Migrated from: src/programs/portfolio/PORTVALD.cbl
 *
 * Validates portfolio data: ID format, account number, investment type,
 * and amount ranges.
 */

import Decimal from 'decimal.js';
import {
  ValidationReturnCode,
  VAL_ID_PREFIX,
  VAL_MIN_AMOUNT,
  VAL_MAX_AMOUNT,
  VALID_INVESTMENT_TYPES,
  ReturnCode,
} from '../../types';

export class PortfolioValidation {
  /** 1000-VALIDATE-ID – check portfolio ID format (e.g. PORT0001). */
  validatePortfolioId(portId: string): number {
    if (!portId || portId.length !== 8) {
      console.log(`Invalid portfolio ID length: "${portId}"`);
      return ValidationReturnCode.InvalidId;
    }
    if (!portId.startsWith(VAL_ID_PREFIX)) {
      console.log(`Portfolio ID must start with ${VAL_ID_PREFIX}: "${portId}"`);
      return ValidationReturnCode.InvalidId;
    }
    const numPart = portId.slice(VAL_ID_PREFIX.length);
    if (!/^\d+$/.test(numPart)) {
      console.log(`Portfolio ID numeric part invalid: "${numPart}"`);
      return ValidationReturnCode.InvalidId;
    }
    return ReturnCode.Success;
  }

  /** 2000-VALIDATE-ACCOUNT – check account number format. */
  validateAccountNo(accountNo: string): number {
    if (!accountNo || accountNo.trim().length === 0) {
      console.log('Account number is empty');
      return ValidationReturnCode.InvalidAccount;
    }
    if (!/^[A-Za-z0-9]+$/.test(accountNo.trim())) {
      console.log(`Invalid account number format: "${accountNo}"`);
      return ValidationReturnCode.InvalidAccount;
    }
    return ReturnCode.Success;
  }

  /** 3000-VALIDATE-TYPE – check investment type. */
  validateInvestmentType(invType: string): number {
    const validTypes: readonly string[] = VALID_INVESTMENT_TYPES;
    if (!validTypes.includes(invType)) {
      console.log(`Invalid investment type: "${invType}"`);
      return ValidationReturnCode.InvalidType;
    }
    return ReturnCode.Success;
  }

  /** 4000-VALIDATE-AMOUNT – check amount range. */
  validateAmount(amount: number): number {
    const d = new Decimal(amount);
    if (d.lt(VAL_MIN_AMOUNT) || d.gt(VAL_MAX_AMOUNT)) {
      console.log(`Amount out of range: ${amount}`);
      return ValidationReturnCode.InvalidAmount;
    }
    return ReturnCode.Success;
  }

  /** Run all validations on a portfolio record's key fields. */
  validateAll(portId: string, accountNo: string, invType?: string, amount?: number): number {
    let rc = this.validatePortfolioId(portId);
    if (rc !== ReturnCode.Success) return rc;

    rc = this.validateAccountNo(accountNo);
    if (rc !== ReturnCode.Success) return rc;

    if (invType !== undefined) {
      rc = this.validateInvestmentType(invType);
      if (rc !== ReturnCode.Success) return rc;
    }

    if (amount !== undefined) {
      rc = this.validateAmount(amount);
      if (rc !== ReturnCode.Success) return rc;
    }

    return ReturnCode.Success;
  }
}
