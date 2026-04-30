/**
 * Transaction History page — mirrors HISMAP screen from INQSET.bms lines 53-85
 * and inquiry logic from INQHIST.cbl
 */

import { useState, useMemo } from 'react';
import { Input, Button, Table, Alert, Typography, Space, Card, Tag, DatePicker } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import type { TransactionHistory, TransactionType } from '../../types/transaction';
import { TRANSACTION_TYPE_LABELS } from '../../types/transaction';
import { ValidationCode } from '../../types/validation';
import { validateAccountNumber } from '../../utils/validation';
import { formatCurrency, formatNumber, formatDate } from '../../utils/formatters';
import transactionsData from '../../mocks/transactions.json';

const { Title, Text } = Typography;

const TYPE_COLORS: Record<TransactionType, string> = {
  BUY: 'green',
  SELL: 'red',
  TRANSFER: 'blue',
  FEE: 'orange',
};

interface TransactionRow extends TransactionHistory {
  key: string;
}

const columns: ColumnsType<TransactionRow> = [
  {
    title: 'Date',
    dataIndex: 'date',
    key: 'date',
    render: (value: string) => formatDate(value),
  },
  {
    title: 'Type',
    dataIndex: 'type',
    key: 'type',
    render: (value: TransactionType) => (
      <Tag color={TYPE_COLORS[value]}>{TRANSACTION_TYPE_LABELS[value]}</Tag>
    ),
  },
  {
    title: 'Units',
    dataIndex: 'units',
    key: 'units',
    align: 'right',
    render: (value: number) => formatNumber(value, 4),
  },
  {
    title: 'Price',
    dataIndex: 'price',
    key: 'price',
    align: 'right',
    render: (value: number) => formatCurrency(value),
  },
  {
    title: 'Amount',
    dataIndex: 'amount',
    key: 'amount',
    align: 'right',
    render: (value: number) => formatCurrency(value),
  },
];

export function Component() {
  const [accountNumber, setAccountNumber] = useState('');
  const [startDate, setStartDate] = useState<Dayjs | null>(null);
  const [endDate, setEndDate] = useState<Dayjs | null>(null);
  const [error, setError] = useState('');
  const [transactions, setTransactions] = useState<TransactionRow[] | null>(null);
  const [searchedAccount, setSearchedAccount] = useState('');
  const [notFound, setNotFound] = useState(false);

  const handleSearch = () => {
    setError('');
    setNotFound(false);
    setTransactions(null);

    const result = validateAccountNumber(accountNumber);
    if (result.code !== ValidationCode.SUCCESS) {
      setError(result.message);
      return;
    }

    const data = (transactionsData as Record<string, TransactionHistory[]>)[accountNumber];
    if (!data) {
      setSearchedAccount(accountNumber);
      setNotFound(true);
      return;
    }

    setSearchedAccount(accountNumber);

    let filtered = data;
    if (startDate) {
      const start = startDate.format('YYYY-MM-DD');
      filtered = filtered.filter((t) => t.date >= start);
    }
    if (endDate) {
      const end = endDate.format('YYYY-MM-DD');
      filtered = filtered.filter((t) => t.date <= end);
    }

    setTransactions(
      filtered.map((t, i) => ({
        ...t,
        key: `${t.date}-${t.type}-${i}`,
      })),
    );
  };

  const summary = useMemo(() => {
    if (!transactions) return null;
    return {
      count: transactions.length,
      totalAmount: transactions.reduce((sum, t) => sum + t.amount, 0),
    };
  }, [transactions]);

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Transaction History</Title>

      <Card style={{ marginBottom: 24 }}>
        <Space wrap>
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
          <DatePicker
            placeholder="Start Date"
            value={startDate}
            onChange={setStartDate}
          />
          <DatePicker
            placeholder="End Date"
            value={endDate}
            onChange={setEndDate}
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
          message={`No transaction history found for account ${searchedAccount}`}
          showIcon
          style={{ marginBottom: 24 }}
        />
      )}

      {transactions && summary && (
        <>
          <Title level={5}>Account: {searchedAccount}</Title>
          <Table
            columns={columns}
            dataSource={transactions}
            pagination={{ pageSize: 10 }}
            bordered
          />
          <Card size="small" style={{ marginTop: 16 }}>
            <Space size="large">
              <Text>
                <Text strong>Total Transactions:</Text> {summary.count}
              </Text>
              <Text>
                <Text strong>Total Amount:</Text>{' '}
                {formatCurrency(summary.totalAmount)}
              </Text>
            </Space>
          </Card>
        </>
      )}

      {!transactions && !notFound && !error && (
        <Text type="secondary">
          Enter an account number to view transaction history
        </Text>
      )}
    </div>
  );
}
