import type { CheckpointRecord } from "../../types";

interface CheckpointTimelineProps {
  checkpoints: CheckpointRecord[];
  totalRecords: number;
}

export default function CheckpointTimeline({
  checkpoints,
  totalRecords,
}: CheckpointTimelineProps) {
  if (checkpoints.length === 0 || totalRecords === 0) {
    return (
      <p className="text-xs text-gray-400 italic">
        No checkpoint data available
      </p>
    );
  }

  const lastCp = checkpoints[checkpoints.length - 1];
  const isFailed = lastCp.status === "saved";

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-xs text-gray-600">
        <span>0</span>
        <span>{totalRecords.toLocaleString()} records</span>
      </div>

      {/* Progress bar */}
      <div className="relative h-4 w-full rounded-full bg-gray-100">
        {/* Filled portion */}
        <div
          className={`h-full rounded-full transition-all ${isFailed ? "bg-red-200" : "bg-green-200"}`}
          style={{
            width: `${(lastCp.recordsProcessedAtCheckpoint / totalRecords) * 100}%`,
          }}
        />

        {/* Checkpoint dots */}
        {checkpoints.map((cp) => {
          const pct = (cp.recordsProcessedAtCheckpoint / totalRecords) * 100;
          const isLast = cp.checkpointNumber === lastCp.checkpointNumber;
          return (
            <div
              key={cp.checkpointNumber}
              className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2"
              style={{ left: `${pct}%` }}
              title={`Checkpoint ${cp.checkpointNumber}: ${cp.recordsProcessedAtCheckpoint.toLocaleString()} records at ${cp.timestamp.split("T")[1]}`}
            >
              <div
                className={`h-3 w-3 rounded-full border-2 ${
                  isLast && isFailed
                    ? "border-red-600 bg-red-400"
                    : "border-green-600 bg-green-400"
                }`}
              />
            </div>
          );
        })}
      </div>

      {/* Checkpoint labels */}
      <div className="flex flex-wrap gap-2">
        {checkpoints.map((cp) => (
          <span
            key={cp.checkpointNumber}
            className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 font-mono text-[10px] ${
              cp.status === "saved"
                ? "bg-yellow-50 text-yellow-700"
                : "bg-green-50 text-green-700"
            }`}
          >
            CP{cp.checkpointNumber}: {cp.recordsProcessedAtCheckpoint.toLocaleString()}
            <span className="text-gray-400">
              ({cp.timestamp.split("T")[1]})
            </span>
          </span>
        ))}
      </div>

      {isFailed && (
        <p className="text-xs font-medium text-red-600">
          Restart point: Checkpoint {lastCp.checkpointNumber} at record{" "}
          {lastCp.recordsProcessedAtCheckpoint.toLocaleString()}
        </p>
      )}
    </div>
  );
}
