import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { MENU_OPTIONS } from '../routes/functionCodes';

/**
 * Main menu screen, route `/`.
 *
 * Reproduces the legacy `MENMAP` menu (`src/maps/INQSET.bms`) and the
 * dispatch logic of the `INQONLN` front controller
 * (`src/programs/online/INQONLN.cbl`): each option carries a legacy 4-char
 * function code that determines where it navigates.
 */
export function MainMenu() {
  const navigate = useNavigate();

  return (
    <Layout title="Portfolio Management System">
      <p className="menu-prompt" id="menu-prompt">
        Select Option:
      </p>
      <ul className="menu-list" aria-labelledby="menu-prompt" role="menu">
        {MENU_OPTIONS.map(({ option, label, code, route }) => (
          <li key={code} role="none">
            <button
              type="button"
              role="menuitem"
              className="menu-item"
              data-function-code={code}
              onClick={() => navigate(route)}
            >
              <span className="menu-item__number">{option}.</span>
              <span className="menu-item__label">{label}</span>
            </button>
          </li>
        ))}
      </ul>
    </Layout>
  );
}
