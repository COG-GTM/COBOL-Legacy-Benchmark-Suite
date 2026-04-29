import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Input, Select, Button, Space, Tag } from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Portfolio, ClientType, PortfolioStatus } from '../../types/portfolio';
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS } from '../../types/portfolio';
import { formatCurrency } from '../../utils/formatters';
import { getPortfolios } from '../../mocks/portfolioStore';

const STATUS_COLORS: Record<PortfolioStatus, string> = {
  A: 'green',
  C: 'red',
  S: 'orange',
};

export function Component() {
  const navigate = useNavigate();
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<PortfolioStatus | 'ALL'>('ALL');
  const [clientTypeFilter, setClientTypeFilter] = useState<ClientType | 'ALL'>('ALL');

  const portfolios = getPortfolios();

  const filteredData = useMemo(() => {
    return portfolios.filter((p) => {
      const matchesSearch =
        !searchText ||
        p.clientName.toLowerCase().includes(searchText.toLowerCase()) ||
        p.accountNo.includes(searchText);
      const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
      const matchesType = clientTypeFilter === 'ALL' || p.clientType === clientTypeFilter;
      return matchesSearch && matchesStatus && matchesType;
    });
  }, [portfolios, searchText, statusFilter, clientTypeFilter]);

  const columns: ColumnsType<Portfolio> = [
    {
      title: 'Portfolio ID',
      dataIndex: 'id',
      key: 'id',
      sorter: (a, b) => a.id.localeCompare(b.id),
    },
    {
      title: 'Account No',
      dataIndex: 'accountNo',
      key: 'accountNo',
    },
    {
      title: 'Client Name',
      dataIndex: 'clientName',
      key: 'clientName',
      sorter: (a, b) => a.clientName.localeCompare(b.clientName),
    },
    {
      title: 'Client Type',
      dataIndex: 'clientType',
      key: 'clientType',
      render: (type: ClientType) => CLIENT_TYPE_LABELS[type],
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: PortfolioStatus) => (
        <Tag color={STATUS_COLORS[status]}>{PORTFOLIO_STATUS_LABELS[status]}</Tag>
      ),
    },
    {
      title: 'Total Value',
      dataIndex: 'totalValue',
      key: 'totalValue',
      render: (value: number) => formatCurrency(value),
      sorter: (a, b) => a.totalValue - b.totalValue,
      align: 'right',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h1 style={{ margin: 0 }}>Portfolios</h1>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate('/portfolios/new')}
        >
          Create Portfolio
        </Button>
      </div>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          placeholder="Search by name or account..."
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: 280 }}
          allowClear
        />
        <Select
          value={statusFilter}
          onChange={setStatusFilter}
          style={{ width: 150 }}
        >
          <Select.Option value="ALL">All Statuses</Select.Option>
          <Select.Option value="A">Active</Select.Option>
          <Select.Option value="C">Closed</Select.Option>
          <Select.Option value="S">Suspended</Select.Option>
        </Select>
        <Select
          value={clientTypeFilter}
          onChange={setClientTypeFilter}
          style={{ width: 150 }}
        >
          <Select.Option value="ALL">All Types</Select.Option>
          <Select.Option value="I">Individual</Select.Option>
          <Select.Option value="C">Corporate</Select.Option>
          <Select.Option value="T">Trust</Select.Option>
        </Select>
      </Space>

      <Table<Portfolio>
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        pagination={{ pageSize: 5 }}
        onRow={(record) => ({
          onClick: () => navigate(`/portfolios/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
