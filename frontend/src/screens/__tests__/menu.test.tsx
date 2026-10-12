import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { currentPath, renderApp } from '../../test/renderApp';

describe('S1 Main Menu', () => {
  it('FR-004: shows the title and the three options in order, with focus on the option field', async () => {
    renderApp('/');
    expect(await screen.findByRole('heading', { level: 1, name: 'Portfolio Management System' })).toBeInTheDocument();
    const options = screen.getAllByRole('listitem').map((li) => li.textContent);
    expect(options).toEqual(['1. Portfolio Position Inquiry', '2. Transaction History', '3. Exit']);
    expect(screen.getByLabelText('Select Option:')).toHaveFocus();
  });

  it('FR-005: option 1 + Enter opens Position Inquiry', async () => {
    const { user } = renderApp('/');
    await user.type(await screen.findByLabelText('Select Option:'), '1{Enter}');
    expect(await screen.findByRole('heading', { name: 'Portfolio Position Inquiry' })).toBeInTheDocument();
    expect(currentPath()).toBe('/positions');
  });

  it('FR-005: option 2 opens Transaction History', async () => {
    const { user } = renderApp('/');
    await user.type(await screen.findByLabelText('Select Option:'), '2');
    await user.click(screen.getByRole('button', { name: 'Go' }));
    expect(await screen.findByRole('heading', { name: 'Transaction History Inquiry' })).toBeInTheDocument();
  });

  it('FR-005: clicking an option link navigates (mouse users)', async () => {
    const { user } = renderApp('/');
    await user.click(await screen.findByRole('link', { name: '2. Transaction History' }));
    expect(currentPath()).toBe('/history');
  });

  it('FR-005/FR-006: an invalid option shows "Select option 1, 2 or 3", keeps the menu and refocuses the field', async () => {
    const { user } = renderApp('/');
    const field = await screen.findByLabelText('Select Option:');
    await user.type(field, '9{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Select option 1, 2 or 3');
    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(field).toHaveFocus();
    expect(currentPath()).toBe('/');
  });

  it('FR-003: option 3 ends the session and clears state', async () => {
    const { user } = renderApp('/');
    await user.type(await screen.findByLabelText('Select Option:'), '3{Enter}');
    expect(await screen.findByRole('heading', { name: 'Session ended' })).toBeInTheDocument();
    expect(screen.queryByText('PINQUSR1')).not.toBeInTheDocument();
  });

  it('FR-003: the top-bar Exit button is available from any screen', async () => {
    const { user } = renderApp('/history/1000000001');
    await user.click(await screen.findByRole('button', { name: 'Exit' }));
    await waitFor(() => expect(currentPath()).toBe('/signed-out'));
  });
});
