import { useState } from 'react';
import type { FormEvent, ChangeEvent } from 'react';
import { Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const ACCOUNT_NUMBER_PATTERN = /^ACC\d{3,6}$/;
const ACCOUNT_NUMBER_MAX_LENGTH = 9;

function validateAccountNumber(value: string): string | null {
  if (!value) {
    return 'Account number is required';
  }
  if (!value.startsWith('ACC')) {
    return 'Account number must start with "ACC"';
  }
  if (value.length < 6) {
    return 'Account number must be at least 6 characters (e.g., ACC001)';
  }
  if (!ACCOUNT_NUMBER_PATTERN.test(value)) {
    return 'Account number must be "ACC" followed by 3-6 digits (e.g., ACC001)';
  }
  return null;
}

interface AccountSearchFormProps {
  onSearch: (accountNumber: string) => void;
  isLoading?: boolean;
  placeholder?: string;
  label?: string;
}

export function AccountSearchForm({
  onSearch,
  isLoading = false,
  placeholder = 'Enter account number (e.g., ACC001)',
  label = 'Account Number',
}: AccountSearchFormProps) {
  const [accountNumber, setAccountNumber] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toUpperCase();
    if (value.length <= ACCOUNT_NUMBER_MAX_LENGTH) {
      setAccountNumber(value);
      if (touched) {
        setError(validateAccountNumber(value));
      }
    }
  };

  const handleBlur = () => {
    setTouched(true);
    if (accountNumber.trim()) {
      setError(validateAccountNumber(accountNumber.trim()));
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = accountNumber.trim();
    setTouched(true);
    const validationError = validateAccountNumber(trimmed);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    onSearch(trimmed);
  };

  const isValid = !validateAccountNumber(accountNumber.trim());

  return (
    <form onSubmit={handleSubmit} aria-label="Account search" className="flex items-end gap-3">
      <div className="flex-1">
        <label htmlFor="account-search" className="mb-2 block text-sm font-medium text-[#CBD5E1]">
          {label}
        </label>
        <Input
          id="account-search"
          value={accountNumber}
          onChange={handleChange}
          onBlur={handleBlur}
          placeholder={placeholder}
          disabled={isLoading}
          maxLength={ACCOUNT_NUMBER_MAX_LENGTH}
          aria-invalid={touched && !!error}
          aria-describedby={error ? 'account-search-error' : undefined}
          className={touched && error ? 'border-[#F87171] focus:ring-[#F87171]' : ''}
        />
        {touched && error && (
          <p id="account-search-error" className="mt-1 text-sm text-[#F87171]" role="alert">
            {error}
          </p>
        )}
      </div>
      <Button type="submit" disabled={isLoading || !isValid}>
        <Search className="mr-2 h-4 w-4" />
        {isLoading ? 'Searching...' : 'Search'}
      </Button>
    </form>
  );
}
