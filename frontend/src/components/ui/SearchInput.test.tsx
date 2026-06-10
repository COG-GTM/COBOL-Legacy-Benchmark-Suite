import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { SearchInput } from './SearchInput';

describe('SearchInput', () => {
  it('renders with placeholder', () => {
    render(<SearchInput value="" onChange={vi.fn()} placeholder="Find..." />);
    expect(screen.getByPlaceholderText('Find...')).toBeInTheDocument();
  });

  it('renders default placeholder when none provided', () => {
    render(<SearchInput value="" onChange={vi.fn()} />);
    expect(screen.getByPlaceholderText('Search...')).toBeInTheDocument();
  });

  it('displays the controlled value', () => {
    render(<SearchInput value="test" onChange={vi.fn()} />);
    expect(screen.getByDisplayValue('test')).toBeInTheDocument();
  });

  it('calls onChange when user types', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    render(<SearchInput value="" onChange={handleChange} />);
    await user.type(screen.getByRole('textbox'), 'a');
    expect(handleChange).toHaveBeenCalledWith('a');
  });
});
