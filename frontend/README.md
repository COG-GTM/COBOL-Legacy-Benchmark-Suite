# Frontend - COBOL Legacy Benchmark Suite

This directory contains the web-based frontend interface for the COBOL Legacy Benchmark Suite. The frontend is built using React, TypeScript, and Vite.

## Technology Stack

- **React 18.3.1** - UI framework
- **TypeScript 5.6.2** - Type-safe JavaScript
- **Vite 6.0.1** - Fast build tool and dev server
- **Tailwind CSS 3.4.16** - Utility-first CSS framework
- **shadcn/ui** - Pre-built accessible UI components
- **Lucide React** - Icon library
- **Recharts** - Charting library

## Getting Started

### Prerequisites

- Node.js (v18 or higher recommended)
- npm (comes with Node.js)

### Installation

Install dependencies:

```bash
npm install
```

### Development

Run the development server with hot module replacement (HMR):

```bash
npm run dev
```

The application will be available at `http://localhost:5173/`

### Building for Production

Create an optimized production build:

```bash
npm run build
```

The built files will be generated in the `dist/` directory.

### Preview Production Build

Preview the production build locally:

```bash
npm run preview
```

### Linting

Run ESLint to check for code quality issues:

```bash
npm run lint
```

## Project Structure

```
frontend/
├── src/
│   ├── assets/          # Static assets (images, fonts, etc.)
│   ├── components/      # React components
│   │   └── ui/         # shadcn/ui components
│   ├── App.tsx         # Main application component
│   ├── main.tsx        # Application entry point
│   └── index.css       # Global styles
├── public/             # Public static files
├── dist/               # Production build output (generated)
├── node_modules/       # Dependencies (generated)
├── index.html          # HTML entry point
├── package.json        # Project dependencies and scripts
├── tsconfig.json       # TypeScript configuration
├── vite.config.ts      # Vite configuration
└── tailwind.config.js  # Tailwind CSS configuration
```

## Purpose

This frontend provides a web-based interface for the COBOL Legacy Benchmark Suite, which is designed to benchmark Large Language Model (LLM) translation tools for COBOL modernization efforts.

## Additional Information

For more information about the COBOL Legacy Benchmark Suite, see the main [README.md](../README.md) in the repository root.
