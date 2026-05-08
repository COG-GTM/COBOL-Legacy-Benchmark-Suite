import { describe, it, expect } from 'vitest'
import {
  createAppError,
  mapReturnCodeToSeverity,
  mapHttpStatusToSeverity,
  ERROR_CODES,
  RETURN_CODES,
} from '@/types/errors'

describe('createAppError', () => {
  it('creates an AppError with correct fields and auto-generated timestamp', () => {
    const before = new Date().toISOString()
    const error = createAppError('E001', 'Record not found', 'error')
    const after = new Date().toISOString()

    expect(error.code).toBe('E001')
    expect(error.message).toBe('Record not found')
    expect(error.severity).toBe('error')
    expect(error.timestamp).toBeDefined()
    expect(error.timestamp! >= before).toBe(true)
    expect(error.timestamp! <= after).toBe(true)
  })

  it('applies optional fields (category, type, details, action)', () => {
    const error = createAppError('E002', 'Duplicate record', 'warning', {
      category: 'validation',
      type: 'database',
      details: 'Duplicate key violation',
      action: 'retry',
    })

    expect(error.code).toBe('E002')
    expect(error.message).toBe('Duplicate record')
    expect(error.severity).toBe('warning')
    expect(error.category).toBe('validation')
    expect(error.type).toBe('database')
    expect(error.details).toBe('Duplicate key violation')
    expect(error.action).toBe('retry')
  })
})

describe('mapReturnCodeToSeverity', () => {
  it('maps 0 to info', () => {
    expect(mapReturnCodeToSeverity(0)).toBe('info')
  })

  it('maps 4 to warning', () => {
    expect(mapReturnCodeToSeverity(4)).toBe('warning')
  })

  it('maps 8 to error', () => {
    expect(mapReturnCodeToSeverity(8)).toBe('error')
  })

  it('maps 12 to error', () => {
    expect(mapReturnCodeToSeverity(12)).toBe('error')
  })

  it('maps 16 to error', () => {
    expect(mapReturnCodeToSeverity(16)).toBe('error')
  })
})

describe('mapHttpStatusToSeverity', () => {
  it('maps 4xx to warning', () => {
    expect(mapHttpStatusToSeverity(400)).toBe('warning')
    expect(mapHttpStatusToSeverity(404)).toBe('warning')
    expect(mapHttpStatusToSeverity(499)).toBe('warning')
  })

  it('maps 5xx to error', () => {
    expect(mapHttpStatusToSeverity(500)).toBe('error')
    expect(mapHttpStatusToSeverity(503)).toBe('error')
  })

  it('maps others to info', () => {
    expect(mapHttpStatusToSeverity(200)).toBe('info')
    expect(mapHttpStatusToSeverity(301)).toBe('info')
  })
})

describe('ERROR_CODES', () => {
  it('contains all E001-E010 entries with correct severity', () => {
    const expectedEntries: Record<string, { code: string; message: string; severity: string }> = {
      E001: { code: 'E001', message: 'Record not found', severity: 'error' },
      E002: { code: 'E002', message: 'Duplicate record', severity: 'error' },
      E003: { code: 'E003', message: 'Invalid input data', severity: 'warning' },
      E004: { code: 'E004', message: 'Database error', severity: 'error' },
      E005: { code: 'E005', message: 'File access error', severity: 'error' },
      E006: { code: 'E006', message: 'Authorization failed', severity: 'error' },
      E007: { code: 'E007', message: 'Processing error', severity: 'error' },
      E008: { code: 'E008', message: 'Communication error', severity: 'warning' },
      E009: { code: 'E009', message: 'Resource unavailable', severity: 'warning' },
      E010: { code: 'E010', message: 'System error', severity: 'error' },
    }

    const keys = Object.keys(ERROR_CODES)
    expect(keys).toHaveLength(10)

    for (const [key, expected] of Object.entries(expectedEntries)) {
      const entry = ERROR_CODES[key as keyof typeof ERROR_CODES]
      expect(entry.code).toBe(expected.code)
      expect(entry.message).toBe(expected.message)
      expect(entry.severity).toBe(expected.severity)
    }
  })
})

describe('RETURN_CODES', () => {
  it('has correct numeric values', () => {
    expect(RETURN_CODES.SUCCESS).toBe(0)
    expect(RETURN_CODES.WARNING).toBe(4)
    expect(RETURN_CODES.ERROR).toBe(8)
    expect(RETURN_CODES.SEVERE).toBe(12)
    expect(RETURN_CODES.TERMINAL).toBe(16)
  })
})
