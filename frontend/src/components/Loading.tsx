export default function Loading({ text = 'Loading...' }: { text?: string }) {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="text-center">
        <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm text-slate-400 mt-3">{text}</p>
      </div>
    </div>
  );
}
