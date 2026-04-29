import { useMemo, useState } from 'react';
import { Button, DatePicker, Row, Col, Select, Space, Table, Tag, Typography, Card } from 'antd';
import type { TableProps } from 'antd';
import type { AuditAction, AuditRecord, AuditStatus } from '../../types/audit';
import { formatDateTime } from '../../utils/formatters';
import auditData from '../../mocks/auditRecords.json';

const { Title, Text } = Typography;

const ACTION_OPTIONS: AuditAction[] = ['CREATE', 'UPDATE', 'DELETE', 'INQUIRE', 'LOGIN', 'LOGOUT'];

const ACTION_COLORS: Record<AuditAction, string> = {
  CREATE: 'green',
  UPDATE: 'blue',
  DELETE: 'red',
  INQUIRE: 'purple',
  LOGIN: 'cyan',
  LOGOUT: 'orange',
};

const STATUS_COLORS: Record<AuditStatus, string> = {
  SUCC: 'green',
  FAIL: 'red',
  WARN: 'orange',
};

const STATUS_LABELS: Record<AuditStatus, string> = {
  SUCC: 'SUCCESS',
  FAIL: 'FAILURE',
  WARN: 'WARNING',
};

interface Filters {
  actions: AuditAction[];
  startDate: string;
  endDate: string;
  status: string;
}

const emptyFilters: Filters = {
  actions: [],
  startDate: '',
  endDate: '',
  status: '',
};

export function Component() {
  const [working, setWorking] = useState<Filters>({ ...emptyFilters });
  const [applied, setApplied] = useState<Filters>({ ...emptyFilters });

  const records = auditData as AuditRecord[];

  const filtered = useMemo(() => {
    return records.filter((r) => {
      if (applied.actions.length > 0 && !applied.actions.includes(r.action)) return false;
      if (applied.status && r.status !== applied.status) return false;
      if (applied.startDate) {
        const recordDate = r.timestamp.slice(0, 10);
        if (recordDate < applied.startDate) return false;
      }
      if (applied.endDate) {
        const recordDate = r.timestamp.slice(0, 10);
        if (recordDate > applied.endDate) return false;
      }
      return true;
    });
  }, [records, applied]);

  const summary = useMemo(() => {
    const byAction: Partial<Record<AuditAction, number>> = {};
    let succCount = 0;
    let failCount = 0;
    let warnCount = 0;
    for (const r of filtered) {
      byAction[r.action] = (byAction[r.action] ?? 0) + 1;
      if (r.status === 'SUCC') succCount++;
      else if (r.status === 'FAIL') failCount++;
      else warnCount++;
    }
    return { total: filtered.length, byAction, succCount, failCount, warnCount };
  }, [filtered]);

  const handleApply = () => setApplied({ ...working });
  const handleReset = () => {
    setWorking({ ...emptyFilters });
    setApplied({ ...emptyFilters });
  };

  const columns: TableProps<AuditRecord>['columns'] = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (v: string) => formatDateTime(v),
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      render: (v: AuditAction) => <Tag color={ACTION_COLORS[v]}>{v}</Tag>,
    },
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (v: AuditStatus) => (
        <Tag color={STATUS_COLORS[v]}>{STATUS_LABELS[v]}</Tag>
      ),
    },
  ];

  return (
    <div>
      <Title level={2}>Audit Trail Report</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12} md={6}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>Action Type</Text>
            <Select
              mode="multiple"
              placeholder="All actions"
              style={{ width: '100%' }}
              value={working.actions}
              onChange={(val: AuditAction[]) => setWorking((f) => ({ ...f, actions: val }))}
              options={ACTION_OPTIONS.map((a) => ({ label: a, value: a }))}
              allowClear
            />
          </Col>
          <Col xs={24} sm={12} md={5}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>Start Date</Text>
            <DatePicker
              style={{ width: '100%' }}
              onChange={(_d, ds) => {
                const val = typeof ds === 'string' ? ds : '';
                setWorking((f) => ({ ...f, startDate: val }));
              }}
              value={undefined}
            />
          </Col>
          <Col xs={24} sm={12} md={5}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>End Date</Text>
            <DatePicker
              style={{ width: '100%' }}
              onChange={(_d, ds) => {
                const val = typeof ds === 'string' ? ds : '';
                setWorking((f) => ({ ...f, endDate: val }));
              }}
              value={undefined}
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>Status</Text>
            <Select
              placeholder="All"
              style={{ width: '100%' }}
              value={working.status || undefined}
              onChange={(val: string) => setWorking((f) => ({ ...f, status: val ?? '' }))}
              allowClear
              options={[
                { label: 'SUCCESS', value: 'SUCC' },
                { label: 'FAILURE', value: 'FAIL' },
                { label: 'WARNING', value: 'WARN' },
              ]}
            />
          </Col>
          <Col xs={24} sm={24} md={4}>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>&nbsp;</Text>
            <Space>
              <Button type="primary" onClick={handleApply}>Apply Filters</Button>
              <Button onClick={handleReset}>Reset</Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Table<AuditRecord>
        columns={columns}
        dataSource={filtered}
        rowKey={(_r, i) => String(i)}
        pagination={{ pageSize: 10 }}
      />

      <Card title="Summary" style={{ marginTop: 16 }}>
        <Row gutter={[24, 16]}>
          <Col xs={24} sm={8}>
            <Text strong>Total Records: </Text>
            <Text>{summary.total}</Text>
          </Col>
          <Col xs={24} sm={8}>
            <Text strong>Success / Failure / Warning: </Text>
            <Text>
              <Text type="success">{summary.succCount}</Text>
              {' / '}
              <Text type="danger">{summary.failCount}</Text>
              {' / '}
              <Text type="warning">{summary.warnCount}</Text>
            </Text>
          </Col>
        </Row>
        <Row gutter={[16, 8]} style={{ marginTop: 12 }}>
          {ACTION_OPTIONS.map((action) => (
            <Col key={action} xs={12} sm={8} md={4}>
              <Tag color={ACTION_COLORS[action]}>{action}</Tag>
              <Text>{summary.byAction[action] ?? 0}</Text>
            </Col>
          ))}
        </Row>
      </Card>
    </div>
  );
}
