export type ClientType = 'I' | 'C' | 'T'
export type PortfolioStatus = 'A' | 'C' | 'S'
export type PositionStatus = 'A' | 'C' | 'P'
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE'
export type TransactionStatus = 'P' | 'D' | 'F' | 'R'
export type HistoryRecordType = 'PT' | 'PS' | 'TR'
export type HistoryActionCode = 'A' | 'C' | 'D'

export interface Portfolio {
  portfolioId: string
  accountNo: string
  clientName: string
  clientType: ClientType
  createDate: string
  lastMaint: string
  status: PortfolioStatus
  totalValue: number
  cashBalance: number
  lastUser: string
  lastTransDate: string
}

export interface Position {
  portfolioId: string
  date: string
  investmentId: string
  quantity: number
  costBasis: number
  marketValue: number
  currency: string
  status: PositionStatus
  fundName: string
}

export interface Transaction {
  date: string
  time: string
  portfolioId: string
  sequenceNo: string
  investmentId: string
  type: TransactionType
  quantity: number
  price: number
  amount: number
  currency: string
  status: TransactionStatus
}

export interface HistoryEntry {
  portfolioId: string
  date: string
  time: string
  sequenceNo: string
  recordType: HistoryRecordType
  actionCode: HistoryActionCode
  reasonCode: string
  processDate: string
  processUser: string
  beforeImage?: string
  afterImage?: string
}

export type InvestmentType = 'STK' | 'BND' | 'MMF' | 'ETF'
