import { useState } from "react";
import { Info } from "lucide-react";
import {
  pipelineDefinition,
  dependencyEdges,
  jobSchedule,
} from "../../mock/batchJobsData";

const phaseColor: Record<string, { fill: string; stroke: string; text: string; bg: string }> = {
  "Start of Day": {
    fill: "#DBEAFE",
    stroke: "#3B82F6",
    text: "text-blue-700",
    bg: "bg-blue-50",
  },
  "Main Process": {
    fill: "#DCFCE7",
    stroke: "#22C55E",
    text: "text-green-700",
    bg: "bg-green-50",
  },
  "End of Day": {
    fill: "#FFF7ED",
    stroke: "#F97316",
    text: "text-orange-700",
    bg: "bg-orange-50",
  },
};

// Fixed positions for the SVG dependency graph
const nodePositions: Record<string, { x: number; y: number; phase: string }> = {
  INITDAY: { x: 60, y: 60, phase: "Start of Day" },
  CKPCLR: { x: 230, y: 60, phase: "Start of Day" },
  DATEVAL: { x: 400, y: 60, phase: "Start of Day" },
  TRNVAL00: { x: 60, y: 160, phase: "Main Process" },
  POSUPD00: { x: 260, y: 160, phase: "Main Process" },
  HISTLD00: { x: 460, y: 160, phase: "Main Process" },
  RPTGEN00: { x: 60, y: 260, phase: "End of Day" },
  BCKLOD00: { x: 260, y: 260, phase: "End of Day" },
  ENDDAY: { x: 460, y: 260, phase: "End of Day" },
};

const NODE_W = 140;
const NODE_H = 44;

function DependencyGraph({
  highlightId,
  onNodeClick,
}: {
  highlightId: string | null;
  onNodeClick: (id: string) => void;
}) {
  return (
    <svg
      viewBox="0 0 620 320"
      className="w-full max-w-3xl"
      role="img"
      aria-label="Pipeline dependency graph showing step relationships"
    >
      {/* Phase swim lanes */}
      {[
        { label: "Start of Day", y: 30, h: 80, phase: "Start of Day" },
        { label: "Main Process", y: 130, h: 80, phase: "Main Process" },
        { label: "End of Day", y: 230, h: 80, phase: "End of Day" },
      ].map((lane) => {
        const c = phaseColor[lane.phase];
        return (
          <g key={lane.label}>
            <rect
              x={5}
              y={lane.y}
              width={610}
              height={lane.h}
              rx={8}
              fill={c.fill}
              stroke={c.stroke}
              strokeWidth={1}
              opacity={0.5}
            />
            <text
              x={12}
              y={lane.y + 14}
              fontSize={9}
              fill="#6B7280"
              fontWeight={600}
            >
              {lane.label}
            </text>
          </g>
        );
      })}

      {/* Edges */}
      {dependencyEdges.map((edge) => {
        const from = nodePositions[edge.from];
        const to = nodePositions[edge.to];
        if (!from || !to) return null;

        const x1 = from.x + NODE_W / 2;
        const y1 = from.y + NODE_H / 2;
        const x2 = to.x + NODE_W / 2;
        const y2 = to.y + NODE_H / 2;

        // Adjust start/end to node edges
        let sx = x1,
          sy = y1,
          ex = x2,
          ey = y2;
        if (from.phase === to.phase) {
          sx = from.x + NODE_W;
          sy = from.y + NODE_H / 2;
          ex = to.x;
          ey = to.y + NODE_H / 2;
        } else {
          sx = from.x + NODE_W / 2;
          sy = from.y + NODE_H;
          ex = to.x + NODE_W / 2;
          ey = to.y;
        }

        const mid = from.phase !== to.phase
          ? `M${sx},${sy} C${sx},${(sy + ey) / 2} ${ex},${(sy + ey) / 2} ${ex},${ey}`
          : `M${sx},${sy} L${ex},${ey}`;

        return (
          <g key={`${edge.from}-${edge.to}`} aria-hidden="true">
            <defs>
              <marker
                id={`arrow-${edge.from}-${edge.to}`}
                viewBox="0 0 10 10"
                refX={10}
                refY={5}
                markerWidth={6}
                markerHeight={6}
                orient="auto"
              >
                <path d="M0,0 L10,5 L0,10 Z" fill="#9CA3AF" />
              </marker>
            </defs>
            <path
              d={mid}
              fill="none"
              stroke="#9CA3AF"
              strokeWidth={1.5}
              markerEnd={`url(#arrow-${edge.from}-${edge.to})`}
            />
            {edge.condition !== "Complete" && (
              <text
                x={(sx + ex) / 2}
                y={(sy + ey) / 2 - 4}
                fontSize={8}
                fill="#6B7280"
                textAnchor="middle"
              >
                {edge.condition}
              </text>
            )}
          </g>
        );
      })}

      {/* Nodes */}
      {Object.entries(nodePositions).map(([id, pos]) => {
        const c = phaseColor[pos.phase];
        const isHighlighted = highlightId === id;
        const allSteps = pipelineDefinition.phases.flatMap((p) => p.steps);
        const stepDef = allSteps.find((s) => s.stepId === id);
        return (
          <g
            key={id}
            onClick={() => onNodeClick(id)}
            style={{ cursor: "pointer" }}
            role="button"
            tabIndex={0}
            aria-label={stepDef ? `${stepDef.stepName} (${id})` : id}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") onNodeClick(id);
            }}
          >
            <rect
              x={pos.x}
              y={pos.y}
              width={NODE_W}
              height={NODE_H}
              rx={6}
              fill="white"
              stroke={isHighlighted ? "#3B82F6" : c.stroke}
              strokeWidth={isHighlighted ? 2.5 : 1.5}
            />
            <text
              x={pos.x + NODE_W / 2}
              y={pos.y + 18}
              textAnchor="middle"
              fontSize={10}
              fontWeight={700}
              fontFamily="monospace"
              fill="#111827"
            >
              {id}
            </text>
            <text
              x={pos.x + NODE_W / 2}
              y={pos.y + 34}
              textAnchor="middle"
              fontSize={8}
              fill="#6B7280"
            >
              {stepDef?.stepName ?? ""}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

export default function PipelineDefinitionTab() {
  const [highlightId, setHighlightId] = useState<string | null>(null);

  return (
    <div className="space-y-6">
      {/* Dependency Graph */}
      <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-gray-900">
          Pipeline Dependency Graph
        </h2>
        <DependencyGraph
          highlightId={highlightId}
          onNodeClick={(id) =>
            setHighlightId(highlightId === id ? null : id)
          }
        />
        <div className="mt-3 flex flex-wrap gap-4 border-t border-gray-100 pt-3">
          {Object.entries(phaseColor).map(([name, c]) => (
            <div key={name} className="flex items-center gap-1.5 text-xs">
              <span
                className="inline-block h-3 w-3 rounded"
                style={{ backgroundColor: c.fill, border: `1px solid ${c.stroke}` }}
              />
              <span className={c.text}>{name}</span>
            </div>
          ))}
          <div className="flex items-center gap-1.5 text-xs text-gray-500">
            <span className="inline-block h-0.5 w-4 bg-gray-400" />
            Dependency (arrow = direction)
          </div>
        </div>
      </div>

      {/* Step Details Table */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Seq
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Step ID
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Program
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Description
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Phase
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Prerequisite
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Max RC
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Time Window
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Checkpoint
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Est. Duration
                </th>
              </tr>
            </thead>
            <tbody>
              {pipelineDefinition.phases.map((phase) => {
                const c = phaseColor[phase.name];
                return (
                  <tbody key={phase.name}>
                    <tr className={c.bg}>
                      <td
                        colSpan={10}
                        className={`px-3 py-1.5 text-xs font-semibold ${c.text}`}
                      >
                        {phase.name}
                      </td>
                    </tr>
                    {phase.steps.map((step, idx) => {
                      const seq =
                        pipelineDefinition.phases
                          .slice(
                            0,
                            pipelineDefinition.phases.indexOf(phase)
                          )
                          .reduce((sum, p) => sum + p.steps.length, 0) +
                        idx +
                        1;
                      const isHighlighted = highlightId === step.stepId;
                      return (
                        <tr
                          key={step.stepId}
                          onClick={() =>
                            setHighlightId(
                              highlightId === step.stepId
                                ? null
                                : step.stepId
                            )
                          }
                          className={`cursor-pointer border-b border-gray-100 transition-colors hover:bg-blue-50 ${isHighlighted ? "bg-blue-50" : ""}`}
                        >
                          <td className="px-3 py-2 font-mono text-gray-500">
                            {seq}
                          </td>
                          <td className="px-3 py-2 font-mono font-medium text-gray-900">
                            {step.stepId}
                          </td>
                          <td className="px-3 py-2 font-mono text-gray-700">
                            {step.program}
                          </td>
                          <td className="max-w-xs px-3 py-2 text-gray-700">
                            {step.description}
                          </td>
                          <td className={`px-3 py-2 ${c.text}`}>
                            {phase.name}
                          </td>
                          <td className="px-3 py-2 font-mono text-gray-700">
                            {step.prerequisite ?? "None"}
                          </td>
                          <td className="px-3 py-2 font-mono text-gray-700">
                            {step.requiredMaxRC !== null
                              ? `\u2264 ${step.requiredMaxRC}`
                              : "N/A"}
                          </td>
                          <td className="px-3 py-2 font-mono text-gray-700">
                            {step.timeWindow}
                          </td>
                          <td className="px-3 py-2 text-gray-700">
                            {step.checkpointFrequency}
                          </td>
                          <td className="px-3 py-2 font-mono text-gray-700">
                            {step.estimatedDuration}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Business Rules */}
      <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center gap-2">
          <Info size={16} className="text-blue-500" />
          <h2 className="text-sm font-semibold text-gray-900">
            Batch Processing Business Rules
          </h2>
        </div>
        <ul className="space-y-1.5 text-xs text-gray-700">
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            Day must be open before TRNVAL00 can start
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            POSUPD00 requires TRNVAL00 to complete with RC &le; 4
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            HISTLD00 requires POSUPD00 to complete with RC &le; 4
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            Report generation has no RC requirement but requires HISTLD00
            completion
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            Checkpoint frequency: 1000 records for validation/history, 500 for
            position update
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            Minimum checkpoint interval: 2 minutes
          </li>
        </ul>
      </div>

      {/* Schedule Table */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="p-4 pb-2">
          <h2 className="text-sm font-semibold text-gray-900">
            Weekly Schedule
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Day
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Scheduled Time
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Status
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Notes
                </th>
              </tr>
            </thead>
            <tbody>
              {jobSchedule.map((entry) => (
                <tr
                  key={entry.dayOfWeek}
                  className={`border-b border-gray-100 ${entry.status === "suspended" ? "text-gray-400 italic" : ""}`}
                >
                  <td className="px-3 py-2 font-medium">
                    {entry.dayOfWeek}
                  </td>
                  <td className="px-3 py-2 font-mono">
                    {entry.scheduledTime}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                        entry.status === "active"
                          ? "bg-green-100 text-green-800"
                          : entry.status === "holiday"
                            ? "bg-yellow-100 text-yellow-800"
                            : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {entry.status}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-gray-500">
                    {entry.note ?? "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
