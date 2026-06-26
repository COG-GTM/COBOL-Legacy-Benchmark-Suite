/**
 * Renders the legacy PF-key navigation hints as clickable buttons.
 * Mirrors the BMS footer: "PF3=Exit  PF7=Previous  PF8=Next".
 */
export interface FunctionKey {
  /** Key label, e.g. "PF3". */
  pf: string;
  /** Action label, e.g. "Exit". */
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

export function FunctionKeyBar({ keys }: { keys: FunctionKey[] }) {
  return (
    <div className="fn-bar">
      {keys.map((k) => (
        <button
          key={k.pf}
          type="button"
          className="fn-key"
          onClick={k.onClick}
          disabled={k.disabled}
        >
          <span className="kbd">{k.pf}</span>
          <span>{k.label}</span>
        </button>
      ))}
    </div>
  );
}
