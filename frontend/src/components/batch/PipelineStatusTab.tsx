import { useState, useEffect, useCallback } from "react";
import { Play, RotateCcw, ChevronDown, ChevronUp } from "lucide-react";
import type { BatchRun, BatchStep } from "../../types";
import { batchRuns } from "../../mock/batchJobsData";
import { checkpointRecords } from "../../mock/batchJobsData";
import { dependencyEdges } from "../../mock/batchJobsData";
import PipelineStepCard from "./PipelineStepCard";
import DependencyArrow from "./DependencyArrow";
import CheckpointTimeline from "./CheckpointTimeline";

const statusBadge: Record<
  BatchRun["overallStatus"],
  { bg: string; text: string; label: string }
> = {
  complete: { bg: "bg-green-100", text: "text-green-800", label: "Complete" },
  running: { bg: "bg-blue-100", text: "text-blue-800", label: "Running" },
  failed: { bg: "bg-red-100", text: "text-red-800", label: "Failed" },
  scheduled: { bg: "bg-gray-100", text: "text-gray-800", label: "Scheduled" },
  partial: {
    bg: "bg-yellow-100",
    text: "text-yellow-800",
    label: "Partial",
  },
};

function rcInterpretation(rc: number): string {
  if (rc === 0) return "Success";
  if (rc <= 4) return "Warning";
  if (rc <= 8) return "Error";
  if (rc <= 12) return "Severe";
  return "Critical";
}

function computeDuration(start: string | null, end: string | null): string {
  if (!start || !end) return "--";
  const [sh, sm, ss] = start.split(":").map(Number);
  const [eh, em, es] = end.split(":").map(Number);
  const totalSec = (eh * 3600 + em * 60 + es) - (sh * 3600 + sm * 60 + ss);
  const mins = Math.floor(totalSec / 60);
  const secs = totalSec % 60;
  if (mins >= 60) {
    const hrs = Math.floor(mins / 60);
    return `${hrs}h ${mins % 60}m ${secs}s`;
  }
  return `${mins}m ${secs}s`;
}

function arrowStatus(
  fromStep: BatchStep
): "complete" | "pending" | "failed" {
  if (fromStep.status === "failed") return "failed";
  if (fromStep.status === "complete") return "complete";
  return "pending";
}

const phaseColors: Record<string, string> = {
  "start-of-day": "bg-blue-50",
  "main-process": "bg-green-50",
  "end-of-day": "bg-orange-50",
};

const phaseLabels: Record<string, string> = {
  "start-of-day": "Start of Day",
  "main-process": "Main Process",
  "end-of-day": "End of Day",
};

export default function PipelineStatusTab() {
  const [selectedRunIdx, setSelectedRunIdx] = useState(0);
  const [simulating, setSimulating] = useState(false);
  const [simStepIdx, setSimStepIdx] = useState(-1);
  const [simSteps, setSimSteps] = useState<BatchStep[]>([]);
  const [selectedStepId, setSelectedStepId] = useState<string | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const baseRun = batchRuns[selectedRunIdx];
  const displaySteps = simulating ? simSteps : baseRun.steps;

  const resetSim = useCallback(() => {
    setSimulating(false);
    setSimStepIdx(-1);
    setSimSteps([]);
  }, []);

  const startSim = useCallback(() => {
    const pending = baseRun.steps.map((s) => ({
      ...s,
      status: "pending" as const,
      actualStart: null,
      actualEnd: null,
      returnCode: null,
      recordsProcessed: null,
      recordsRead: s.recordsRead,
      recordsRejected: null,
      checkpointCount: null,
      lastCheckpoint: null,
      errorMessage: null,
    }));
    setSimSteps(pending);
    setSimStepIdx(0);
    setSimulating(true);
  }, [baseRun]);

  useEffect(() => {
    if (!simulating || simStepIdx < 0) return;

    if (simStepIdx >= baseRun.steps.length) {
      return;
    }

    const original = baseRun.steps[simStepIdx];

    // Step 1: mark as running after a brief delay
    const runTimer = setTimeout(() => {
      setSimSteps((prev) =>
        prev.map((s, i) =>
          i === simStepIdx ? { ...s, status: "running" as const, actualStart: original.actualStart } : s
        )
      );
    }, 300);

    // Step 2: mark as complete (or failed/skipped) after 2s
    const doneTimer = setTimeout(() => {
      setSimSteps((prev) =>
        prev.map((s, i) => {
          if (i === simStepIdx) {
            return { ...original };
          }
          if (
            original.status === "failed" &&
            i > simStepIdx &&
            baseRun.steps[i].status === "skipped"
          ) {
            return { ...baseRun.steps[i] };
          }
          return s;
        })
      );

      if (original.status === "failed") {
        // Simulation ends on failure
        return;
      }
      setSimStepIdx((idx) => idx + 1);
    }, 2000);

    return () => {
      clearTimeout(runTimer);
      clearTimeout(doneTimer);
    };
  }, [simulating, simStepIdx, baseRun]);

  const selectedStep = selectedStepId
    ? displaySteps.find((s) => s.stepId === selectedStepId) ?? null
    : null;

  const handleStepClick = (stepId: string) => {
    if (selectedStepId === stepId) {
      setDetailOpen(!detailOpen);
    } else {
      setSelectedStepId(stepId);
      setDetailOpen(true);
    }
  };

  // Group steps by phase
  const phases = ["start-of-day", "main-process", "end-of-day"] as const;
  const groupedSteps = phases.map((phase) => ({
    phase,
    steps: displaySteps.filter((s) => s.phase === phase),
  }));

  const badge = statusBadge[baseRun.overallStatus];

  return (
    <div className="space-y-6">
      {/* Run Summary Banner */}
      <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-3">
            <span className="font-mono text-sm font-bold text-gray-900">
              {baseRun.runId}
            </span>
            <span
              className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${badge.bg} ${badge.text}`}
            >
              {badge.label}
            </span>
            <span className="text-sm text-gray-500">{baseRun.runDate}</span>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={selectedRunIdx}
              onChange={(e) => {
                resetSim();
                setSelectedRunIdx(Number(e.target.value));
              }}
              className="rounded-md border border-gray-300 bg-white px-2 py-1.5 text-xs font-medium text-gray-700"
              aria-label="Select batch run"
            >
              {batchRuns.map((r, i) => (
                <option key={r.runId} value={i}>
                  {r.runId}
                </option>
              ))}
            </select>

            {!simulating ? (
              <button
                onClick={startSim}
                className="flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white shadow-sm hover:bg-blue-700"
              >
                <Play size={14} />
                Simulate Run
              </button>
            ) : (
              <button
                onClick={resetSim}
                className="flex items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 shadow-sm hover:bg-gray-50"
              >
                <RotateCcw size={14} />
                Reset
              </button>
            )}
          </div>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-3 text-xs sm:grid-cols-4">
          <div>
            <span className="text-gray-500">Start</span>
            <p className="font-mono font-medium text-gray-900">
              {baseRun.actualStart ?? "--"}
            </p>
          </div>
          <div>
            <span className="text-gray-500">End</span>
            <p className="font-mono font-medium text-gray-900">
              {baseRun.actualEnd ?? "--"}
            </p>
          </div>
          <div>
            <span className="text-gray-500">Duration</span>
            <p className="font-mono font-medium text-gray-900">
              {computeDuration(baseRun.actualStart, baseRun.actualEnd)}
            </p>
          </div>
          <div>
            <span className="text-gray-500">Records / Errors</span>
            <p className="font-mono font-medium text-gray-900">
              {baseRun.totalRecordsProcessed.toLocaleString()} /{" "}
              <span
                className={
                  baseRun.totalErrors > 0 ? "text-red-600" : "text-gray-900"
                }
              >
                {baseRun.totalErrors}
              </span>
            </p>
          </div>
        </div>
      </div>

      {/* Pipeline Visualization */}
      <div aria-live="polite">
        {/* Horizontal layout for lg+ */}
        <div className="hidden lg:block">
          <div className="overflow-x-auto">
            <div className="flex items-start gap-0 pb-4">
              {groupedSteps.map((group) => (
                <div
                  key={group.phase}
                  role="group"
                  aria-label={phaseLabels[group.phase]}
                  className={`flex flex-col rounded-lg px-3 py-3 ${phaseColors[group.phase]}`}
                >
                  <span className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-gray-500">
                    {phaseLabels[group.phase]}
                  </span>
                  <div className="flex items-center gap-1">
                    {group.steps.map((step, i) => {
                      const edge = dependencyEdges.find(
                        (e) => e.to === step.stepId
                      );
                      const prevStep = edge
                        ? displaySteps.find((s) => s.stepId === edge.from)
                        : null;
                      const showArrow =
                        step.sequence > 1 &&
                        prevStep &&
                        prevStep.phase === step.phase;

                      return (
                        <div key={step.stepId} className="flex items-center">
                          {showArrow && prevStep && (
                            <DependencyArrow
                              status={arrowStatus(prevStep)}
                              condition={edge?.condition ?? ""}
                            />
                          )}
                          {i === 0 && step.sequence > 1 && prevStep && prevStep.phase !== step.phase && (
                            <DependencyArrow
                              status={arrowStatus(prevStep)}
                              condition={edge?.condition ?? ""}
                            />
                          )}
                          <PipelineStepCard
                            step={step}
                            isSelected={selectedStepId === step.stepId}
                            onClick={() => handleStepClick(step.stepId)}
                          />
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Vertical layout for <lg */}
        <div className="lg:hidden">
          <div className="space-y-1">
            {groupedSteps.map((group) => (
              <div
                key={group.phase}
                role="group"
                aria-label={phaseLabels[group.phase]}
                className={`rounded-lg px-3 py-3 ${phaseColors[group.phase]}`}
              >
                <span className="mb-2 block text-[10px] font-semibold uppercase tracking-wider text-gray-500">
                  {phaseLabels[group.phase]}
                </span>
                <div className="flex flex-col items-center gap-0">
                  {group.steps.map((step, i) => {
                    const edge = dependencyEdges.find(
                      (e) => e.to === step.stepId
                    );
                    const prevStep = edge
                      ? displaySteps.find((s) => s.stepId === edge.from)
                      : null;
                    const showArrow = i > 0 || (step.sequence > 1 && prevStep);

                    return (
                      <div
                        key={step.stepId}
                        className="flex flex-col items-center"
                      >
                        {showArrow && prevStep && (
                          <DependencyArrow
                            status={arrowStatus(prevStep)}
                            condition={edge?.condition ?? ""}
                            vertical
                          />
                        )}
                        <PipelineStepCard
                          step={step}
                          isSelected={selectedStepId === step.stepId}
                          onClick={() => handleStepClick(step.stepId)}
                        />
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Step Detail Panel */}
      {selectedStep && (
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          <button
            onClick={() => setDetailOpen(!detailOpen)}
            className="flex w-full items-center justify-between p-4 text-left"
            aria-expanded={detailOpen}
          >
            <span className="text-sm font-semibold text-gray-900">
              Step Detail: {selectedStep.stepId} — {selectedStep.stepName}
            </span>
            {detailOpen ? (
              <ChevronUp size={16} className="text-gray-500" />
            ) : (
              <ChevronDown size={16} className="text-gray-500" />
            )}
          </button>

          {detailOpen && (
            <div className="border-t border-gray-200 p-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase text-gray-500">
                    Identity
                  </h4>
                  <dl className="space-y-1 text-xs">
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Step ID:</dt>
                      <dd className="font-mono font-medium text-gray-900">
                        {selectedStep.stepId}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Name:</dt>
                      <dd className="text-gray-900">
                        {selectedStep.stepName}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Phase:</dt>
                      <dd className="text-gray-900">
                        {phaseLabels[selectedStep.phase]}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Prerequisite:</dt>
                      <dd className="font-mono text-gray-900">
                        {selectedStep.prerequisite ?? "None"}
                        {selectedStep.requiredMaxRC !== null &&
                          ` (RC \u2264 ${selectedStep.requiredMaxRC})`}
                      </dd>
                    </div>
                  </dl>
                </div>

                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase text-gray-500">
                    Timing
                  </h4>
                  <dl className="space-y-1 text-xs">
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Scheduled:</dt>
                      <dd className="font-mono text-gray-900">
                        {selectedStep.scheduledStart} -{" "}
                        {selectedStep.scheduledEnd}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Actual Start:</dt>
                      <dd className="font-mono text-gray-900">
                        {selectedStep.actualStart ?? "--"}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Actual End:</dt>
                      <dd className="font-mono text-gray-900">
                        {selectedStep.actualEnd ?? "--"}
                      </dd>
                    </div>
                    <div className="flex gap-2">
                      <dt className="text-gray-500">Duration:</dt>
                      <dd className="font-mono text-gray-900">
                        {computeDuration(
                          selectedStep.actualStart,
                          selectedStep.actualEnd
                        )}
                      </dd>
                    </div>
                  </dl>
                </div>

                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase text-gray-500">
                    Return Code
                  </h4>
                  {selectedStep.returnCode !== null ? (
                    <div className="flex items-center gap-2">
                      <span
                        className={`rounded-full px-2 py-0.5 font-mono text-xs font-bold ${
                          selectedStep.returnCode === 0
                            ? "bg-green-100 text-green-800"
                            : selectedStep.returnCode <= 4
                              ? "bg-yellow-100 text-yellow-800"
                              : "bg-red-100 text-red-800"
                        }`}
                      >
                        RC={selectedStep.returnCode}
                      </span>
                      <span className="text-xs text-gray-600">
                        {rcInterpretation(selectedStep.returnCode)}
                      </span>
                    </div>
                  ) : (
                    <span className="text-xs text-gray-400">--</span>
                  )}
                </div>

                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase text-gray-500">
                    Records
                  </h4>
                  {selectedStep.recordsRead !== null ? (
                    <>
                      <dl className="space-y-1 text-xs">
                        <div className="flex gap-2">
                          <dt className="text-gray-500">Read:</dt>
                          <dd className="font-mono text-gray-900">
                            {selectedStep.recordsRead?.toLocaleString()}
                          </dd>
                        </div>
                        <div className="flex gap-2">
                          <dt className="text-gray-500">Processed:</dt>
                          <dd className="font-mono text-gray-900">
                            {selectedStep.recordsProcessed?.toLocaleString()}
                          </dd>
                        </div>
                        <div className="flex gap-2">
                          <dt className="text-gray-500">Rejected:</dt>
                          <dd
                            className={`font-mono ${(selectedStep.recordsRejected ?? 0) > 0 ? "text-red-600" : "text-gray-900"}`}
                          >
                            {selectedStep.recordsRejected?.toLocaleString()}
                            {selectedStep.recordsRead &&
                              selectedStep.recordsRead > 0 &&
                              selectedStep.recordsRejected !== null && (
                                <span className="ml-1 text-gray-400">
                                  (
                                  {(
                                    (selectedStep.recordsRejected /
                                      selectedStep.recordsRead) *
                                    100
                                  ).toFixed(1)}
                                  %)
                                </span>
                              )}
                          </dd>
                        </div>
                      </dl>
                      {/* Progress bar */}
                      {selectedStep.recordsRead > 0 && (
                        <div className="mt-2 h-2 w-full rounded-full bg-gray-100">
                          <div
                            className="h-full rounded-full bg-blue-500"
                            style={{
                              width: `${((selectedStep.recordsProcessed ?? 0) / selectedStep.recordsRead) * 100}%`,
                            }}
                          />
                        </div>
                      )}
                    </>
                  ) : (
                    <span className="text-xs text-gray-400">N/A</span>
                  )}
                </div>

                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase text-gray-500">
                    Checkpoints
                  </h4>
                  {selectedStep.checkpointCount !== null &&
                  selectedStep.checkpointCount > 0 ? (
                    <dl className="space-y-1 text-xs">
                      <div className="flex gap-2">
                        <dt className="text-gray-500">Count:</dt>
                        <dd className="font-mono text-gray-900">
                          {selectedStep.checkpointCount}
                        </dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="text-gray-500">Last:</dt>
                        <dd className="font-mono text-gray-900">
                          {selectedStep.lastCheckpoint}
                        </dd>
                      </div>
                    </dl>
                  ) : (
                    <span className="text-xs text-gray-400">N/A</span>
                  )}
                </div>
              </div>

              {/* Checkpoint timeline */}
              {(() => {
                const key = `${baseRun.runId}:${selectedStep.stepId}`;
                const cps = checkpointRecords[key];
                if (cps && selectedStep.recordsRead) {
                  return (
                    <div className="mt-4 border-t border-gray-100 pt-4">
                      <h4 className="mb-2 text-xs font-semibold uppercase text-gray-500">
                        Checkpoint Timeline
                      </h4>
                      <CheckpointTimeline
                        checkpoints={cps}
                        totalRecords={selectedStep.recordsRead}
                      />
                    </div>
                  );
                }
                return null;
              })()}

              {/* Error message */}
              {selectedStep.errorMessage && (
                <div className="mt-4 rounded-md border border-red-200 bg-red-50 p-3">
                  <p className="font-mono text-xs text-red-800">
                    {selectedStep.errorMessage}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
