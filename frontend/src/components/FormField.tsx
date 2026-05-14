import type { InputHTMLAttributes, SelectHTMLAttributes, ReactNode } from 'react';

interface BaseProps {
  label: string;
  error?: string;
  children?: ReactNode;
}

type InputFieldProps = BaseProps & InputHTMLAttributes<HTMLInputElement> & { as?: 'input' };
type SelectFieldProps = BaseProps & SelectHTMLAttributes<HTMLSelectElement> & { as: 'select' };

type FormFieldProps = InputFieldProps | SelectFieldProps;

export default function FormField(props: FormFieldProps) {
  const { label, error, children, as, ...rest } = props;
  const id = rest.id ?? label.toLowerCase().replace(/\s+/g, '-');
  const baseClass =
    'w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ' +
    (error ? 'border-red-500' : 'border-gray-300');

  return (
    <div className="mb-4">
      <label htmlFor={id} className="block text-sm font-medium text-gray-700 mb-1">
        {label}
      </label>
      {as === 'select' ? (
        <select id={id} className={baseClass} {...(rest as SelectHTMLAttributes<HTMLSelectElement>)}>
          {children}
        </select>
      ) : (
        <input id={id} className={baseClass} {...(rest as InputHTMLAttributes<HTMLInputElement>)} />
      )}
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}
