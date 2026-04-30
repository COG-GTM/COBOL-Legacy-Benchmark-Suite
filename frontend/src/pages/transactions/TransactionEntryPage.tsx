import { useState } from 'react';
import {
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Steps,
  Descriptions,
  Space,
  Card,
} from 'antd';
import { useForm, Controller } from 'react-hook-form';
import type {
  TransactionEntry,
  TransactionType,
  InvestmentType,
} from '../../types/transaction';
import {
  TRANSACTION_TYPE_LABELS,
  INVESTMENT_TYPE_LABELS,
} from '../../types/transaction';
import { ValidationCode } from '../../types/validation';
import {
  validatePortfolioId,
  validateAccountNumber,
  validateInvestmentType,
  validateAmount,
} from '../../utils/validation';
import { formatCurrency, formatNumber } from '../../utils/formatters';
import { toast } from '../../components/Toast';

const transactionTypeOptions = (
  Object.entries(TRANSACTION_TYPE_LABELS) as [TransactionType, string][]
).map(([value, label]) => ({ value, label }));

const investmentTypeOptions = (
  Object.entries(INVESTMENT_TYPE_LABELS) as [InvestmentType, string][]
).map(([value, label]) => ({ value, label }));

const stepItems = [
  { title: 'Enter Details' },
  { title: 'Review & Confirm' },
];

export function Component() {
  const [currentStep, setCurrentStep] = useState(0);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const {
    control,
    handleSubmit,
    reset,
    getValues,
    trigger,
    formState: { errors },
  } = useForm<TransactionEntry>({
    defaultValues: {
      portfolioId: '',
      accountNo: '',
      transactionType: undefined as unknown as TransactionType,
      investmentType: undefined as unknown as InvestmentType,
      units: undefined as unknown as number,
      amount: undefined as unknown as number,
    },
    mode: 'onBlur',
  });

  const setFieldError = (field: string, message: string) => {
    setFieldErrors((prev) => ({ ...prev, [field]: message }));
  };

  const clearFieldError = (field: string) => {
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const validateField = (
    field: string,
    validator: () => { code: ValidationCode; message: string },
  ) => {
    const result = validator();
    if (result.code !== ValidationCode.SUCCESS) {
      setFieldError(field, `Error ${result.code}: ${result.message}`);
      return false;
    }
    clearFieldError(field);
    return true;
  };

  const handleReview = async () => {
    const rhfValid = await trigger();
    const values = getValues();

    const newErrors: Record<string, string> = {};
    let allValid = true;

    const portfolioResult = validatePortfolioId(values.portfolioId);
    if (portfolioResult.code !== ValidationCode.SUCCESS) {
      newErrors.portfolioId = `Error ${portfolioResult.code}: ${portfolioResult.message}`;
      allValid = false;
    }

    const accountResult = validateAccountNumber(values.accountNo);
    if (accountResult.code !== ValidationCode.SUCCESS) {
      newErrors.accountNo = `Error ${accountResult.code}: ${accountResult.message}`;
      allValid = false;
    }

    if (values.investmentType) {
      const investmentResult = validateInvestmentType(values.investmentType);
      if (investmentResult.code !== ValidationCode.SUCCESS) {
        newErrors.investmentType = `Error ${investmentResult.code}: ${investmentResult.message}`;
        allValid = false;
      }
    }

    if (values.amount != null && values.amount !== 0) {
      const amountResult = validateAmount(values.amount);
      if (amountResult.code !== ValidationCode.SUCCESS) {
        newErrors.amount = `Error ${amountResult.code}: ${amountResult.message}`;
        allValid = false;
      }
    }

    setFieldErrors(newErrors);

    if (rhfValid && allValid) {
      setCurrentStep(1);
    }
  };

  const handleConfirm = () => {
    toast.success('Transaction submitted successfully');
    reset();
    setFieldErrors({});
    setCurrentStep(0);
  };

  const handleBackToEdit = () => {
    setCurrentStep(0);
  };

  const handleReset = () => {
    reset();
    setFieldErrors({});
  };

  const getValidationProps = (field: string) => {
    const error = fieldErrors[field];
    if (error) {
      return { validateStatus: 'error' as const, help: error };
    }
    return {};
  };

  const values = getValues();

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <Steps current={currentStep} items={stepItems} style={{ marginBottom: 32 }} />

      {currentStep === 0 && (
        <Card title="Enter Transaction Details">
          <Form layout="vertical">
            <Controller
              name="portfolioId"
              control={control}
              rules={{ required: 'Portfolio ID is required' }}
              render={({ field }) => (
                <Form.Item
                  label="Portfolio ID"
                  required
                  {...getValidationProps('portfolioId')}
                  {...(errors.portfolioId && !fieldErrors.portfolioId
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.portfolioId.message,
                      }
                    : {})}
                >
                  <Input
                    {...field}
                    placeholder="PORT0001"
                    onBlur={(e) => {
                      field.onBlur();
                      if (e.target.value) {
                        validateField('portfolioId', () =>
                          validatePortfolioId(e.target.value),
                        );
                      } else {
                        clearFieldError('portfolioId');
                      }
                    }}
                  />
                </Form.Item>
              )}
            />

            <Controller
              name="accountNo"
              control={control}
              rules={{ required: 'Account Number is required' }}
              render={({ field }) => (
                <Form.Item
                  label="Account Number"
                  required
                  {...getValidationProps('accountNo')}
                  {...(errors.accountNo && !fieldErrors.accountNo
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.accountNo.message,
                      }
                    : {})}
                >
                  <Input
                    {...field}
                    placeholder="1000000001"
                    onBlur={(e) => {
                      field.onBlur();
                      if (e.target.value) {
                        validateField('accountNo', () =>
                          validateAccountNumber(e.target.value),
                        );
                      } else {
                        clearFieldError('accountNo');
                      }
                    }}
                  />
                </Form.Item>
              )}
            />

            <Controller
              name="transactionType"
              control={control}
              rules={{ required: 'Transaction Type is required' }}
              render={({ field }) => (
                <Form.Item
                  label="Transaction Type"
                  required
                  {...(errors.transactionType
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.transactionType.message,
                      }
                    : {})}
                >
                  <Select
                    {...field}
                    placeholder="Select transaction type"
                    options={transactionTypeOptions}
                  />
                </Form.Item>
              )}
            />

            <Controller
              name="investmentType"
              control={control}
              rules={{ required: 'Investment Type is required' }}
              render={({ field }) => (
                <Form.Item
                  label="Investment Type"
                  required
                  {...getValidationProps('investmentType')}
                  {...(errors.investmentType && !fieldErrors.investmentType
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.investmentType.message,
                      }
                    : {})}
                >
                  <Select
                    {...field}
                    placeholder="Select investment type"
                    options={investmentTypeOptions}
                    onChange={(value) => {
                      field.onChange(value);
                      if (value) {
                        validateField('investmentType', () =>
                          validateInvestmentType(value),
                        );
                      }
                    }}
                  />
                </Form.Item>
              )}
            />

            <Controller
              name="units"
              control={control}
              rules={{
                required: 'Units is required',
                validate: (v) => (v > 0 ? true : 'Units must be greater than 0'),
              }}
              render={({ field }) => (
                <Form.Item
                  label="Units"
                  required
                  {...(errors.units
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.units.message,
                      }
                    : {})}
                >
                  <InputNumber
                    {...field}
                    style={{ width: '100%' }}
                    placeholder="0.0000"
                    precision={4}
                    min={0}
                  />
                </Form.Item>
              )}
            />

            <Controller
              name="amount"
              control={control}
              rules={{
                required: 'Amount is required',
                validate: (v) =>
                  v > 0 ? true : 'Amount must be greater than 0',
              }}
              render={({ field }) => (
                <Form.Item
                  label="Amount"
                  required
                  {...getValidationProps('amount')}
                  {...(errors.amount && !fieldErrors.amount
                    ? {
                        validateStatus: 'error' as const,
                        help: errors.amount.message,
                      }
                    : {})}
                >
                  <InputNumber
                    {...field}
                    style={{ width: '100%' }}
                    prefix="$"
                    placeholder="0.00"
                    precision={2}
                    min={0}
                    onBlur={(e) => {
                      field.onBlur();
                      const raw = e.target.value.replace(/[$,]/g, '');
                      const num = parseFloat(raw);
                      if (!isNaN(num) && num > 0) {
                        validateField('amount', () => validateAmount(num));
                      } else {
                        clearFieldError('amount');
                      }
                    }}
                  />
                </Form.Item>
              )}
            />

            <Form.Item>
              <Space>
                <Button type="primary" onClick={handleSubmit(handleReview)}>
                  Review Transaction
                </Button>
                <Button onClick={handleReset}>Reset</Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>
      )}

      {currentStep === 1 && (
        <Card title="Review & Confirm Transaction">
          <Descriptions bordered column={1} style={{ marginBottom: 24 }}>
            <Descriptions.Item label="Portfolio ID">
              {values.portfolioId}
            </Descriptions.Item>
            <Descriptions.Item label="Account Number">
              {values.accountNo}
            </Descriptions.Item>
            <Descriptions.Item label="Transaction Type">
              {TRANSACTION_TYPE_LABELS[values.transactionType]}
            </Descriptions.Item>
            <Descriptions.Item label="Investment Type">
              {INVESTMENT_TYPE_LABELS[values.investmentType]}
            </Descriptions.Item>
            <Descriptions.Item label="Units">
              {formatNumber(values.units, 4)}
            </Descriptions.Item>
            <Descriptions.Item label="Amount">
              {formatCurrency(values.amount)}
            </Descriptions.Item>
          </Descriptions>

          <Space>
            <Button type="primary" onClick={handleConfirm}>
              Confirm &amp; Submit
            </Button>
            <Button onClick={handleBackToEdit}>Back to Edit</Button>
          </Space>
        </Card>
      )}
    </div>
  );
}
