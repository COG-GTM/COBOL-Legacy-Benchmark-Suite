import { FileQuestion, Home } from 'lucide-react'

interface NotFoundPageProps {
  onNavigateHome?: () => void
}

export function NotFoundPage({ onNavigateHome }: NotFoundPageProps) {
  return (
    <div
      className="flex min-h-screen flex-col items-center justify-center bg-neutral-white px-4 text-center"
      role="main"
    >
      <FileQuestion
        className="mb-6 text-action-blue-50"
        size={80}
        strokeWidth={1.5}
        aria-hidden="true"
      />

      <h1 className="mb-2 text-4xl font-bold text-navy-100">
        404
      </h1>

      <h2 className="mb-4 text-xl font-medium text-neutral-100">
        Page Not Found
      </h2>

      <p className="mb-8 max-w-md text-sm leading-relaxed text-neutral-80">
        The page you are looking for does not exist or has been moved.
        Please check the URL or return to the home page.
      </p>

      {onNavigateHome && (
        <button
          type="button"
          onClick={onNavigateHome}
          className="inline-flex items-center gap-2 rounded-pill bg-action-blue-100 px-6 py-3 text-sm font-bold text-neutral-white transition-colors hover:bg-action-blue-140"
        >
          <Home size={16} />
          Return Home
        </button>
      )}
    </div>
  )
}
