import { useState, useCallback } from 'react';
import type { Portfolio } from '../types';

export interface PortfolioFormErrors {
  id?: string;
  accountNo?: string;
  clientName?: string;
  status?: string;
  totalValue?: string;
}

export interface PortfolioFormData {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: Portfolio['clientType'];
  status: Portfolio['status'];
  totalValue: string;
  cashBalance: string;
}

const initialData: PortfolioFormData = {
  id: '',
  accountNo: '',
  clientName: '',
  clientType: 'I',
  status: 'A',
  totalValue: '',
  cashBalance: '',
};

export function usePortfolioForm(initial?: Partial<PortfolioFormData>) {
  const [formData, setFormData] = useState<PortfolioFormData>({
    ...initialData,
    ...initial,
  });
  const [errors, setErrors] = useState<PortfolioFormErrors>({});

  const updateField = useCallback(
    (field: keyof PortfolioFormData, value: string) => {
      setFormData((prev) => ({ ...prev, [field]: value }));
      setErrors((prev) => ({ ...prev, [field]: undefined }));
    },
    [],
  );

  const validate = useCallback((): boolean => {
    const newErrors: PortfolioFormErrors = {};

    if (!formData.id || !/^[A-Z]{4}\d{4}$/.test(formData.id)) {
      newErrors.id = 'Portfolio ID must be 4 uppercase letters + 4 digits (e.g. PORT0001)';
    }
    if (!formData.accountNo || !/^\d{10}$/.test(formData.accountNo)) {
      newErrors.accountNo = 'Account number must be exactly 10 digits';
    }
    if (!formData.clientName.trim()) {
      newErrors.clientName = 'Client name is required';
    }
    if (!['A', 'I', 'C'].includes(formData.status)) {
      newErrors.status = 'Status must be A (Active), I (Inactive), or C (Closed)';
    }
    if (formData.totalValue && isNaN(Number(formData.totalValue))) {
      newErrors.totalValue = 'Total value must be a valid number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  return { formData, errors, updateField, validate, setFormData };
}
