interface Props {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  label?: string;
}

/** ACCTIN / HISAIN unprotected input fields (10 chars). */
export default function AccountInput({ value, onChange, onSubmit, label = 'Account:' }: Props) {
  return (
    <form
      className="account-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <label htmlFor="account">{label}</label>
      <input
        id="account"
        name="account"
        maxLength={10}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="100000001"
      />
      <button type="submit">Inquire</button>
    </form>
  );
}
