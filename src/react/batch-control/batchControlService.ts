/**
 * Batch Control Service
 * Simulates VSAM file operations for the batch control system
 * This service adapts the COBOL BCHCTL00 logic for a web environment
 */

import {
  BatchControlRecord,
  BatchControlKey,
  BatchControlStatus,
  ControlRequest,
  ControlResponse,
  ReturnCodes,
  PrerequisiteJob,
} from './types';

/**
 * Simulated VSAM file storage using in-memory Map
 * In a real application, this would be replaced with REST API calls
 */
const vsamStorage = new Map<string, BatchControlRecord>();

/**
 * Connection state (simulates file open/close status)
 */
let isConnected = false;

/**
 * Generate a unique key string from BatchControlKey
 */
const generateKeyString = (key: BatchControlKey): string => {
  return `${key.jobName.padEnd(8)}${key.processDate.padEnd(8)}${String(key.sequenceNo).padStart(4, '0')}`;
};

/**
 * Simulate network delay for realistic behavior
 */
const simulateDelay = (ms: number = 500): Promise<void> => {
  return new Promise((resolve) => setTimeout(resolve, ms));
};

/**
 * Initialize sample data for testing
 */
export const initializeSampleData = (): void => {
  const sampleRecords: BatchControlRecord[] = [
    {
      key: {
        jobName: 'TRNVAL00',
        processDate: '20240115',
        sequenceNo: 1,
      },
      status: 'R',
      processControl: {
        stepName: 'STEP0010',
        programName: 'TRNVAL00',
        startTime: '',
        endTime: '',
      },
      dependencies: {
        prerequisiteCount: 0,
        prerequisiteJobs: [],
      },
      returnInfo: {
        returnCode: 0,
        errorDescription: '',
      },
      statistics: {
        restartCount: 0,
        attemptTimestamp: '',
        completeTimestamp: '',
      },
    },
    {
      key: {
        jobName: 'POSUPD00',
        processDate: '20240115',
        sequenceNo: 2,
      },
      status: 'R',
      processControl: {
        stepName: 'STEP0020',
        programName: 'POSUPD00',
        startTime: '',
        endTime: '',
      },
      dependencies: {
        prerequisiteCount: 1,
        prerequisiteJobs: [
          { name: 'TRNVAL00', sequenceNo: 1, returnCode: 0 },
        ],
      },
      returnInfo: {
        returnCode: 0,
        errorDescription: '',
      },
      statistics: {
        restartCount: 0,
        attemptTimestamp: '',
        completeTimestamp: '',
      },
    },
    {
      key: {
        jobName: 'HISTLD00',
        processDate: '20240115',
        sequenceNo: 3,
      },
      status: 'W',
      processControl: {
        stepName: 'STEP0030',
        programName: 'HISTLD00',
        startTime: '',
        endTime: '',
      },
      dependencies: {
        prerequisiteCount: 2,
        prerequisiteJobs: [
          { name: 'TRNVAL00', sequenceNo: 1, returnCode: 0 },
          { name: 'POSUPD00', sequenceNo: 2, returnCode: 0 },
        ],
      },
      returnInfo: {
        returnCode: 0,
        errorDescription: '',
      },
      statistics: {
        restartCount: 0,
        attemptTimestamp: '',
        completeTimestamp: '',
      },
    },
  ];

  sampleRecords.forEach((record) => {
    const keyString = generateKeyString(record.key);
    vsamStorage.set(keyString, record);
  });
};

/**
 * 1100-OPEN-FILES equivalent
 * Opens connection to the data storage (simulates VSAM file open)
 */
export const openConnection = async (): Promise<ControlResponse> => {
  await simulateDelay(300);

  if (isConnected) {
    return {
      returnCode: ReturnCodes.WARNING,
      errorMessage: 'Connection already open',
    };
  }

  try {
    // Initialize sample data if storage is empty
    if (vsamStorage.size === 0) {
      initializeSampleData();
    }

    isConnected = true;
    return {
      returnCode: ReturnCodes.SUCCESS,
    };
  } catch (error) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: `Failed to open connection: ${error instanceof Error ? error.message : 'Unknown error'}`,
    };
  }
};

/**
 * 1200-READ-CONTROL-RECORD equivalent
 * Reads a batch control record from storage
 */
export const readControlRecord = async (
  key: BatchControlKey
): Promise<ControlResponse> => {
  await simulateDelay(400);

  if (!isConnected) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Connection not open. Call openConnection first.',
    };
  }

  const keyString = generateKeyString(key);
  const record = vsamStorage.get(keyString);

  if (!record) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: `Record not found for key: ${key.jobName}/${key.processDate}/${key.sequenceNo}`,
    };
  }

  return {
    returnCode: ReturnCodes.SUCCESS,
    record: { ...record },
  };
};

/**
 * 1300-VALIDATE-PROCESS equivalent
 * Validates process parameters and prerequisites
 */
export const validateProcess = async (
  record: BatchControlRecord
): Promise<ControlResponse> => {
  await simulateDelay(350);

  if (!isConnected) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Connection not open. Call openConnection first.',
    };
  }

  // Validate job name (must be non-empty and max 8 characters)
  if (!record.key.jobName || record.key.jobName.trim().length === 0) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Invalid job name: Job name cannot be empty',
    };
  }

  if (record.key.jobName.length > 8) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Invalid job name: Job name exceeds 8 characters',
    };
  }

  // Validate process date (must be 8 characters in YYYYMMDD format)
  if (!record.key.processDate || record.key.processDate.length !== 8) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Invalid process date: Must be 8 characters (YYYYMMDD)',
    };
  }

  const dateRegex = /^\d{8}$/;
  if (!dateRegex.test(record.key.processDate)) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Invalid process date: Must be numeric (YYYYMMDD)',
    };
  }

  // Validate sequence number (must be between 0 and 9999)
  if (record.key.sequenceNo < 0 || record.key.sequenceNo > 9999) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Invalid sequence number: Must be between 0 and 9999',
    };
  }

  // Validate status (must be READY to initialize)
  if (record.status !== 'R') {
    return {
      returnCode: ReturnCodes.WARNING,
      errorMessage: `Process not ready: Current status is ${record.status}`,
    };
  }

  // Check prerequisites (2200-CHECK-DEPENDENCIES logic)
  if (record.dependencies.prerequisiteCount > 0) {
    for (const prereq of record.dependencies.prerequisiteJobs) {
      if (!prereq.name) continue;

      const prereqKey: BatchControlKey = {
        jobName: prereq.name,
        processDate: record.key.processDate,
        sequenceNo: prereq.sequenceNo,
      };

      const prereqKeyString = generateKeyString(prereqKey);
      const prereqRecord = vsamStorage.get(prereqKeyString);

      if (!prereqRecord) {
        return {
          returnCode: ReturnCodes.WARNING,
          errorMessage: `Prerequisite job not found: ${prereq.name}`,
        };
      }

      if (prereqRecord.status !== 'D') {
        return {
          returnCode: ReturnCodes.WARNING,
          errorMessage: `Prerequisite job ${prereq.name} not completed (status: ${prereqRecord.status})`,
        };
      }

      if (prereqRecord.returnInfo.returnCode > ReturnCodes.WARNING) {
        return {
          returnCode: ReturnCodes.WARNING,
          errorMessage: `Prerequisite job ${prereq.name} ended with error (RC: ${prereqRecord.returnInfo.returnCode})`,
        };
      }
    }
  }

  return {
    returnCode: ReturnCodes.SUCCESS,
    record,
  };
};

/**
 * 1400-UPDATE-START-STATUS equivalent
 * Updates the record status to ACTIVE and sets start time
 */
export const updateStartStatus = async (
  key: BatchControlKey
): Promise<ControlResponse> => {
  await simulateDelay(300);

  if (!isConnected) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Connection not open. Call openConnection first.',
    };
  }

  const keyString = generateKeyString(key);
  const record = vsamStorage.get(keyString);

  if (!record) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: `Record not found for key: ${key.jobName}/${key.processDate}/${key.sequenceNo}`,
    };
  }

  // Update status to ACTIVE
  const updatedRecord: BatchControlRecord = {
    ...record,
    status: 'A' as BatchControlStatus,
    processControl: {
      ...record.processControl,
      startTime: new Date().toISOString().replace(/[-:T.Z]/g, '').substring(0, 8),
    },
    statistics: {
      ...record.statistics,
      attemptTimestamp: new Date().toISOString(),
      restartCount: record.statistics.restartCount + 1,
    },
  };

  vsamStorage.set(keyString, updatedRecord);

  return {
    returnCode: ReturnCodes.SUCCESS,
    record: updatedRecord,
  };
};

/**
 * Close connection (4200-CLOSE-FILES equivalent)
 */
export const closeConnection = async (): Promise<ControlResponse> => {
  await simulateDelay(200);

  if (!isConnected) {
    return {
      returnCode: ReturnCodes.WARNING,
      errorMessage: 'Connection already closed',
    };
  }

  isConnected = false;
  return {
    returnCode: ReturnCodes.SUCCESS,
  };
};

/**
 * Get connection status
 */
export const getConnectionStatus = (): boolean => {
  return isConnected;
};

/**
 * Get all records (for listing purposes)
 */
export const getAllRecords = async (): Promise<BatchControlRecord[]> => {
  await simulateDelay(200);
  return Array.from(vsamStorage.values());
};

/**
 * Process initialization - combines all steps of 1000-PROCESS-INITIALIZE
 */
export const processInitialize = async (
  request: ControlRequest
): Promise<ControlResponse> => {
  // Step 1: Open files (1100-OPEN-FILES)
  const openResult = await openConnection();
  if (openResult.returnCode >= ReturnCodes.ERROR) {
    return openResult;
  }

  // Step 2: Read control record (1200-READ-CONTROL-RECORD)
  const key: BatchControlKey = {
    jobName: request.jobName,
    processDate: request.processDate,
    sequenceNo: request.sequenceNo,
  };

  const readResult = await readControlRecord(key);
  if (readResult.returnCode >= ReturnCodes.ERROR) {
    return readResult;
  }

  if (!readResult.record) {
    return {
      returnCode: ReturnCodes.ERROR,
      errorMessage: 'Failed to read control record',
    };
  }

  // Step 3: Validate process (1300-VALIDATE-PROCESS)
  const validateResult = await validateProcess(readResult.record);
  if (validateResult.returnCode >= ReturnCodes.ERROR) {
    return validateResult;
  }

  // Step 4: Update start status (1400-UPDATE-START-STATUS)
  const updateResult = await updateStartStatus(key);
  return updateResult;
};
