interface InlineErrorProps {
  message: string;
}

export function InlineError({ message }: InlineErrorProps) {
  if (!message) return null;
  return <p className="text-red-600 text-sm mt-1">{message}</p>;
}
