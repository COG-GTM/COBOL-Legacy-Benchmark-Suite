import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Descriptions, Button, Space, Tag, Modal, Input, Typography } from 'antd';
import { EditOutlined, DeleteOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS } from '../../types/portfolio';
import type { PortfolioStatus } from '../../types/portfolio';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { getPortfolioById, deletePortfolio } from '../../mocks/portfolioStore';
import { toast } from '../../components/Toast';

const STATUS_COLORS: Record<PortfolioStatus, string> = {
  A: 'green',
  C: 'red',
  S: 'orange',
};

export function Component() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [reasonCode, setReasonCode] = useState('');

  const portfolio = getPortfolioById(id ?? '');

  if (!portfolio) {
    return (
      <div style={{ padding: 24 }}>
        <Typography.Title level={3}>Portfolio Not Found</Typography.Title>
        <Typography.Text>The portfolio with ID &quot;{id}&quot; does not exist.</Typography.Text>
        <br />
        <Button
          type="link"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/portfolios')}
          style={{ marginTop: 16 }}
        >
          Back to List
        </Button>
      </div>
    );
  }

  const handleDelete = () => {
    if (!reasonCode.trim()) return;
    deletePortfolio(portfolio.id);
    toast.success(`Portfolio ${portfolio.id} deleted successfully`);
    setDeleteModalOpen(false);
    navigate('/portfolios');
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Portfolio: {portfolio.id}</h1>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/portfolios')}>
            Back to List
          </Button>
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={() => navigate(`/portfolios/${portfolio.id}/edit`)}
          >
            Edit
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={() => setDeleteModalOpen(true)}
          >
            Delete
          </Button>
        </Space>
      </div>

      <Descriptions bordered column={2}>
        <Descriptions.Item label="Portfolio ID">{portfolio.id}</Descriptions.Item>
        <Descriptions.Item label="Account Number">{portfolio.accountNo}</Descriptions.Item>
        <Descriptions.Item label="Client Name">{portfolio.clientName}</Descriptions.Item>
        <Descriptions.Item label="Client Type">
          {CLIENT_TYPE_LABELS[portfolio.clientType]}
        </Descriptions.Item>
        <Descriptions.Item label="Status">
          <Tag color={STATUS_COLORS[portfolio.status]}>
            {PORTFOLIO_STATUS_LABELS[portfolio.status]}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Created Date">{formatDate(portfolio.createDate)}</Descriptions.Item>
        <Descriptions.Item label="Last Maintenance">{formatDate(portfolio.lastMaint)}</Descriptions.Item>
        <Descriptions.Item label="Total Value">{formatCurrency(portfolio.totalValue)}</Descriptions.Item>
        <Descriptions.Item label="Cash Balance">{formatCurrency(portfolio.cashBalance)}</Descriptions.Item>
        <Descriptions.Item label="Last User">{portfolio.lastUser}</Descriptions.Item>
        <Descriptions.Item label="Last Transaction">{portfolio.lastTrans}</Descriptions.Item>
      </Descriptions>

      <Modal
        title="Delete Portfolio"
        open={deleteModalOpen}
        onOk={handleDelete}
        onCancel={() => {
          setDeleteModalOpen(false);
          setReasonCode('');
        }}
        okText="Confirm Delete"
        okButtonProps={{ danger: true, disabled: !reasonCode.trim() }}
      >
        <Typography.Paragraph type="warning" strong>
          This action is permanent and cannot be undone.
        </Typography.Paragraph>
        <Typography.Paragraph>
          Please provide a reason code for deleting portfolio {portfolio.id}:
        </Typography.Paragraph>
        <Input
          placeholder="Enter reason code (e.g., ACCT-CLOSED)"
          value={reasonCode}
          onChange={(e) => setReasonCode(e.target.value)}
        />
      </Modal>
    </div>
  );
}
