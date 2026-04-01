import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import InlineError from '../InlineError';

describe('InlineError', () => {
  it('renders nothing when visible is false', () => {
    const { container } = render(
      <InlineError message="Some error" severity="error" visible={false} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders the message when visible is true', () => {
    render(<InlineError message="Field is required" severity="error" visible={true} />);
    expect(screen.getByText('Field is required')).toBeInTheDocument();
  });

  it('has role="alert" for accessibility', () => {
    render(<InlineError message="Error" severity="error" visible={true} />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('applies red color for error severity', () => {
    render(<InlineError message="Error" severity="error" visible={true} />);
    const el = screen.getByTestId('inline-error');
    expect(el.style.color).toBe('rgb(211, 47, 47)');
  });

  it('applies orange color for warning severity', () => {
    render(<InlineError message="Warning" severity="warning" visible={true} />);
    const el = screen.getByTestId('inline-error');
    expect(el.style.color).toBe('rgb(237, 108, 2)');
  });

  it('applies blue color for info severity', () => {
    render(<InlineError message="Info" severity="info" visible={true} />);
    const el = screen.getByTestId('inline-error');
    expect(el.style.color).toBe('rgb(2, 136, 209)');
  });
});
