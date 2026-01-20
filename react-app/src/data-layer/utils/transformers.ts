/**
 * Data Transformation Utilities
 * Provides functions to transform data between COBOL formats and modern formats
 * 
 * These utilities handle the conversion of:
 * - COBOL date formats (YYYYMMDD) to JavaScript Date objects
 * - COBOL time formats (HHMMSS) to time strings
 * - COBOL packed decimal (COMP-3) representations to numbers
 * - COBOL character fields to trimmed strings
 */

/**
 * Convert COBOL date format (YYYYMMDD) to JavaScript Date
 */
export function cobolDateToDate(cobolDate: string): Date | null {
  if (!cobolDate || cobolDate.length !== 8) {
    return null;
  }

  const year = parseInt(cobolDate.substring(0, 4), 10);
  const month = parseInt(cobolDate.substring(4, 6), 10) - 1; // JS months are 0-indexed
  const day = parseInt(cobolDate.substring(6, 8), 10);

  if (isNaN(year) || isNaN(month) || isNaN(day)) {
    return null;
  }

  return new Date(year, month, day);
}

/**
 * Convert JavaScript Date to COBOL date format (YYYYMMDD)
 */
export function dateToCobolDate(date: Date): string {
  const year = date.getFullYear().toString();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}${month}${day}`;
}

/**
 * Convert COBOL time format (HHMMSS) to time string (HH:MM:SS)
 */
export function cobolTimeToTimeString(cobolTime: string): string {
  if (!cobolTime || cobolTime.length !== 6) {
    return '';
  }

  const hours = cobolTime.substring(0, 2);
  const minutes = cobolTime.substring(2, 4);
  const seconds = cobolTime.substring(4, 6);

  return `${hours}:${minutes}:${seconds}`;
}

/**
 * Convert time string (HH:MM:SS) to COBOL time format (HHMMSS)
 */
export function timeStringToCobolTime(timeString: string): string {
  return timeString.replace(/:/g, '');
}

/**
 * Convert COBOL timestamp (26 characters) to JavaScript Date
 * Format: YYYY-MM-DD-HH.MM.SS.MMMMMM
 */
export function cobolTimestampToDate(timestamp: string): Date | null {
  if (!timestamp || timestamp.length < 19) {
    return null;
  }

  try {
    // Parse the timestamp format: YYYY-MM-DD-HH.MM.SS.MMMMMM
    const year = parseInt(timestamp.substring(0, 4), 10);
    const month = parseInt(timestamp.substring(5, 7), 10) - 1;
    const day = parseInt(timestamp.substring(8, 10), 10);
    const hours = parseInt(timestamp.substring(11, 13), 10);
    const minutes = parseInt(timestamp.substring(14, 16), 10);
    const seconds = parseInt(timestamp.substring(17, 19), 10);
    const microseconds = timestamp.length >= 26 
      ? parseInt(timestamp.substring(20, 26), 10) 
      : 0;

    const date = new Date(year, month, day, hours, minutes, seconds, Math.floor(microseconds / 1000));
    return date;
  } catch {
    return null;
  }
}

/**
 * Convert JavaScript Date to COBOL timestamp format
 * Format: YYYY-MM-DD-HH.MM.SS.MMMMMM
 */
export function dateToCobolTimestamp(date: Date): string {
  const year = date.getFullYear().toString();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const seconds = date.getSeconds().toString().padStart(2, '0');
  const microseconds = (date.getMilliseconds() * 1000).toString().padStart(6, '0');

  return `${year}-${month}-${day}-${hours}.${minutes}.${seconds}.${microseconds}`;
}

/**
 * Convert ISO date string to COBOL date format
 */
export function isoDateToCobolDate(isoDate: string): string {
  const date = new Date(isoDate);
  return dateToCobolDate(date);
}

/**
 * Convert COBOL date to ISO date string
 */
export function cobolDateToIsoDate(cobolDate: string): string {
  const date = cobolDateToDate(cobolDate);
  if (!date) {
    return '';
  }
  return date.toISOString().split('T')[0];
}

/**
 * Trim and normalize COBOL character field
 * COBOL fields are often padded with spaces
 */
export function trimCobolField(field: string): string {
  if (!field) {
    return '';
  }
  return field.trim();
}

/**
 * Pad string to COBOL field length
 */
export function padCobolField(value: string, length: number, padChar: string = ' '): string {
  if (value.length >= length) {
    return value.substring(0, length);
  }
  return value.padEnd(length, padChar);
}

/**
 * Pad numeric string with leading zeros
 */
export function padNumericField(value: number | string, length: number): string {
  const strValue = typeof value === 'number' ? value.toString() : value;
  return strValue.padStart(length, '0');
}

/**
 * Convert COBOL packed decimal representation to number
 * COBOL COMP-3 format stores digits in nibbles with sign in last nibble
 * This is a simplified representation for JavaScript
 */
export function parseCobolDecimal(
  value: string,
  integerDigits: number,
  decimalDigits: number
): number {
  const cleanValue = value.replace(/[^0-9.-]/g, '');
  const numValue = parseFloat(cleanValue);
  
  if (isNaN(numValue)) {
    return 0;
  }

  // Round to specified decimal places
  const multiplier = Math.pow(10, decimalDigits);
  return Math.round(numValue * multiplier) / multiplier;
}

/**
 * Format number as COBOL decimal string
 */
export function formatCobolDecimal(
  value: number,
  integerDigits: number,
  decimalDigits: number
): string {
  const isNegative = value < 0;
  const absValue = Math.abs(value);
  
  const multiplier = Math.pow(10, decimalDigits);
  const scaledValue = Math.round(absValue * multiplier);
  
  const totalDigits = integerDigits + decimalDigits;
  let strValue = scaledValue.toString().padStart(totalDigits, '0');
  
  // Insert decimal point
  if (decimalDigits > 0) {
    const intPart = strValue.substring(0, strValue.length - decimalDigits);
    const decPart = strValue.substring(strValue.length - decimalDigits);
    strValue = `${intPart}.${decPart}`;
  }
  
  return isNegative ? `-${strValue}` : strValue;
}

/**
 * Convert currency amount to display format
 */
export function formatCurrency(amount: number, currencyCode: string = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currencyCode,
  }).format(amount);
}

/**
 * Convert display currency to number
 */
export function parseCurrency(displayValue: string): number {
  const cleanValue = displayValue.replace(/[^0-9.-]/g, '');
  return parseFloat(cleanValue) || 0;
}

/**
 * Format quantity with decimal places
 */
export function formatQuantity(quantity: number, decimalPlaces: number = 4): string {
  return quantity.toFixed(decimalPlaces);
}

/**
 * Format price with decimal places
 */
export function formatPrice(price: number, decimalPlaces: number = 4): string {
  return price.toFixed(decimalPlaces);
}

/**
 * Format percentage
 */
export function formatPercentage(value: number, decimalPlaces: number = 2): string {
  return `${value.toFixed(decimalPlaces)}%`;
}

/**
 * Generate a unique ID in COBOL format
 * Format: YYYYMMDDHHMMSS + sequence
 */
export function generateCobolId(sequence: number): string {
  const now = new Date();
  const datePart = dateToCobolDate(now);
  const timePart = now.toTimeString().substring(0, 8).replace(/:/g, '');
  const seqPart = sequence.toString().padStart(6, '0');
  return `${datePart}${timePart}${seqPart}`;
}

/**
 * Parse COBOL ID to extract date and sequence
 */
export function parseCobolId(cobolId: string): { date: Date | null; sequence: number } {
  if (!cobolId || cobolId.length < 20) {
    return { date: null, sequence: 0 };
  }

  const datePart = cobolId.substring(0, 8);
  const timePart = cobolId.substring(8, 14);
  const seqPart = cobolId.substring(14);

  const date = cobolDateToDate(datePart);
  if (date && timePart.length === 6) {
    const hours = parseInt(timePart.substring(0, 2), 10);
    const minutes = parseInt(timePart.substring(2, 4), 10);
    const seconds = parseInt(timePart.substring(4, 6), 10);
    date.setHours(hours, minutes, seconds);
  }

  const sequence = parseInt(seqPart, 10) || 0;

  return { date, sequence };
}

/**
 * Convert COBOL record to JSON-friendly object
 * Handles common COBOL data types and formats
 */
export function normalizeCobolRecord<T extends Record<string, unknown>>(
  record: T,
  fieldDefinitions: FieldDefinition[]
): T {
  const normalized = { ...record };

  for (const field of fieldDefinitions) {
    const value = normalized[field.name as keyof T];
    
    if (value === undefined || value === null) {
      continue;
    }

    switch (field.type) {
      case 'date':
        if (typeof value === 'string') {
          (normalized as Record<string, unknown>)[field.name] = cobolDateToDate(value);
        }
        break;
      case 'timestamp':
        if (typeof value === 'string') {
          (normalized as Record<string, unknown>)[field.name] = cobolTimestampToDate(value);
        }
        break;
      case 'decimal':
        if (typeof value === 'string') {
          (normalized as Record<string, unknown>)[field.name] = parseCobolDecimal(
            value,
            field.integerDigits ?? 9,
            field.decimalDigits ?? 2
          );
        }
        break;
      case 'string':
        if (typeof value === 'string') {
          (normalized as Record<string, unknown>)[field.name] = trimCobolField(value);
        }
        break;
    }
  }

  return normalized;
}

/**
 * Field definition for COBOL record normalization
 */
export interface FieldDefinition {
  name: string;
  type: 'string' | 'date' | 'timestamp' | 'decimal' | 'integer';
  integerDigits?: number;
  decimalDigits?: number;
  length?: number;
}

/**
 * Convert JavaScript object to COBOL record format
 */
export function toCobolRecord<T extends Record<string, unknown>>(
  record: T,
  fieldDefinitions: FieldDefinition[]
): Record<string, string> {
  const cobolRecord: Record<string, string> = {};

  for (const field of fieldDefinitions) {
    const value = record[field.name as keyof T];
    
    if (value === undefined || value === null) {
      cobolRecord[field.name] = '';
      continue;
    }

    switch (field.type) {
      case 'date':
        if (value instanceof Date) {
          cobolRecord[field.name] = dateToCobolDate(value);
        } else if (typeof value === 'string') {
          cobolRecord[field.name] = value;
        }
        break;
      case 'timestamp':
        if (value instanceof Date) {
          cobolRecord[field.name] = dateToCobolTimestamp(value);
        } else if (typeof value === 'string') {
          cobolRecord[field.name] = value;
        }
        break;
      case 'decimal':
        if (typeof value === 'number') {
          cobolRecord[field.name] = formatCobolDecimal(
            value,
            field.integerDigits ?? 9,
            field.decimalDigits ?? 2
          );
        }
        break;
      case 'integer':
        if (typeof value === 'number') {
          cobolRecord[field.name] = padNumericField(value, field.length ?? 9);
        }
        break;
      case 'string':
        cobolRecord[field.name] = padCobolField(
          String(value),
          field.length ?? 8
        );
        break;
    }
  }

  return cobolRecord;
}
