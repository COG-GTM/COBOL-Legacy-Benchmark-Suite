import { useNavigate } from 'react-router-dom'
import { useSession } from '../context/sessionContext'
import { FunctionCode } from '../legacy/functionCodes'
import './BackToMenu.css'

/**
 * Returns to the main menu, mirroring the PF3=Exit control shown on the
 * legacy POSMAP/HISMAP screens in `src/maps/INQSET.bms`.
 */
export default function BackToMenu() {
  const navigate = useNavigate()
  const { setFunction, clearError } = useSession()

  return (
    <button
      type="button"
      className="back-to-menu"
      onClick={() => {
        clearError()
        setFunction(FunctionCode.MENU)
        navigate('/menu')
      }}
    >
      &larr; Back to Menu <span className="back-to-menu__pf">PF3</span>
    </button>
  )
}
