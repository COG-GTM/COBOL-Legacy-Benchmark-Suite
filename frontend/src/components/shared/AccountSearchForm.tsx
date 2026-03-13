import { useState } from 'react';
import type { FormEvent } from 'react';
import { Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

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

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = accountNumber.trim();
    if (trimmed) {
      onSearch(trimmed);
    }
  };

  return (
    <form onSubmit={handleSubmit} aria-label="Account search" className="flex items-end gap-3">
      <div className="flex-1">
        <label htmlFor="account-search" className="mb-2 block text-sm font-medium text-[#CBD5E1]">
          {label}
        </label>
        <Input
          id="account-search"
          value={accountNumber}
          onChange={(e) => setAccountNumber(e.target.value)}
          placeholder={placeholder}
          disabled={isLoading}
        />
      </div>
      <Button type="submit" disabled={isLoading || !accountNumber.trim()}>
        <Search className="mr-2 h-4 w-4" />
        {isLoading ? 'Searching...' : 'Search'}
      </Button>
    </form>
  );
}
