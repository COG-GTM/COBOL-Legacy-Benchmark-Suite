export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="p-8 flex items-center justify-center min-h-96">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">{title}</h1>
        <p className="text-gray-500">This page is under development.</p>
      </div>
    </div>
  );
}
