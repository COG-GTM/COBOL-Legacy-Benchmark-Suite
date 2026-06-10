import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { DataTable } from './DataTable';
import type { Column } from './DataTable';

type Row = Record<string, unknown> & { id: string; name: string; value: number };

const columns: Column<Row>[] = [
  { key: 'name', header: 'Name', sortable: true },
  { key: 'value', header: 'Value', sortable: true },
];

const data: Row[] = [
  { id: '1', name: 'Alpha', value: 10 },
  { id: '2', name: 'Bravo', value: 5 },
  { id: '3', name: 'Charlie', value: 20 },
];

describe('DataTable', () => {
  it('renders empty message when data is empty', () => {
    render(<DataTable columns={columns} data={[]} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('No data found')).toBeInTheDocument();
  });

  it('renders custom empty message', () => {
    render(
      <DataTable columns={columns} data={[]} keyExtractor={(r) => r.id} emptyMessage="Nothing here" />,
    );
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('renders column headers', () => {
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Value')).toBeInTheDocument();
  });

  it('renders data rows', () => {
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.getByText('Bravo')).toBeInTheDocument();
    expect(screen.getByText('Charlie')).toBeInTheDocument();
  });

  it('sorts ascending then descending on column clicks (uncontrolled)', async () => {
    const user = userEvent.setup();
    render(<DataTable columns={columns} data={data} keyExtractor={(r) => r.id} />);

    const nameHeader = screen.getByText('Name');
    await user.click(nameHeader);

    const rows = screen.getAllByRole('row');
    // row 0 = header, rows 1-3 = data
    expect(rows[1]).toHaveTextContent('Alpha');
    expect(rows[3]).toHaveTextContent('Charlie');

    await user.click(nameHeader);
    const rows2 = screen.getAllByRole('row');
    expect(rows2[1]).toHaveTextContent('Charlie');
    expect(rows2[3]).toHaveTextContent('Alpha');
  });

  it('calls onSortChange in controlled mode', async () => {
    const user = userEvent.setup();
    const onSortChange = vi.fn();
    render(
      <DataTable
        columns={columns}
        data={data}
        keyExtractor={(r) => r.id}
        sortKey={null}
        sortDirection={null}
        onSortChange={onSortChange}
      />,
    );
    await user.click(screen.getByText('Name'));
    expect(onSortChange).toHaveBeenCalledWith('name', 'asc');
  });

  it('renders custom cell via render prop', () => {
    const cols: Column<Row>[] = [
      { key: 'name', header: 'Name', render: (r) => <em>{r.name}!</em> },
    ];
    render(<DataTable columns={cols} data={data} keyExtractor={(r) => r.id} />);
    expect(screen.getByText('Alpha!')).toBeInTheDocument();
  });

  it('renders footer when provided', () => {
    render(
      <DataTable
        columns={columns}
        data={data}
        keyExtractor={(r) => r.id}
        footer={<tfoot><tr><td>Total</td></tr></tfoot>}
      />,
    );
    expect(screen.getByText('Total')).toBeInTheDocument();
  });
});
