import { useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import MenuOption from '../components/MenuOption'
import { useSession } from '../context/sessionContext'
import { FunctionCode, routeForFunction } from '../legacy/functionCodes'
import './MainMenu.css'

/**
 * Main menu screen. The three options replicate the BMS `MENMAP` definition in
 * `src/maps/INQSET.bms` exactly, and selection routing mirrors the
 * `EVALUATE WS-COMMAREA-FUNCTION` dispatch in `INQONLN.cbl`.
 */
const MENU_OPTIONS = [
  {
    optionNumber: 1,
    label: 'Portfolio Position Inquiry',
    functionCode: FunctionCode.PORTFOLIO,
  },
  {
    optionNumber: 2,
    label: 'Transaction History',
    functionCode: FunctionCode.HISTORY,
  },
  {
    optionNumber: 3,
    label: 'Exit',
    functionCode: FunctionCode.EXIT,
  },
] as const

export default function MainMenu() {
  const navigate = useNavigate()
  const { setFunction, raiseError, clearError } = useSession()

  const handleSelect = useCallback(
    (functionCode: string) => {
      const route = routeForFunction(functionCode)
      if (route === null) {
        // Mirrors the WHEN OTHER branch in INQONLN.cbl.
        raiseError(`Invalid selection: ${functionCode}`)
        navigate('/error')
        return
      }
      clearError()
      setFunction(functionCode as FunctionCode)
      navigate(route)
    },
    [navigate, setFunction, raiseError, clearError],
  )

  // Numeric key selection, echoing the UNPROT NUM `OPTION` field in MENMAP.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const option = MENU_OPTIONS.find(
        (item) => String(item.optionNumber) === event.key,
      )
      if (option) {
        handleSelect(option.functionCode)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [handleSelect])

  return (
    <section className="main-menu" aria-labelledby="main-menu-heading">
      <h2 id="main-menu-heading" className="main-menu__heading">
        Select Option:
      </h2>

      <div className="main-menu__options">
        {MENU_OPTIONS.map((option) => (
          <MenuOption
            key={option.optionNumber}
            optionNumber={option.optionNumber}
            label={option.label}
            functionCode={option.functionCode}
            onSelect={handleSelect}
          />
        ))}
      </div>

      <p className="main-menu__hint">
        Click an option or press <kbd>1</kbd>, <kbd>2</kbd>, or <kbd>3</kbd>.
      </p>
    </section>
  )
}
