import { useParams } from 'react-router-dom';
import { PlaceholderPage } from '@/pages/PlaceholderPage';

export function PortfolioDetailPage() {
  const { id } = useParams();
  return <PlaceholderPage title={`Portfolio ${id ?? ''}`} description="View portfolio details and positions" />;
}
