import { Construction } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';

interface PlaceholderPageProps {
  title: string;
  description?: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <div>
      <PageHeader title={title} description={description} />
      <div className="flex flex-col items-center justify-center py-24 text-center">
        <div className="p-4 bg-slate-100 rounded-2xl mb-6">
          <Construction className="w-12 h-12 text-slate-400" />
        </div>
        <h2 className="text-xl font-semibold text-slate-900 mb-2">{title}</h2>
        <p className="text-slate-500 max-w-md">
          This page is coming soon. It will be built in an upcoming session as part of the
          COBOL modernization effort.
        </p>
      </div>
    </div>
  );
}
