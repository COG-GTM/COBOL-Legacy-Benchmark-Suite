import { history, portfolios, positions, systemJobs, transactions } from './mockData'
import type { HistoryEntry, Portfolio, Position, Transaction } from './types'

export interface ApiClient {
  listPortfolios(): Promise<Portfolio[]>
  getPortfolio(id: string): Promise<Portfolio | undefined>
  savePortfolio(portfolio: Portfolio): Promise<Portfolio>
  deletePortfolio(id: string): Promise<void>
  listPositions(portfolioId?: string): Promise<Position[]>
  listTransactions(portfolioId?: string): Promise<Transaction[]>
  createTransaction(transaction: Transaction): Promise<Transaction>
  listHistory(): Promise<HistoryEntry[]>
  getSystemJobs(): Promise<typeof systemJobs>
}

const wait = (milliseconds = 180) => new Promise((resolve) => setTimeout(resolve, milliseconds))

export class MockApiClient implements ApiClient {
  private readonly portfolios = portfolios
  private readonly positions = positions
  private readonly transactions = transactions
  private readonly history = history

  async listPortfolios() {
    await wait()
    return [...this.portfolios]
  }

  async getPortfolio(id: string) {
    await wait()
    return this.portfolios.find((portfolio) => portfolio.portfolioId === id)
  }

  async savePortfolio(portfolio: Portfolio) {
    await wait()
    const existingIndex = this.portfolios.findIndex((item) => item.portfolioId === portfolio.portfolioId)
    if (existingIndex >= 0) this.portfolios[existingIndex] = portfolio
    else this.portfolios.push(portfolio)
    return portfolio
  }

  async deletePortfolio(id: string) {
    await wait()
    const index = this.portfolios.findIndex((portfolio) => portfolio.portfolioId === id)
    if (index >= 0) this.portfolios.splice(index, 1)
  }

  async listPositions(portfolioId?: string) {
    await wait()
    return this.positions.filter((position) => !portfolioId || position.portfolioId === portfolioId)
  }

  async listTransactions(portfolioId?: string) {
    await wait()
    return this.transactions.filter((transaction) => !portfolioId || transaction.portfolioId === portfolioId)
  }

  async createTransaction(transaction: Transaction) {
    await wait()
    this.transactions.unshift(transaction)
    return transaction
  }

  async listHistory() {
    await wait()
    return [...this.history]
  }

  async getSystemJobs() {
    await wait()
    return [...systemJobs]
  }
}

export const apiClient: ApiClient = new MockApiClient()
