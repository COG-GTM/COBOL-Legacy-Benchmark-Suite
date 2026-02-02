/**
 * Batch Control Module
 * 
 * React implementation of COBOL batch control functionality from BCHCTL00.cbl
 * This module provides components and services for managing batch process initialization.
 */

// Main component
export { default as BatchProcessInitialize } from './BatchProcessInitialize';

// Types
export * from './types';

// Services
export {
  openConnection,
  readControlRecord,
  validateProcess,
  updateStartStatus,
  closeConnection,
  getConnectionStatus,
  getAllRecords,
  processInitialize,
  initializeSampleData,
} from './batchControlService';
