import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Briefcase } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { usePortfolios } from './usePortfolios';
import { PortfolioForm } from './PortfolioForm';
import { validatePortfolioForm } from './portfolioData';
import type { PortfolioFormValues, PortfolioMaster } from './portfolioData';

export function PortfolioEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { loading, getPortfolio, updatePortfolio } = usePortfolios();

  if (loading) {
    return <LoadingSpinner message="Loading portfolio..." />;
  }

  const portfolio = id ? getPortfolio(id) : undefined;

  if (!portfolio) {
    return (
      <EmptyState
        title="Portfolio not found"
        message={`No portfolio exists with ID "${id ?? ''}".`}
        icon={<Briefcase className="w-12 h-12" />}
        action={
          <Link
            to="/portfolios"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Portfolios
          </Link>
        }
      />
    );
  }

  const initialValues: PortfolioFormValues = {
    id: portfolio.id,
    accountNo: portfolio.accountNo,
    clientName: portfolio.clientName,
    clientType: portfolio.clientType,
    status: portfolio.status,
    totalValue: portfolio.totalValue.toFixed(2),
    cashBalance: portfolio.cashBalance.toFixed(2),
  };

  const handleSubmit = (values: PortfolioFormValues) => {
    const errors = validatePortfolioForm(values, { isNew: false });
    if (Object.keys(errors).length > 0) return errors;

    updatePortfolio(portfolio.id, {
      accountNo: values.accountNo,
      clientName: values.clientName.trim(),
      clientType: values.clientType as PortfolioMaster['clientType'],
      status: values.status as PortfolioMaster['status'],
      totalValue: Number(values.totalValue),
      cashBalance: Number(values.cashBalance),
    });
    navigate(`/portfolios/${portfolio.id}`);
    return null;
  };

  return (
    <div>
      <PageHeader
        title={`Edit ${portfolio.id}`}
        description={portfolio.clientName}
      />
      <Card className="max-w-3xl">
        <PortfolioForm
          initialValues={initialValues}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/portfolios/${portfolio.id}`)}
          submitLabel="Save Changes"
          idEditable={false}
        />
      </Card>
    </div>
  );
}
