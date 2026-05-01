"use client";

import { useState, type FormEvent } from "react";
import { Search } from "lucide-react";

interface AccountSearchProps {
  onSearch: (accountNo: string) => void;
  initialValue?: string;
}

export default function AccountSearch({
  onSearch,
  initialValue = "",
}: AccountSearchProps) {
  const [value, setValue] = useState(initialValue);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed) onSearch(trimmed);
  }

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-2">
      <label
        htmlFor="account-search"
        className="text-sm font-medium text-gray-700"
      >
        Account:
      </label>
      <div className="relative">
        <input
          id="account-search"
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Enter account number"
          maxLength={10}
          className="w-48 rounded-lg border border-gray-300 py-2 pl-3 pr-10 text-sm font-mono placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <button
          type="submit"
          className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-indigo-600"
        >
          <Search className="h-4 w-4" />
        </button>
      </div>
    </form>
  );
}
