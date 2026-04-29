# Testing Frontend Portfolio Management

## Dev Server

```bash
cd frontend && npm run dev
```

The dev server runs on `http://localhost:5173/` by default (port may vary — check terminal output for the actual port, e.g., 5174, 5175).

## Authentication

The app uses a mock auth system. Navigate to `/login` and enter any non-empty userId and password (e.g., userId: "admin", password: "test"). This redirects to the dashboard.

## Portfolio CRUD Testing Flow

### List Page (`/portfolios`)
- Verify table shows all portfolios (default: 12 rows PORT0001-PORT0012)
- Summary cards show Total Portfolios count and Total Value (currency formatted)
- Status badges: Active=green, Inactive=yellow (PORT0007), Closed=red (PORT0010)
- Search box filters by name or ID (case-insensitive)
- Status dropdown filters by Active/Inactive/Closed
- Column headers are clickable for sorting

### Create (`/portfolios/new`)
- Portfolio ID format: "PORT" + exactly 4 digits (e.g., PORT9999)
- Name is required (cannot be empty)
- Status dropdown: Active, Inactive, Closed
- Validation errors appear inline below each field
- On success: redirects to list, shows success toast, new row appears in table

### Detail (`/portfolios/:id`)
- Shows all portfolio fields + positions sub-table + transactions sub-table
- Positions/transactions are linked via account number mapping (hardcoded in PortfolioContext)
- Newly created portfolios will show empty sub-tables (no account mapping exists for them)
- Action buttons: Edit, Delete, Back to List

### Edit (`/portfolios/:id/edit`)
- Portfolio ID field is read-only/disabled
- Form pre-populated with existing data
- Same validation as Create (except no duplicate ID check)
- On success: redirects to detail page, shows success toast

### Delete
- Delete button on list page rows and detail page
- Shows confirmation dialog with portfolio name
- On confirm: removes from list, shows success toast, count updates

## Toast Notifications
- Success toasts appear on create/update/delete
- Auto-dismiss after 4 seconds
- Uses useRef to track timer and prevent race conditions

## Mock Data Notes
- All CRUD operations are in-memory via React Context (PortfolioContext)
- Changes do NOT persist across page refreshes
- 12 default portfolios (PORT0001-PORT0012)
- Portfolio-to-position/transaction linking uses hardcoded `portfolioAccountMap` in PortfolioContext

## Common Issues
- If the dev server port is different from expected, check the terminal output after `npm run dev`
- Toast animation classes (`animate-in`, `fade-in`, `slide-in-from-top-2`) may require `tailwindcss-animate` plugin — if toasts don't animate, this might be why
- Portfolio ID validation is case-sensitive (requires uppercase "PORT"), but the form calls `.toUpperCase()` on submit

## Devin Secrets Needed
None — the app uses mock auth and mock data, no external services required.
