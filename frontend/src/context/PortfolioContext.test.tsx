import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { PortfolioProvider, usePortfolios } from './PortfolioContext';

function TestConsumer() {
  const { portfolios, addPortfolio, updatePortfolio, deletePortfolio, getPortfolio } = usePortfolios();
  return (
    <div>
      <span data-testid="count">{portfolios.length}</span>
      <span data-testid="first">{portfolios[0]?.name ?? 'none'}</span>
      <span data-testid="lookup">{getPortfolio('PORT0001')?.name ?? 'not found'}</span>
      <button
        onClick={() =>
          addPortfolio({ id: 'PORT9999', name: 'Test Fund', createDate: '2024-01-01', status: 'A', totalValue: 100 })
        }
      >
        add
      </button>
      <button onClick={() => updatePortfolio('PORT0001', { name: 'Updated' })}>update</button>
      <button onClick={() => deletePortfolio('PORT0001')}>delete</button>
    </div>
  );
}

describe('PortfolioContext', () => {
  it('throws when usePortfolios is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('usePortfolios must be used within a PortfolioProvider');
    spy.mockRestore();
  });

  it('provides initial portfolios from mock data', () => {
    render(<PortfolioProvider><TestConsumer /></PortfolioProvider>);
    expect(Number(screen.getByTestId('count').textContent)).toBeGreaterThan(0);
    expect(screen.getByTestId('first')).toHaveTextContent('Growth Equity Fund');
  });

  it('getPortfolio finds a portfolio by id', () => {
    render(<PortfolioProvider><TestConsumer /></PortfolioProvider>);
    expect(screen.getByTestId('lookup')).toHaveTextContent('Growth Equity Fund');
  });

  it('addPortfolio adds a new portfolio', async () => {
    const user = userEvent.setup();
    render(<PortfolioProvider><TestConsumer /></PortfolioProvider>);
    const initialCount = Number(screen.getByTestId('count').textContent);
    await user.click(screen.getByText('add'));
    expect(Number(screen.getByTestId('count').textContent)).toBe(initialCount + 1);
  });

  it('updatePortfolio modifies a portfolio', async () => {
    const user = userEvent.setup();
    render(<PortfolioProvider><TestConsumer /></PortfolioProvider>);
    await user.click(screen.getByText('update'));
    expect(screen.getByTestId('first')).toHaveTextContent('Updated');
  });

  it('deletePortfolio removes a portfolio', async () => {
    const user = userEvent.setup();
    render(<PortfolioProvider><TestConsumer /></PortfolioProvider>);
    const initialCount = Number(screen.getByTestId('count').textContent);
    await user.click(screen.getByText('delete'));
    expect(Number(screen.getByTestId('count').textContent)).toBe(initialCount - 1);
  });
});
