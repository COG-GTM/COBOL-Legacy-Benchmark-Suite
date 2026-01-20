/**
 * Data Layer for Investment Portfolio Management System
 * 
 * This module provides a modern TypeScript data layer that replaces the
 * VSAM files and DB2 tables from the legacy COBOL system.
 * 
 * Architecture:
 * - Types: Enums and type definitions migrated from COBOL copybooks
 * - Models: Data models with interfaces, factory functions, and utilities
 * - Repositories: Data access layer with repository pattern
 * - Validation: Data validation utilities based on COBOL validation rules
 * - Utils: Data transformation utilities for COBOL format conversion
 * 
 * Source COBOL Components:
 * - VSAM files: PORTMSTR, TRANHIST, POSHIST
 * - DB2 tables: PORTFOLIO_MASTER, INVESTMENT_POSITIONS, TRANSACTION_HISTORY, POSHIST, ERRLOG
 * - Copybooks: PORTFLIO.cpy, POSREC.cpy, TRNREC.cpy, HISTREC.cpy, ERRHAND.cpy, AUDITLOG.cpy
 * - Programs: TRNVAL00, POSUPD00, HISTLD00, INQPORT, INQHIST
 */

// Types and Enums
export * from './types';

// Data Models
export * from './models';

// Repositories
export * from './repositories';

// Validation
export * from './validation';

// Utilities
export * from './utils';
