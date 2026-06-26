import type { FunctionCode } from '../legacy/functionCodes'
import './MenuOption.css'

interface MenuOptionProps {
  /** The numeric option key as shown on the legacy menu (1, 2, 3). */
  optionNumber: number
  /** Option label, matching the BMS `MENMAP` text in INQSET.bms. */
  label: string
  /** Legacy 4-char function code this option maps to. */
  functionCode: FunctionCode
  onSelect: (functionCode: FunctionCode) => void
}

export default function MenuOption({
  optionNumber,
  label,
  functionCode,
  onSelect,
}: MenuOptionProps) {
  return (
    <button
      type="button"
      className="menu-option"
      onClick={() => onSelect(functionCode)}
      data-function-code={functionCode}
    >
      <span className="menu-option__number">{optionNumber}.</span>
      <span className="menu-option__label">{label}</span>
      <span className="menu-option__code">{functionCode}</span>
    </button>
  )
}
