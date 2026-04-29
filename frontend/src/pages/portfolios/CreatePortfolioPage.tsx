import { useNavigate } from 'react-router-dom';
import { Form, Input, Select, InputNumber, Button, Space } from 'antd';
import type { ClientType, PortfolioStatus } from '../../types/portfolio';
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS } from '../../types/portfolio';
import { validateAccountNumber } from '../../utils/validation';
import { ValidationCode } from '../../types/validation';
import { toast } from '../../components/Toast';
import { addPortfolio, getNextPortfolioId } from '../../mocks/portfolioStore';
import { useAuth } from '../../contexts/AuthContext';

interface FormValues {
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
}

export function Component() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form] = Form.useForm<FormValues>();
  const nextId = getNextPortfolioId();

  const handleSubmit = (values: FormValues) => {
    const today = new Date().toISOString().split('T')[0];
    addPortfolio({
      id: nextId,
      accountNo: values.accountNo,
      clientName: values.clientName,
      clientType: values.clientType,
      status: values.status,
      totalValue: values.totalValue ?? 0,
      cashBalance: values.cashBalance ?? 0,
      createDate: today,
      lastMaint: today,
      lastUser: user?.username ?? 'SYSTEM',
      lastTrans: today.replace(/-/g, ''),
    });
    toast.success(`Portfolio ${nextId} created successfully`);
    navigate(`/portfolios/${nextId}`);
  };

  return (
    <div style={{ padding: 24, maxWidth: 600 }}>
      <h1>Create Portfolio</h1>
      <Form<FormValues>
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ status: 'A' as PortfolioStatus, totalValue: 0, cashBalance: 0 }}
      >
        <Form.Item label="Portfolio ID">
          <Input value={nextId} disabled />
        </Form.Item>

        <Form.Item
          name="accountNo"
          label="Account Number"
          rules={[
            { required: true, message: 'Account number is required' },
            {
              validator: (_, value) => {
                if (!value) return Promise.resolve();
                const result = validateAccountNumber(value);
                if (result.code !== ValidationCode.SUCCESS) {
                  return Promise.reject(new Error(result.message));
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <Input maxLength={10} placeholder="10-digit account number" />
        </Form.Item>

        <Form.Item
          name="clientName"
          label="Client Name"
          rules={[{ required: true, message: 'Client name is required' }]}
        >
          <Input placeholder="Enter client name" />
        </Form.Item>

        <Form.Item
          name="clientType"
          label="Client Type"
          rules={[{ required: true, message: 'Client type is required' }]}
        >
          <Select placeholder="Select client type">
            {(Object.entries(CLIENT_TYPE_LABELS) as [ClientType, string][]).map(
              ([value, label]) => (
                <Select.Option key={value} value={value}>
                  {label}
                </Select.Option>
              ),
            )}
          </Select>
        </Form.Item>

        <Form.Item
          name="status"
          label="Status"
          rules={[{ required: true, message: 'Status is required' }]}
        >
          <Select>
            {(Object.entries(PORTFOLIO_STATUS_LABELS) as [PortfolioStatus, string][]).map(
              ([value, label]) => (
                <Select.Option key={value} value={value}>
                  {label}
                </Select.Option>
              ),
            )}
          </Select>
        </Form.Item>

        <Form.Item name="totalValue" label="Total Value">
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            precision={2}
            prefix="$"
          />
        </Form.Item>

        <Form.Item name="cashBalance" label="Cash Balance">
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            precision={2}
            prefix="$"
          />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              Create Portfolio
            </Button>
            <Button onClick={() => navigate('/portfolios')}>Cancel</Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
}
