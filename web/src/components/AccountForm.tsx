import { useState, type FormEvent } from 'react';

interface AccountFormProps {
  label: string;
  initialValue?: string;
  onSubmit: (account: string) => void;
}

/**
 * Account-number entry field, mirroring the unprotected ACCTIN / HISAIN input
 * fields (PIC X(10)) on the POSMAP / HISMAP BMS screens.
 */
export function AccountForm({ label, initialValue = '', onSubmit }: AccountFormProps) {
  const [account, setAccount] = useState(initialValue);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = account.trim();
    if (trimmed) {
      onSubmit(trimmed.toUpperCase());
    }
  };

  return (
    <form className="account-form" onSubmit={handleSubmit}>
      <label className="account-form__label" htmlFor="account">
        {label}
      </label>
      <input
        id="account"
        name="account"
        className="account-form__input"
        maxLength={10}
        autoComplete="off"
        placeholder="ACCT000001"
        value={account}
        onChange={(event) => setAccount(event.target.value)}
      />
      <button type="submit" className="btn">
        Inquire
      </button>
    </form>
  );
}
