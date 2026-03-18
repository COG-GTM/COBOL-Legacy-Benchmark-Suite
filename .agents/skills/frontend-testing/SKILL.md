# Frontend Testing

## Dev Server

```bash
cd frontend && npx vite --host 0.0.0.0 --port 5173
```

If port 5173 is busy, Vite auto-selects the next available port (e.g. 5174). Check the terminal output for the actual URL.

## Key Routes

- `/` — Dashboard (main menu)
- `/portfolio-inquiry` — Portfolio Inquiry
- `/transaction-history` — Transaction History
- `/system-monitor` — System Monitoring Dashboard
- `/reports` — Reports (Coming Soon placeholder)
- `/batch-jobs` — Batch Jobs (Coming Soon placeholder)

## Testing Approach

1. Start dev server in a background shell
2. Use `set_mobile` browser action to test responsive layouts (~375px mobile viewport)
3. Use `set_mobile enabled=false` to return to desktop viewport
4. Use screen recording (`recording_start` / `recording_stop`) with `annotate_recording` for UI test evidence

## Lint & Build

```bash
cd frontend && npm run lint
cd frontend && npm run build
```

## Notes

- Frontend-only project with mock/static data (no backend)
- Uses React 19 + TypeScript + Vite + Tailwind CSS
- SVG-based visualizations (no external charting libraries)
- Auto-refresh simulation uses `setInterval` (30s) with random jitter on metric values
