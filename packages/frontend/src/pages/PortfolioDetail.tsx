import { useParams } from 'react-router-dom';

export default function PortfolioDetail() {
  const { id } = useParams();
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Portfolio: {id}</h2>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-500">Portfolio detail will be implemented in Wave 2.</p>
      </div>
    </div>
  );
}
