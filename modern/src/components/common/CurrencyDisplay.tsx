interface CurrencyDisplayProps {
  amount: number;
  currency?: string;
  className?: string;
}

const formatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export default function CurrencyDisplay({
  amount,
  currency = "USD",
  className = "",
}: CurrencyDisplayProps) {
  const formatted = formatter.format(amount);
  const display = currency !== "USD" ? `${formatted} ${currency}` : formatted;
  const color = amount < 0 ? "text-red-600" : "";

  return <span className={`font-mono ${color} ${className}`.trim()}>{display}</span>;
}
