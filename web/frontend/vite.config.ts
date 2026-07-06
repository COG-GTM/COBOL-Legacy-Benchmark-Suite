import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// The dev server proxies /api to the backend so the frontend can use relative
// URLs. Override the target with the VITE_API_TARGET env var if the API runs
// elsewhere. For production, set VITE_API_BASE_URL at build time instead.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_TARGET ?? "http://localhost:4000",
        changeOrigin: true,
      },
    },
  },
});
