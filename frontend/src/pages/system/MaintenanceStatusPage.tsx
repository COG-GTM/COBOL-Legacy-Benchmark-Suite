import { useMemo, useState } from 'react';
import { Card, Col, Row, Table, Tag, Typography, Select, Progress, Statistic } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import maintenanceData from '../../mocks/maintenance.json';
import { formatDateTime, formatNumber } from '../../utils/formatters';

const { Title, Text } = Typography;

interface MaintenanceTask {
  id: string;
  function: string;
  fileName: string;
  status: string;
  lastRun: string;
  nextScheduled: string;
  recordsProcessed: number;
  recordsWritten: number;
  errors: number;
  durationSeconds: number;
}

interface SpaceEntry {
  fileName: string;
  allocatedMB: number;
  usedMB: number;
  extents: number;
  ciSize: number;
}

const tasks = maintenanceData.tasks as MaintenanceTask[];
const spaceData = maintenanceData.spaceUtilization as SpaceEntry[];

const STATUS_MAP: Record<string, { color: string; icon: React.ReactNode }> = {
  COMPLETE: { color: 'success', icon: <CheckCircleOutlined /> },
  RUNNING: { color: 'processing', icon: <SyncOutlined spin /> },
  PENDING: { color: 'blue', icon: <ClockCircleOutlined /> },
  ERROR: { color: 'error', icon: <CloseCircleOutlined /> },
};

const FUNCTION_COLORS: Record<string, string> = {
  ARCHIVE: 'purple',
  CLEANUP: 'cyan',
  REORG: 'geekblue',
  ANALYZE: 'green',
};

export function Component() {
  const [functionFilter, setFunctionFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);

  const filteredTasks = useMemo(() => {
    let result = tasks;
    if (functionFilter) result = result.filter((t) => t.function === functionFilter);
    if (statusFilter) result = result.filter((t) => t.status === statusFilter);
    return result;
  }, [functionFilter, statusFilter]);

  const summary = useMemo(() => ({
    total: tasks.length,
    complete: tasks.filter((t) => t.status === 'COMPLETE').length,
    running: tasks.filter((t) => t.status === 'RUNNING').length,
    error: tasks.filter((t) => t.status === 'ERROR').length,
    totalRecords: tasks.reduce((acc, t) => acc + t.recordsProcessed, 0),
  }), []);

  const totalSpace = useMemo(() => {
    const allocated = spaceData.reduce((acc, s) => acc + s.allocatedMB, 0);
    const used = spaceData.reduce((acc, s) => acc + s.usedMB, 0);
    return { allocated, used, utilization: Math.round((used / allocated) * 100) };
  }, []);

  const taskColumns: ColumnsType<MaintenanceTask> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: 'Function',
      dataIndex: 'function',
      key: 'function',
      render: (fn: string) => <Tag color={FUNCTION_COLORS[fn] ?? 'default'}>{fn}</Tag>,
    },
    {
      title: 'File Name',
      dataIndex: 'fileName',
      key: 'fileName',
      render: (name: string) => <Text code>{name}</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const s = STATUS_MAP[status] ?? { color: 'default', icon: null };
        return <Tag icon={s.icon} color={s.color}>{status}</Tag>;
      },
    },
    {
      title: 'Last Run',
      dataIndex: 'lastRun',
      key: 'lastRun',
      render: (ts: string) => ts ? formatDateTime(ts) : '—',
    },
    {
      title: 'Next Scheduled',
      dataIndex: 'nextScheduled',
      key: 'nextScheduled',
      render: (ts: string) => ts ? formatDateTime(ts) : '—',
    },
    {
      title: 'Records',
      dataIndex: 'recordsProcessed',
      key: 'recordsProcessed',
      render: (v: number) => formatNumber(v, 0),
      align: 'right',
    },
    {
      title: 'Errors',
      dataIndex: 'errors',
      key: 'errors',
      render: (v: number) => v > 0 ? <Text type="danger">{v}</Text> : '0',
      align: 'right',
    },
    {
      title: 'Duration',
      dataIndex: 'durationSeconds',
      key: 'durationSeconds',
      render: (v: number, record: MaintenanceTask) => {
        if (record.status === 'PENDING') return '—';
        if (record.status === 'RUNNING') return <Text type="warning">In progress</Text>;
        if (v < 60) return `${v}s`;
        return `${Math.floor(v / 60)}m ${v % 60}s`;
      },
      align: 'right',
    },
  ];

  const spaceColumns: ColumnsType<SpaceEntry> = [
    {
      title: 'File Name',
      dataIndex: 'fileName',
      key: 'fileName',
      render: (name: string) => <Text code>{name}</Text>,
    },
    {
      title: 'Allocated',
      dataIndex: 'allocatedMB',
      key: 'allocatedMB',
      render: (v: number) => `${formatNumber(v, 0)} MB`,
      align: 'right',
    },
    {
      title: 'Used',
      dataIndex: 'usedMB',
      key: 'usedMB',
      render: (v: number) => `${formatNumber(v, 0)} MB`,
      align: 'right',
    },
    {
      title: 'Utilization',
      key: 'utilization',
      render: (_: unknown, record: SpaceEntry) => {
        const pct = Math.round((record.usedMB / record.allocatedMB) * 100);
        const color = pct > 90 ? '#ff4d4f' : pct > 75 ? '#faad14' : '#52c41a';
        return <Progress percent={pct} size="small" strokeColor={color} style={{ width: 120 }} />;
      },
      width: 160,
    },
    {
      title: 'Extents',
      dataIndex: 'extents',
      key: 'extents',
      align: 'right',
    },
    {
      title: 'CI Size',
      dataIndex: 'ciSize',
      key: 'ciSize',
      render: (v: number) => `${formatNumber(v, 0)}`,
      align: 'right',
    },
  ];

  return (
    <div>
      <Title level={2}>File Maintenance Status</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Maps to COBOL UTLMNT00 — File Maintenance Utility
      </Text>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={8} md={4}>
          <Card size="small"><Statistic title="Total Tasks" value={summary.total} /></Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small"><Statistic title="Complete" value={summary.complete} valueStyle={{ color: '#52c41a' }} prefix={<CheckCircleOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small"><Statistic title="Running" value={summary.running} valueStyle={{ color: '#1890ff' }} prefix={<SyncOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small"><Statistic title="Errors" value={summary.error} valueStyle={{ color: '#ff4d4f' }} prefix={<CloseCircleOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small"><Statistic title="Records Processed" value={summary.totalRecords} formatter={(v) => formatNumber(Number(v), 0)} prefix={<DatabaseOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="DASD Usage" value={totalSpace.utilization} suffix="%" valueStyle={{ color: totalSpace.utilization > 80 ? '#faad14' : '#52c41a' }} />
          </Card>
        </Col>
      </Row>

      <Card
        title="Maintenance Tasks"
        style={{ marginBottom: 24 }}
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            <Select
              placeholder="Function"
              allowClear
              value={functionFilter}
              onChange={setFunctionFilter}
              style={{ width: 130 }}
              options={[
                { value: 'ARCHIVE', label: 'Archive' },
                { value: 'CLEANUP', label: 'Cleanup' },
                { value: 'REORG', label: 'Reorg' },
                { value: 'ANALYZE', label: 'Analyze' },
              ]}
            />
            <Select
              placeholder="Status"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
              style={{ width: 130 }}
              options={[
                { value: 'COMPLETE', label: 'Complete' },
                { value: 'RUNNING', label: 'Running' },
                { value: 'PENDING', label: 'Pending' },
                { value: 'ERROR', label: 'Error' },
              ]}
            />
          </div>
        }
      >
        <Table<MaintenanceTask>
          dataSource={filteredTasks}
          columns={taskColumns}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          size="small"
        />
      </Card>

      <Card title="VSAM/File Space Utilization">
        <div style={{ marginBottom: 16 }}>
          <Text>
            Total: <Text strong>{formatNumber(totalSpace.used, 0)} MB</Text> used
            of <Text strong>{formatNumber(totalSpace.allocated, 0)} MB</Text> allocated
            ({totalSpace.utilization}%)
          </Text>
        </div>
        <Table<SpaceEntry>
          dataSource={spaceData}
          columns={spaceColumns}
          rowKey="fileName"
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
}
