import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PageHeader } from './PageHeader';

describe('PageHeader', () => {
  it('renders the title', () => {
    render(<PageHeader title="Dashboard" />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(<PageHeader title="T" description="Some info" />);
    expect(screen.getByText('Some info')).toBeInTheDocument();
  });

  it('does not render description when not provided', () => {
    const { container } = render(<PageHeader title="T" />);
    expect(container.querySelectorAll('p')).toHaveLength(0);
  });

  it('renders actions when provided', () => {
    render(<PageHeader title="T" actions={<button>Do</button>} />);
    expect(screen.getByRole('button', { name: 'Do' })).toBeInTheDocument();
  });
});
