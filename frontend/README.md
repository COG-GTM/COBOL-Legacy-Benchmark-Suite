# Portfolio Management System — Modernized Web Frontend

A modern React + TypeScript web UI that replaces the legacy COBOL/CICS 3270 terminal interface of the Portfolio Management System.

## Overview

This frontend application modernizes the original mainframe-based Portfolio Management System. The original system used CICS BMS maps (defined in `src/maps/INQSET.bms`) to render 24x80 character terminal screens. This project provides a rich, responsive web dashboard as a modern replacement.

### Current Status

**Phase 1: Dashboard & Navigation (Mock Data)**

- Dashboard page with summary metrics, quick actions, and recent activity feed
- Application shell with sidebar navigation and header
- Placeholder pages for all future screens
- All data is currently mock/static — no backend integration yet

### COBOL Screen to Web Page Mapping

| Original CICS Screen | BMS Map   | New Web Route            | Status      |
| --------------------- | --------- | ------------------------ | ----------- |
| Main Menu             | `MENMAP`  | `/` (Dashboard)          | Implemented |
| Portfolio Inquiry     | `POSMAP`  | `/portfolio-inquiry`     | Placeholder |
| Transaction History   | `HISMAP`  | `/transaction-history`   | Placeholder |
| Error Display         | `ERRMAP`  | ErrorBanner component    | Implemented |
| Reports               | —         | `/reports`               | Placeholder |
| Batch Jobs            | —         | `/batch-jobs`            | Placeholder |
| System Monitor        | —         | `/system-monitor`        | Placeholder |

## Getting Started

### Prerequisites

- Node.js 20+ and npm

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The app will be available at `http://localhost:5173`.

### Build

```bash
npm run build
```

### Lint

```bash
npm run lint
```

## Tech Stack

- **React 19** with TypeScript
- **Vite 8** for build tooling
- **Tailwind CSS 3** for styling
- **React Router 7** for client-side routing
- **Lucide React** for icons

## Project Structure

```
frontend/src/
├── components/
│   ├── layout/          # AppShell, Sidebar, Header, Footer
│   ├── dashboard/       # DashboardPage, QuickActions, SummaryCards, RecentActivity
│   └── common/          # ErrorBanner, LoadingSpinner
├── pages/               # Route-level page components
├── router/              # React Router configuration
├── mock/                # Mock/static data for dashboard widgets
├── types/               # TypeScript type definitions
├── App.tsx              # Root app component
├── main.tsx             # Entry point
└── index.css            # Global styles with Tailwind directives
```
