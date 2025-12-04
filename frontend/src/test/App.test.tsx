import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '../App';

describe('App', () => {
  it('renders the Portfolio Management System header', () => {
    render(<App />);
    expect(screen.getByText('Portfolio Management System')).toBeInTheDocument();
  });

  it('renders the main menu by default', () => {
    render(<App />);
    expect(screen.getByText('Select Option:')).toBeInTheDocument();
  });

  it('renders navigation tabs', () => {
    render(<App />);
    expect(screen.getByText('Main Menu')).toBeInTheDocument();
    expect(screen.getByText('Portfolio Inquiry')).toBeInTheDocument();
    expect(screen.getByText('Transaction History')).toBeInTheDocument();
    expect(screen.getByText('Exit')).toBeInTheDocument();
  });
});
