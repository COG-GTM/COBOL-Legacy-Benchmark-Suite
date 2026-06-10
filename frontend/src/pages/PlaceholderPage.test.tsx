import { screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PlaceholderPage } from './PlaceholderPage';
import { renderWithRouter } from '@/test/helpers';

describe('PlaceholderPage', () => {
  it('renders the title', () => {
    renderWithRouter(<PlaceholderPage title="Batch Monitor" />);
    expect(screen.getAllByText('Batch Monitor').length).toBeGreaterThanOrEqual(1);
  });

  it('renders the description when provided', () => {
    renderWithRouter(<PlaceholderPage title="Test" description="Test description" />);
    expect(screen.getByText('Test description')).toBeInTheDocument();
  });

  it('renders coming soon message', () => {
    renderWithRouter(<PlaceholderPage title="Test" />);
    expect(screen.getByText(/This page is coming soon/)).toBeInTheDocument();
  });
});
