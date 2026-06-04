import { useState, useMemo } from 'react';
import { Card, Col, Row, Table, Tag, Typography, Select, Button, Descriptions, Progress } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import batchJobsData from '../../mocks/batchJobs.json';

const { Title, Text } = Typography;

interface PrereqJob {
  name: string;
  seq: number;
  rc: number;
}

interface BatchJob {
  jobName: string;
  processDate: string;
  sequenceNo: number;
  status: string;
  stepName: string;
  programName: string;
  startTime: string;
  endTime: string;
  returnCode: number;
  errorDesc: string;
  restartCount: number;
  attemptTs: string;
  completeTs: string;
  prereqCount: number;
  prereqJobs: PrereqJob[];
}

const STATUS_MAP: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  R: { label: 'Ready', color: 'blue', icon: <ClockCircleOutlined /> },
  A: { label: 'Active', color: 'processing', icon: <SyncOutlined spin /> },
  W: { label: 'Waiting', color: 'orange', icon: <ExclamationCircleOutlined /> },
  D: { label: 'Done', color: 'success', icon: <CheckCircleOutlined /> },
  E: { label: 'Error', color: 'error', icon: <CloseCircleOutlined /> },
};

const jobs = batchJobsData as BatchJob[];

function getUniqueProcessDates(): string[] {
  return [...new Set(jobs.map((j) => j.processDate))].sort().reverse();
}

export function Component() {
  const [statusFilter, setStatusFilter] = useState<string | null>(null);
  const [dateFilter, setDateFilter] = useState<string | null>(null);

  const processDates = useMemo(() => getUniqueProcessDates(), []);

  const filteredJobs = useMemo(() => {
    let result = jobs;
    if (statusFilter) {
      result = result.filter((j) => j.status === statusFilter);
    }
    if (dateFilter) {
      result = result.filter((j) => j.processDate === dateFilter);
    }
    return result;
  }, [statusFilter, dateFilter]);

  const summary = useMemo(() => {
    const total = filteredJobs.length;
    const done = filteredJobs.filter((j) => j.status === 'D').length;
    const active = filteredJobs.filter((j) => j.status === 'A').length;
    const error = filteredJobs.filter((j) => j.status === 'E').length;
    const waiting = filteredJobs.filter((j) => j.status === 'W').length;
    const ready = filteredJobs.filter((j) => j.status === 'R').length;
    return { total, done, active, error, waiting, ready };
  }, [filteredJobs]);

  const overallProgress = summary.total > 0 ? Math.round((summary.done / summary.total) * 100) : 0;

  const columns: ColumnsType<BatchJob> = [
    {
      title: 'Seq',
      dataIndex: 'sequenceNo',
      key: 'sequenceNo',
      width: 60,
      sorter: (a, b) => a.sequenceNo - b.sequenceNo,
      defaultSortOrder: 'ascend',
    },
    {
      title: 'Job Name',
      dataIndex: 'jobName',
      key: 'jobName',
      render: (name: string) => <Text strong code>{name}</Text>,
    },
    {
      title: 'Program',
      dataIndex: 'programName',
      key: 'programName',
      render: (name: string) => <Text code>{name}</Text>,
    },
    {
      title: 'Step',
      dataIndex: 'stepName',
      key: 'stepName',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const s = STATUS_MAP[status] ?? { label: status, color: 'default', icon: null };
        return <Tag icon={s.icon} color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: 'Start',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (t: string) => t || '—',
    },
    {
      title: 'End',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (t: string) => t || '—',
    },
    {
      title: 'RC',
      dataIndex: 'returnCode',
      key: 'returnCode',
      render: (rc: number, record: BatchJob) => {
        if (record.status === 'R' || record.status === 'W') return '—';
        const color = rc === 0 ? '#52c41a' : rc <= 4 ? '#faad14' : '#ff4d4f';
        return <Text style={{ color }}>{rc}</Text>;
      },
    },
    {
      title: 'Restarts',
      dataIndex: 'restartCount',
      key: 'restartCount',
      render: (count: number) => count > 0 ? <Text type="warning">{count}</Text> : '0',
    },
  ];

  return (
    <div>
      <Title level={2}>Batch Job Status</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Maps to COBOL BCHCTL00 — Batch Control Processor
      </Text>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Total Jobs</Text>
            <div><Text strong style={{ fontSize: 24 }}>{summary.total}</Text></div>
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Done</Text>
            <div><Text strong style={{ fontSize: 24, color: '#52c41a' }}>{summary.done}</Text></div>
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Active</Text>
            <div><Text strong style={{ fontSize: 24, color: '#1890ff' }}>{summary.active}</Text></div>
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Waiting</Text>
            <div><Text strong style={{ fontSize: 24, color: '#faad14' }}>{summary.waiting}</Text></div>
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Ready</Text>
            <div><Text strong style={{ fontSize: 24, color: '#1890ff' }}>{summary.ready}</Text></div>
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Text type="secondary">Error</Text>
            <div><Text strong style={{ fontSize: 24, color: '#ff4d4f' }}>{summary.error}</Text></div>
          </Card>
        </Col>
      </Row>

      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Text strong>Overall Progress:</Text>
          <Progress percent={overallProgress} style={{ flex: 1, maxWidth: 400 }} />
        </div>
      </Card>

      <Card
        title="Filters"
        size="small"
        style={{ marginBottom: 16 }}
        extra={
          <Button size="small" onClick={() => { setStatusFilter(null); setDateFilter(null); }}>
            Reset
          </Button>
        }
      >
        <Row gutter={16}>
          <Col>
            <Select
              placeholder="Status"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
              style={{ width: 140 }}
              options={[
                { value: 'R', label: 'Ready' },
                { value: 'A', label: 'Active' },
                { value: 'W', label: 'Waiting' },
                { value: 'D', label: 'Done' },
                { value: 'E', label: 'Error' },
              ]}
            />
          </Col>
          <Col>
            <Select
              placeholder="Process Date"
              allowClear
              value={dateFilter}
              onChange={setDateFilter}
              style={{ width: 160 }}
              options={processDates.map((d) => ({ value: d, label: d }))}
            />
          </Col>
        </Row>
      </Card>

      <Table<BatchJob>
        dataSource={filteredJobs}
        columns={columns}
        rowKey={(r) => `${r.jobName}-${r.processDate}-${r.sequenceNo}`}
        pagination={{ pageSize: 10 }}
        expandable={{
          expandedRowRender: (record) => (
            <Descriptions size="small" column={2} bordered>
              <Descriptions.Item label="Process Date">{record.processDate}</Descriptions.Item>
              <Descriptions.Item label="Program">{record.programName}</Descriptions.Item>
              <Descriptions.Item label="Attempt Timestamp">{record.attemptTs || '—'}</Descriptions.Item>
              <Descriptions.Item label="Complete Timestamp">{record.completeTs || '—'}</Descriptions.Item>
              <Descriptions.Item label="Return Code">{record.returnCode}</Descriptions.Item>
              <Descriptions.Item label="Restart Count">{record.restartCount}</Descriptions.Item>
              {record.errorDesc && (
                <Descriptions.Item label="Error" span={2}>
                  <Text type="danger">{record.errorDesc}</Text>
                </Descriptions.Item>
              )}
              {record.prereqCount > 0 && (
                <Descriptions.Item label="Prerequisites" span={2}>
                  {record.prereqJobs.map((p) => (
                    <Tag key={`${p.name}-${p.seq}`} color={p.rc === 0 ? 'success' : 'error'}>
                      {p.name} (seq {p.seq}, RC={p.rc})
                    </Tag>
                  ))}
                </Descriptions.Item>
              )}
            </Descriptions>
          ),
        }}
      />
    </div>
  );
}
