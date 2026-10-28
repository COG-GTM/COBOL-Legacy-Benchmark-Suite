export function portfolioDetailPath(portfolioId: string): string {
  return `/portfolios/${encodeURIComponent(portfolioId)}`
}

export function portfolioPositionsPath(portfolioId: string): string {
  return `/positions?portfolioId=${encodeURIComponent(portfolioId)}`
}

export function portfolioTransactionsPath(portfolioId: string): string {
  return `/transactions?portfolioId=${encodeURIComponent(portfolioId)}`
}
