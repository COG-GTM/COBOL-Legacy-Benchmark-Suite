export interface Commarea {
  inqcomFunction: 'MENU' | 'INQP' | 'INQH' | 'EXIT';
  inqcomAccountNo: string;
  inqcomResponseCode: number;
  inqcomErrorMsg: string;
}

export interface MenuOption {
  option: string;
  label: string;
  function: Commarea['inqcomFunction'];
  route: string;
}

export interface PositionRecord {
  posPortfolioId: string;
  posDate: string;
  posInvestmentId: string;
  posFundName: string;
  posQuantity: number;
  posCostBasis: number;
  posMarketValue: number;
  posCurrency: string;
  posStatus: 'A' | 'C' | 'P';
  posLastMaintDate: string;
  posLastMaintUser: string;
}

export interface HistoryRecord {
  histPortfolioId: string;
  histDate: string;
  histTime: string;
  histSeqNo: string;
  histRecordType: 'PT' | 'PS' | 'TR';
  histActionCode: 'A' | 'C' | 'D';
  histInvestmentId: string;
  histUnits: number;
  histPrice: number;
  histAmount: number;
  histReasonCode: string;
  histProcessDate: string;
  histProcessUser: string;
}

export interface MenuResponse {
  commarea: Commarea;
  options: MenuOption[];
}
export interface PositionResponse {
  commarea: Commarea;
  position: PositionRecord | null;
}
export interface HistoryResponse {
  commarea: Commarea;
  rows: HistoryRecord[];
  page: number;
  pageSize: number;
  totalRows: number;
  totalPages: number;
}

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url);
  return (await res.json()) as T;
}

export const fetchMenu = () => get<MenuResponse>('/api/menu');
export const fetchPosition = (account: string) =>
  get<PositionResponse>(`/api/position?account=${encodeURIComponent(account)}`);
export const fetchHistory = (account: string, page: number) =>
  get<HistoryResponse>(`/api/history?account=${encodeURIComponent(account)}&page=${page}`);
