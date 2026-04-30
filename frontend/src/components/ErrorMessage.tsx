import { AlertCircle } from 'lucide-react';

export default function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 p-4 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400">
      <AlertCircle size={20} />
      <p className="text-sm">{message}</p>
    </div>
  );
}
