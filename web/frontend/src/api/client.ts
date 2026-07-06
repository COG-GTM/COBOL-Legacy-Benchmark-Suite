import axios from "axios";
import { HistoryResponse, PositionResponse } from "./types";

// In dev, Vite proxies "/api" to the backend (see vite.config.ts). In prod,
// set VITE_API_BASE_URL to point at the deployed API.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? "";

const http = axios.create({ baseURL });

/** Extracts a user-facing message from an axios error, mapping the API's
 * error envelope / 404 "not found" paths (mirrors the COBOL not-found flow). */
export function toErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
    if (err.response?.status === 404) return fallback;
    if (err.message) return err.message;
  }
  return fallback;
}

export async function getPosition(
  accountNo: string
): Promise<PositionResponse> {
  const { data } = await http.get<PositionResponse>(
    `/api/portfolios/${encodeURIComponent(accountNo)}/position`
  );
  return data;
}

export async function getHistory(
  accountNo: string
): Promise<HistoryResponse> {
  const { data } = await http.get<HistoryResponse>(
    `/api/portfolios/${encodeURIComponent(accountNo)}/history`
  );
  return data;
}
