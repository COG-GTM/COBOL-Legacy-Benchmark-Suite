import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { usePortfolios } from './usePortfolios';
import { PortfolioForm } from './PortfolioForm';
import { validatePortfolioForm } from './portfolioData';
import type { PortfolioFormValues, PortfolioMaster } from './portfolioData';

const initialValues: PortfolioFormValues = {
  id: '',
  accountNo: '',
  clientName: '',
  clientType: '',
  status: 'A',
  totalValue: '0.00',
  cashBalance: '0.00',
};

export function PortfolioNewPage() {
  const navigate = useNavigate();
  const { portfolios, loading, addPortfolio } = usePortfolios();

  if (loading) {
    return <LoadingSpinner message="Loading..." />;
  }

  const handleSubmit = (values: PortfolioFormValues) => {
    const errors = validatePortfolioForm(values, {
      isNew: true,
      existingIds: portfolios.map((p) => p.id),
    });
    if (Object.keys(errors).length > 0) return errors;

    const today = new Date().toISOString().slice(0, 10);
    const portfolio: PortfolioMaster = {
      id: values.id,
      accountNo: values.accountNo,
      clientName: values.clientName.trim(),
      clientType: values.clientType as PortfolioMaster['clientType'],
      createDate: today,
      lastMaint: today,
      status: values.status as PortfolioMaster['status'],
      totalValue: Number(values.totalValue),
      cashBalance: Number(values.cashBalance),
    };
    addPortfolio(portfolio);
    navigate(`/portfolios/${portfolio.id}`);
    return null;
  };

  return (
    <div>
      <PageHeader title="New Portfolio" description="Create a new investment portfolio" />
      <Card className="max-w-3xl">
        <PortfolioForm
          initialValues={initialValues}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/portfolios')}
          submitLabel="Create Portfolio"
        />
      </Card>
    </div>
  );
}
