import { Link } from "react-router-dom";

// Mirrors the BMS main menu (MENMAP): "Select Option" with the two inquiries.
export default function MenuPage() {
  return (
    <section className="card">
      <h2>Select Option</h2>
      <ul className="menu-list">
        <li>
          <Link to="/position">1. Portfolio Position Inquiry</Link>
          <p>Look up the current holding for an account (mirrors INQPORT).</p>
        </li>
        <li>
          <Link to="/history">2. Transaction History</Link>
          <p>List dated transactions for an account (mirrors INQHIST).</p>
        </li>
      </ul>
      <p className="hint">
        Sample accounts: <code>1000000001</code>, <code>1000000002</code>,{" "}
        <code>1000000003</code>
      </p>
    </section>
  );
}
