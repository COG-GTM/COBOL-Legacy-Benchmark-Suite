import type { AppError } from '@/types/errors'
import { createAppError, mapHttpStatusToSeverity } from '@/types/errors'
import { addToast } from '@/hooks/useToast'

export interface ApiErrorInterceptorConfig {
  onUnauthorized?: () => void
  onNotFound?: () => void
  onError?: (error: AppError) => void
}

let config: ApiErrorInterceptorConfig = {}

export function configureApiErrorInterceptor(
  newConfig: ApiErrorInterceptorConfig,
): void {
  config = { ...config, ...newConfig }
}

export function handleApiError(
  status: number,
  body?: string | Record<string, unknown>,
): AppError {
  const severity = mapHttpStatusToSeverity(status)
  const message = typeof body === 'string' ? body : extractMessage(body, status)

  const error = createAppError(
    `HTTP_${status}`,
    message,
    severity,
    {
      category: status >= 500 ? 'system' : 'validation',
      type: status === 401 || status === 403 ? 'security' : 'processing',
      details: typeof body === 'object' ? JSON.stringify(body, null, 2) : undefined,
    },
  )

  switch (status) {
    case 401:
      config.onUnauthorized?.()
      break
    case 404:
      config.onNotFound?.()
      break
    default:
      if (status >= 400) {
        addToast(error)
      }
      break
  }

  config.onError?.(error)
  return error
}

function extractMessage(
  body: Record<string, unknown> | undefined,
  status: number,
): string {
  if (!body) return getDefaultMessage(status)

  if (typeof body.message === 'string') return body.message
  if (typeof body.error === 'string') return body.error
  if (typeof body.detail === 'string') return body.detail

  return getDefaultMessage(status)
}

function getDefaultMessage(status: number): string {
  const messages: Record<number, string> = {
    400: 'Invalid request. Please check your input and try again.',
    401: 'Your session has expired. Please log in again.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    408: 'The request timed out. Please try again.',
    409: 'A conflict occurred. The resource may have been modified.',
    422: 'The submitted data could not be processed.',
    429: 'Too many requests. Please wait and try again.',
    500: 'An internal server error occurred. Please try again later.',
    502: 'The server is temporarily unavailable. Please try again later.',
    503: 'The service is currently unavailable. Please try again later.',
    504: 'The server took too long to respond. Please try again later.',
  }

  return messages[status] ?? `An unexpected error occurred (${status}).`
}

export async function fetchWithErrorHandling(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const response = await fetch(input, init)

  if (!response.ok) {
    let body: string | Record<string, unknown> | undefined
    try {
      const contentType = response.headers.get('content-type')
      if (contentType?.includes('application/json')) {
        body = (await response.json()) as Record<string, unknown>
      } else {
        const text = await response.text()
        body = text || undefined
      }
    } catch {
      // body parsing failed, use default message
    }

    handleApiError(response.status, body)
  }

  return response
}
