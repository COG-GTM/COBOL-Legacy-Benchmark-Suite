import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import App from "./App";
import HistoryPage from "./pages/HistoryPage";
import MenuPage from "./pages/MenuPage";
import PositionPage from "./pages/PositionPage";
import "./styles.css";

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <MenuPage /> },
      { path: "position", element: <PositionPage /> },
      { path: "history", element: <HistoryPage /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
