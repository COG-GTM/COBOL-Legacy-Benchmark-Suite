import { Link } from 'react-router-dom';

/** INQCOM-EXIT: session terminated (INQONLN WHEN 'EXIT'). */
export default function ExitPage() {
  return (
    <section className="panel">
      <h1>Session Ended</h1>
      <p>The inquiry session has been terminated (INQCOM-FUNCTION = EXIT).</p>
      <Link to="/">Start a new session</Link>
    </section>
  );
}
