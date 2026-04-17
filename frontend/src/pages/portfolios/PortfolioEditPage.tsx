import { useParams } from 'react-router-dom';
import { PlaceholderPage } from '@/pages/PlaceholderPage';

export function PortfolioEditPage() {
  const { id } = useParams();
  return <PlaceholderPage title={`Edit Portfolio ${id ?? ''}`} description="Modify portfolio settings" />;
}
