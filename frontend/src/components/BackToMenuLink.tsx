import { Link } from 'react-router-dom';
import { Routes } from '../routes/functionCodes';

/**
 * Standard "Back to Menu" navigation, mirroring the legacy PF3=Exit action
 * that returns the terminal to the main `MENMAP` screen.
 */
export function BackToMenuLink() {
  return (
    <Link className="back-link" to={Routes.MENU}>
      &larr; Back to Menu
    </Link>
  );
}
