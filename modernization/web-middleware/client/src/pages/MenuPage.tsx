import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchMenu, MenuOption } from '../api/client';
import ErrorPanel from '../components/ErrorPanel';

/** MENMAP */
export default function MenuPage() {
  const [options, setOptions] = useState<MenuOption[]>([]);
  const [error, setError] = useState<string>('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchMenu()
      .then((res) => setOptions(res.options))
      .catch(() => setError('Unable to load menu options'));
  }, []);

  if (error) return <ErrorPanel code="INQ001" details={error} />;

  return (
    <section className="panel">
      <h1>Portfolio Management System</h1>
      <p className="subtitle">Select Option:</p>
      <ul className="menu-list">
        {options.map((o) => (
          <li key={o.option}>
            <button type="button" onClick={() => navigate(o.route)}>
              <span className="menu-number">{o.option}.</span> {o.label}
              <span className="menu-fn">{o.function}</span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
