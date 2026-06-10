import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { Sidebar } from './Sidebar';
import { MemoryRouter } from 'react-router-dom';

function renderSidebar(props?: { collapsed?: boolean }) {
  const onToggle = vi.fn();
  const result = render(
    <MemoryRouter>
      <Sidebar collapsed={props?.collapsed ?? false} onToggle={onToggle} />
    </MemoryRouter>,
  );
  return { ...result, onToggle };
}

describe('Sidebar', () => {
  it('renders nav items when expanded', () => {
    renderSidebar();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Position Inquiry')).toBeInTheDocument();
    expect(screen.getByText('Portfolios')).toBeInTheDocument();
    expect(screen.getByText('Transactions')).toBeInTheDocument();
    expect(screen.getByText('Reports')).toBeInTheDocument();
    expect(screen.getByText('Batch Monitor')).toBeInTheDocument();
    expect(screen.getByText('Error Log')).toBeInTheDocument();
  });

  it('renders IPM branding when expanded', () => {
    renderSidebar();
    expect(screen.getByText('IPM')).toBeInTheDocument();
  });

  it('does not render IPM branding when collapsed', () => {
    renderSidebar({ collapsed: true });
    expect(screen.queryByText('IPM')).not.toBeInTheDocument();
  });

  it('renders collapse button with correct aria-label', () => {
    renderSidebar();
    expect(screen.getByLabelText('Collapse sidebar')).toBeInTheDocument();
  });

  it('renders expand button when collapsed', () => {
    renderSidebar({ collapsed: true });
    expect(screen.getByLabelText('Expand sidebar')).toBeInTheDocument();
  });

  it('calls onToggle when toggle button is clicked', async () => {
    const user = userEvent.setup();
    const { onToggle } = renderSidebar();
    await user.click(screen.getByLabelText('Collapse sidebar'));
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('shows Reports sub-menu expanded by default', () => {
    renderSidebar();
    expect(screen.getByText('Position Report')).toBeInTheDocument();
    expect(screen.getByText('Audit Report')).toBeInTheDocument();
    expect(screen.getByText('Statistics')).toBeInTheDocument();
  });

  it('can collapse Reports sub-menu', async () => {
    const user = userEvent.setup();
    renderSidebar();
    const reportsBtn = screen.getByText('Reports');
    await user.click(reportsBtn);
    expect(screen.queryByText('Position Report')).not.toBeInTheDocument();
  });

  it('can expand Reports sub-menu after collapsing', async () => {
    const user = userEvent.setup();
    renderSidebar();
    const reportsBtn = screen.getByText('Reports');
    await user.click(reportsBtn);
    expect(screen.queryByText('Position Report')).not.toBeInTheDocument();
    await user.click(reportsBtn);
    expect(screen.getByText('Position Report')).toBeInTheDocument();
  });

  it('links to correct paths', () => {
    renderSidebar();
    const dashboardLink = screen.getByText('Dashboard').closest('a');
    expect(dashboardLink).toHaveAttribute('href', '/');
    const portfolioLink = screen.getByText('Portfolios').closest('a');
    expect(portfolioLink).toHaveAttribute('href', '/portfolios');
  });
});
