import { useState, useCallback } from 'react';
import type { ValidationResult } from '@/utils/validation';

type ValidatorFn = (value: string) => ValidationResult;

interface UseValidationResult {
  errors: Record<string, string>;
  validateField: (field: string, value: string, validator: ValidatorFn) => boolean;
  clearError: (field: string) => void;
  clearAllErrors: () => void;
  hasErrors: boolean;
}

export function useValidation(): UseValidationResult {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateField = useCallback((field: string, value: string, validator: ValidatorFn): boolean => {
    const result = validator(value);
    if (!result.valid && result.error) {
      setErrors((prev) => ({ ...prev, [field]: result.error! }));
      return false;
    }
    setErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
    return true;
  }, []);

  const clearError = useCallback((field: string) => {
    setErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  const clearAllErrors = useCallback(() => {
    setErrors({});
  }, []);

  return {
    errors,
    validateField,
    clearError,
    clearAllErrors,
    hasErrors: Object.keys(errors).length > 0,
  };
}
