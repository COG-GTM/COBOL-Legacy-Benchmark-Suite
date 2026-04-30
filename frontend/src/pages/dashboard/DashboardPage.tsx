import { useMemo } from 'react';
import { Card, Col, Row, Statistic, Table, Tag, Typography } from 'antd';
import {
  FolderOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { TransactionHistory, TransactionType } from '../../types/transaction';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { getPortfolios } from '../../mocks/portfolioStore';
import transactionsData from '../../mocks/transactions.json';

const { Title } = Typography;

const TRANSACTION_TYPE_COLORS: Record<TransactionType, string> = {
  BUY: 'green',
  SELL: 'red',
  TRANSFER: 'blue',
  FEE: 'orange',
};

interface RecentTransaction extends TransactionHistory {
  accountNo: string;
}

export function Component() {
  const portfolios = getPortfolios();

  const totalPortfolios = portfolios.length;
  const totalAUM = portfolios.reduce((sum, p) => sum + p.totalValue, 0);
  const activePortfolios = portfolios.filter((p) => p.status === 'A').length;

  const allTransactions = useMemo(() => {
    const txns: RecentTransaction[] = [];
    for (const [accountNo, accountTxns] of Object.entries(
      transactionsData as Record<string, TransactionHistory[]>,
    )) {
      for (const txn of accountTxns) {
        txns.push({ ...txn, accountNo });
      }
    }
    txns.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    return txns;
  }, []);

  const recentTransactions = allTransactions.slice(0, 5);
  const totalTransactions = allTransactions.length;

  const columns: ColumnsType<RecentTransaction> = [
    {
      title: 'Date',
      dataIndex: 'date',
      key: 'date',
      render: (val: string) => formatDate(val),
    },
    {
      title: 'Account',
      dataIndex: 'accountNo',
      key: 'accountNo',
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (val: TransactionType) => (
        <Tag color={TRANSACTION_TYPE_COLORS[val]}>{val}</Tag>
      ),
    },
    {
      title: 'Units',
      dataIndex: 'units',
      key: 'units',
      align: 'right',
      render: (val: number) => val.toFixed(3),
    },
    {
      title: 'Price',
      dataIndex: 'price',
      key: 'price',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      align: 'right',
      render: (val: number) => formatCurrency(val),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        Dashboard
      </Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Portfolios"
              value={totalPortfolios}
              prefix={<FolderOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total AUM"
              value={totalAUM}
              precision={2}
              prefix={<DollarOutlined />}
              formatter={(val) => formatCurrency(val as number)}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Active Portfolios"
              value={activePortfolios}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Recent Transactions"
              value={totalTransactions}
              prefix={<SwapOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Transactions" style={{ marginTop: 24 }}>
        <Table<RecentTransaction>
          columns={columns}
          dataSource={recentTransactions}
          rowKey={(record) => `${record.accountNo}-${record.date}-${record.type}`}
          pagination={false}
          size="middle"
        />
      </Card>
    </div>
  );
}
