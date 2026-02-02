/**
 * BatchProcessInitialize Component
 * 
 * React implementation of the COBOL procedure 1000-PROCESS-INITIALIZE from BCHCTL00.cbl
 * This component provides a user interface for initializing batch processes,
 * implementing the four-step initialization flow:
 * 
 * 1. Open Files (1100-OPEN-FILES) - Connect to data storage
 * 2. Read Control Record (1200-READ-CONTROL-RECORD) - Retrieve job record
 * 3. Validate Process (1300-VALIDATE-PROCESS) - Validate parameters and prerequisites
 * 4. Update Start Status (1400-UPDATE-START-STATUS) - Mark process as ACTIVE
 */

import React, { useState, useCallback, useEffect } from 'react';
import {
  BatchControlRecord,
  BatchControlKey,
  BatchControlStatusLabels,
  InitializationStep,
  InitializationStepStatus,
  ReturnCodes,
} from './types';
import {
  openConnection,
  readControlRecord,
  validateProcess,
  updateStartStatus,
  closeConnection,
  getConnectionStatus,
  getAllRecords,
} from './batchControlService';

/**
 * Styles for the component
 */
const styles: Record<string, React.CSSProperties> = {
  container: {
    fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
    maxWidth: '900px',
    margin: '0 auto',
    padding: '24px',
    backgroundColor: '#f8f9fa',
    minHeight: '100vh',
  },
  header: {
    backgroundColor: '#1a365d',
    color: 'white',
    padding: '20px 24px',
    borderRadius: '8px 8px 0 0',
    marginBottom: '0',
  },
  headerTitle: {
    margin: '0 0 8px 0',
    fontSize: '24px',
    fontWeight: '600',
  },
  headerSubtitle: {
    margin: '0',
    fontSize: '14px',
    opacity: 0.9,
  },
  mainContent: {
    backgroundColor: 'white',
    borderRadius: '0 0 8px 8px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
    padding: '24px',
  },
  section: {
    marginBottom: '24px',
  },
  sectionTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2d3748',
    marginBottom: '16px',
    paddingBottom: '8px',
    borderBottom: '2px solid #e2e8f0',
  },
  formGroup: {
    marginBottom: '16px',
  },
  label: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '500',
    color: '#4a5568',
    marginBottom: '6px',
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    fontSize: '14px',
    border: '1px solid #e2e8f0',
    borderRadius: '6px',
    boxSizing: 'border-box' as const,
    transition: 'border-color 0.2s, box-shadow 0.2s',
  },
  inputFocus: {
    borderColor: '#3182ce',
    boxShadow: '0 0 0 3px rgba(49, 130, 206, 0.1)',
    outline: 'none',
  },
  formRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '16px',
  },
  button: {
    padding: '12px 24px',
    fontSize: '14px',
    fontWeight: '600',
    borderRadius: '6px',
    border: 'none',
    cursor: 'pointer',
    transition: 'background-color 0.2s, transform 0.1s',
    marginRight: '12px',
  },
  primaryButton: {
    backgroundColor: '#3182ce',
    color: 'white',
  },
  primaryButtonDisabled: {
    backgroundColor: '#a0aec0',
    cursor: 'not-allowed',
  },
  secondaryButton: {
    backgroundColor: '#e2e8f0',
    color: '#4a5568',
  },
  stepContainer: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  step: {
    display: 'flex',
    alignItems: 'center',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #e2e8f0',
    backgroundColor: '#fafafa',
  },
  stepPending: {
    borderColor: '#e2e8f0',
    backgroundColor: '#fafafa',
  },
  stepInProgress: {
    borderColor: '#3182ce',
    backgroundColor: '#ebf8ff',
  },
  stepCompleted: {
    borderColor: '#38a169',
    backgroundColor: '#f0fff4',
  },
  stepError: {
    borderColor: '#e53e3e',
    backgroundColor: '#fff5f5',
  },
  stepIcon: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: '16px',
    fontSize: '14px',
    fontWeight: '600',
  },
  stepIconPending: {
    backgroundColor: '#e2e8f0',
    color: '#718096',
  },
  stepIconInProgress: {
    backgroundColor: '#3182ce',
    color: 'white',
  },
  stepIconCompleted: {
    backgroundColor: '#38a169',
    color: 'white',
  },
  stepIconError: {
    backgroundColor: '#e53e3e',
    color: 'white',
  },
  stepContent: {
    flex: 1,
  },
  stepName: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#2d3748',
    marginBottom: '4px',
  },
  stepDescription: {
    fontSize: '12px',
    color: '#718096',
  },
  stepError: {
    fontSize: '12px',
    color: '#e53e3e',
    marginTop: '4px',
  },
  spinner: {
    width: '16px',
    height: '16px',
    border: '2px solid #ffffff',
    borderTopColor: 'transparent',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  resultCard: {
    padding: '20px',
    borderRadius: '8px',
    marginTop: '16px',
  },
  resultSuccess: {
    backgroundColor: '#f0fff4',
    border: '1px solid #38a169',
  },
  resultError: {
    backgroundColor: '#fff5f5',
    border: '1px solid #e53e3e',
  },
  resultTitle: {
    fontSize: '16px',
    fontWeight: '600',
    marginBottom: '12px',
  },
  resultTitleSuccess: {
    color: '#276749',
  },
  resultTitleError: {
    color: '#c53030',
  },
  recordDetails: {
    backgroundColor: '#f7fafc',
    padding: '16px',
    borderRadius: '6px',
    fontSize: '13px',
    fontFamily: "'Consolas', 'Monaco', monospace",
  },
  detailRow: {
    display: 'flex',
    marginBottom: '8px',
  },
  detailLabel: {
    width: '160px',
    fontWeight: '600',
    color: '#4a5568',
  },
  detailValue: {
    color: '#2d3748',
  },
  statusBadge: {
    display: 'inline-block',
    padding: '4px 12px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: '600',
  },
  statusReady: {
    backgroundColor: '#bee3f8',
    color: '#2b6cb0',
  },
  statusActive: {
    backgroundColor: '#c6f6d5',
    color: '#276749',
  },
  statusDone: {
    backgroundColor: '#e2e8f0',
    color: '#4a5568',
  },
  statusWaiting: {
    backgroundColor: '#feebc8',
    color: '#c05621',
  },
  statusError: {
    backgroundColor: '#fed7d7',
    color: '#c53030',
  },
  connectionStatus: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '13px',
    marginBottom: '16px',
  },
  connectionDot: {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
  },
  connectionConnected: {
    backgroundColor: '#38a169',
  },
  connectionDisconnected: {
    backgroundColor: '#e53e3e',
  },
  availableJobs: {
    marginTop: '16px',
  },
  jobList: {
    display: 'grid',
    gap: '8px',
  },
  jobItem: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 16px',
    backgroundColor: '#f7fafc',
    borderRadius: '6px',
    border: '1px solid #e2e8f0',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  jobItemHover: {
    backgroundColor: '#edf2f7',
  },
  jobInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  },
  jobName: {
    fontWeight: '600',
    color: '#2d3748',
    fontSize: '14px',
  },
  jobDate: {
    color: '#718096',
    fontSize: '13px',
  },
};

/**
 * CSS keyframes for spinner animation (injected into document)
 */
const injectSpinnerAnimation = () => {
  const styleId = 'batch-process-spinner-animation';
  if (!document.getElementById(styleId)) {
    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = `
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
    `;
    document.head.appendChild(style);
  }
};

/**
 * Initial steps configuration
 */
const createInitialSteps = (): InitializationStep[] => [
  {
    id: 'open-files',
    name: '1100-OPEN-FILES',
    description: 'Abrir conexao com o armazenamento de dados (VSAM)',
    status: 'pending',
  },
  {
    id: 'read-record',
    name: '1200-READ-CONTROL-RECORD',
    description: 'Ler registro de controle para o job/processo atual',
    status: 'pending',
  },
  {
    id: 'validate-process',
    name: '1300-VALIDATE-PROCESS',
    description: 'Validar parametros do processo e pre-requisitos',
    status: 'pending',
  },
  {
    id: 'update-status',
    name: '1400-UPDATE-START-STATUS',
    description: 'Atualizar status para ACTIVE (A)',
    status: 'pending',
  },
];

/**
 * Get status badge style
 */
const getStatusBadgeStyle = (status: string): React.CSSProperties => {
  const baseStyle = styles.statusBadge;
  switch (status) {
    case 'R':
      return { ...baseStyle, ...styles.statusReady };
    case 'A':
      return { ...baseStyle, ...styles.statusActive };
    case 'D':
      return { ...baseStyle, ...styles.statusDone };
    case 'W':
      return { ...baseStyle, ...styles.statusWaiting };
    case 'E':
      return { ...baseStyle, ...styles.statusError };
    default:
      return baseStyle;
  }
};

/**
 * Get step style based on status
 */
const getStepStyle = (status: InitializationStepStatus): React.CSSProperties => {
  const baseStyle = styles.step;
  switch (status) {
    case 'in_progress':
      return { ...baseStyle, ...styles.stepInProgress };
    case 'completed':
      return { ...baseStyle, ...styles.stepCompleted };
    case 'error':
      return { ...baseStyle, ...styles.stepError };
    default:
      return { ...baseStyle, ...styles.stepPending };
  }
};

/**
 * Get step icon style based on status
 */
const getStepIconStyle = (status: InitializationStepStatus): React.CSSProperties => {
  const baseStyle = styles.stepIcon;
  switch (status) {
    case 'in_progress':
      return { ...baseStyle, ...styles.stepIconInProgress };
    case 'completed':
      return { ...baseStyle, ...styles.stepIconCompleted };
    case 'error':
      return { ...baseStyle, ...styles.stepIconError };
    default:
      return { ...baseStyle, ...styles.stepIconPending };
  }
};

/**
 * Step icon content
 */
const StepIcon: React.FC<{ status: InitializationStepStatus; index: number }> = ({
  status,
  index,
}) => {
  if (status === 'in_progress') {
    return <div style={styles.spinner} />;
  }
  if (status === 'completed') {
    return <span>&#10003;</span>;
  }
  if (status === 'error') {
    return <span>&#10007;</span>;
  }
  return <span>{index + 1}</span>;
};

/**
 * BatchProcessInitialize Component
 */
const BatchProcessInitialize: React.FC = () => {
  // Form state
  const [jobName, setJobName] = useState<string>('');
  const [processDate, setProcessDate] = useState<string>('');
  const [sequenceNo, setSequenceNo] = useState<string>('');

  // Process state
  const [steps, setSteps] = useState<InitializationStep[]>(createInitialSteps());
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [result, setResult] = useState<{
    success: boolean;
    record?: BatchControlRecord;
    errorMessage?: string;
  } | null>(null);

  // Available jobs state
  const [availableJobs, setAvailableJobs] = useState<BatchControlRecord[]>([]);
  const [hoveredJob, setHoveredJob] = useState<string | null>(null);

  // Inject spinner animation on mount
  useEffect(() => {
    injectSpinnerAnimation();
  }, []);

  // Load available jobs on mount
  useEffect(() => {
    const loadJobs = async () => {
      // Initialize connection to load sample data
      await openConnection();
      const jobs = await getAllRecords();
      setAvailableJobs(jobs);
      setIsConnected(getConnectionStatus());
    };
    loadJobs();
  }, []);

  /**
   * Update a specific step's status
   */
  const updateStepStatus = useCallback(
    (stepId: string, status: InitializationStepStatus, errorMessage?: string) => {
      setSteps((prevSteps) =>
        prevSteps.map((step) =>
          step.id === stepId ? { ...step, status, errorMessage } : step
        )
      );
    },
    []
  );

  /**
   * Reset all steps to pending
   */
  const resetSteps = useCallback(() => {
    setSteps(createInitialSteps());
    setResult(null);
  }, []);

  /**
   * Select a job from the available jobs list
   */
  const selectJob = useCallback((record: BatchControlRecord) => {
    setJobName(record.key.jobName);
    setProcessDate(record.key.processDate);
    setSequenceNo(String(record.key.sequenceNo));
    resetSteps();
  }, [resetSteps]);

  /**
   * Execute the initialization process
   * Implements 1000-PROCESS-INITIALIZE from BCHCTL00
   */
  const executeInitialization = useCallback(async () => {
    if (!jobName || !processDate || !sequenceNo) {
      alert('Por favor, preencha todos os campos obrigatorios.');
      return;
    }

    setIsProcessing(true);
    resetSteps();

    const key: BatchControlKey = {
      jobName: jobName.trim(),
      processDate: processDate.trim(),
      sequenceNo: parseInt(sequenceNo, 10),
    };

    try {
      // Step 1: 1100-OPEN-FILES
      updateStepStatus('open-files', 'in_progress');
      const openResult = await openConnection();
      setIsConnected(getConnectionStatus());

      if (openResult.returnCode >= ReturnCodes.ERROR) {
        updateStepStatus('open-files', 'error', openResult.errorMessage);
        setResult({ success: false, errorMessage: openResult.errorMessage });
        setIsProcessing(false);
        return;
      }
      updateStepStatus('open-files', 'completed');

      // Step 2: 1200-READ-CONTROL-RECORD
      updateStepStatus('read-record', 'in_progress');
      const readResult = await readControlRecord(key);

      if (readResult.returnCode >= ReturnCodes.ERROR || !readResult.record) {
        updateStepStatus('read-record', 'error', readResult.errorMessage);
        setResult({ success: false, errorMessage: readResult.errorMessage });
        setIsProcessing(false);
        return;
      }
      updateStepStatus('read-record', 'completed');

      // Step 3: 1300-VALIDATE-PROCESS
      updateStepStatus('validate-process', 'in_progress');
      const validateResult = await validateProcess(readResult.record);

      if (validateResult.returnCode >= ReturnCodes.ERROR) {
        updateStepStatus('validate-process', 'error', validateResult.errorMessage);
        setResult({ success: false, errorMessage: validateResult.errorMessage });
        setIsProcessing(false);
        return;
      }

      if (validateResult.returnCode === ReturnCodes.WARNING) {
        updateStepStatus('validate-process', 'error', validateResult.errorMessage);
        setResult({ success: false, errorMessage: validateResult.errorMessage });
        setIsProcessing(false);
        return;
      }
      updateStepStatus('validate-process', 'completed');

      // Step 4: 1400-UPDATE-START-STATUS
      updateStepStatus('update-status', 'in_progress');
      const updateResult = await updateStartStatus(key);

      if (updateResult.returnCode >= ReturnCodes.ERROR) {
        updateStepStatus('update-status', 'error', updateResult.errorMessage);
        setResult({ success: false, errorMessage: updateResult.errorMessage });
        setIsProcessing(false);
        return;
      }
      updateStepStatus('update-status', 'completed');

      // Success!
      setResult({ success: true, record: updateResult.record });

      // Refresh available jobs list
      const jobs = await getAllRecords();
      setAvailableJobs(jobs);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
      setResult({ success: false, errorMessage });
    } finally {
      setIsProcessing(false);
    }
  }, [jobName, processDate, sequenceNo, updateStepStatus, resetSteps]);

  /**
   * Handle disconnect
   */
  const handleDisconnect = useCallback(async () => {
    await closeConnection();
    setIsConnected(getConnectionStatus());
    resetSteps();
  }, [resetSteps]);

  return (
    <div style={styles.container}>
      {/* Header */}
      <header style={styles.header}>
        <h1 style={styles.headerTitle}>Batch Process Initialize</h1>
        <p style={styles.headerSubtitle}>
          Migracao do procedimento 1000-PROCESS-INITIALIZE (BCHCTL00.cbl)
        </p>
      </header>

      <main style={styles.mainContent}>
        {/* Connection Status */}
        <div style={styles.connectionStatus}>
          <div
            style={{
              ...styles.connectionDot,
              ...(isConnected
                ? styles.connectionConnected
                : styles.connectionDisconnected),
            }}
          />
          <span>
            {isConnected ? 'Conectado ao VSAM' : 'Desconectado'}
          </span>
          {isConnected && (
            <button
              style={{ ...styles.button, ...styles.secondaryButton, padding: '6px 12px', fontSize: '12px' }}
              onClick={handleDisconnect}
            >
              Desconectar
            </button>
          )}
        </div>

        {/* Input Form */}
        <section style={styles.section}>
          <h2 style={styles.sectionTitle}>Parametros de Inicializacao</h2>
          <div style={styles.formRow}>
            <div style={styles.formGroup}>
              <label style={styles.label} htmlFor="jobName">
                Nome do Job (BCT-JOB-NAME)
              </label>
              <input
                id="jobName"
                type="text"
                style={styles.input}
                value={jobName}
                onChange={(e) => setJobName(e.target.value.toUpperCase())}
                maxLength={8}
                placeholder="Ex: TRNVAL00"
                disabled={isProcessing}
              />
            </div>
            <div style={styles.formGroup}>
              <label style={styles.label} htmlFor="processDate">
                Data do Processo (BCT-PROCESS-DATE)
              </label>
              <input
                id="processDate"
                type="text"
                style={styles.input}
                value={processDate}
                onChange={(e) => setProcessDate(e.target.value.replace(/\D/g, ''))}
                maxLength={8}
                placeholder="YYYYMMDD"
                disabled={isProcessing}
              />
            </div>
            <div style={styles.formGroup}>
              <label style={styles.label} htmlFor="sequenceNo">
                Numero de Sequencia (BCT-SEQUENCE-NO)
              </label>
              <input
                id="sequenceNo"
                type="text"
                style={styles.input}
                value={sequenceNo}
                onChange={(e) => setSequenceNo(e.target.value.replace(/\D/g, ''))}
                maxLength={4}
                placeholder="0001"
                disabled={isProcessing}
              />
            </div>
          </div>
          <div>
            <button
              style={{
                ...styles.button,
                ...styles.primaryButton,
                ...(isProcessing ? styles.primaryButtonDisabled : {}),
              }}
              onClick={executeInitialization}
              disabled={isProcessing}
            >
              {isProcessing ? 'Processando...' : 'Inicializar Processo'}
            </button>
            <button
              style={{ ...styles.button, ...styles.secondaryButton }}
              onClick={resetSteps}
              disabled={isProcessing}
            >
              Limpar
            </button>
          </div>
        </section>

        {/* Initialization Steps */}
        <section style={styles.section}>
          <h2 style={styles.sectionTitle}>Etapas de Inicializacao</h2>
          <div style={styles.stepContainer}>
            {steps.map((step, index) => (
              <div key={step.id} style={getStepStyle(step.status)}>
                <div style={getStepIconStyle(step.status)}>
                  <StepIcon status={step.status} index={index} />
                </div>
                <div style={styles.stepContent}>
                  <div style={styles.stepName}>{step.name}</div>
                  <div style={styles.stepDescription}>{step.description}</div>
                  {step.errorMessage && (
                    <div style={{ fontSize: '12px', color: '#e53e3e', marginTop: '4px' }}>
                      {step.errorMessage}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Result */}
        {result && (
          <section style={styles.section}>
            <h2 style={styles.sectionTitle}>Resultado</h2>
            <div
              style={{
                ...styles.resultCard,
                ...(result.success ? styles.resultSuccess : styles.resultError),
              }}
            >
              <div
                style={{
                  ...styles.resultTitle,
                  ...(result.success
                    ? styles.resultTitleSuccess
                    : styles.resultTitleError),
                }}
              >
                {result.success
                  ? 'Processo inicializado com sucesso!'
                  : 'Falha na inicializacao do processo'}
              </div>

              {result.success && result.record && (
                <div style={styles.recordDetails}>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Job Name:</span>
                    <span style={styles.detailValue}>{result.record.key.jobName}</span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Process Date:</span>
                    <span style={styles.detailValue}>{result.record.key.processDate}</span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Sequence No:</span>
                    <span style={styles.detailValue}>{result.record.key.sequenceNo}</span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Status:</span>
                    <span style={getStatusBadgeStyle(result.record.status)}>
                      {BatchControlStatusLabels[result.record.status]}
                    </span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Program Name:</span>
                    <span style={styles.detailValue}>
                      {result.record.processControl.programName}
                    </span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Start Time:</span>
                    <span style={styles.detailValue}>
                      {result.record.processControl.startTime || 'N/A'}
                    </span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Attempt Timestamp:</span>
                    <span style={styles.detailValue}>
                      {result.record.statistics.attemptTimestamp || 'N/A'}
                    </span>
                  </div>
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Restart Count:</span>
                    <span style={styles.detailValue}>
                      {result.record.statistics.restartCount}
                    </span>
                  </div>
                </div>
              )}

              {!result.success && result.errorMessage && (
                <div style={{ color: '#c53030', fontSize: '14px' }}>
                  <strong>Erro:</strong> {result.errorMessage}
                </div>
              )}
            </div>
          </section>
        )}

        {/* Available Jobs */}
        <section style={styles.section}>
          <h2 style={styles.sectionTitle}>Jobs Disponiveis</h2>
          <p style={{ fontSize: '13px', color: '#718096', marginBottom: '12px' }}>
            Clique em um job para preencher os campos automaticamente
          </p>
          <div style={styles.jobList}>
            {availableJobs.map((job) => {
              const keyString = `${job.key.jobName}-${job.key.processDate}-${job.key.sequenceNo}`;
              return (
                <div
                  key={keyString}
                  style={{
                    ...styles.jobItem,
                    ...(hoveredJob === keyString ? styles.jobItemHover : {}),
                  }}
                  onClick={() => selectJob(job)}
                  onMouseEnter={() => setHoveredJob(keyString)}
                  onMouseLeave={() => setHoveredJob(null)}
                >
                  <div style={styles.jobInfo}>
                    <span style={styles.jobName}>{job.key.jobName}</span>
                    <span style={styles.jobDate}>
                      {job.key.processDate} / Seq: {job.key.sequenceNo}
                    </span>
                  </div>
                  <span style={getStatusBadgeStyle(job.status)}>
                    {BatchControlStatusLabels[job.status]}
                  </span>
                </div>
              );
            })}
            {availableJobs.length === 0 && (
              <div style={{ color: '#718096', fontSize: '14px', padding: '16px' }}>
                Nenhum job disponivel. Conecte-se ao VSAM para carregar os dados.
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
};

export default BatchProcessInitialize;
