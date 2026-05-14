interface Props {
  code: string;
  labels?: Record<string, string>;
}

const COLORS: Record<string, string> = {
  A: "bg-[#4ADE80]/15 text-[#4ADE80]",
  Active: "bg-[#4ADE80]/15 text-[#4ADE80]",
  D: "bg-[#4ADE80]/15 text-[#4ADE80]",
  Done: "bg-[#4ADE80]/15 text-[#4ADE80]",
  C: "bg-[#94A3B8]/15 text-[#94A3B8]",
  Closed: "bg-[#94A3B8]/15 text-[#94A3B8]",
  S: "bg-[#FBBF24]/15 text-[#FBBF24]",
  Suspended: "bg-[#FBBF24]/15 text-[#FBBF24]",
  P: "bg-[#60A5FA]/15 text-[#60A5FA]",
  Pending: "bg-[#60A5FA]/15 text-[#60A5FA]",
  F: "bg-[#F87171]/15 text-[#F87171]",
  Failed: "bg-[#F87171]/15 text-[#F87171]",
  R: "bg-[#C084FC]/15 text-[#C084FC]",
  Reversed: "bg-[#C084FC]/15 text-[#C084FC]",
};

const DEFAULT_LABELS: Record<string, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
  D: "Done",
  P: "Pending",
  F: "Failed",
  R: "Reversed",
};

export default function StatusBadge({ code, labels }: Props) {
  const map = labels ?? DEFAULT_LABELS;
  const label = map[code] ?? code;
  const cls = COLORS[code] ?? COLORS[label] ?? "bg-[#94A3B8]/15 text-[#94A3B8]";

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {label}
    </span>
  );
}
