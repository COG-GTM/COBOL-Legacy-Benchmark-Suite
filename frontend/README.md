# Portfolio Management System - React Frontend

A modern React frontend for the Investment Portfolio Management System, modernized from the COBOL Legacy Benchmark Suite.

## Overview

This application provides a web-based user interface that replicates the functionality of the original COBOL/CICS-based Portfolio Management System. It serves as the frontend component of the modernization effort, providing read-only inquiry functionality for portfolio positions and transaction history.

## Features

- **Main Menu Navigation**: Navigate between different inquiry screens
- **Portfolio Position Inquiry**: View portfolio positions by account
- **Transaction History Inquiry**: View transaction history by account
- **Exit/Logout**: End the session

## Technology Stack

- **React 19** - UI library
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool and development server
- **React Router** - Client-side routing
- **Material-UI (MUI)** - Component library and styling
- **Vitest** - Testing framework
- **ESLint** - Code linting
- **Prettier** - Code formatting

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── common/
│   │   │   ├── Layout.tsx          # Main layout wrapper
│   │   │   ├── Navigation.tsx      # Navigation tabs
│   │   │   └── ErrorBoundary.tsx   # Error handling wrapper
│   │   └── screens/
│   │       ├── MainMenu.tsx        # Main menu screen
│   │       ├── PortfolioInquiry.tsx # Portfolio inquiry screen
│   │       ├── TransactionHistory.tsx # Transaction history screen
│   │       └── ErrorDisplay.tsx    # Error display screen
│   ├── services/
│   │   ├── NavigationService.ts    # Navigation state management
│   │   └── MockDataService.ts      # Mock data for development
│   ├── types/
│   │   └── index.ts                # TypeScript type definitions
│   ├── test/
│   │   ├── setup.ts                # Test setup configuration
│   │   └── App.test.tsx            # App component tests
│   ├── App.tsx                     # Main application component
│   ├── main.tsx                    # Application entry point
│   └── index.css                   # Global styles
├── index.html                      # HTML template
├── vite.config.ts                  # Vite configuration
├── tsconfig.json                   # TypeScript configuration
├── eslint.config.js                # ESLint configuration
├── .prettierrc                     # Prettier configuration
└── package.json                    # Dependencies and scripts
```

## Prerequisites

- Node.js 18.x or higher
- npm 9.x or higher

## Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Running the Application

### Development Mode

Start the development server with hot module replacement:

```bash
npm run dev
```

The application will be available at `http://localhost:3000`.

### Production Build

Build the application for production:

```bash
npm run build
```

Preview the production build:

```bash
npm run preview
```

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |
| `npm run lint:fix` | Run ESLint with auto-fix |
| `npm run format` | Format code with Prettier |
| `npm run format:check` | Check code formatting |
| `npm run test` | Run tests |
| `npm run test:coverage` | Run tests with coverage |

## Routes

| Route | Description |
|-------|-------------|
| `/` | Redirects to `/menu` |
| `/menu` | Main menu screen |
| `/portfolio` | Portfolio position inquiry |
| `/history` | Transaction history inquiry |
| `/exit` | Exit/logout screen |

## Mapping to Original COBOL System

This frontend mirrors the BMS (Basic Mapping Support) screens from the original COBOL system:

| Original BMS Map | React Component |
|------------------|-----------------|
| MENMAP | MainMenu.tsx |
| POSMAP | PortfolioInquiry.tsx |
| HISMAP | TransactionHistory.tsx |
| ERRMAP | ErrorDisplay.tsx |

## Development Notes

- This is a frontend-only implementation; backend integration will be added in later phases
- The system is read-only (inquiry/display functionality only)
- Mock data is available in `MockDataService.ts` for development purposes
- The original COBOL pseudo-conversational model has been adapted to React's stateless patterns

## Future Phases

- Phase 2: Implement actual data fetching from backend APIs
- Phase 3: Add authentication and session management
- Phase 4: Implement additional inquiry features
- Phase 5: Performance optimization and production hardening

## Contributing

Please refer to the main repository's [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

## License

This project is part of the COBOL Legacy Benchmark Suite. See the main repository for license information.
