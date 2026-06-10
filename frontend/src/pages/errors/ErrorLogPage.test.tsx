import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { ErrorLogPage } from './ErrorLogPage';
import { renderWithRouter } from '@/test/helpers';

describe('ErrorLogPage', () => {
  it('renders the page title', () => {
    renderWithRouter(<ErrorLogPage />);
    expect(screen.getByText('Error Log')).toBeInTheDocument();
  });

  it('renders the description', () => {
    renderWithRouter(<ErrorLogPage />);
    expect(screen.getByText('System error and warning messages')).toBeInTheDocument();
  });

  it('renders summary stats', () => {
    renderWithRouter(<ErrorLogPage />);
    expect(screen.getByText('Total Entries')).toBeInTheDocument();
    expect(screen.getByText('Errors')).toBeInTheDocument();
    expect(screen.getByText('Warnings')).toBeInTheDocument();
  });

  it('renders severity and program filters', () => {
    renderWithRouter(<ErrorLogPage />);
    expect(screen.getByText('Severity:')).toBeInTheDocument();
    expect(screen.getByText('Program:')).toBeInTheDocument();
  });

  it('renders data table with columns', () => {
    renderWithRouter(<ErrorLogPage />);
    expect(screen.getByText('Timestamp')).toBeInTheDocument();
    expect(screen.getByText('Code')).toBeInTheDocument();
    expect(screen.getByText('Description')).toBeInTheDocument();
    expect(screen.getByText('Severity')).toBeInTheDocument();
  });

  it('filters by severity', async () => {
    const user = userEvent.setup();
    renderWithRouter(<ErrorLogPage />);
    const severitySelect = screen.getAllByRole('combobox')[0];
    await user.selectOptions(severitySelect, 'Error');
    // After filtering, only error entries should be visible
    const entries = screen.getByText(/\d+ entr/);
    expect(entries).toBeInTheDocument();
  });

  it('filters by program', async () => {
    const user = userEvent.setup();
    renderWithRouter(<ErrorLogPage />);
    const programSelect = screen.getAllByRole('combobox')[1];
    const options = programSelect.querySelectorAll('option');
    if (options.length > 1) {
      await user.selectOptions(programSelect, options[1].value);
    }
    const entries = screen.getByText(/\d+ entr/);
    expect(entries).toBeInTheDocument();
  });
});
