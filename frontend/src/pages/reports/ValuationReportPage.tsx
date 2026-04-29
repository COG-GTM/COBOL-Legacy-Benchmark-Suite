import { useMemo } from 'react';
import { Table, Typography } from 'antd';
import type { TableProps } from 'antd';
import { formatCurrency } from '../../utils/formatters';
import valuationData from '../../mocks/valuationReport.json';

const { Title, Text } = Typography;

interface ValuationRecord {
  portfolioId: string;
  currentValue: number;
  previousValue: number;
}

interface ValuationRow extends ValuationRecord {
  changeAmount: number;
  changePercent: number;
}

export function Component() {
  const data = useMemo<ValuationRow[]>(() => {
    return (valuationData as ValuationRecord[])
      .map((r) => ({
        ...r,
        changeAmount: r.currentValue - r.previousValue,
        changePercent: ((r.currentValue - r.previousValue) / r.previousValue) * 100,
      }))
      .sort((a, b) => b.changePercent - a.changePercent);
  }, []);

  const totals = useMemo(() => {
    const totalCurrent = data.reduce((sum, r) => sum + r.currentValue, 0);
    const totalPrevious = data.reduce((sum, r) => sum + r.previousValue, 0);
    const totalChange = totalCurrent - totalPrevious;
    const totalPercent = totalPrevious !== 0 ? (totalChange / totalPrevious) * 100 : 0;
    return { totalCurrent, totalPrevious, totalChange, totalPercent };
  }, [data]);

  const columns: TableProps<ValuationRow>['columns'] = [
    {
      title: 'Portfolio ID',
      dataIndex: 'portfolioId',
      key: 'portfolioId',
    },
    {
      title: 'Current Value',
      dataIndex: 'currentValue',
      key: 'currentValue',
      align: 'right',
      render: (v: number) => formatCurrency(v),
    },
    {
      title: 'Previous Value',
      dataIndex: 'previousValue',
      key: 'previousValue',
      align: 'right',
      render: (v: number) => formatCurrency(v),
    },
    {
      title: 'Change Amount',
      dataIndex: 'changeAmount',
      key: 'changeAmount',
      align: 'right',
      render: (v: number) => (
        <Text type={v >= 0 ? 'success' : 'danger'}>{formatCurrency(v)}</Text>
      ),
    },
    {
      title: '% Change',
      dataIndex: 'changePercent',
      key: 'changePercent',
      align: 'right',
      defaultSortOrder: 'descend',
      sorter: (a: ValuationRow, b: ValuationRow) => a.changePercent - b.changePercent,
      render: (v: number) => (
        <Text type={v >= 0 ? 'success' : 'danger'}>{v.toFixed(2)}%</Text>
      ),
    },
  ];

  return (
    <div>
      <Title level={2}>Portfolio Valuation Report</Title>
      <Table<ValuationRow>
        columns={columns}
        dataSource={data}
        rowKey="portfolioId"
        pagination={false}
        summary={() => (
          <Table.Summary fixed>
            <Table.Summary.Row>
              <Table.Summary.Cell index={0}>
                <Text strong>Totals</Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={1} align="right">
                <Text strong>{formatCurrency(totals.totalCurrent)}</Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={2} align="right">
                <Text strong>{formatCurrency(totals.totalPrevious)}</Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={3} align="right">
                <Text strong type={totals.totalChange >= 0 ? 'success' : 'danger'}>
                  {formatCurrency(totals.totalChange)}
                </Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={4} align="right">
                <Text strong type={totals.totalPercent >= 0 ? 'success' : 'danger'}>
                  {totals.totalPercent.toFixed(2)}%
                </Text>
              </Table.Summary.Cell>
            </Table.Summary.Row>
          </Table.Summary>
        )}
      />
    </div>
  );
}
