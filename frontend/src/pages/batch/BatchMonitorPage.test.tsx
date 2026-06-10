import { screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BatchMonitorPage } from './BatchMonitorPage';
import { renderWithRouter } from '@/test/helpers';

describe('BatchMonitorPage', () => {
  it('renders the batch monitor page', () => {
    renderWithRouter(<BatchMonitorPage />);
    expect(screen.getAllByText('Batch Monitor').length).toBeGreaterThanOrEqual(1);
  });

  it('shows coming soon message', () => {
    renderWithRouter(<BatchMonitorPage />);
    expect(screen.getByText(/This page is coming soon/)).toBeInTheDocument();
  });
});
