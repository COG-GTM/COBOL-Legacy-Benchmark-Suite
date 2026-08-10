import type { ReactNode } from 'react';

interface FormFieldProps {
  /** Id of the control the label points at. */
  id: string;
  label: string;
  required?: boolean;
  error?: string;
  /** Helper text shown while the field has no error (usually the PIC clause). */
  hint?: string;
  /** Renders the field across the full width of a `.form__grid`. */
  wide?: boolean;
  children: ReactNode;
}

/** Labelled form control with hint and error text, used by the entry forms. */
export function FormField({
  id,
  label,
  required,
  error,
  hint,
  wide,
  children,
}: FormFieldProps) {
  return (
    <div className={wide ? 'field field--wide' : 'field'}>
      <label htmlFor={id}>
        {label}
        {required && <span className="field__required"> *</span>}
      </label>
      {children}
      {hint && !error && <span className="field__hint">{hint}</span>}
      {error && (
        <span className="field__error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
