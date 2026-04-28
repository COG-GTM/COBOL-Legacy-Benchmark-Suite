import { useNavigate } from 'react-router-dom';
import { PortfolioForm } from '../components/PortfolioForm';
import { useToast } from '../hooks/useToast';

/**
 * Maps to PORTMSTR CREATE action (PORTMSTR.cbl lines 86-98)
 */
export function PortfolioCreatePage() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Create Portfolio</h1>
      <PortfolioForm
        onSubmit={data => {
          addToast(`Portfolio ${data.portfolioId} created successfully`, 'success');
          navigate('/portfolios');
        }}
        onCancel={() => navigate('/portfolios')}
      />
    </div>
  );
}
