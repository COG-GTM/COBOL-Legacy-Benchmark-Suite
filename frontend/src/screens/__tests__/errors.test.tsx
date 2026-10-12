import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MessageRegion } from '../../components/MessageRegion';
import { TraceId } from '../../components/TraceId';
import { ERROR_CATALOGUE } from '../../api/errors';
import { SystemErrorView } from '../SystemErrorScreen';
import { currentPath, renderApp } from '../../test/renderApp';

describe('S4 System Error and messages', () => {
  it('FR-027: shows Error Code and Details and focuses Continue; Continue returns to the menu', async () => {
    const { user } = renderApp('/history/9999999999?page=1');
    const button = await screen.findByRole('button', { name: 'Continue' });
    expect(button).toHaveFocus();
    expect(screen.getByText('Error Code:')).toBeInTheDocument();
    expect(screen.getByText('Details:')).toBeInTheDocument();
    expect(screen.getByText('A system error occurred')).toBeInTheDocument();
    await user.keyboard('{Enter}');
    expect(currentPath()).toBe('/');
  });

  it('FR-027: after Continue, picking history again starts from a blank form instead of re-raising the error', async () => {
    const { user } = renderApp('/history/9999999999?page=1');
    await user.click(await screen.findByRole('button', { name: 'Continue' }));
    await user.click(screen.getByRole('link', { name: '2. Transaction History' }));
    expect(await screen.findByLabelText('Account:')).toHaveValue('');
    expect(currentPath()).toBe('/history');
  });

  it('FR-027: renders an arbitrary ApiError', () => {
    const onContinue = vi.fn();
    render(
      <SystemErrorView
        error={{ code: 'SYSTEM_ERROR', message: 'Boom', severity: 'FATAL', action: 'ABEND', traceId: 'T-1', timestamp: '' }}
        onContinue={onContinue}
      />,
    );
    expect(screen.getByTestId('error-code')).toHaveTextContent('SYSTEM_ERROR');
    expect(screen.getByText('Boom')).toBeInTheDocument();
    expect(screen.getByTestId('trace-id')).toHaveTextContent('T-1');
  });

  it('FR-028: messages use role=alert and an icon + text prefix, not colour alone', () => {
    render(<MessageRegion id="m" message={{ text: 'Error accessing position data', severity: 'ERROR' }} />);
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Error');
    expect(alert).toHaveTextContent('Error accessing position data');
  });

  it('FR-030: the trace ID can be copied', async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    render(<TraceId traceId="ABC123" />);
    await user.click(screen.getByRole('button', { name: /Copy trace ID/ }));
    expect(writeText).toHaveBeenCalledWith('ABC123');
    expect(await screen.findAllByText('Copied')).not.toHaveLength(0);
  });

  it('section 7.3: catalogue messages match the legacy texts', () => {
    expect(ERROR_CATALOGUE.INVALID_ACCOUNT_FORMAT.message).toBe('Invalid Account Number format');
    expect(ERROR_CATALOGUE.POSITION_NOT_FOUND.message).toBe('Position not found for account');
    expect(ERROR_CATALOGUE.POSITION_READ_ERROR.message).toBe('Error accessing position data');
    expect(ERROR_CATALOGUE.HISTORY_READ_ERROR.message).toBe('Error accessing transaction history');
    expect(ERROR_CATALOGUE.ACCESS_DENIED.message).toBe('Access denied');
    expect(ERROR_CATALOGUE.SYSTEM_ERROR.action).toBe('ABEND');
  });
});
