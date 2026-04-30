/**
 * Position Inquiry page — mirrors POSMAP screen from INQSET.bms lines 23-49
 * and inquiry logic from INQPORT.cbl
 */

import { useState } from 'react';
import { Input, Button, Table, Alert, Typography, Space, Card } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Position } from '../../types/position';
import { ValidationCode } from '../../types/validation';
import { validateAccountNumber } from '../../utils/validation';
import { formatCurrency, formatNumber } from '../../utils/formatters';
import positionsData from '../../mocks/positions.json';

const { Title, Text } = Typography;

interface PositionRow extends Position {
  key: string;
  gainLoss: number;
}

const columns: ColumnsType<PositionRow> = [
  {
    title: 'Fund ID',
    dataIndex: 'fundId',
    key: 'fundId',
  },
  {
    title: 'Fund Name',
    dataIndex: 'fundName',
    key: 'fundName',
  },
  {
    title: 'Units',
    dataIndex: 'units',
    key: 'units',
    align: 'right',
    render: (value: number) => formatNumber(value, 4),
  },
  {
    title: 'Cost Basis',
    dataIndex: 'costBasis',
    key: 'costBasis',
    align: 'right',
    render: (value: number) => formatCurrency(value),
  },
  {
    title: 'Market Value',
    dataIndex: 'marketValue',
    key: 'marketValue',
    align: 'right',
    render: (value: number) => formatCurrency(value),
  },
  {
    title: 'Gain/Loss',
    dataIndex: 'gainLoss',
    key: 'gainLoss',
    align: 'right',
    render: (value: number) => (
      <span style={{ color: value >= 0 ? '#389e0d' : '#cf1322' }}>
        {formatCurrency(value)}
      </span>
    ),
  },
];

export function Component() {
  const [accountNumber, setAccountNumber] = useState('');
  const [error, setError] = useState('');
  const [positions, setPositions] = useState<PositionRow[] | null>(null);
  const [searchedAccount, setSearchedAccount] = useState('');
  const [notFound, setNotFound] = useState(false);

  const handleSearch = () => {
    setError('');
    setNotFound(false);
    setPositions(null);

    const result = validateAccountNumber(accountNumber);
    if (result.code !== ValidationCode.SUCCESS) {
      setError(result.message);
      return;
    }

    const data = (positionsData as Record<string, Position[]>)[accountNumber];
    if (!data) {
      setSearchedAccount(accountNumber);
      setNotFound(true);
      return;
    }

    setSearchedAccount(accountNumber);
    setPositions(
      data.map((p, i) => ({
        ...p,
        key: `${p.fundId}-${i}`,
        gainLoss: p.marketValue - p.costBasis,
      })),
    );
  };

  const totals = positions
    ? positions.reduce(
        (acc, p) => ({
          costBasis: acc.costBasis + p.costBasis,
          marketValue: acc.marketValue + p.marketValue,
          gainLoss: acc.gainLoss + p.gainLoss,
        }),
        { costBasis: 0, marketValue: 0, gainLoss: 0 },
      )
    : null;

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Position Inquiry</Title>

      <Card style={{ marginBottom: 24 }}>
        <Space>
          <Input
            placeholder="Account Number (10 digits)"
            value={accountNumber}
            onChange={(e) => {
              const val = e.target.value.replace(/\D/g, '').slice(0, 10);
              setAccountNumber(val);
            }}
            onPressEnter={handleSearch}
            style={{ width: 240 }}
            maxLength={10}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            Search
          </Button>
        </Space>
        {error && (
          <Alert
            type="error"
            message={error}
            showIcon
            style={{ marginTop: 12 }}
          />
        )}
      </Card>

      {notFound && (
        <Alert
          type="warning"
          message={`Position not found for account ${searchedAccount}`}
          showIcon
          style={{ marginBottom: 24 }}
        />
      )}

      {positions && totals && (
        <>
          <Title level={5}>Account: {searchedAccount}</Title>
          <Table
            columns={columns}
            dataSource={positions}
            pagination={false}
            bordered
            summary={() => (
              <Table.Summary fixed>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={3}>
                    <Text strong>Total</Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={3} align="right">
                    <Text strong>{formatCurrency(totals.costBasis)}</Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={4} align="right">
                    <Text strong>{formatCurrency(totals.marketValue)}</Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={5} align="right">
                    <Text
                      strong
                      style={{
                        color: totals.gainLoss >= 0 ? '#389e0d' : '#cf1322',
                      }}
                    >
                      {formatCurrency(totals.gainLoss)}
                    </Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              </Table.Summary>
            )}
          />
        </>
      )}

      {!positions && !notFound && !error && (
        <Text type="secondary">
          Enter an account number to view positions
        </Text>
      )}
    </div>
  );
}
