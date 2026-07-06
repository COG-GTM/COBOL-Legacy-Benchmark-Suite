import { Link } from 'react-router-dom';

/**
 * Main menu view — replaces the BMS MENMAP (INQONLN P200-DISPLAY-MENU).
 * The 3270 menu offered: 1) Portfolio Position Inquiry, 2) Transaction History,
 * 3) Exit. The "Exit" option is intentionally omitted in the web port.
 */
export function MenuView() {
  return (
    <section className="card menu">
      <h1 className="menu__title">Select Option</h1>
      <p className="menu__subtitle">
        Online inquiry menu, modernized from the CICS INQONLN controller.
      </p>
      <ul className="menu__list">
        <li>
          <Link to="/positions" className="menu__item">
            <span className="menu__num">1</span>
            <span>
              <strong>Portfolio Position Inquiry</strong>
              <span className="menu__desc">
                Current fund positions for an account (INQPORT / POSMAP).
              </span>
            </span>
          </Link>
        </li>
        <li>
          <Link to="/transactions" className="menu__item">
            <span className="menu__num">2</span>
            <span>
              <strong>Transaction History</strong>
              <span className="menu__desc">
                Paginated transaction history for an account (INQHIST / HISMAP).
              </span>
            </span>
          </Link>
        </li>
      </ul>
    </section>
  );
}
