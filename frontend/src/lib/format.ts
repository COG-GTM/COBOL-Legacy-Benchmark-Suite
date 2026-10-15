const currencyFormatters = new Map<string, Intl.NumberFormat>()

function currencyFormatter(currency: string): Intl.NumberFormat {
  let formatter = currencyFormatters.get(currency)
  if (!formatter) {
    formatter = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
    currencyFormatters.set(currency, formatter)
  }
  return formatter
}

/** Money fields are PIC S9(13)V99 — always two decimal places. */
export function formatCurrency(amount: number, currency = 'USD'): string {
  return currencyFormatter(currency).format(amount)
}

/** Formats an exact integer-cent amount (e.g. an aggregate beyond Number.MAX_SAFE_INTEGER cents). */
export function formatCents(cents: bigint, currency = 'USD'): string {
  const sign = cents < 0n ? '-' : ''
  const abs = cents < 0n ? -cents : cents
  const fraction = (abs % 100n).toString().padStart(2, '0')
  return currencyFormatter(currency).format(`${sign}${abs / 100n}.${fraction}` as Intl.StringNumericLiteral)
}

const quantityFormatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 4,
})

/** Quantity fields are PIC S9(11)V9(4) — always four decimal places. */
export function formatQuantity(quantity: number): string {
  return quantityFormatter.format(quantity)
}

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: '2-digit',
  timeZone: 'UTC',
})

/** Formats an ISO yyyy-mm-dd date without shifting it into the local time zone. */
export function formatDate(isoDate: string): string {
  return dateFormatter.format(new Date(`${isoDate}T00:00:00Z`))
}
