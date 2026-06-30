interface PlaceholderPageProps {
  title: string;
  description: string;
  /** The Jira ticket / user story that will deliver this section. */
  plannedIn?: string;
}

/**
 * Lightweight stub used for sections whose full implementation is delivered by
 * sibling tickets in the epic. It keeps the navigation shell coherent (every
 * primary nav target resolves to a real page) without pre-empting that work.
 */
export function PlaceholderPage({
  title,
  description,
  plannedIn,
}: PlaceholderPageProps) {
  return (
    <section className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{description}</p>
      </header>
      <div className="placeholder">
        <p>This section is part of the modernization roadmap.</p>
        {plannedIn && (
          <p className="placeholder__meta">Planned in {plannedIn}.</p>
        )}
      </div>
    </section>
  );
}
