import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Input, Select, InputNumber, Button, Space, Radio, Typography } from 'antd';
import type { PortfolioStatus } from '../../types/portfolio';
import { PORTFOLIO_STATUS_LABELS } from '../../types/portfolio';
import { getPortfolioById, updatePortfolio } from '../../mocks/portfolioStore';
import { toast } from '../../components/Toast';
import { useAuth } from '../../contexts/AuthContext';

type ActionType = 'S' | 'V' | 'N';

export function Component() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form] = Form.useForm();
  const [actionType, setActionType] = useState<ActionType>('S');

  const portfolio = getPortfolioById(id ?? '');

  useEffect(() => {
    if (portfolio) {
      form.setFieldsValue({
        status: portfolio.status,
        totalValue: portfolio.totalValue,
        cashBalance: portfolio.cashBalance,
        clientName: portfolio.clientName,
      });
    }
  }, [portfolio, form]);

  if (!portfolio) {
    return (
      <div style={{ padding: 24 }}>
        <Typography.Title level={3}>Portfolio Not Found</Typography.Title>
        <Typography.Text>The portfolio with ID &quot;{id}&quot; does not exist.</Typography.Text>
        <br />
        <Button type="link" onClick={() => navigate('/portfolios')} style={{ marginTop: 16 }}>
          Back to List
        </Button>
      </div>
    );
  }

  const handleSubmit = (values: Record<string, unknown>) => {
    const today = new Date().toISOString().split('T')[0];
    const updates: Record<string, unknown> = {
      lastMaint: today,
      lastUser: user?.username ?? 'SYSTEM',
    };

    if (actionType === 'S') {
      updates.status = values.status;
    } else if (actionType === 'V') {
      updates.totalValue = values.totalValue;
      updates.cashBalance = values.cashBalance;
    } else if (actionType === 'N') {
      updates.clientName = values.clientName;
    }

    updatePortfolio(portfolio.id, updates);
    toast.success(`Portfolio ${portfolio.id} updated successfully`);
    navigate(`/portfolios/${portfolio.id}`);
  };

  return (
    <div style={{ padding: 24, maxWidth: 600 }}>
      <h1>Edit Portfolio: {portfolio.id}</h1>

      <div style={{ marginBottom: 24 }}>
        <Typography.Text strong>Update Type:</Typography.Text>
        <br />
        <Radio.Group
          value={actionType}
          onChange={(e) => setActionType(e.target.value)}
          style={{ marginTop: 8 }}
        >
          <Radio.Button value="S">Status Change</Radio.Button>
          <Radio.Button value="V">Value Update</Radio.Button>
          <Radio.Button value="N">Name Change</Radio.Button>
        </Radio.Group>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
      >
        <Form.Item label="Portfolio ID">
          <Input value={portfolio.id} disabled />
        </Form.Item>

        <Form.Item label="Account Number">
          <Input value={portfolio.accountNo} disabled />
        </Form.Item>

        <Form.Item
          name="clientName"
          label="Client Name"
          rules={actionType === 'N' ? [{ required: true, message: 'Client name is required' }] : []}
        >
          <Input disabled={actionType !== 'N'} />
        </Form.Item>

        <Form.Item
          name="status"
          label="Status"
          rules={actionType === 'S' ? [{ required: true, message: 'Status is required' }] : []}
        >
          <Select disabled={actionType !== 'S'}>
            {(Object.entries(PORTFOLIO_STATUS_LABELS) as [PortfolioStatus, string][]).map(
              ([value, label]) => (
                <Select.Option key={value} value={value}>
                  {label}
                </Select.Option>
              ),
            )}
          </Select>
        </Form.Item>

        <Form.Item
          name="totalValue"
          label="Total Value"
        >
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            precision={2}
            prefix="$"
            disabled={actionType !== 'V'}
          />
        </Form.Item>

        <Form.Item
          name="cashBalance"
          label="Cash Balance"
        >
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            precision={2}
            prefix="$"
            disabled={actionType !== 'V'}
          />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              Save Changes
            </Button>
            <Button onClick={() => navigate(`/portfolios/${portfolio.id}`)}>
              Cancel
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
}
