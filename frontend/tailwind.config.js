/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#3b82f6', dark: '#2563eb' },
        success: '#22c55e',
        warning: '#f59e0b',
        danger: '#ef4444',
        surface: { DEFAULT: '#1e293b', light: '#334155', dark: '#0f172a' },
      },
    },
  },
  plugins: [],
};
