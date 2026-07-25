import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ErrorPanel from '../src/components/ErrorPanel';

describe('ErrorPanel (ERRMAP)', () => {
  it('renders the error code and details', () => {
    render(<ErrorPanel code="INQ001" details="Error accessing position data" />);
    expect(screen.getByText('System Error')).toBeInTheDocument();
    expect(screen.getByText('Error Code:')).toBeInTheDocument();
    expect(screen.getByText('INQ001')).toBeInTheDocument();
    expect(screen.getByText('Details:')).toBeInTheDocument();
    expect(screen.getByText('Error accessing position data')).toBeInTheDocument();
  });
});
