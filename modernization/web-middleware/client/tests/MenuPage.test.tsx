import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MenuPage from '../src/pages/MenuPage';

const menuPayload = {
  commarea: { inqcomFunction: 'MENU', inqcomAccountNo: '          ', inqcomResponseCode: 0, inqcomErrorMsg: ' '.repeat(80) },
  options: [
    { option: '1', label: 'Portfolio Position Inquiry', function: 'INQP', route: '/position' },
    { option: '2', label: 'Transaction History', function: 'INQH', route: '/history' },
    { option: '3', label: 'Exit', function: 'EXIT', route: '/exit' },
  ],
};

describe('MenuPage (MENMAP)', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => menuPayload }));
  });

  it('renders the three legacy menu options', async () => {
    render(
      <MemoryRouter>
        <MenuPage />
      </MemoryRouter>
    );
    expect(await screen.findByText('Portfolio Position Inquiry')).toBeInTheDocument();
    expect(screen.getByText('Transaction History')).toBeInTheDocument();
    expect(screen.getByText('Exit')).toBeInTheDocument();
    expect(screen.getByText('Select Option:')).toBeInTheDocument();
  });
});
