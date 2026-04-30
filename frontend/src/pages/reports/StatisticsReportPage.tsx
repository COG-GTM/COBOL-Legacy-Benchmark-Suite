import { Card, Col, Progress, Row, Statistic, Typography } from 'antd';
import { formatNumber } from '../../utils/formatters';
import statsData from '../../mocks/systemStats.json';

const { Title, Text } = Typography;

interface SystemStats {
  db2Metrics: {
    totalCalls: number;
    avgResponseMs: number;
    cpuTimeMs: number;
    waitTimeMs: number;
  };
  batchMetrics: {
    totalJobs: number;
    successfulJobs: number;
    failedJobs: number;
    avgElapsedSeconds: number;
  };
  processingMetrics: {
    transactionsProcessed: number;
    portfoliosUpdated: number;
    positionsRecalculated: number;
    auditRecordsWritten: number;
  };
}

const stats = statsData as SystemStats;

const batchSuccessRate = (stats.batchMetrics.successfulJobs / stats.batchMetrics.totalJobs) * 100;

function successRateColor(rate: number): string {
  if (rate > 95) return '#52c41a';
  if (rate > 90) return '#faad14';
  return '#ff4d4f';
}

export function Component() {
  return (
    <div>
      <Title level={2}>System Statistics Dashboard</Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card title="DB2 Metrics" bordered>
            <Statistic
              title="Avg Response Time"
              value={stats.db2Metrics.avgResponseMs}
              suffix="ms"
              precision={1}
            />
            <Statistic
              title="Total Calls"
              value={stats.db2Metrics.totalCalls}
              formatter={(v) => formatNumber(Number(v), 0)}
              style={{ marginTop: 16 }}
            />
            <Statistic
              title="CPU Time"
              value={stats.db2Metrics.cpuTimeMs}
              suffix="ms"
              precision={2}
              style={{ marginTop: 16 }}
            />
            <Statistic
              title="Wait Time"
              value={stats.db2Metrics.waitTimeMs}
              suffix="ms"
              precision={2}
              style={{ marginTop: 16 }}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title="Batch Processing" bordered>
            <Statistic
              title="Total Jobs"
              value={stats.batchMetrics.totalJobs}
              formatter={(v) => formatNumber(Number(v), 0)}
            />
            <div style={{ marginTop: 16 }}>
              <Text strong>Success Rate</Text>
              <Progress
                percent={Number(batchSuccessRate.toFixed(1))}
                strokeColor={successRateColor(batchSuccessRate)}
                style={{ marginTop: 4 }}
              />
            </div>
            <Statistic
              title="Failed Jobs"
              value={stats.batchMetrics.failedJobs}
              valueStyle={stats.batchMetrics.failedJobs > 0 ? { color: '#ff4d4f' } : undefined}
              style={{ marginTop: 16 }}
            />
            <Statistic
              title="Avg Duration"
              value={stats.batchMetrics.avgElapsedSeconds}
              suffix="s"
              precision={1}
              style={{ marginTop: 16 }}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title="Transaction Processing" bordered>
            <Statistic
              title="Transactions Processed"
              value={stats.processingMetrics.transactionsProcessed}
              formatter={(v) => formatNumber(Number(v), 0)}
            />
            <Statistic
              title="Portfolios Updated"
              value={stats.processingMetrics.portfoliosUpdated}
              formatter={(v) => formatNumber(Number(v), 0)}
              style={{ marginTop: 16 }}
            />
            <Statistic
              title="Positions Recalculated"
              value={stats.processingMetrics.positionsRecalculated}
              formatter={(v) => formatNumber(Number(v), 0)}
              style={{ marginTop: 16 }}
            />
            <Statistic
              title="Audit Records Written"
              value={stats.processingMetrics.auditRecordsWritten}
              formatter={(v) => formatNumber(Number(v), 0)}
              style={{ marginTop: 16 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
