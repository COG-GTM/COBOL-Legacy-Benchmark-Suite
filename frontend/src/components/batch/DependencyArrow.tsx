interface DependencyArrowProps {
  status: "complete" | "pending" | "failed";
  condition: string;
  vertical?: boolean;
}

export default function DependencyArrow({
  status,
  condition,
  vertical = false,
}: DependencyArrowProps) {
  const colorMap = {
    complete: "text-green-500",
    pending: "text-gray-300",
    failed: "text-red-500",
  };
  const strokeMap = {
    complete: "stroke-green-500",
    pending: "stroke-gray-300",
    failed: "stroke-red-500",
  };
  const isDashed = status === "pending";

  if (vertical) {
    return (
      <div className="flex flex-col items-center" aria-hidden="true">
        <svg
          width="24"
          height="32"
          viewBox="0 0 24 32"
          className={strokeMap[status]}
        >
          <line
            x1="12"
            y1="0"
            x2="12"
            y2="24"
            strokeWidth="2"
            fill="none"
            strokeDasharray={isDashed ? "4 3" : undefined}
          />
          <polygon
            points="6,24 12,32 18,24"
            className={`fill-current ${colorMap[status]}`}
            stroke="none"
          />
        </svg>
        {condition && condition !== "Complete" && (
          <span className={`text-[9px] font-medium ${colorMap[status]}`}>
            {condition}
          </span>
        )}
      </div>
    );
  }

  return (
    <div
      className="flex shrink-0 flex-col items-center justify-center"
      aria-hidden="true"
    >
      <svg
        width="48"
        height="24"
        viewBox="0 0 48 24"
        className={strokeMap[status]}
      >
        <line
          x1="0"
          y1="12"
          x2="38"
          y2="12"
          strokeWidth="2"
          fill="none"
          strokeDasharray={isDashed ? "4 3" : undefined}
        />
        <polygon
          points="38,6 48,12 38,18"
          className={`fill-current ${colorMap[status]}`}
          stroke="none"
        />
      </svg>
      {condition && condition !== "Complete" && (
        <span className={`text-[9px] font-medium ${colorMap[status]}`}>
          {condition}
        </span>
      )}
    </div>
  );
}
