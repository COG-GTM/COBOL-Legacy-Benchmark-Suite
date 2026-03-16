import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        sidebar: {
          DEFAULT: "#1e293b",
          foreground: "#e2e8f0",
          accent: "#334155",
          hover: "#475569",
        },
        primary: {
          DEFAULT: "#2563eb",
          foreground: "#ffffff",
          hover: "#1d4ed8",
        },
        success: {
          DEFAULT: "#16a34a",
          light: "#dcfce7",
        },
        warning: {
          DEFAULT: "#d97706",
          light: "#fef3c7",
        },
        danger: {
          DEFAULT: "#dc2626",
          light: "#fee2e2",
        },
        info: {
          DEFAULT: "#2563eb",
          light: "#dbeafe",
        },
      },
    },
  },
  plugins: [],
};

export default config;
