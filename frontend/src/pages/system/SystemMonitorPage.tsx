import { useMemo, useState } from 'react';
import { Card, Col, Row, Table, Tag, Typography, Progress, Select, Statistic } from 'antd';
import {
  AlertOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import monitorData from '../../mocks/systemMonitor.json';
import { formatDateTime } from '../../utils/formatters';

const { Title, Text } = Typography;

interface ResourceMetric {
  type: string;
  metricName: string;
  currentValue: number;
  threshold: number;
  status: string;
  unit: string;
}

interface Alert {
  timestamp: string;
  level: string;
  resource: string;
  message: string;
}

interface Threshold {
  resource: string;
  metric: string;
  warnAt: number;
  criticalAt: number;
  unit: string;
}

const resources = monitorData.resources as ResourceMetric[];
const alerts = monitorData.alerts as Alert[];
const thresholds = monitorData.thresholds as Threshold[];

const ALERT_LEVEL_MAP: Record<string, { color: string; icon: React.ReactNode }> = {
  INFO: { color: 'blue', icon: <InfoCircleOutlined /> },
  WARNING: { color: 'orange', icon: <WarningOutlined /> },
  CRITICAL: { color: 'red', icon: <AlertOutlined /> },
};

function getGaugeColor(value: number, threshold: number, isInverse = false): string {
  const ratio = isInverse ? threshold / value : value / threshold;
  if (ratio >= 0.95) return '#ff4d4f';
  if (ratio >= 0.8) return '#faad14';
  return '#52c41a';
}

export function Component() {
  const [alertLevelFilter, setAlertLevelFilter] = useState<string | null>(null);

  const filteredAlerts = useMemo(() => {
    if (!alertLevelFilter) return alerts;
    return alerts.filter((a) => a.level === alertLevelFilter);
  }, [alertLevelFilter]);

  const resourcesByType = useMemo(() => {
    const grouped: Record<string, ResourceMetric[]> = {};
    for (const r of resources) {
      if (!grouped[r.type]) grouped[r.type] = [];
      grouped[r.type].push(r);
    }
    return grouped;
  }, []);

  const alertSummary = useMemo(() => ({
    total: alerts.length,
    critical: alerts.filter((a) => a.level === 'CRITICAL').length,
    warning: alerts.filter((a) => a.level === 'WARNING').length,
    info: alerts.filter((a) => a.level === 'INFO').length,
  }), []);

  const alertColumns: ColumnsType<Alert> = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (ts: string) => formatDateTime(ts),
      sorter: (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
      defaultSortOrder: 'descend',
    },
    {
      title: 'Level',
      dataIndex: 'level',
      key: 'level',
      width: 110,
      render: (level: string) => {
        const cfg = ALERT_LEVEL_MAP[level] ?? { color: 'default', icon: null };
        return <Tag icon={cfg.icon} color={cfg.color}>{level}</Tag>;
      },
    },
    {
      title: 'Resource',
      dataIndex: 'resource',
      key: 'resource',
      width: 100,
      render: (r: string) => <Tag>{r}</Tag>,
    },
    {
      title: 'Message',
      dataIndex: 'message',
      key: 'message',
    },
  ];

  const thresholdColumns: ColumnsType<Threshold> = [
    { title: 'Resource', dataIndex: 'resource', key: 'resource' },
    { title: 'Metric', dataIndex: 'metric', key: 'metric' },
    {
      title: 'Warning',
      dataIndex: 'warnAt',
      key: 'warnAt',
      render: (v: number, r: Threshold) => <Text type="warning">{v}{r.unit}</Text>,
    },
    {
      title: 'Critical',
      dataIndex: 'criticalAt',
      key: 'criticalAt',
      render: (v: number, r: Threshold) => <Text type="danger">{v}{r.unit}</Text>,
    },
  ];

  return (
    <div>
      <Title level={2}>System Monitor</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Maps to COBOL UTLMON00 — System Monitoring Utility
      </Text>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {Object.entries(resourcesByType).map(([type, metrics]) => (
          <Col xs={24} sm={12} md={6} key={type}>
            <Card title={type} size="small" bordered>
              {metrics.map((m) => {
                const isInverse = m.metricName === 'Buffer Pool Hit Ratio';
                const percent = isInverse
                  ? 100 - ((100 - m.currentValue) / (100 - m.threshold)) * 100
                  : (m.currentValue / m.threshold) * 100;
                const color = getGaugeColor(m.currentValue, m.threshold, isInverse);
                return (
                  <div key={m.metricName} style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text>{m.metricName}</Text>
                      <Text strong style={{ color }}>
                        {m.currentValue}{m.unit}
                      </Text>
                    </div>
                    <Progress
                      percent={Math.min(Math.round(percent), 100)}
                      size="small"
                      strokeColor={color}
                      showInfo={false}
                    />
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      Threshold: {m.threshold}{m.unit}
                    </Text>
                  </div>
                );
              })}
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={8} md={4}>
          <Card size="small"><Statistic title="Total Alerts" value={alertSummary.total} /></Card>
        </Col>
        <Col xs={8} md={4}>
          <Card size="small"><Statistic title="Critical" value={alertSummary.critical} valueStyle={{ color: '#ff4d4f' }} prefix={<AlertOutlined />} /></Card>
        </Col>
        <Col xs={8} md={4}>
          <Card size="small"><Statistic title="Warnings" value={alertSummary.warning} valueStyle={{ color: '#faad14' }} prefix={<WarningOutlined />} /></Card>
        </Col>
        <Col xs={8} md={4}>
          <Card size="small"><Statistic title="Info" value={alertSummary.info} valueStyle={{ color: '#1890ff' }} prefix={<InfoCircleOutlined />} /></Card>
        </Col>
        <Col xs={8} md={4}>
          <Card size="small">
            <Statistic
              title="Health"
              value={alertSummary.critical === 0 ? 'OK' : 'DEGRADED'}
              valueStyle={{ color: alertSummary.critical === 0 ? '#52c41a' : '#ff4d4f' }}
              prefix={alertSummary.critical === 0 ? <CheckCircleOutlined /> : <AlertOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title="Alert Log"
        style={{ marginBottom: 24 }}
        extra={
          <Select
            placeholder="Filter by level"
            allowClear
            value={alertLevelFilter}
            onChange={setAlertLevelFilter}
            style={{ width: 140 }}
            options={[
              { value: 'CRITICAL', label: 'Critical' },
              { value: 'WARNING', label: 'Warning' },
              { value: 'INFO', label: 'Info' },
            ]}
          />
        }
      >
        <Table<Alert>
          dataSource={filteredAlerts}
          columns={alertColumns}
          rowKey={(r) => `${r.timestamp}-${r.resource}`}
          pagination={{ pageSize: 5 }}
          size="small"
        />
      </Card>

      <Card title="Threshold Configuration">
        <Table<Threshold>
          dataSource={thresholds}
          columns={thresholdColumns}
          rowKey={(r) => `${r.resource}-${r.metric}`}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
}
