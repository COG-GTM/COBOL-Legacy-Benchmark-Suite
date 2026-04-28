import { useParams, useNavigate } from 'react-router-dom';
import { mockPortfolios } from '../mocks/mockData';
import { PortfolioForm } from '../components/PortfolioForm';
import { ErrorDisplay } from '../components/ErrorDisplay';
import { useToast } from '../hooks/useToast';

/**
 * Maps to PORTUPDT.cbl - supports action codes: S (Status), V (Value), N (Name)
 */
export function PortfolioEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const portfolio = mockPortfolios.find(p => p.portfolioId === id);

  if (!portfolio) {
    return (
      <div className="p-8">
        <ErrorDisplay code="VS23" details={`Portfolio ${id} not found`} />
      </div>
    );
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Edit Portfolio: {id}</h1>
      <p className="text-sm text-gray-500 mb-4">
        Supports: Status change (S), Value update (V), Name change (N) per PORTUPDT.cbl
      </p>
      <PortfolioForm
        initial={portfolio}
        isEdit
        onSubmit={data => {
          addToast(`Portfolio ${data.portfolioId} updated successfully`, 'success');
          navigate(`/portfolios/${data.portfolioId}`);
        }}
        onCancel={() => navigate(`/portfolios/${id}`)}
      />
    </div>
  );
}
