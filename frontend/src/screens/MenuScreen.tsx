import { useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { messages } from '../i18n/messages';
import { MessageRegion, type Message } from '../components/MessageRegion';
import { PageHeading } from '../components/PageHeading';
import { useSession } from '../state/SessionContext';
import { useDocumentTitle } from '../state/useDocumentTitle';

/** S1 Main Menu (MENMAP). Option 1 -> INQP, 2 -> INQH, 3 -> EXIT (GAP-04 assumed mapping). */
export function MenuScreen() {
  useDocumentTitle(messages.app.name);
  const navigate = useNavigate();
  const { flash, setFlash, endSession } = useSession();
  const [option, setOption] = useState('');
  const [invalid, setInvalid] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const message: Message | null = invalid
    ? { text: messages.menu.invalidOption, severity: 'WARNING' }
    : flash
      ? { text: flash.text, severity: flash.severity, traceId: flash.traceId }
      : null;

  function exit() {
    endSession();
    navigate('/signed-out', { replace: true });
  }

  function go(path: string) {
    setFlash(null);
    navigate(path);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const value = option.trim();
    if (value === '1') return go('/positions');
    if (value === '2') return go('/history');
    if (value === '3') return exit();
    setFlash(null);
    setInvalid(true);
    inputRef.current?.focus();
    inputRef.current?.select();
  }

  return (
    <section className="card menu" aria-labelledby="menu-title">
      <div id="menu-title">
        <PageHeading>{messages.menu.title}</PageHeading>
      </div>
      <ul className="menu__options" aria-label={messages.menu.optionsLabel}>
        <li>
          <Link to="/positions" className="menu__option" onClick={() => setFlash(null)}>
            <span>{messages.menu.option1}</span>
          </Link>
        </li>
        <li>
          <Link to="/history" className="menu__option" onClick={() => setFlash(null)}>
            <span>{messages.menu.option2}</span>
          </Link>
        </li>
        <li>
          <button type="button" className="menu__option" onClick={exit}>
            <span>{messages.menu.option3}</span>
          </button>
        </li>
      </ul>
      <form className="menu__form" onSubmit={submit} noValidate>
        <label htmlFor="menu-option" className="search-form__label">
          {messages.menu.prompt}
        </label>
        <div className="search-form__row">
          <input
            ref={inputRef}
            id="menu-option"
            name="option"
            className="input input--option"
            inputMode="numeric"
            autoComplete="off"
            maxLength={1}
            autoFocus
            value={option}
            onChange={(e) => {
              setOption(e.target.value);
              setInvalid(false);
            }}
            aria-invalid={invalid || undefined}
            aria-describedby="menu-option-hint menu-message"
          />
          <button type="submit" className="btn btn--primary">
            {messages.menu.go}
          </button>
        </div>
        <p id="menu-option-hint" className="hint">
          {messages.menu.optionHint}
        </p>
      </form>
      <MessageRegion id="menu-message" message={message} />
    </section>
  );
}
